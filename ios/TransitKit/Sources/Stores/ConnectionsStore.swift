import Foundation

// MARK: - ConnectionsStore
//
// Manages lifecycle of the RoutingEngine:
//   1. Downloads connections.json.zlib from CDN to disk cache
//   2. Loads it into the RoutingEngine actor
//   3. Exposes query methods on MainActor for the UI
//
// CDN URL pattern: {config.cdnUrl}/{config.id}/connections.json.zlib
// Falls back to bundle resource if CDN unavailable.

@MainActor
@Observable
final class ConnectionsStore {

    enum LoadState: Equatable {
        case idle
        case downloading
        case loading
        case ready
        case unavailable(String)
    }

    private(set) var state: LoadState = .idle
    private(set) var downloadProgress: Double = 0

    var isReady: Bool { state == .ready }

    let engine = RoutingEngine()

    // MARK: - Bootstrap

    func load(config: OperatorConfig, routes: [APIRoute]) async {
        guard state == .idle else { return }

        // Try disk cache first
        if let cached = loadFromDisk(config: config) {
            state = .loading
            do {
                try await engine.load(connectionsData: cached, allRoutes: routes)
                state = .ready
                // Background refresh
                Task { await refreshFromCDN(config: config, routes: routes) }
            } catch {
                state = .unavailable(error.localizedDescription)
            }
            return
        }

        // Download from CDN
        await downloadAndLoad(config: config, routes: routes)
    }

    // MARK: - Query

    func query(originId: String, destId: String, after: Date) async -> [Journey] {
        await engine.query(originId: originId, destId: destId, afterDate: after)
    }

    func queryArriveBy(originId: String, destId: String, before: Date) async -> [Journey] {
        await engine.queryArriveBy(originId: originId, destId: destId, beforeDate: before)
    }

    // MARK: - Download

    private func downloadAndLoad(config: OperatorConfig, routes: [APIRoute]) async {
        guard let url = cdnURL(config: config) else {
            state = .unavailable("No CDN URL configured")
            return
        }

        state = .downloading
        downloadProgress = 0

        do {
            let data = try await download(from: url)
            saveToDisk(data: data, config: config)
            state = .loading
            try await engine.load(connectionsData: data, allRoutes: routes)
            state = .ready
        } catch {
            state = .unavailable(error.localizedDescription)
        }
    }

    private func refreshFromCDN(config: OperatorConfig, routes: [APIRoute]) async {
        guard let url = cdnURL(config: config) else { return }
        guard let data = try? await download(from: url) else { return }
        saveToDisk(data: data, config: config)
        // Only swap if the download succeeded and data differs
        let currentReady = await engine.isReady
        if currentReady {
            try? await engine.load(connectionsData: data, allRoutes: routes)
        }
    }

    private func download(from url: URL) async throws -> Data {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        let session = URLSession(configuration: config)

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        return data
    }

    // MARK: - Disk Cache

    private func cacheURL(config: OperatorConfig) -> URL? {
        guard let dir = FileManager.default.urls(
            for: .applicationSupportDirectory, in: .userDomainMask
        ).first else { return nil }
        let folder = dir.appendingPathComponent("TransitKit/Connections", isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder.appendingPathComponent("\(config.id)_connections.json.zlib")
    }

    private func loadFromDisk(config: OperatorConfig) -> Data? {
        guard let url = cacheURL(config: config) else { return nil }
        return try? Data(contentsOf: url)
    }

    private func saveToDisk(data: Data, config: OperatorConfig) {
        guard let url = cacheURL(config: config) else { return }
        try? data.write(to: url, options: .atomic)
    }

    private func cdnURL(config: OperatorConfig) -> URL? {
        guard let base = config.cdnUrl else { return nil }
        let urlString = "\(base.trimmingCharacters(in: .init(charactersIn: "/")))/\(config.id)/connections.json.zlib"
        return URL(string: urlString)
    }
}
