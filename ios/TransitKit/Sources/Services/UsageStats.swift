import Foundation
import UIKit
import os

// MARK: - UsageStats
//
// Statistiche d'uso ANONIME con interruttore in Impostazioni (contratto v1 di
// `collect.andreatoffanello.com`, repo `stats`). Versione ridotta del client
// DoVe: solo eventi aggregati per giorno/ora, nessuna entità.
//
// Tre regole:
//  - Attive SOLO per gli operatori in `allowlist` (decisione a build-time: per
//    gli altri non c'è client, né interruttore, né invio). Oggi: AppalRider.
//  - Mai invio in build DEBUG: gli eventi si loggano (subsystem
//    `com.transitkit.stats`) e restano in coda.
//  - Spento = coda e install id cancellati; riacceso = install id NUOVO.
//
// Eventi ammessi (allowlist del server, `COMMON_EVENTS` in stats/collect/src/apps.ts):
//  app_open {source, cold}, session_end {duration}, screen {screen, from},
//  settings {language, color_scheme}.

@MainActor
enum UsageStats {
    /// Operatori per cui le statistiche esistono. Aggiungerne uno = decisione
    /// di prodotto + registrarlo in stats/collect/src/apps.ts + etichette store.
    static let allowlist: Set<String> = ["appalcart"]

    private static let enabledKey = "usage_stats_enabled"
    private static let settingsDayKey = "usage_stats_settings_last_day"
    private static var operatorId: String?
    private static var timeZone: TimeZone = .current
    private static var epoch = 0
    private static var sessionStart: Date?
    private static var hasOpened = false
    private static var isForeground = false
    private static var lastScreen = "none"
    /// Schermata visibile all'apertura (aggiornata da ContentView a ogni cambio tab).
    static var initialScreen = "home"

    /// Chiamata una volta, a config caricata.
    static func configure(operatorId: String, timeZoneId: String) {
        self.operatorId = operatorId
        self.timeZone = TimeZone(identifier: timeZoneId) ?? .current
        // La scena diventa attiva PRIMA che la config sia caricata: la sessione
        // parte qui, non in `appDidBecomeActive`.
        startSessionIfPossible()
    }

    /// L'operatore corrente partecipa alle statistiche (mostra l'interruttore).
    static var isAvailable: Bool {
        guard let operatorId else { return false }
        return allowlist.contains(operatorId)
    }

    /// Default ON: chiave assente = mai toccata dall'utente.
    static var isEnabled: Bool {
        isAvailable && (UserDefaults.standard.object(forKey: enabledKey) as? Bool ?? true)
    }

    static func setEnabled(_ on: Bool) {
        guard isAvailable else { return }
        UserDefaults.standard.set(on, forKey: enabledKey)
        let epoch: Int
        if on {
            epoch = self.epoch
        } else {
            self.epoch += 1
            epoch = self.epoch
            sessionStart = nil
        }
        Task { await UsageStatsClient.shared.setEnabled(on, epoch: epoch) }
        if on { startSessionIfPossible() }
    }

    // MARK: Ciclo di vita (scenePhase)

    static func appDidBecomeActive() {
        isForeground = true
        startSessionIfPossible()
    }

    /// Anche riaccendendo l'interruttore dalle Impostazioni (app in primo piano).
    private static func startSessionIfPossible() {
        guard isForeground, isEnabled, !isRunningTests, sessionStart == nil else { return }
        sessionStart = Date()
        let cold = !hasOpened
        hasOpened = true
        record(name: "app_open", props: ["source": .s("launcher"), "cold": .b(cold)])
        recordSettingsOnceADay()
        if lastScreen == "none" { screen(initialScreen) }
        Task { await UsageStatsClient.shared.flushIfNeeded() }
    }

    static func appDidEnterBackground() {
        isForeground = false
        lastScreen = "none"
        guard isEnabled, !isRunningTests, let started = sessionStart else { return }
        sessionStart = nil
        let seconds = Date().timeIntervalSince(started)
        let bucket: String
        switch seconds {
        case ..<30: bucket = "lt30s"
        case ..<120: bucket = "30s_2m"
        case ..<600: bucket = "2m_10m"
        default: bucket = "gt10m"
        }
        record(name: "session_end", props: ["duration": .s(bucket)])
        // L'invio gira sotto un background task: senza, iOS sospende il
        // processo a richiesta partita.
        var taskId = UIBackgroundTaskIdentifier.invalid
        taskId = UIApplication.shared.beginBackgroundTask(withName: "usage-stats-flush") {
            UIApplication.shared.endBackgroundTask(taskId)
        }
        Task {
            await UsageStatsClient.shared.flush()
            UIApplication.shared.endBackgroundTask(taskId)
        }
    }

    /// Nomi schermata snake_case stabili, identici su Android.
    static func screen(_ name: String) {
        guard isEnabled, !isRunningTests, name != lastScreen else { return }
        let from = lastScreen
        lastScreen = name
        record(name: "screen", props: ["screen": .s(name), "from": .s(from)])
    }

    // MARK: Interno

    private static var isRunningTests: Bool {
        ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil
    }

    private static func record(name: String, props: [String: UsageStatsValue]) {
        guard let operatorId, allowlist.contains(operatorId) else { return }
        let now = Date()
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let c = cal.dateComponents([.year, .month, .day, .hour], from: now)
        let day = String(format: "%04d-%02d-%02d", c.year ?? 1970, c.month ?? 1, c.day ?? 1)
        let row = UsageRow(e: name, d: day, h: c.hour ?? 0, p: props, n: 1)
        let epoch = self.epoch
        let lang = currentLanguage
        Task { await UsageStatsClient.shared.record(row, epoch: epoch, operatorId: operatorId, lang: lang) }
    }

    private static func recordSettingsOnceADay() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let c = cal.dateComponents([.year, .month, .day], from: Date())
        let today = String(format: "%04d-%02d-%02d", c.year ?? 1970, c.month ?? 1, c.day ?? 1)
        guard UserDefaults.standard.string(forKey: settingsDayKey) != today else { return }
        UserDefaults.standard.set(today, forKey: settingsDayKey)
        // Il motore non ha un selettore di tema: l'aspetto segue sempre il sistema.
        record(name: "settings", props: ["language": .s(currentLanguage), "color_scheme": .s("system")])
    }

    /// Lingua effettiva dell'interfaccia (en | es | it).
    static var currentLanguage: String {
        let code = LocalizationManager.shared.language.localeCode
            ?? Locale.preferredLanguages.first
            ?? "en"
        let base = code.split(separator: "-").first.map(String.init)?.lowercased() ?? "en"
        return ["en", "es", "it"].contains(base) ? base : "en"
    }
}

// MARK: - Modello

enum UsageStatsValue: Codable, Equatable, Sendable {
    case s(String)
    case b(Bool)

    func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        switch self {
        case .s(let v): try c.encode(v)
        case .b(let v): try c.encode(v)
        }
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if let b = try? c.decode(Bool.self) { self = .b(b) } else { self = .s(try c.decode(String.self)) }
    }
}

/// Riga di `events[]`: `{ e, d, h, p, n }`.
struct UsageRow: Codable, Equatable, Sendable {
    let e: String
    let d: String
    let h: Int
    var p: [String: UsageStatsValue]
    var n: Int
}

private struct UsageState: Codable {
    var install: String?
    var rows: [UsageRow] = []
    var pendingId: String?
    var pendingRows: [UsageRow] = []
    var operatorId: String?
    var lang: String = "en"
}

// MARK: - Client (actor)
//
// WHY `generation` ricontrollata dopo OGNI await: un actor è rientrante, e
// mentre un flush aspetta la rete l'utente può spegnere le statistiche. Senza
// la guardia, al ritorno il flush riscriverebbe la coda appena cancellata.

actor UsageStatsClient {
    static let shared = UsageStatsClient()

    private static let endpoint = URL(string: "https://collect.andreatoffanello.com/v1/events")!
    private static let logger = Logger(subsystem: "com.transitkit.stats", category: "UsageStats")
    private let fileURL: URL
    private let session: URLSession

    private var state = UsageState()
    private var loaded = false
    private var enabled = true
    private var generation = 0
    private var minEpoch = 0
    private var flushing = false

    init() {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        fileURL = dir.appendingPathComponent("usage-stats.json")
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 15
        session = URLSession(configuration: config)
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        enabled = UserDefaults.standard.object(forKey: "usage_stats_enabled") as? Bool ?? true
        guard enabled else { try? FileManager.default.removeItem(at: fileURL); return }
        if let data = try? Data(contentsOf: fileURL), let s = try? JSONDecoder().decode(UsageState.self, from: data) {
            state = s
        }
    }

    private func save() {
        guard enabled else { return }
        if let data = try? JSONEncoder().encode(state) { try? data.write(to: fileURL, options: .atomic) }
    }

    func setEnabled(_ on: Bool, epoch: Int) {
        minEpoch = max(minEpoch, epoch)
        load()
        enabled = on
        if !on {
            generation &+= 1
            state = UsageState()
            try? FileManager.default.removeItem(at: fileURL)
        }
    }

    func record(_ row: UsageRow, epoch: Int, operatorId: String, lang: String) {
        load()
        guard enabled, epoch >= minEpoch else { return }
        state.operatorId = operatorId
        state.lang = lang
        if let i = state.rows.firstIndex(where: { $0.e == row.e && $0.d == row.d && $0.h == row.h && $0.p == row.p }) {
            state.rows[i].n += row.n
        } else {
            state.rows.append(row)
        }
        if state.rows.count > 500 { state.rows.removeFirst(state.rows.count - 500) }
        Self.logger.debug("evento \(row.e, privacy: .public) \(String(describing: row.p), privacy: .public)")
        save()
    }

    func flushIfNeeded() async {
        load()
        if state.pendingId != nil || state.rows.count > 50 { await flush() }
    }

    func flush() async {
        load()
        guard enabled, !flushing, let op = state.operatorId else { return }
        flushing = true
        defer { flushing = false }
        let gen = generation
        for _ in 0..<5 {
            guard enabled, gen == generation else { return }
            if state.pendingId == nil {
                guard !state.rows.isEmpty else { return }
                state.pendingRows = Array(state.rows.prefix(400))
                state.rows.removeFirst(state.pendingRows.count)
                state.pendingId = UUID().uuidString.lowercased()
                save()
            }
            if state.install == nil { state.install = UUID().uuidString.lowercased(); save() }
            guard let batchId = state.pendingId, let install = state.install else { return }
            let payload = Payload(batch: batchId, install: install, app: op,
                                  version: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0",
                                  os: String(ProcessInfo.processInfo.operatingSystemVersion.majorVersion),
                                  lang: state.lang, events: state.pendingRows)
            guard let body = try? JSONEncoder().encode(payload) else { state.pendingId = nil; state.pendingRows = []; return }

            #if DEBUG
            // Mai invio da DEBUG: si logga il JSON conforme e basta.
            Self.logger.debug("[DEBUG, non inviato] \(String(data: body, encoding: .utf8) ?? "", privacy: .public)")
            return
            #else
            var request = URLRequest(url: Self.endpoint)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = body
            let status: Int
            do {
                let (_, response) = try await session.data(for: request)
                status = (response as? HTTPURLResponse)?.statusCode ?? 0
            } catch {
                return // rete: il batch resta con lo stesso id per il retry
            }
            guard enabled, gen == generation else { return }
            switch status {
            case 200...299, 400, 413:
                // Inviato, oppure rifiutato come malformato: in entrambi i casi si scarta.
                state.pendingId = nil
                state.pendingRows = []
                save()
            default:
                return // 429/5xx: ritenta più tardi
            }
            #endif
        }
    }

    private struct Payload: Encodable {
        let v = 1
        let batch: String
        let install: String
        let platform = "ios"
        let app: String
        let version: String
        let os: String
        let lang: String
        let events: [UsageRow]
    }
}
