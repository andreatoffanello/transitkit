import SwiftUI

// MARK: - Dock Badge (inline)

/// Badge dock segnaletico (lettera nera su disco giallo) — colori fissi
/// intenzionali stile cartellonistica, leggibili in entrambi i temi.
struct DockBadgeView: View {
    let letter: String

    var body: some View {
        Text(letter)
            .font(.system(size: 10, weight: .heavy, design: .rounded))
            .foregroundStyle(.black)
            .frame(width: 18, height: 18)
            .background(Color(red: 1.0, green: 0.82, blue: 0.0), in: Circle())
            .accessibilityLabel(String(format: NSLocalizedString("dock_label", comment: ""), letter))
    }
}
