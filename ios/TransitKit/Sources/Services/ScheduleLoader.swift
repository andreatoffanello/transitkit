import Foundation

// MARK: - Schedule Loader

/// Loads and caches the operator's schedule data.
/// On first launch: downloads from API.
/// On subsequent launches: loads from disk cache, checks freshness in background.
actor ScheduleLoader {
    private var cached: ScheduleResponse?
    private let operatorId: String
    private let apiUrl: String?

    init(operatorId: String, apiUrl: String? = nil) {
        self.operatorId = operatorId
        self.apiUrl = apiUrl
    }

    /// Load schedule data. Tries: memory cache → disk cache → API download.
    func load() async throws -> ScheduleResponse {
        if let cached { return cached }

        if let diskData = loadFromDisk() {
            cached = diskData
            Task { await checkForUpdates() }
            return diskData
        }

        let downloaded = try await downloadFromAPI()
        cached = downloaded
        saveToDisk(downloaded)
        return downloaded
    }

    /// Force-refresh from API.
    func refresh() async throws -> ScheduleResponse {
        let data = try await downloadFromAPI()
        cached = data
        saveToDisk(data)
        return data
    }

    /// Called by Background App Refresh handler to persist freshly-downloaded response.
    func saveResponseToDisk(_ response: ScheduleResponse) {
        cached = response
        saveToDisk(response)
    }

    // MARK: - Disk Cache

    private var cacheURL: URL {
        let dir = FileManager.default.urls(
            for: .applicationSupportDirectory, in: .userDomainMask
        )[0].appendingPathComponent("TransitKit", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("\(operatorId)_schedule_v2.json")
    }

    private func loadFromDisk() -> ScheduleResponse? {
        guard let data = try? Data(contentsOf: cacheURL) else { return nil }
        return try? JSONDecoder().decode(ScheduleResponse.self, from: data)
    }

    private func saveToDisk(_ response: ScheduleResponse) {
        guard let data = try? JSONEncoder().encode(response) else { return }
        try? data.write(to: cacheURL, options: .atomic)
    }

    // MARK: - API Download

    private func downloadFromAPI() async throws -> ScheduleResponse {
        guard let apiUrl else {
            throw ScheduleError.noAPIURLConfigured
        }
        let client = try APIClient(apiUrl: apiUrl)
        return try await client.fetchSchedule()
    }

    private func checkForUpdates() async {
        guard let newData = try? await downloadFromAPI() else { return }
        if newData.lastUpdated != cached?.lastUpdated {
            cached = newData
            saveToDisk(newData)
        }
    }

    // MARK: - Errors

    enum ScheduleError: LocalizedError {
        case noAPIURLConfigured
        case downloadFailed

        var errorDescription: String? {
            switch self {
            case .noAPIURLConfigured: "No API URL configured for this operator"
            case .downloadFailed:     "Failed to download schedule"
            }
        }
    }
}
