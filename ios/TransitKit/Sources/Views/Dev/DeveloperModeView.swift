import SwiftUI

// MARK: - DeveloperModeView

/// Hidden settings panel — registers this device with the transitkit-console
/// CMS so the operator can pick it from the "Send preview" dropdown.
///
/// Reachable from `SettingsTab` after 7 taps on the version number (Android
/// developer-mode pattern, familiar to power users).
struct DeveloperModeView: View {
    @Environment(PushNotificationManager.self) private var push
    @Environment(\.dismiss) private var dismiss

    let operatorId: String
    let consoleApiUrl: String?

    @State private var label: String = ""
    @State private var registering = false
    @State private var resultMessage: String?
    @State private var resultColor: Color = .green
    @State private var copiedToClipboard = false

    var body: some View {
        Form {
            Section {
                if let token = push.fcmToken {
                    LabeledContent {
                        Text(tokenSuffix(token))
                            .font(.system(.footnote, design: .monospaced))
                            .foregroundStyle(AppTheme.textSecondary)
                            .textSelection(.enabled)
                    } label: {
                        Text("FCM token")
                    }
                    Button {
                        UIPasteboard.general.string = token
                        copiedToClipboard = true
                        Task {
                            try? await Task.sleep(for: .seconds(2))
                            copiedToClipboard = false
                        }
                    } label: {
                        Label(
                            copiedToClipboard ? "Copied" : "Copy full token",
                            systemImage: copiedToClipboard ? "checkmark" : "doc.on.doc"
                        )
                    }
                } else {
                    Label {
                        Text("FCM token not available yet — enable notifications in Settings first.")
                    } icon: {
                        Image(systemName: "exclamationmark.triangle")
                            .foregroundStyle(.orange)
                    }
                    .font(.footnote)
                }
            } header: {
                Text("Device identity")
            }

            Section {
                TextField("Device label", text: $label)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.words)

                Button {
                    Task { await register() }
                } label: {
                    HStack {
                        if registering {
                            ProgressView()
                                .controlSize(.small)
                                .padding(.trailing, 4)
                        }
                        Text(registering ? "Registering…" : "Register this device")
                    }
                }
                .disabled(push.fcmToken == nil || label.trimmingCharacters(in: .whitespaces).isEmpty || registering)
            } header: {
                Text("Register as test device")
            } footer: {
                Text("The operator will see this device in the “Send preview” list inside the CMS.")
            }

            if let message = resultMessage {
                Section {
                    Text(message)
                        .font(.footnote)
                        .foregroundStyle(resultColor)
                }
            }

            Section {
                LabeledContent("Operator ID",   value: operatorId)
                LabeledContent("Authorization", value: authStatusLabel)
                LabeledContent("Firebase",      value: push.firebaseConfigured ? "configured" : "missing plist")
                if let consoleApiUrl, !consoleApiUrl.isEmpty {
                    LabeledContent("Console API", value: consoleApiUrl)
                        .font(.system(.body, design: .monospaced))
                }
                if let err = push.lastError {
                    LabeledContent("Last error", value: err)
                        .foregroundStyle(AppTheme.realtimeRed)
                }
            } header: {
                Text("Status")
            }
        }
        .navigationTitle("Developer mode")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if label.isEmpty {
                label = PushApiClient.defaultDeviceLabel()
            }
        }
    }

    private var authStatusLabel: String {
        switch push.authorizationStatus {
        case .authorized:     "authorized"
        case .denied:         "denied"
        case .notDetermined:  "not asked"
        case .unknown:        "unknown"
        }
    }

    private func tokenSuffix(_ token: String) -> String {
        guard token.count > 16 else { return token }
        return "…\(token.suffix(12))"
    }

    private func register() async {
        guard let token = push.fcmToken else { return }
        registering = true
        defer { registering = false }
        do {
            try await PushApiClient.registerTestDevice(
                consoleBaseUrl: consoleApiUrl,
                operatorId: operatorId,
                fcmToken: token,
                label: label.trimmingCharacters(in: .whitespaces)
            )
            resultMessage = "✓ Registered. You can now receive previews from the CMS."
            resultColor   = .green
        } catch {
            resultMessage = "Registration failed: \(error.localizedDescription)"
            resultColor   = .red
        }
    }
}
