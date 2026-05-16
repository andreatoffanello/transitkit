import UIKit
import SwiftUI

// MARK: - AppDelegate

/// Hosts the bits of UIKit's app lifecycle that SwiftUI doesn't expose —
/// specifically, APNs token registration callbacks for Firebase Messaging.
///
/// Wired into `TransitKitApp` with `@UIApplicationDelegateAdaptor`.
final class AppDelegate: NSObject, UIApplicationDelegate {
    /// Set during `TransitKitApp.bootstrap()` once the operator config has
    /// been loaded — we need the operator id to build the right topic names.
    nonisolated(unsafe) static var pushManager: PushNotificationManager?

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Self.pushManager?.didRegisterAPNs(deviceToken: deviceToken)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        Self.pushManager?.didFailToRegisterAPNs(error: error)
    }
}
