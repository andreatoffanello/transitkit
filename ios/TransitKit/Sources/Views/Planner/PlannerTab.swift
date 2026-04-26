import SwiftUI

// MARK: - PlannerTab
// NavigationStack wrapper for the trip planner feature.

struct PlannerTab: View {
    var body: some View {
        NavigationStack {
            PlannerScreen()
        }
    }
}
