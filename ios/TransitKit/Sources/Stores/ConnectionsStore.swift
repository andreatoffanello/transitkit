import Foundation

/// Wrapper sottile sul `RemoteRoutingProvider` (`api.transitkit.app/v1/route`,
/// MOTIS upstream). Mantiene il nome storico `ConnectionsStore` per non
/// rompere i chiamanti, ma non c'è più alcun CSA in-app / connections.json /
/// disk cache — tutte le query vanno al backend remoto come fa Movete.
///
/// API key + base URL sono in `TransitKitSecrets.swift` (gitignored).
@MainActor
@Observable
final class ConnectionsStore {

    enum LoadState: Equatable {
        case idle
        case ready
        case unavailable(String)
    }

    private(set) var state: LoadState = .idle

    var isReady: Bool { state == .ready }

    private let provider = RemoteRoutingProvider()

    // MARK: - Bootstrap

    /// Indicizza le fermate nel `RemoteRoutingProvider` per la risoluzione
    /// MOTIS → domain, poi si mette in `.ready`. Pattern equivalente al
    /// `AppState` di Movete che chiama `remoteRouting.configure(stops:routes:)`
    /// dopo il load del DataProvider.
    func load(stops: [ResolvedStop]) async {
        await provider.configure(stops: stops)
        state = .ready
    }

    // MARK: - Query

    func query(origin: PlannerStop, destination: PlannerStop, after: Date) async -> [Journey] {
        await provider.query(origin: origin, destination: destination, after: after)
    }

    func queryArriveBy(origin: PlannerStop, destination: PlannerStop, before: Date) async -> [Journey] {
        await provider.queryArriveBy(origin: origin, destination: destination, before: before)
    }
}
