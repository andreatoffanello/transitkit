import SwiftUI

// MARK: - OverflowBadge

/// Small pill showing a "+N" overflow count for truncated lists of line badges or similar items.
/// Used wherever a list of badges is capped and the remaining count is shown inline.
///
/// Usage:
/// ```swift
/// OverflowBadge(count: lines.count - 6)
/// ```
struct OverflowBadge: View {
    let count: Int

    var body: some View {
        Text("+\(count)")
            .font(.system(size: 9, weight: .semibold, design: .rounded))
            .foregroundStyle(AppTheme.textTertiary)
            .padding(.horizontal, 4)
            .padding(.vertical, 2)
            .background(AppTheme.glassFill, in: RoundedRectangle(cornerRadius: 3))
    }
}
