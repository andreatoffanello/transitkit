import SwiftUI

// MARK: - Cluster Annotation View

/// Map annotation displayed when stops are binned into a cluster at far zoom levels.
struct ClusterAnnotationView: View {
    let count: Int

    var body: some View {
        ZStack {
            Circle()
                .fill(AppTheme.accent)
                .frame(width: 36, height: 36)
                .shadow(color: AppTheme.accent.opacity(0.4), radius: 6, y: 2)
            Text("\(count)")
                .font(.system(size: 13, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
        }
        .accessibilityLabel("\(count) fermate raggruppate")
        .accessibilityIdentifier("map_cluster_\(count)")
    }
}
