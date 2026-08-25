import Foundation
import Observation

// MARK: - AppLanguage

/// Lingue selezionabili dall'utente in Impostazioni.
/// Aggiungere un caso qui è l'unico step necessario lato UI: la lista della
/// picker, la persistenza e il bundle di lookup ne discendono. Va accompagnato
/// dal relativo `.lproj` (cioè da una lingua in `Localizable.xcstrings`).
enum AppLanguage: String, CaseIterable, Identifiable, Sendable {
    case system
    case english
    case spanish
    case italian

    var id: String { rawValue }

    /// Codice della localizzazione (nome dell'`.lproj` nel bundle).
    /// `nil` = segui la lingua del dispositivo.
    var localeCode: String? {
        switch self {
        case .system:  nil
        case .english: "en"
        case .spanish: "es-419"
        case .italian: "it"
        }
    }

    /// Endonimo: il nome della lingua *nella lingua stessa*. Non si localizza —
    /// un utente che cerca la propria lingua in una lista la riconosce scritta
    /// come la scrive lui, non tradotta nella lingua corrente dell'app.
    var endonym: String {
        switch self {
        case .system:  L("settings_language_system")
        case .english: "English"
        case .spanish: "Español"
        case .italian: "Italiano"
        }
    }

    /// Match del codice scritto in `AppleLanguages` (nostro o di Impostazioni
    /// iOS) sul caso corrispondente. Il confronto è sul language code perché
    /// il sistema può scrivere varianti regionali ("en-US", "es-419", "it-IT").
    static func matching(code: String) -> AppLanguage? {
        let language = code.split(separator: "-").first.map(String.init)?.lowercased()
        return allCases.first { candidate in
            guard let candidateCode = candidate.localeCode else { return false }
            return candidateCode.split(separator: "-").first.map(String.init)?.lowercased() == language
        }
    }
}

// MARK: - Lookup

/// Unico punto di lookup delle stringhe dell'app. Sostituisce
/// `String(localized:)`/`NSLocalizedString`, che risolvono su `Bundle.main` —
/// bloccato sulla lingua decisa dall'OS al lancio e insensibile sia al
/// re-classing di `Bundle.main` sia a `\.locale` nell'environment. Passare il
/// bundle esplicitamente è l'unico modo per cambiare lingua senza riavviare.
func L(_ key: String.LocalizationValue) -> String {
    String(localized: key, bundle: LocalizationManager.stringsBundle)
}

/// Variante per le stringhe usate come formato (`String(format:)`) e per i
/// plurali risolti via `.stringsdict`.
func L(_ key: String, comment: String) -> String {
    NSLocalizedString(key, bundle: LocalizationManager.stringsBundle, comment: comment)
}

// MARK: - LocalizationManager

/// Lingua dell'interfaccia scelta dall'utente.
///
/// La fonte di verità è `AppleLanguages` nel *dominio persistente dell'app* —
/// la stessa chiave che iOS scrive quando l'utente sceglie la lingua da
/// Impostazioni → App. Usarla al posto di una chiave nostra tiene allineate le
/// due strade: qualunque sia il punto d'ingresso, la selezione è una sola.
/// Va letta dal dominio dell'app e non da `UserDefaults.standard`, che
/// altrimenti ricadrebbe su `NSGlobalDomain` (dove `AppleLanguages` esiste
/// sempre) rendendo indistinguibile "sistema" da "inglese".
@Observable
@MainActor
final class LocalizationManager {
    static let shared = LocalizationManager()

    /// Bundle su cui `L(_:)` risolve le stringhe. Letto da thread arbitrari
    /// (i lookup non sono confinati al main), scritto solo da `apply(_:)`,
    /// che gira sul main actor.
    nonisolated(unsafe) private(set) static var stringsBundle: Bundle = .main

    /// Locale per i `DateFormatter`: **regione del dispositivo + lingua scelta
    /// dall'utente**. Le due cose vengono da impostazioni diverse e vanno
    /// tenute separate: il ciclo 12/24h, l'ordine dei campi e il primo giorno
    /// della settimana sono preferenze di regione (un americano che sceglie
    /// l'italiano vuole ancora le 4:45 PM, non le 16:45), mentre i nomi di
    /// giorni e mesi e i simboli AM/PM sono lingua.
    ///
    /// Senza questo i formatter userebbero `Locale.current`, che segue la
    /// lingua di *sistema*: la data del periodo di validità di un avviso
    /// usciva in italiano su un'app messa in inglese.
    nonisolated(unsafe) private(set) static var formattingLocale: Locale = .autoupdatingCurrent

    private(set) var language: AppLanguage

    private init() {
        language = Self.storedLanguage()
    }

    /// Da chiamare una volta sola, prima che venga renderizzata la prima view.
    func bootstrap() {
        apply(language)
    }

    func select(_ newLanguage: AppLanguage) {
        guard newLanguage != language else { return }
        persist(newLanguage)
        language = newLanguage
        apply(newLanguage)
    }

    // MARK: - Private

    private func apply(_ newLanguage: AppLanguage) {
        guard let code = resolvedCode(for: newLanguage) else {
            Self.stringsBundle = .main
            Self.formattingLocale = .autoupdatingCurrent
            return
        }
        Self.formattingLocale = Self.locale(forLanguage: code)
        guard
            let path = Self.lprojPath(for: code),
            let bundle = Bundle(path: path)
        else {
            Self.stringsBundle = .main
            return
        }
        Self.stringsBundle = bundle
    }

    /// Regione corrente + lingua `code`. `Locale.Components` è l'unico modo di
    /// cambiare solo la lingua senza portarsi dietro anche le convenzioni
    /// numeriche e orarie del paese di quella lingua.
    private static func locale(forLanguage code: String) -> Locale {
        var components = Locale.Components(locale: .autoupdatingCurrent)
        // SOLO il language code. Sostituire l'intero `languageComponents`
        // azzererebbe anche la region della lingua e il locale collasserebbe
        // su "it" puro: su un iPhone americano messo in italiano gli orari
        // diventavano 16:45 invece di 4:45 PM. Il ciclo 12/24h lo decide la
        // regione, che qui resta quella del dispositivo.
        let base = code.split(separator: "-").first.map(String.init) ?? code
        components.languageComponents.languageCode = Locale.LanguageCode(base)
        return Locale(components: components)
    }

    /// Anche "sistema" viene risolto a un `.lproj` concreto invece di ricadere
    /// su `Bundle.main`: la localizzazione di `Bundle.main` è fissata al lancio,
    /// quindi tornare a "sistema" dopo aver scelto l'italiano lascerebbe l'app
    /// in italiano fino al riavvio. `Locale.preferredLanguages` è invece già
    /// aggiornato, perché `persist` ha appena rimosso l'override dal dominio
    /// dell'app e la lettura ricade sulle lingue del dispositivo.
    private func resolvedCode(for language: AppLanguage) -> String? {
        if let code = language.localeCode { return code }
        return Bundle.preferredLocalizations(
            from: Bundle.main.localizations.filter { $0 != "Base" },
            forPreferences: Locale.preferredLanguages
        ).first
    }

    private func persist(_ newLanguage: AppLanguage) {
        let defaults = UserDefaults.standard
        if let code = newLanguage.localeCode {
            // Scrivere `AppleLanguages` allinea anche le API di sistema
            // (date, numeri, nomi dei giorni) alla lingua scelta dal prossimo
            // lancio in poi — non solo le stringhe dell'app.
            defaults.set([code], forKey: "AppleLanguages")
        } else {
            defaults.removeObject(forKey: "AppleLanguages")
        }
        defaults.synchronize()
    }

    private static func storedLanguage() -> AppLanguage {
        guard
            let bundleId = Bundle.main.bundleIdentifier,
            let domain = UserDefaults.standard.persistentDomain(forName: bundleId),
            let codes = domain["AppleLanguages"] as? [String],
            let code = codes.first
        else { return .system }
        return AppLanguage.matching(code: code) ?? .system
    }

    /// `es-419` è una localizzazione regionale: il suo `.lproj` ha il nome
    /// completo. Il fallback sul solo language code copre il caso in cui il
    /// bundle contenga la variante generica.
    private static func lprojPath(for code: String) -> String? {
        if let path = Bundle.main.path(forResource: code, ofType: "lproj") {
            return path
        }
        guard let base = code.split(separator: "-").first else { return nil }
        return Bundle.main.path(forResource: String(base), ofType: "lproj")
    }
}
