import Foundation

/// A cluster of nearby stops displayed as a single map annotation when zoomed out.
struct StopCluster: Identifiable {
    let id: String         // e.g. "cluster_\(binKey)"
    let centerLat: Double
    let centerLng: Double
    let count: Int
    let stops: [ResolvedStop]
}
