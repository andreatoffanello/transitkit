import Foundation

// MARK: - Schedule Loader

/// Loads and caches the operator's schedule data.
/// On first launch: loads from app bundle.
/// On subsequent launches: loads from disk cache, then checks CDN for updates.
actor ScheduleLoader {
    private var cached: ScheduleData?
    private let operatorId: String
    private let cdnBaseURL: String?

    init(operatorId: String, cdnBaseURL: String? = nil) {
        self.operatorId = operatorId
        self.cdnBaseURL = cdnBaseURL
    }

    /// Load schedule data. Tries: memory cache → disk cache → bundle → CDN.
    func load() async throws -> ScheduleData {
        // 1. Memory cache
        if let cached { return cached }

        // 2. Disk cache
        if let diskData = loadFromDisk() {
            cached = diskData
            // Check for updates in background (don't block)
            Task { await checkForUpdates() }
            return diskData
        }

        // 3. Bundle fallback
        if let bundleData = loadFromBundle() {
            cached = bundleData
            saveToDisk(bundleData)
            Task { await checkForUpdates() }
            return bundleData
        }

        // 4. CDN download
        if let cdnData = try await downloadFromCDN() {
            cached = cdnData
            saveToDisk(cdnData)
            return cdnData
        }

        throw ScheduleError.noDataAvailable
    }

    /// Force refresh from CDN.
    func refresh() async throws -> ScheduleData {
        guard let data = try await downloadFromCDN() else {
            throw ScheduleError.downloadFailed
        }
        cached = data
        saveToDisk(data)
        return data
    }

    // MARK: - Bundle

    private func loadFromBundle() -> ScheduleData? {
        guard let url = Bundle.main.url(forResource: "schedules", withExtension: "json") else {
            return nil
        }
        guard let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(ScheduleData.self, from: data)
    }

    // MARK: - Disk Cache

    private var cacheURL: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("TransitKit", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("\(operatorId)_schedules.json")
    }

    private func loadFromDisk() -> ScheduleData? {
        guard let data = try? Data(contentsOf: cacheURL) else { return nil }
        return try? JSONDecoder().decode(ScheduleData.self, from: data)
    }

    private func saveToDisk(_ schedule: ScheduleData) {
        guard let data = try? JSONEncoder().encode(schedule) else { return }
        try? data.write(to: cacheURL)
    }

    // MARK: - CDN

    private func downloadFromCDN() async throws -> ScheduleData? {
        guard let baseURL = cdnBaseURL else { return nil }
        let urlString = "\(baseURL)/\(operatorId)/schedules.json"
        guard let url = URL(string: urlString) else { return nil }

        let (data, response) = try await URLSession.shared.data(from: url)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            return nil
        }

        let schedule = try JSONDecoder().decode(ScheduleData.self, from: data)
        return schedule
    }

    private func checkForUpdates() async {
        guard let newData = try? await downloadFromCDN() else { return }
        // Only update if newer
        if newData.lastUpdated != cached?.lastUpdated {
            cached = newData
            saveToDisk(newData)
        }
    }

    // MARK: - Errors

    enum ScheduleError: LocalizedError {
        case noDataAvailable
        case downloadFailed

        var errorDescription: String? {
            switch self {
            case .noDataAvailable: "No schedule data available"
            case .downloadFailed: "Failed to download schedule"
            }
        }
    }
}
