import Foundation

// MARK: - Schedule Loader

/// Loads and caches the operator's schedule data.
/// On first launch: downloads from CDN (static GitHub Pages JSON).
/// On subsequent launches: loads from disk cache, checks freshness in background.
actor ScheduleLoader {
    private var cached: ScheduleResponse?
    private let operatorId: String
    private let apiUrl: String?
    private let operatorConfig: OperatorConfig?

    init(operatorId: String, apiUrl: String? = nil, operatorConfig: OperatorConfig? = nil) {
        self.operatorId = operatorId
        self.apiUrl = apiUrl
        self.operatorConfig = operatorConfig
    }

    /// Load schedule data. Tries: memory cache → disk cache → CDN download.
    func load() async throws -> ScheduleResponse {
        if let cached { return cached }

        if let diskData = loadFromDisk() {
            cached = diskData
            Task { await checkForUpdates() }
            return diskData
        }

        let downloaded = try await downloadFromCDN()
        cached = downloaded
        saveToDisk(downloaded)
        return downloaded
    }

    /// Force-refresh from CDN.
    func refresh() async throws -> ScheduleResponse {
        let data = try await downloadFromCDN()
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

    // MARK: - CDN Download

    private func scheduleURL() throws -> URL {
        // Prefer cdnUrl (static CDN) over legacy apiUrl
        if let cdnUrl = operatorConfig?.cdnUrl,
           let url = URL(string: "\(cdnUrl)/\(operatorId)/schedules.json") {
            return url
        }
        // Fallback: legacy API endpoint
        if let apiUrl,
           let url = URL(string: "\(apiUrl)/schedule") {
            return url
        }
        throw ScheduleError.noURLConfigured
    }

    private func downloadFromCDN() async throws -> ScheduleResponse {
        let url = try scheduleURL()
        let (data, response) = try await URLSession.shared.data(from: url)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            throw ScheduleError.downloadFailed
        }
        do {
            return try JSONDecoder().decode(ScheduleResponse.self, from: data)
        } catch {
            throw ScheduleError.decodingFailed(error)
        }
    }

    private func checkForUpdates() async {
        guard let newData = try? await downloadFromCDN() else { return }
        if newData.lastUpdated != cached?.lastUpdated {
            cached = newData
            saveToDisk(newData)
        }
    }

    // MARK: - Errors

    enum ScheduleError: LocalizedError {
        case noURLConfigured
        case downloadFailed
        case decodingFailed(Error)

        var errorDescription: String? {
            switch self {
            case .noURLConfigured:        "No CDN or API URL configured for this operator"
            case .downloadFailed:         "Failed to download schedule"
            case .decodingFailed(let e):  "Failed to decode schedule: \(e.localizedDescription)"
            }
        }
    }
}
