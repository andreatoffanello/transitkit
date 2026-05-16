import Foundation
import SwiftUI
import UserNotifications
import UIKit
@preconcurrency import FirebaseCore
@preconcurrency import FirebaseMessaging

// MARK: - PushNotificationManager

/// Owns the push lifecycle for the app:
///   - Configures Firebase if a `GoogleService-Info.plist` is in the bundle
///   - Requests notification permission on demand
///   - Subscribes/unsubscribes to FCM topics matching the operator + favorites
///   - Exposes the FCM registration token for the "register as test device" flow
///
/// Topics follow the convention pinned by transitkit-console:
///   `{operatorId}_all`            → broadcast to every user of the operator
///   `{operatorId}_line_{routeId}` → users who favorited that line
///
/// All subscriptions live inside FCM — the server never learns who likes what.
@MainActor
@Observable
final class PushNotificationManager: NSObject {
    // MARK: State exposed to UI

    enum AuthorizationStatus: Equatable {
        case unknown
        case notDetermined
        case denied
        case authorized
    }

    private(set) var firebaseConfigured = false
    private(set) var authorizationStatus: AuthorizationStatus = .unknown
    private(set) var fcmToken: String?
    private(set) var lastError: String?

    /// User has granted permission but we're still waiting for FCM to mint
    /// a registration token. Once `didReceiveRegistrationToken` fires we
    /// flush the pending subscribes.
    private var subscribePendingFlush = false

    // MARK: Internals

    private let operatorId: String

    /// Set of route IDs we have subscribed to. Kept in memory + UserDefaults
    /// so we can unsubscribe from the previous set on operator switch / reset.
    private var subscribedRoutes: Set<String> = []
    private let subscribedKey: String

    /// `appalcart_all` etc. — written once we successfully request permission.
    private var allTopic: String { "\(operatorId)_all" }
    /// `appalcart_preview` — Debug builds only subscribe to this, so the CMS
    /// can send previews to dev devices without touching the broadcast topic.
    private var previewTopic: String { "\(operatorId)_preview" }
    private func lineTopic(_ routeId: String) -> String {
        "\(operatorId)_line_\(Self.topicSafe(routeId))"
    }

    // MARK: Init

    init(operatorId: String) {
        self.operatorId = Self.topicSafe(operatorId)
        self.subscribedKey = "fcm_subscribed_routes_\(operatorId)"
        super.init()
        if let raw = UserDefaults.standard.stringArray(forKey: subscribedKey) {
            subscribedRoutes = Set(raw)
        }
        configureFirebaseIfPossible()
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        Task {
            await refreshAuthorizationStatus()
            // If push was authorized in a previous session, re-register for
            // remote notifications. APNs returns the token via AppDelegate,
            // FCM then fires `didReceiveRegistrationToken` and we re-flush
            // the topic subscribes (operator-all, preview in Debug, known
            // routes). Without this, a fresh launch never re-subscribes.
            if firebaseConfigured, authorizationStatus == .authorized {
                subscribePendingFlush = true
                await registerForRemoteNotifications()
            }
        }
    }

    // MARK: Firebase configuration

    /// Configures `FirebaseApp.configure()` only if `GoogleService-Info.plist`
    /// is present in the bundle. Allows the app to build & run for operators
    /// who haven't been onboarded to Firebase yet.
    private func configureFirebaseIfPossible() {
        guard FirebaseApp.app() == nil else {
            firebaseConfigured = true
            return
        }
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") else {
            lastError = "GoogleService-Info.plist not found in bundle — push notifications disabled."
            firebaseConfigured = false
            return
        }
        guard let options = FirebaseOptions(contentsOfFile: path) else {
            lastError = "Failed to parse GoogleService-Info.plist"
            firebaseConfigured = false
            return
        }
        FirebaseApp.configure(options: options)
        firebaseConfigured = true
    }

    // MARK: Permission flow

    /// Request push permission. On success, registers for APNs (which leads
    /// to the FCM token via the FirebaseMessaging delegate) and subscribes
    /// to the operator-wide topic.
    @discardableResult
    func requestAuthorization() async -> Bool {
        guard firebaseConfigured else {
            lastError = "Firebase is not configured — cannot enable push."
            return false
        }
        do {
            let granted = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .badge, .sound])
            await refreshAuthorizationStatus()
            if granted {
                // Defer the topic subscriptions: they would fail with
                // "No APNS token specified before fetching FCM Token"
                // because the APNs token round-trip is asynchronous and
                // arrives via AppDelegate after registerForRemoteNotifications
                // returns. We flush the pending subscribes inside
                // messaging(didReceiveRegistrationToken:) instead.
                subscribePendingFlush = true
                await registerForRemoteNotifications()
            }
            return granted
        } catch {
            lastError = error.localizedDescription
            return false
        }
    }

    func refreshAuthorizationStatus() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        authorizationStatus = Self.map(settings.authorizationStatus)
    }

    private func registerForRemoteNotifications() async {
        await MainActor.run {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }

    /// Called by AppDelegate when iOS hands us the APNs device token —
    /// forwarded to FCM so it can correlate APNs ↔ FCM token.
    nonisolated func didRegisterAPNs(deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    nonisolated func didFailToRegisterAPNs(error: Error) {
        Task { @MainActor in
            self.lastError = "APNs registration failed: \(error.localizedDescription)"
        }
    }

    // MARK: Topic subscriptions

    private func subscribeToOperatorAll() async {
        guard firebaseConfigured else { return }
        do {
            try await Messaging.messaging().subscribe(toTopic: allTopic)
        } catch {
            lastError = "Subscribe \(allTopic) failed: \(error.localizedDescription)"
        }
    }

    private func unsubscribeFromOperatorAll() async {
        guard firebaseConfigured else { return }
        try? await Messaging.messaging().unsubscribe(fromTopic: allTopic)
    }

    /// Debug builds subscribe to a private `_preview` topic so the CMS can
    /// send previews to dev devices without touching the broadcast topic.
    /// Release builds never subscribe → real users never receive previews.
    private func subscribePreviewIfDebug() async {
        #if DEBUG
        guard firebaseConfigured else { return }
        do {
            try await Messaging.messaging().subscribe(toTopic: previewTopic)
        } catch {
            lastError = "Subscribe \(previewTopic) failed: \(error.localizedDescription)"
        }
        #endif
    }

    private func unsubscribeFromPreview() async {
        guard firebaseConfigured else { return }
        try? await Messaging.messaging().unsubscribe(fromTopic: previewTopic)
    }

    /// Subscribe to a route-specific topic. Idempotent.
    func subscribeRoute(_ routeId: String) async {
        guard firebaseConfigured, authorizationStatus == .authorized else { return }
        let topic = lineTopic(routeId)
        do {
            try await Messaging.messaging().subscribe(toTopic: topic)
            subscribedRoutes.insert(routeId)
            persistSubscribedRoutes()
        } catch {
            lastError = "Subscribe \(topic) failed: \(error.localizedDescription)"
        }
    }

    /// Unsubscribe from a route-specific topic. Idempotent.
    func unsubscribeRoute(_ routeId: String) async {
        guard firebaseConfigured else { return }
        let topic = lineTopic(routeId)
        try? await Messaging.messaging().unsubscribe(fromTopic: topic)
        subscribedRoutes.remove(routeId)
        persistSubscribedRoutes()
    }

    private func resubscribeAllKnownRoutes() async {
        guard firebaseConfigured, authorizationStatus == .authorized else { return }
        for routeId in subscribedRoutes {
            try? await Messaging.messaging().subscribe(toTopic: lineTopic(routeId))
        }
    }

    /// Hook called by SettingsTab toggle when the user disables notifications.
    func disableAndUnsubscribe() async {
        guard firebaseConfigured else { return }
        await unsubscribeFromOperatorAll()
        await unsubscribeFromPreview()
        for routeId in subscribedRoutes {
            try? await Messaging.messaging().unsubscribe(fromTopic: lineTopic(routeId))
        }
        // Don't clear `subscribedRoutes` — if the user re-enables later, we
        // want to restore their previous opt-ins.
        try? await Messaging.messaging().deleteToken()
        fcmToken = nil
    }

    // MARK: Helpers

    private func persistSubscribedRoutes() {
        UserDefaults.standard.set(Array(subscribedRoutes), forKey: subscribedKey)
    }

    /// FCM topic name allowed chars: [a-zA-Z0-9-_.~%]. Normalize anything
    /// else to `_` so route IDs containing spaces/slashes don't poison the
    /// topic name (and so the CMS builds identical names for delivery).
    static func topicSafe(_ s: String) -> String {
        var out = ""
        for ch in s {
            if ch.isLetter || ch.isNumber || "-_.~%".contains(ch) {
                out.append(ch)
            } else {
                out.append("_")
            }
        }
        return out
    }

    private static func map(_ status: UNAuthorizationStatus) -> AuthorizationStatus {
        switch status {
        case .authorized, .provisional, .ephemeral: .authorized
        case .denied:                                .denied
        case .notDetermined:                         .notDetermined
        @unknown default:                            .unknown
        }
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension PushNotificationManager: @preconcurrency UNUserNotificationCenterDelegate {
    /// Show the notification banner even when the app is in the foreground.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .badge, .list]
    }

    /// Forward `data.deep_link` to the existing DeepLinkRouter when the
    /// user taps the notification.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let info = response.notification.request.content.userInfo
        if let link = info["deep_link"] as? String,
           let url  = URL(string: link),
           url.scheme == "transitkit" {
            await UIApplication.shared.open(url)
        }
    }
}

// MARK: - MessagingDelegate

extension PushNotificationManager: @preconcurrency MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        Task { @MainActor in
            self.fcmToken = fcmToken
            // The APNs ↔ FCM token round-trip is complete. Flush any
            // subscribes that were queued in requestAuthorization().
            if self.subscribePendingFlush, fcmToken != nil {
                self.subscribePendingFlush = false
                await self.subscribeToOperatorAll()
                await self.subscribePreviewIfDebug()
                await self.resubscribeAllKnownRoutes()
            }
        }
    }
}
