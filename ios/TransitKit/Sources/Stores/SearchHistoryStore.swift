import Foundation
import Observation

/// Persists recent stop and line searches per operator.
@Observable
final class SearchHistoryStore {
    private let stopsKey: String
    private let linesKey: String
    private let maxItems = 8

    private(set) var recentStopIds: [String] = []
    private(set) var recentLineIds: [String] = []

    init(operatorId: String) {
        stopsKey = "\(operatorId)_recent_stops"
        linesKey = "\(operatorId)_recent_lines"
        recentStopIds = UserDefaults.standard.stringArray(forKey: stopsKey) ?? []
        recentLineIds = UserDefaults.standard.stringArray(forKey: linesKey) ?? []
    }

    func recordStop(_ id: String) {
        var ids = recentStopIds.filter { $0 != id }
        ids.insert(id, at: 0)
        recentStopIds = Array(ids.prefix(maxItems))
        UserDefaults.standard.set(recentStopIds, forKey: stopsKey)
    }

    func recordLine(_ id: String) {
        var ids = recentLineIds.filter { $0 != id }
        ids.insert(id, at: 0)
        recentLineIds = Array(ids.prefix(maxItems))
        UserDefaults.standard.set(recentLineIds, forKey: linesKey)
    }

    func clearAll() {
        recentStopIds = []
        recentLineIds = []
        UserDefaults.standard.removeObject(forKey: stopsKey)
        UserDefaults.standard.removeObject(forKey: linesKey)
    }
}
