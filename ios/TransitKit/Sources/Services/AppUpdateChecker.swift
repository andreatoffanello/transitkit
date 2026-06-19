import Foundation
import UIKit

/// Handles both soft (non-blocking) and forced (blocking) app update prompts.
///
/// Two mechanisms — mirroring the implementation in DoVe (civici repo):
///
/// 1. **Force gate** — reads `appUpdate.ios.minVersion` + `force` from the
///    already-loaded `OperatorConfig`. If current version < minVersion and
///    force == true, sets `requirement = .forced(...)`. Runs synchronously on
///    launch (no network) so the gate is instant.
///
/// 2. **Soft banner** — asks the iTunes Lookup API for the current App Store
///    version and compares to the installed version. If a newer version is
///    available and the user hasn't dismissed it yet, populates `softUpdate`.
///    Dismissal is persisted per-version in `UserDefaults` — no re-nag for
///    the same version.
///
/// White-label adaptation: DoVe uses a hardcoded bundleId; here we always
/// use `Bundle.main.bundleIdentifier` so the lookup is correct for every
/// operator build (`com.transitkit.appalcart`, etc.).
@MainActor @Observable
final class AppUpdateChecker {
    static let shared = AppUpdateChecker()

    enum Requirement: Equatable {
        case none
        case forced(message: String, storeUrl: URL)
    }

    struct SoftUpdate: Equatable {
        let message: String
        let storeUrl: URL
        let version: String
    }

    private(set) var requirement: Requirement = .none
    private(set) var softUpdate: SoftUpdate?

    /// UserDefaults key: the version string for which the user tapped "Later".
    /// Cleared automatically when a newer store version appears.
    private let dismissedKey = "tk_dismissedSoftUpdateVersion"

    /// iTunes Lookup response wrapper.
    private struct LookupResponse: Decodable {
        let results: [LookupResult]
    }
    private struct LookupResult: Decodable {
        let version: String
        let trackViewUrl: String?
    }

    private init() {}

    // MARK: - Public API

    /// Entry point — call on launch and on every `scenePhase == .active`.
    ///
    /// - Parameter config: The already-loaded operator config. Must not be nil.
    ///   When nil (bootstrapping not yet finished) the call is a no-op.
    func check(config: OperatorConfig?) {
        guard let config else { return }
        let language = resolvedLanguage()
        checkForced(config: config, language: language)
        // Soft check fires async so it doesn't block the calling site.
        Task { await refreshSoftUpdate(config: config, language: language) }
    }

    /// Dismiss the soft banner for the current version. Persists so we don't
    /// re-nag after foregrounding.
    func dismissSoftUpdate() {
        if let version = softUpdate?.version {
            UserDefaults.standard.set(version, forKey: dismissedKey)
        }
        softUpdate = nil
    }

    /// Open the App Store page using the `itms-apps://` deep link when possible,
    /// falling back to the https URL.
    func openStore(_ url: URL) {
        let itms = url.absoluteString
            .replacingOccurrences(of: "https://apps.apple.com",
                                   with: "itms-apps://apps.apple.com")
        let target = URL(string: itms) ?? url
        UIApplication.shared.open(target)
    }

    // MARK: - Force gate

    /// Synchronous check against config — no network required. Sets
    /// `requirement = .forced(...)` if current version < minVersion and force==true.
    private func checkForced(config: OperatorConfig, language: String) {
        guard let block = config.appUpdate,
              let ios = block.ios,
              let minVersion = ios.minVersion,
              let force = ios.force, force,
              let storeUrlString = ios.storeUrl,
              let storeUrl = URL(string: storeUrlString),
              let current = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        else {
            requirement = .none
            return
        }

        if current.compare(minVersion, options: .numeric) == .orderedAscending {
            let message = localizedMessage(block: block, language: language,
                                           en: block.messageEn,
                                           it: block.messageIt,
                                           es: block.messageEs)
                ?? String(localized: "update_force_fallback_message")
            requirement = .forced(message: message, storeUrl: storeUrl)
            softUpdate = nil
        } else {
            requirement = .none
        }
    }

    // MARK: - Soft banner (async, iTunes Lookup)

    /// Fetches the current App Store version via iTunes Lookup and updates
    /// `softUpdate` if a newer version is available and not already dismissed.
    private func refreshSoftUpdate(config: OperatorConfig, language: String) async {
        // Force gate has priority — no soft banner when blocked.
        guard case .none = requirement else { return }

        guard let (storeVersion, trackViewUrl) = await fetchAppStoreVersion(),
              let current = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
              current.compare(storeVersion, options: .numeric) == .orderedAscending,
              UserDefaults.standard.string(forKey: dismissedKey) != storeVersion
        else {
            softUpdate = nil
            return
        }

        let block = config.appUpdate
        // storeUrl priority: config field → iTunes trackViewUrl → nil (guard out)
        let resolvedStoreUrlString = block?.ios?.storeUrl ?? trackViewUrl
        guard let resolvedStoreUrlString, let storeUrl = URL(string: resolvedStoreUrlString) else {
            softUpdate = nil
            return
        }

        let message = block.flatMap { b in
            localizedMessage(block: b, language: language,
                             en: b.whatsNewEn, it: b.whatsNewIt, es: b.whatsNewEs)
        } ?? String(localized: "update_soft_fallback_message")

        softUpdate = SoftUpdate(message: message, storeUrl: storeUrl, version: storeVersion)
    }

    // MARK: - iTunes Lookup

    /// Queries `itunes.apple.com/lookup` with the running app's bundle id.
    /// Uses `Bundle.main.bundleIdentifier` so every white-label build (e.g.
    /// `com.transitkit.appalcart`) looks up its own App Store entry.
    /// Returns `(version, trackViewUrl?)` or nil on any failure.
    private func fetchAppStoreVersion() async -> (String, String?)? {
        guard let bundleId = Bundle.main.bundleIdentifier else { return nil }
        var components = URLComponents(string: "https://itunes.apple.com/lookup")
        var items: [URLQueryItem] = [URLQueryItem(name: "bundleId", value: bundleId)]
        if let region = Locale.current.region?.identifier {
            items.append(URLQueryItem(name: "country", value: region))
        }
        components?.queryItems = items
        guard let url = components?.url else { return nil }

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData
        request.timeoutInterval = 10
        guard let (data, _) = try? await URLSession.shared.data(for: request),
              let response = try? JSONDecoder().decode(LookupResponse.self, from: data),
              let result = response.results.first
        else { return nil }
        return (result.version, result.trackViewUrl)
    }

    // MARK: - Helpers

    /// Best language match from the running locale. Falls through to "en".
    private func resolvedLanguage() -> String {
        let lang = String((Locale.preferredLanguages.first ?? "en").prefix(2))
        // Supported: en, it, es (es-419 → "es")
        return ["en", "it", "es"].contains(lang) ? lang : "en"
    }

    private func localizedMessage(block: AppUpdateConfig, language: String,
                                  en: String?, it: String?, es: String?) -> String? {
        switch language {
        case "it": return it ?? en
        case "es": return es ?? en
        default:   return en ?? it
        }
    }
}
