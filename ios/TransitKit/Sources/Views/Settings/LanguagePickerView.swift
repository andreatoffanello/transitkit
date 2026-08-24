import SwiftUI

// MARK: - LanguagePickerView

/// Selettore della lingua dell'interfaccia, pushato da Impostazioni.
///
/// Il cambio è immediato: `LocalizationManager` ridireziona il bundle delle
/// stringhe e `ContentView` ricrea i tab (`.id(localization.language)`), così
/// quando l'utente torna indietro l'app è già tutta nella lingua nuova.
struct LanguagePickerView: View {
    private let localization = LocalizationManager.shared

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                GlassCard(cornerRadius: 16) {
                    VStack(spacing: 0) {
                        ForEach(Array(AppLanguage.allCases.enumerated()), id: \.element.id) { index, language in
                            Button {
                                guard language != localization.language else { return }
                                UISelectionFeedbackGenerator().selectionChanged()
                                withAnimation(.snappy(duration: 0.25)) {
                                    localization.select(language)
                                }
                            } label: {
                                row(for: language)
                            }
                            .buttonStyle(.plain)
                            .accessibilityIdentifier("language_row_\(language.rawValue)")

                            if index < AppLanguage.allCases.count - 1 {
                                Rectangle()
                                    .fill(AppTheme.separatorLine)
                                    .frame(height: 0.5)
                                    .padding(.leading, 16)
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(AppTheme.background.ignoresSafeArea())
        // Il titolo della nav bar di una destination pushata non si aggiorna
        // quando cambia la stringa mentre la schermata è già a video: serve
        // cambiarle identità perché SwiftUI la riapplichi.
        .id(localization.language)
        .navigationTitle(L("settings_section_language"))
        .navigationBarTitleDisplayMode(.large)
    }

    // MARK: - Row

    private func row(for language: AppLanguage) -> some View {
        let isSelected = localization.language == language
        return HStack(spacing: 12) {
            Text(language.endonym)
                .font(.subheadline.weight(isSelected ? .semibold : .regular))
                .foregroundStyle(AppTheme.textPrimary)

            Spacer(minLength: 0)

            LucideIcon.check.sized(17)
                .foregroundStyle(AppTheme.accent)
                .opacity(isSelected ? 1 : 0)
                .scaleEffect(isSelected ? 1 : 0.6)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 15)
        .contentShape(Rectangle())
    }
}
