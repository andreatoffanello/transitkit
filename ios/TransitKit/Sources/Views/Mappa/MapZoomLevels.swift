import Foundation

// MARK: - MapZoomTier
//
// Tier discreti di zoom mappa. Single source of truth per:
//  - quale shape renderizzare per le fermate (dot piccolo vs pin pieno)
//  - quali embellishment per i veicoli (badge route name)
//  - parità con Android Mapbox (vedi MapZoomLevels.kt — soglie convertite
//    da zoom-level Mapbox a latitudeDelta MapKit tramite la regola pratica
//    latDelta ≈ 360 / (2^zoom · cos(lat))).
//
// I bucket sono volutamente pochi (3) per evitare flicker su transizioni.

enum MapZoomTier: Hashable {
    /// Vista cittadina, dot piccolo.
    case city
    /// Mid zoom, dot più grande.
    case neighborhood
    /// Street, pin pieno con icona.
    case street

    init(latitudeDelta: Double) {
        switch latitudeDelta {
        case ..<MapZoomLevels.streetMaxDelta:        self = .street
        case ..<MapZoomLevels.neighborhoodMaxDelta:  self = .neighborhood
        default:                                     self = .city
        }
    }
}

// MARK: - MapZoomLevel
//
// Bucket più granulare dell'ambient zoom, usato per scegliere la dimensione
// di una singola shape di marker (es. dot 7dp vs 11dp). Triplica i tier sui
// confini di transizione visiva.

enum MapZoomLevel: Hashable {
    case far    // overview — dots only
    case medium // neighborhood — dot più grande
    case close  // street — pin pieno

    init(latitudeDelta: Double) {
        switch latitudeDelta {
        case ..<MapZoomLevels.closeMaxDelta:   self = .close
        case ..<MapZoomLevels.mediumMaxDelta:  self = .medium
        default:                               self = .far
        }
    }
}

// MARK: - MapZoomLevels
//
// Single source of truth per soglie e zoom preferenziali della mappa MapKit.
// Esprime tutto in `MKCoordinateRegion.latitudeDelta`; le soglie sono allineate
// alle soglie Mapbox Android (`MapZoomLevels.kt`) — quando aggiorni una,
// aggiorna anche l'altra.

enum MapZoomLevels {

    // ── Soglie tier (confine SUPERIORE — strict less than) ───────────────────
    //
    // Allineate a Android: cityMaxZoom = 12.0, neighborhoodMaxZoom = 14.0.
    // Su MapKit (~lat 41°N media) le soglie diventano:
    //   zoom 12 ≈ latDelta 0.088
    //   zoom 14 ≈ latDelta 0.022

    /// latDelta ≥ questo valore → `.city` (zoom Mapbox < 12).
    static let neighborhoodMaxDelta = 0.088

    /// latDelta < questo valore → `.street` (zoom Mapbox ≥ 14).
    static let streetMaxDelta = 0.022

    // ── Soglie MapZoomLevel (più granulari per scelta dimensione marker) ────

    /// latDelta < questo → `.close`.
    static let closeMaxDelta = 0.008

    /// latDelta < questo → `.medium`. ≥ → `.far`.
    static let mediumMaxDelta = 0.02

    // ── Preferred zoom (espressi come latDelta) per use-case ─────────────────

    /// Zoom default all'apertura della Mappa quando NON c'è posizione utente.
    static let cityDefaultEntry = 0.06

    /// Zoom default all'apertura della Mappa quando c'è posizione utente.
    static let userDefaultEntry = 0.012

    /// Zoom per follow veicolo (auto-focus quando seleziono un veicolo).
    static let vehicleFocus = 0.006

    /// Zoom per focus su singola fermata (auto-zoom su tap se zoom inferiore).
    static let stopFocus = 0.008

    /// Zoom overview di una linea intera (clamping per "Vedi linea").
    static let lineOverview = 0.08

    // ── Cap di zoom-out (app cittadina) ──────────────────────────────────────

    /// Distanza camera massima consentita (zoom-out cap). ≈ zoom Mapbox 8 —
    /// allineato a `MapZoomLevels.minUserZoom` su Android; quando aggiorni
    /// uno, aggiorna l'altro.
    static let maxCameraDistance: Double = 250_000

    // ── Pitch (3D) ───────────────────────────────────────────────────────────
    //
    // Il 3D è una feature dello zoom vicino, non una modalità globale: tilt e
    // panning prospettico hanno senso solo quando gli edifici sono estrusi.
    // Sopra questa soglia (= tier `.close`) la camera torna piatta, così la
    // vista d'insieme resta leggibile. La preferenza dell'utente (`is3D`) NON
    // viene persa: è intent, e il pitch effettivo è `intent && zoom lo consente`.
    //
    // Allineato ad Android `MapZoomLevels.pitch3DMinZoom = 15.5`
    // (latDelta 0.009 ≈ zoom Mapbox 15.5); quando aggiorni uno, aggiorna l'altro.

    /// latDelta < questo valore → il 3D è consentito. ≥ → camera piatta.
    static let pitch3DMaxDelta = 0.009

    /// Zoom (latDelta) a cui portarsi quando l'utente chiede il 3D da troppo
    /// lontano: entrare in 3D senza avvicinarsi darebbe una mappa piatta inclinata.
    static let pitch3DEntryDelta = 0.006

    /// Angolo di tilt delle mappe interattive. Uno solo, ovunque.
    static let pitch3D: Double = 45

    /// Angolo di tilt delle hero map non interattive (header dettaglio fermata):
    /// più cinematico perché non ci si naviga dentro. Parità con Android.
    static let pitch3DHero: Double = 50

    /// Pitch effettivo per uno span dato l'intent dell'utente.
    /// Unico punto in cui si decide se la camera è inclinata o piatta.
    static func pitch(for latitudeDelta: Double, intent3D: Bool, angle: Double = pitch3D) -> Double {
        (intent3D && latitudeDelta < pitch3DMaxDelta) ? angle : 0
    }

    /// True se a questo span il 3D è consentito (per etichette e stato dei controlli).
    static func allows3D(latitudeDelta: Double) -> Bool {
        latitudeDelta < pitch3DMaxDelta
    }
}
