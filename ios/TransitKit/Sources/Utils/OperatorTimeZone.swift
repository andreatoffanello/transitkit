import SwiftUI

/// Operator timezone (e.g. "America/New_York") propagated through the SwiftUI
/// environment so any view can format transit times in the operator's local
/// time rather than the device's. Set once in `TransitKitApp` from
/// `OperatorConfig.timezone`. Default is the device timezone as a safe fallback.
private struct OperatorTimeZoneKey: EnvironmentKey {
    static let defaultValue: TimeZone = .current
}

extension EnvironmentValues {
    var operatorTimeZone: TimeZone {
        get { self[OperatorTimeZoneKey.self] }
        set { self[OperatorTimeZoneKey.self] = newValue }
    }
}
