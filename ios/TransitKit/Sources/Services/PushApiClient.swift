import Foundation
import UIKit

// MARK: - PushApiClient

/// Talks to the transitkit-console CMS for endpoints that are public
/// (no Supabase JWT) — currently just the test-device registration.
///
/// The base URL comes from `OperatorConfig.consoleApiUrl`. When the field
/// is missing in config.json the client refuses to send.
enum PushApiClient {
    /// Resolve the CMS base URL.
    ///
    /// - Debug build:   `TRANSITKIT_LOCAL_CONSOLE` env → else `http://localhost:3000`.
    ///                  The operator's production URL is ignored so the dev
    ///                  loop never accidentally hits prod.
    /// - Release build: the operator's `console_api_url` from config.json.
    ///
    /// The Info.plist `NSAllowsLocalNetworking` exception covers the http://
    /// localhost case for Debug.
    static func resolveBaseUrl(operatorConsoleUrl: String?) -> String? {
        #if DEBUG
        if let envOverride = ProcessInfo.processInfo.environment["TRANSITKIT_LOCAL_CONSOLE"],
           !envOverride.isEmpty {
            return envOverride
        }
        return "http://localhost:3000"
        #else
        return operatorConsoleUrl
        #endif
    }

    struct RegisterTestDeviceRequest: Encodable {
        let operator_id: String
        let fcm_token: String
        let platform: String
        let label: String
    }

    enum Failure: LocalizedError {
        case missingBaseUrl
        case invalidUrl
        case httpError(status: Int, body: String?)
        case transport(Error)
        case decoding(Error)

        var errorDescription: String? {
            switch self {
            case .missingBaseUrl: "console_api_url missing from operator config.json"
            case .invalidUrl:     "console_api_url is not a valid URL"
            case let .httpError(status, body): "Server replied \(status)\(body.map { ": \($0)" } ?? "")"
            case let .transport(e): "Network error: \(e.localizedDescription)"
            case let .decoding(e):  "Decoding error: \(e.localizedDescription)"
            }
        }
    }

    static func registerTestDevice(
        consoleBaseUrl: String?,
        operatorId: String,
        fcmToken: String,
        label: String
    ) async throws {
        let resolved = resolveBaseUrl(operatorConsoleUrl: consoleBaseUrl)
        guard let base = resolved, !base.isEmpty else { throw Failure.missingBaseUrl }
        guard let url  = URL(string: base.trimmingCharacters(in: .init(charactersIn: "/")) + "/api/test-devices/register") else {
            throw Failure.invalidUrl
        }

        let body = RegisterTestDeviceRequest(
            operator_id: operatorId,
            fcm_token:   fcmToken,
            platform:    "ios",
            label:       label
        )

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(body)

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                throw Failure.transport(URLError(.badServerResponse))
            }
            if !(200..<300).contains(http.statusCode) {
                let bodyText = String(data: data, encoding: .utf8)
                throw Failure.httpError(status: http.statusCode, body: bodyText)
            }
        } catch let failure as Failure {
            throw failure
        } catch {
            throw Failure.transport(error)
        }
    }

    /// A user-facing label like "iPhone 16 Pro (sim)" / "iPhone di Marco".
    @MainActor
    static func defaultDeviceLabel() -> String {
        let name = UIDevice.current.name
        let model = UIDevice.current.model
        // `UIDevice.name` returns generic "iPhone" on iOS 16+ unless the
        // app has the special entitlement. Show model as a fallback.
        if name.localizedCaseInsensitiveContains("iPhone") || name == model {
            return "\(model) — \(UIDevice.current.systemName) \(UIDevice.current.systemVersion)"
        }
        return name
    }
}
