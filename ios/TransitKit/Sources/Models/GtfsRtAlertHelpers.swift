import SwiftUI

// MARK: - Localized cause/effect labels

extension GtfsRtAlert {
    /// Localized human label for the GTFS-RT cause. `nil` for unknown/other.
    var causeName: String? {
        switch cause {
        case .unknownCause, .otherCause: return nil
        case .technicalProblem:  return String(localized: "alert_cause_technical")
        case .strike:            return String(localized: "alert_cause_strike")
        case .demonstration:     return String(localized: "alert_cause_demonstration")
        case .accident:          return String(localized: "alert_cause_accident")
        case .holiday:           return String(localized: "alert_cause_holiday")
        case .weather:           return String(localized: "alert_cause_weather")
        case .maintenance:       return String(localized: "alert_cause_maintenance")
        case .construction:      return String(localized: "alert_cause_construction")
        case .policeActivity:    return String(localized: "alert_cause_police")
        case .medicalEmergency:  return String(localized: "alert_cause_medical")
        }
    }

    /// Localized human label for the GTFS-RT effect. `nil` for unknown/other/no-effect.
    var effectName: String? {
        switch effect {
        case .noService:          return String(localized: "alert_effect_no_service")
        case .reducedService:     return String(localized: "alert_effect_reduced_service")
        case .significantDelays:  return String(localized: "alert_effect_delays")
        case .detour:             return String(localized: "alert_effect_detour")
        case .additionalService:  return String(localized: "alert_effect_additional")
        case .modifiedService:    return String(localized: "alert_effect_modified")
        case .stopMoved:          return String(localized: "alert_effect_stop_moved")
        case .accessibilityIssue: return String(localized: "alert_effect_accessibility")
        case .noEffect, .otherEffect, .unknownEffect: return nil
        }
    }

    /// Actionable title built from cause + effect — replaces the bureaucratic
    /// header text ("Sciopero generale di 24h…") in the primary UI role.
    /// The original `headerText` is kept as secondary detail.
    var displayTitle: String {
        switch (causeName, effectName) {
        case let (c?, e?):  return "\(c) · \(e)"
        case let (c?, nil): return c
        case let (nil, e?): return e
        case (nil, nil):    return String(localized: "alert_default_title")
        }
    }
}

// MARK: - Cause signifier icon

enum AlertCauseIcon {
    /// Lucide signifier icon for a GTFS-RT cause. Falls back to `alertTriangle`
    /// for any cause whose dedicated icon isn't in the asset catalog yet.
    static func icon(for cause: AlertCause) -> LucideIcon {
        switch cause {
        case .strike:           return .users
        case .demonstration:    return .users
        case .holiday:          return .bell
        case .policeActivity:   return .shield
        case .technicalProblem, .maintenance,
             .accident, .weather, .construction,
             .medicalEmergency,
             .unknownCause, .otherCause:
            return .alertTriangle
        }
    }
}
