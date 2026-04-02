import SwiftUI

// MARK: - Shimmer Modifier

/// Shimmer loading effect. Shows a static gradient when Reduce Motion is enabled.
struct ShimmerModifier: ViewModifier {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var phase: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .overlay {
                GeometryReader { geo in
                    let width = geo.size.width
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: .white.opacity(0.25), location: 0.5),
                            .init(color: .clear, location: 1)
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: width * 0.6)
                    .offset(x: reduceMotion ? width * 0.2 : -width * 0.6 + phase * (width * 1.6))
                    .onAppear {
                        guard !reduceMotion else { return }
                        withAnimation(
                            .linear(duration: 1.2)
                            .repeatForever(autoreverses: false)
                        ) {
                            phase = 1
                        }
                    }
                }
                .mask(content)
            }
    }
}

extension View {
    /// Applies a shimmer loading animation. Respects Reduce Motion accessibility setting.
    func shimmer() -> some View {
        modifier(ShimmerModifier())
    }
}

// MARK: - Skeleton Building Blocks

/// A rounded rectangle placeholder with shimmer animation.
struct SkeletonRect: View {
    var width: CGFloat? = nil
    var height: CGFloat = 14
    var cornerRadius: CGFloat = 6

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(AppTheme.glassFill)
            .frame(width: width, height: height)
            .shimmer()
    }
}

// MARK: - DepartureRowSkeleton

/// Matches `DepartureRow` layout with shimmer placeholder rectangles.
struct DepartureRowSkeleton: View {
    var body: some View {
        HStack(spacing: 8) {
            // Line badge
            SkeletonRect(width: 44, height: 22, cornerRadius: 4)

            // Headsign
            VStack(alignment: .leading, spacing: 4) {
                SkeletonRect(width: 120, height: 14)
            }

            Spacer(minLength: 8)

            // Countdown
            VStack(alignment: .trailing, spacing: 3) {
                SkeletonRect(width: 36, height: 20)
            }

            // Time
            SkeletonRect(width: 44, height: 14)
        }
        .padding(.vertical, 10)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(AppTheme.separatorLine)
                .frame(height: 0.5)
        }
    }
}

// MARK: - StopCardSkeleton

/// Matches `StopCard` layout with shimmer placeholder rectangles.
struct StopCardSkeleton: View {
    var body: some View {
        HStack(spacing: 12) {
            // Transit icon
            SkeletonRect(width: 36, height: 36, cornerRadius: 8)

            VStack(alignment: .leading, spacing: 8) {
                // Stop name
                SkeletonRect(width: 140, height: 14)
                // Line badges
                HStack(spacing: 5) {
                    ForEach(0..<3, id: \.self) { _ in
                        SkeletonRect(width: 40, height: 20, cornerRadius: 4)
                    }
                }
            }

            Spacer(minLength: 8)

            // Time placeholder
            SkeletonRect(width: 44, height: 18)
        }
        .padding(12)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - DeparturesSkeletonCard

/// Shows multiple `DepartureRowSkeleton`s inside a glass card.
struct DeparturesSkeletonCard: View {
    let count: Int

    init(count: Int = 3) {
        self.count = count
    }

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<count, id: \.self) { _ in
                DepartureRowSkeleton()
            }
        }
        .padding(.horizontal, 12)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - LineRowSkeleton

/// Matches a line list row layout with shimmer placeholders.
struct LineRowSkeleton: View {
    var body: some View {
        HStack(spacing: 12) {
            SkeletonRect(width: 48, height: 28, cornerRadius: 4)

            VStack(alignment: .leading, spacing: 4) {
                SkeletonRect(width: 100, height: 14)
                SkeletonRect(width: 60, height: 10)
            }

            Spacer()

            SkeletonRect(width: 8, height: 14)
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 12)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12))
    }
}
