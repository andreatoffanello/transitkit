import Foundation

// MARK: - API Client

/// Fetches data from the TransitKit API (deployed Vercel Functions).
/// All methods are async and throw on network or decode failure.
/// Caching is handled by `ScheduleLoader` — `APIClient` is a pure HTTP layer.
actor APIClient {
    private let baseURL: URL
    private let session: URLSession

    init(apiUrl: String, session: URLSession = .shared) throws {
        guard let url = URL(string: apiUrl) else {
            throw APIError.invalidBaseURL(apiUrl)
        }
        self.baseURL = url
        self.session = session
    }

    // MARK: - /stops/{id}/departures

    func fetchDepartures(
        stopId: String,
        date: Date = Date(),
        limit: Int = 50
    ) async throws -> [APIDeparture] {
        var components = URLComponents(
            url: baseURL.appendingPathComponent("stops/\(stopId)/departures"),
            resolvingAgainstBaseURL: false
        )!
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        components.queryItems = [
            URLQueryItem(name: "date",  value: formatter.string(from: date)),
            URLQueryItem(name: "limit", value: String(limit)),
        ]
        guard let url = components.url else {
            throw APIError.invalidURL("stops/\(stopId)/departures")
        }
        return try await fetch([APIDeparture].self, from: url)
    }

    // MARK: - /trips/{id}

    func fetchTrip(tripId: String) async throws -> TripDetail {
        let url = baseURL.appendingPathComponent("trips/\(tripId)")
        return try await fetch(TripDetail.self, from: url)
    }

    // MARK: - Private

    private func fetch<T: Decodable>(_ type: T.Type, from url: URL) async throws -> T {
        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        switch http.statusCode {
        case 200:
            do {
                return try JSONDecoder().decode(type, from: data)
            } catch {
                throw APIError.decodingFailed(url.path, error)
            }
        case 404:
            throw APIError.notFound(url.path)
        default:
            throw APIError.httpError(http.statusCode, url.path)
        }
    }

    // MARK: - Errors

    enum APIError: LocalizedError {
        case invalidBaseURL(String)
        case invalidURL(String)
        case invalidResponse
        case notFound(String)
        case httpError(Int, String)
        case decodingFailed(String, Error)

        var errorDescription: String? {
            switch self {
            case .invalidBaseURL(let u):        "Invalid API base URL: \(u)"
            case .invalidURL(let path):          "Could not build URL for path: \(path)"
            case .invalidResponse:               "Response was not an HTTP response"
            case .notFound(let path):            "Not found: \(path)"
            case .httpError(let code, let path): "HTTP \(code) for \(path)"
            case .decodingFailed(let path, let err): "Decoding failed for \(path): \(err.localizedDescription)"
            }
        }
    }
}
