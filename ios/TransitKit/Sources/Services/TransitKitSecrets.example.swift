import Foundation

/// Configurazione TransitKit Routing API (api.transitkit.app).
///
/// COME USARE:
/// 1. Copia questo file come `TransitKitSecrets.swift` nella stessa cartella
///    (gitignored — non finirà nel repo).
/// 2. Sostituisci i due placeholder con le chiavi reali da Bitwarden,
///    voce "TransitKit" → TransitKit dev + TransitKit prod.
/// 3. Apri Xcode (o `xcodegen generate` se cambia struttura) e verifica che
///    `TransitKitSecrets.swift` sia incluso nel target `TransitKit`.
///    NON aggiungere questo `.example.swift` al target (duplicate symbol).
///
/// Dev/prod si selezionano automaticamente via `#if DEBUG`:
/// build Debug → dev key, Archive/Release (App Store) → prod key.
enum TransitKitSecrets {
    #if DEBUG
    static let apiKey = "REPLACE_WITH_TRANSITKIT_DEV_API_KEY"
    #else
    static let apiKey = "REPLACE_WITH_TRANSITKIT_PROD_API_KEY"
    #endif

    static let baseURL = "https://api.transitkit.app"
}
