import SwiftUI

// MARK: - Dock Pin (map)

/// NOTE: currently unused in the codebase (no call sites). Kept as-is pending a
/// decision on where to reintroduce dock pins on the map annotations.
struct DockPin: View {
    let letter: String

    var body: some View {
        ZStack {
            Circle()
                .fill(.white)
                .frame(width: 26, height: 26)
            Circle()
                .fill(Color(red: 1.0, green: 0.82, blue: 0.0))
                .frame(width: 22, height: 22)
            Text(letter)
                .font(.system(size: 13, weight: .heavy, design: .rounded))
                .foregroundStyle(.black)
        }
        .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
    }
}

// MARK: - Dock Badge (inline)

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
