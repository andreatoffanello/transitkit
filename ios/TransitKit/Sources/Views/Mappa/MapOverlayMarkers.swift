import SwiftUI
import MapKit

// MARK: - Map overlay markers (pattern Movete/DoVe)
//
// SwiftUI `Map` non espone `MKAnnotationView.zPriority` e non rispetta in modo
// affidabile il declaration-order tra `Annotation`/`UserAnnotation`: in 3D le
// annotazioni vengono ordinate per profondità (più vicine alla camera davanti),
// quindi un mezzo può finire SOTTO una fermata, e `UserAnnotation()` viene resa
// nera e proiettata sul piano terra.
//
// Soluzione (identica a Movete `MapTab` e DoVe `WaterBusListView`): mezzi e puck
// NON sono `Annotation` ma **overlay SwiftUI** sopra la `Map`, posizionati via
// `MapProxy.convert(coord, to: .local)`. Così stanno SEMPRE sopra a tutto per
// costruzione e restano billboard (in piedi) anche in 3D. Le fermate restano
// `Annotation` dentro la mappa (layer base).

/// Punto blu posizione utente — overlay SwiftUI (mai `UserAnnotation`).
struct UserLocationDot: View {
    var body: some View {
        Circle()
            .fill(Color(red: 0.0, green: 0.478, blue: 1.0))   // systemBlue esplicito
            .frame(width: 16, height: 16)
            .overlay(Circle().stroke(.white, lineWidth: 3))
            .shadow(color: .black.opacity(0.18), radius: 2, y: 1)
            .accessibilityLabel("La tua posizione")
    }
}

/// Marker mezzo con glide morbido tra due fix RT, reso come overlay via
/// `proxy.convert`. I `VehiclePosition` arrivano ~ogni 5-15s: assegnare la
/// coordinata diretta fa "teletrasportare" il marker; qui ogni mezzo interpola
/// la propria coordinata mostrata con un glide LINEARE breve.
///
/// WHY child view con @State: la coordinata animata vive per-mezzo (identità =
/// vehicle.id nel ForEach) e persiste tra i re-render per-frame della camera.
/// `proxy.convert` rieseguito a ogni step d'animazione → marker liscio;
/// rieseguito a ogni frame camera → segue pan/zoom anche da fermo.
struct AnimatedVehicleMarker: View {
    let vehicle: GtfsRtVehicle
    let proxy: MapProxy
    let routeColor: String?
    let transitType: TransitType
    let route: APIRoute?
    let tier: MapZoomTier
    let isSelected: Bool
    let onTap: () -> Void

    /// Glide lineare: velocità costante, niente accelerazione/frenata innaturale.
    private static let glide: TimeInterval = 0.9

    @State private var lat: Double
    @State private var lng: Double

    init(
        vehicle: GtfsRtVehicle,
        proxy: MapProxy,
        routeColor: String?,
        transitType: TransitType,
        route: APIRoute?,
        tier: MapZoomTier,
        isSelected: Bool = false,
        onTap: @escaping () -> Void = {}
    ) {
        self.vehicle = vehicle
        self.proxy = proxy
        self.routeColor = routeColor
        self.transitType = transitType
        self.route = route
        self.tier = tier
        self.isSelected = isSelected
        self.onTap = onTap
        // Seed alla coordinata corrente: il primo render NON anima.
        _lat = State(initialValue: Double(vehicle.latitude))
        _lng = State(initialValue: Double(vehicle.longitude))
    }

    var body: some View {
        let coord = CLLocationCoordinate2D(latitude: lat, longitude: lng)
        // Group sempre presente (anche off-screen → point nil): tiene vivo
        // l'onChange così il glide parte anche per i mezzi non visibili.
        Group {
            // Guard punto finito: in 3D pitchato `proxy.convert` può restituire
            // coordinate NaN/infinite (orizzonte/dietro la camera) → `.position`
            // con NaN fa crashare SwiftUI (EXC_BAD_ACCESS). Skip quei frame.
            if let point = proxy.convert(coord, to: .local),
               point.x.isFinite, point.y.isFinite {
                VehicleAnnotationView(
                    vehicle: vehicle,
                    routeColor: routeColor,
                    transitType: transitType,
                    tier: tier,
                    route: route,
                    isSelected: isSelected
                )
                .onTapGesture(perform: onTap)
                .position(point)
            }
        }
        .onChange(of: [Double(vehicle.latitude), Double(vehicle.longitude)]) { _, _ in
            withAnimation(.linear(duration: Self.glide)) {
                lat = Double(vehicle.latitude)
                lng = Double(vehicle.longitude)
            }
        }
    }
}
