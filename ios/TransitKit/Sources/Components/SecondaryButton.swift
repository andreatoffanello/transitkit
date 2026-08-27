import SwiftUI

// MARK: - Secondary / ghost buttons
//
// In dark mode il brand NON regge come foreground. Gli accent della flotta
// stanno fra 3.5:1 e 4.2:1 sul background #080C18, e su una card mappa
// (`surfaceOverMap`, velata di accent) scendono sotto 3:1. Un accent nuovo può
// stare peggio: `accentColor` è config di operatore ed è validato solo contro
// il bianco. Quindi la regola è la stessa già usata per le icone — bianco — e
// l'identità brand resta al fill pieno dei bottoni primari.
//
// Questi due modifier sono l'unico modo corretto di vestire un bottone non
// primario. Non scrivere `.foregroundStyle(AppTheme.accent)` su una label
// tappabile: è esattamente il difetto che questi token esistono per chiudere.

extension View {

    /// Bottone tonale: label bianca (dark) o accent (light) su un fill che
    /// suggerisce l'area tappabile senza doverne portare il contrasto.
    func secondaryButtonSurface(
        cornerRadius: CGFloat = 12,
        horizontalPadding: CGFloat = 14,
        verticalPadding: CGFloat = 10
    ) -> some View {
        self
            .foregroundStyle(AppTheme.secondaryFg)
            .padding(.horizontal, horizontalPadding)
            .padding(.vertical, verticalPadding)
            .background(
                AppTheme.secondaryFill,
                in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            )
    }

    /// Variante outline (bordo, niente fill) per le azioni terziarie.
    /// Il bordo è neutro: `accent.opacity(...)` in dark è un bordo che non si vede.
    func secondaryButtonOutline(
        cornerRadius: CGFloat = 12,
        horizontalPadding: CGFloat = 14,
        verticalPadding: CGFloat = 10
    ) -> some View {
        self
            .foregroundStyle(AppTheme.secondaryFg)
            .padding(.horizontal, horizontalPadding)
            .padding(.vertical, verticalPadding)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(AppTheme.secondaryBorder, lineWidth: 1)
            )
    }

    /// Chip circolare secondaria (icona sola) — es. il bottone "vai a" nella
    /// testata del dettaglio fermata.
    func secondaryIconCircle(diameter: CGFloat = 36) -> some View {
        self
            .foregroundStyle(AppTheme.secondaryFg)
            .frame(width: diameter, height: diameter)
            .background(AppTheme.secondaryFill, in: Circle())
            .contentShape(Circle())
    }
}
