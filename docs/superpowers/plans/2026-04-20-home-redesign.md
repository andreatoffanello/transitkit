# Home Screen Redesign — Implementation Plan (v2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rimuovere l'hero card verde, far emergere lo sfondo dell'operatore (mappa cartografica) come elemento visivo principale, introdurre un onboarding per i permessi di localizzazione, e mantenere consistenza strutturale tra iOS e Android.

**Architecture:**
- Sfondo operatore visibile a ~15-20% opacità (vs 4% attuale) con spotlight Lissajous animato; card `adaptiveGlass`/Material surface fungono da "gradient mask" locale per la leggibilità; footer gradient fade per il disclaimer.
- Onboarding location primer screen mostrato al primo avvio; **fallback chip "Attiva posizione"** in Home quando lo stato è `notDetermined` (utente che ha skippato onboarding o non ha mai visto il prompt); Settings > Privacy deep-link per chi ha negato.
- Preferiti in **ordine manuale** (come salvati dall'utente), niente urgency sort che fa saltare le card; enfasi visuale sulla prossima partenza via stroke accent + countdown prominente.
- Alert chip **sopra** l'header (safety-critical).
- Tipi corretti: Android usa `Departure` (non `ResolvedDeparture`), `ServiceAlert` (non `GtfsRtAlert`), `TransitTheme.colors.*` (non `AppTheme.colors.*`), `stopIcon(...)` come funzione `@DrawableRes` wrappata in `Icon(painterResource(...))`, `TimeDisplay(state:)` che accetta `DepartureTimeState` (non `Departure`).

**Tech Stack:**
- iOS: SwiftUI + Metal shader esistente (`MapBackground.metal`, tuning parametri), xcstrings
- Android: Jetpack Compose, `InfiniteTransition`, `Canvas`, WebP drawable, Material3 Surface

---

## File Map

### iOS
- **Create:** `ios/TransitKit/Sources/Components/PressableButtonStyle.swift` (estratto da `ServiceDetailView.swift`)
- **Create:** `ios/TransitKit/Sources/Views/Home/LocationPrimerView.swift`
- **Modify:** `ios/TransitKit/Sources/Views/ServiceDetailView.swift` (rimozione inline style)
- **Modify:** `ios/TransitKit/Sources/Views/Home/HomeTab.swift` (ristruttura intera)
- **Modify:** `ios/TransitKit/Sources/Shaders/MapBackground.metal` (tuning opacità)
- **Modify:** `ios/TransitKit/Sources/Views/Settings/SettingsTab.swift` (sezione Informazioni)
- **Modify:** `ios/TransitKit/Sources/App/` (entry point per primer screen al primo launch)
- **Modify:** `ios/TransitKit/Sources/Resources/Localizable.xcstrings`

### Android
- **Create:** `android/app/src/main/res/drawable/operator_background.webp`
- **Create:** `android/app/src/main/java/com/transitkit/app/ui/home/LocationPrimerScreen.kt`
- **Create:** `android/app/src/main/java/com/transitkit/app/ui/home/OperatorMapBackground.kt`
- **Modify:** `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt` (ristruttura)
- **Modify:** `android/app/src/main/java/com/transitkit/app/ui/home/HomeViewModel.kt` (filtro 400m + nearbyDepartures flow)
- **Modify:** `android/app/src/main/java/com/transitkit/app/ui/settings/SettingsScreen.kt`
- **Modify:** `android/app/src/main/java/com/transitkit/app/MainActivity.kt` (rotta primer al primo launch)
- **Modify:** `android/app/src/main/res/values/strings.xml`
- **Modify:** `android/app/src/main/res/values-it/strings.xml`
- **Modify:** `android/app/src/main/res/values-es/strings.xml`

---

## PHASE 1 — Foundation (cross-cutting)

### Task 1 — iOS: estrai `PressableButtonStyle` in file dedicato

**Files:**
- Create: `ios/TransitKit/Sources/Components/PressableButtonStyle.swift`
- Modify: `ios/TransitKit/Sources/Views/ServiceDetailView.swift`

- [ ] **Step 1: Trovare la definizione attuale**

```bash
grep -n "struct PressableButtonStyle" ios/TransitKit/Sources/Views/ServiceDetailView.swift
```

- [ ] **Step 2: Creare il nuovo file con il contenuto estratto**

```swift
// ios/TransitKit/Sources/Components/PressableButtonStyle.swift
import SwiftUI

/// Button style che applica scale + opacity al press, con spring smooth.
/// Usato ovunque ci serva tattilità su card/chip tappabili.
struct PressableButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .opacity(configuration.isPressed ? 0.9 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: configuration.isPressed)
    }
}
```

- [ ] **Step 3: Rimuovere la definizione inline da `ServiceDetailView.swift`**

Aprire `ios/TransitKit/Sources/Views/ServiceDetailView.swift`, trovare `struct PressableButtonStyle: ButtonStyle { ... }` e rimuoverlo completamente (i riferimenti negli altri file continueranno a trovarlo nel nuovo file perché stesso target).

- [ ] **Step 4: Build check**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/ios && xcodegen generate
xcodebuild -project TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=4302AFD9-496E-4586-A5D0-D6BAC735FFFD" \
  build OPERATOR_ID=appalcart CODE_SIGNING_ALLOWED=NO 2>&1 | grep -E "error:|Build succeeded"
```

Atteso: `Build succeeded`.

- [ ] **Step 5: Commit**

```bash
git add ios/TransitKit/Sources/Components/PressableButtonStyle.swift \
        ios/TransitKit/Sources/Views/ServiceDetailView.swift
git commit -m "refactor(ios): estrai PressableButtonStyle in file dedicato"
```

---

### Task 2 — i18n: tutte le stringhe nuove

**Files:**
- Modify: `ios/TransitKit/Sources/Resources/Localizable.xcstrings`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/main/res/values-it/strings.xml`
- Modify: `android/app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Aggiungere stringhe in `Localizable.xcstrings`**

Aprire il file JSON. Trovare la sezione `strings: { ... }` e aggiungere queste entry al suo interno (preservando la sintassi e le virgole di separazione tra oggetti):

```json
"walking_1_min": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "1 min walk" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "1 min a pie" } },
    "it": { "stringUnit": { "state": "translated", "value": "1 min a piedi" } }
  }
},
"walking_n_min": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "%d min walk" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "%d min a pie" } },
    "it": { "stringUnit": { "state": "translated", "value": "%d min a piedi" } }
  }
},
"walking_10_plus_min": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "10+ min walk" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "10+ min a pie" } },
    "it": { "stringUnit": { "state": "translated", "value": "10+ min a piedi" } }
  }
},
"home_footer_disclaimer": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Official %@ data · Unofficial app" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Datos oficiales %@ · App no oficial" } },
    "it": { "stringUnit": { "state": "translated", "value": "Dati ufficiali %@ · App non ufficiale" } }
  }
},
"home_enable_location_chip": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Enable location to find nearby stops" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Activa la ubicación para ver paradas cercanas" } },
    "it": { "stringUnit": { "state": "translated", "value": "Attiva la posizione per vedere fermate vicine" } }
  }
},
"home_empty_favorites_title": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Your stops, always at hand" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Tus paradas, siempre a mano" } },
    "it": { "stringUnit": { "state": "translated", "value": "Le tue fermate, sempre a portata" } }
  }
},
"home_empty_favorites_body": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Add your daily stops to see real-time departures here." } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Añade tus paradas habituales para ver los horarios en tiempo real." } },
    "it": { "stringUnit": { "state": "translated", "value": "Aggiungi le tue fermate quotidiane per vedere le partenze in tempo reale." } }
  }
},
"home_empty_favorites_cta": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Browse stops" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Explorar paradas" } },
    "it": { "stringUnit": { "state": "translated", "value": "Sfoglia fermate" } }
  }
},
"location_primer_title": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Find stops near you" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Encuentra paradas cerca de ti" } },
    "it": { "stringUnit": { "state": "translated", "value": "Trova fermate vicino a te" } }
  }
},
"location_primer_body": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Allow location access to see the closest stops and their next departures. You can change this anytime in Settings." } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Permite el acceso a la ubicación para ver las paradas más cercanas y sus próximas salidas. Puedes cambiarlo en Ajustes." } },
    "it": { "stringUnit": { "state": "translated", "value": "Concedi l'accesso alla posizione per vedere le fermate più vicine e le loro prossime partenze. Puoi cambiare idea in Impostazioni." } }
  }
},
"location_primer_cta_enable": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Enable location" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Activar ubicación" } },
    "it": { "stringUnit": { "state": "translated", "value": "Attiva posizione" } }
  }
},
"location_primer_cta_skip": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Not now" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Ahora no" } },
    "it": { "stringUnit": { "state": "translated", "value": "Non ora" } }
  }
},
"settings_info_section": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "About" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Información" } },
    "it": { "stringUnit": { "state": "translated", "value": "Informazioni" } }
  }
},
"settings_disclaimer_body": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "%1$@ is not developed or managed by %2$@. Timetable data is officially provided by %2$@." } },
    "es-419": { "stringUnit": { "state": "translated", "value": "%1$@ no está desarrollada ni gestionada por %2$@. Los datos son proporcionados oficialmente por %2$@." } },
    "it": { "stringUnit": { "state": "translated", "value": "I dati degli orari sono forniti ufficialmente da %2$@. %1$@ non è sviluppata né gestita da %2$@." } }
  }
},
"settings_location_section": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Privacy" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Privacidad" } },
    "it": { "stringUnit": { "state": "translated", "value": "Privacy" } }
  }
},
"settings_location_title": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Location access" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Acceso a la ubicación" } },
    "it": { "stringUnit": { "state": "translated", "value": "Accesso alla posizione" } }
  }
},
"settings_location_enable": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Enable" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Activar" } },
    "it": { "stringUnit": { "state": "translated", "value": "Attiva" } }
  }
},
"settings_location_open": {
  "extractionState": "manual",
  "localizations": {
    "en": { "stringUnit": { "state": "translated", "value": "Open Settings" } },
    "es-419": { "stringUnit": { "state": "translated", "value": "Abrir Ajustes" } },
    "it": { "stringUnit": { "state": "translated", "value": "Apri Impostazioni" } }
  }
}
```

- [ ] **Step 2: Aggiungere stringhe in `values/strings.xml` (inglese base)**

Aprire il file e aggiungere prima di `</resources>`:

```xml
<string name="walking_1_min">1 min walk</string>
<string name="walking_n_min">%d min walk</string>
<string name="walking_10_plus_min">10+ min walk</string>
<string name="home_footer_disclaimer">Official %1$s data · Unofficial app</string>
<string name="home_enable_location_chip">Enable location to find nearby stops</string>
<string name="home_empty_favorites_title">Your stops, always at hand</string>
<string name="home_empty_favorites_body">Add your daily stops to see real-time departures here.</string>
<string name="home_empty_favorites_cta">Browse stops</string>
<string name="location_primer_title">Find stops near you</string>
<string name="location_primer_body">Allow location access to see the closest stops and their next departures. You can change this anytime in Settings.</string>
<string name="location_primer_cta_enable">Enable location</string>
<string name="location_primer_cta_skip">Not now</string>
<string name="settings_info_section">About</string>
<string name="settings_disclaimer_body">%1$s is not developed or managed by %2$s. Timetable data is officially provided by %2$s.</string>
<string name="settings_location_section">Privacy</string>
<string name="settings_location_title">Location access</string>
<string name="settings_location_enable">Enable</string>
<string name="settings_location_open">Open Settings</string>
```

- [ ] **Step 3: Aggiungere stringhe in `values-it/strings.xml`**

```xml
<string name="walking_1_min">1 min a piedi</string>
<string name="walking_n_min">%d min a piedi</string>
<string name="walking_10_plus_min">10+ min a piedi</string>
<string name="home_footer_disclaimer">Dati ufficiali %1$s · App non ufficiale</string>
<string name="home_enable_location_chip">Attiva la posizione per vedere fermate vicine</string>
<string name="home_empty_favorites_title">Le tue fermate, sempre a portata</string>
<string name="home_empty_favorites_body">Aggiungi le tue fermate quotidiane per vedere le partenze in tempo reale.</string>
<string name="home_empty_favorites_cta">Sfoglia fermate</string>
<string name="location_primer_title">Trova fermate vicino a te</string>
<string name="location_primer_body">Concedi l\'accesso alla posizione per vedere le fermate più vicine e le loro prossime partenze. Puoi cambiare idea in Impostazioni.</string>
<string name="location_primer_cta_enable">Attiva posizione</string>
<string name="location_primer_cta_skip">Non ora</string>
<string name="settings_info_section">Informazioni</string>
<string name="settings_disclaimer_body">I dati degli orari sono forniti ufficialmente da %2$s. %1$s non è sviluppata né gestita da %2$s.</string>
<string name="settings_location_section">Privacy</string>
<string name="settings_location_title">Accesso alla posizione</string>
<string name="settings_location_enable">Attiva</string>
<string name="settings_location_open">Apri Impostazioni</string>
```

- [ ] **Step 4: Aggiungere stringhe in `values-es/strings.xml`**

```xml
<string name="walking_1_min">1 min a pie</string>
<string name="walking_n_min">%d min a pie</string>
<string name="walking_10_plus_min">10+ min a pie</string>
<string name="home_footer_disclaimer">Datos oficiales %1$s · App no oficial</string>
<string name="home_enable_location_chip">Activa la ubicación para ver paradas cercanas</string>
<string name="home_empty_favorites_title">Tus paradas, siempre a mano</string>
<string name="home_empty_favorites_body">Añade tus paradas habituales para ver los horarios en tiempo real.</string>
<string name="home_empty_favorites_cta">Explorar paradas</string>
<string name="location_primer_title">Encuentra paradas cerca de ti</string>
<string name="location_primer_body">Permite el acceso a la ubicación para ver las paradas más cercanas y sus próximas salidas. Puedes cambiarlo en Ajustes.</string>
<string name="location_primer_cta_enable">Activar ubicación</string>
<string name="location_primer_cta_skip">Ahora no</string>
<string name="settings_info_section">Información</string>
<string name="settings_disclaimer_body">%1$s no está desarrollada ni gestionada por %2$s. Los datos son proporcionados oficialmente por %2$s.</string>
<string name="settings_location_section">Privacidad</string>
<string name="settings_location_title">Acceso a la ubicación</string>
<string name="settings_location_enable">Activar</string>
<string name="settings_location_open">Abrir Ajustes</string>
```

- [ ] **Step 5: Build check entrambe le piattaforme**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/ios
xcodebuild -project TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=4302AFD9-496E-4586-A5D0-D6BAC735FFFD" \
  build OPERATOR_ID=appalcart CODE_SIGNING_ALLOWED=NO 2>&1 | grep -E "error:|Build succeeded"

cd /Users/andreatoffanello/GitHub/transit-engine/android
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD SUCCESSFUL|BUILD FAILED"
```

- [ ] **Step 6: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine
git add ios/TransitKit/Sources/Resources/Localizable.xcstrings \
        android/app/src/main/res/values/strings.xml \
        android/app/src/main/res/values-it/strings.xml \
        android/app/src/main/res/values-es/strings.xml
git commit -m "feat(i18n): stringhe per home redesign (walking time, empty state, primer, disclaimer)"
```

---

### Task 3 — Android: asset sfondo operatore

**Files:**
- Create: `android/app/src/main/res/drawable/operator_background.webp`

- [ ] **Step 1: Copia il file**

```bash
cp /Users/andreatoffanello/GitHub/transit-engine/shared/operators/appalcart/sfondo.webp \
   /Users/andreatoffanello/GitHub/transit-engine/android/app/src/main/res/drawable/operator_background.webp
```

- [ ] **Step 2: Verifica**

```bash
ls -lh /Users/andreatoffanello/GitHub/transit-engine/android/app/src/main/res/drawable/operator_background.webp
```

Atteso: file presente, ~250KB.

- [ ] **Step 3: Commit**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine
git add android/app/src/main/res/drawable/operator_background.webp
git commit -m "feat(android): asset sfondo operatore (mappa territorio)"
```

---

## PHASE 2 — iOS Home

### Task 4 — iOS: tuning shader sfondo (opacità più visibile)

**Files:**
- Modify: `ios/TransitKit/Sources/Shaders/MapBackground.metal`

- [ ] **Step 1: Aggiornare i parametri di opacità nello shader**

Aprire `ios/TransitKit/Sources/Shaders/MapBackground.metal` e sostituire il blocco finale:

```metal
    // Base visibility: 4% for ink lines; up to +7% near the spotlight centre
    float alpha = ink * (0.04 + spotlight * 0.07);
```

Con:

```metal
    // Base visibility: 15% for ink lines; up to +10% near the spotlight centre.
    // Bumped from 4%/+7% — card glass materials + footer gradient provide local readability.
    float alpha = ink * (0.15 + spotlight * 0.10);
```

- [ ] **Step 2: Build check**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/ios
xcodebuild -project TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=4302AFD9-496E-4586-A5D0-D6BAC735FFFD" \
  build OPERATOR_ID=appalcart CODE_SIGNING_ALLOWED=NO 2>&1 | grep -E "error:|Build succeeded"
```

- [ ] **Step 3: Commit**

```bash
git add ios/TransitKit/Sources/Shaders/MapBackground.metal
git commit -m "feat(ios/shader): bump opacity mappa sfondo (4→15% base, 11→25% peak)"
```

---

### Task 5 — iOS: Location primer screen

**Files:**
- Create: `ios/TransitKit/Sources/Views/Home/LocationPrimerView.swift`

- [ ] **Step 1: Creare la view primer**

```swift
// ios/TransitKit/Sources/Views/Home/LocationPrimerView.swift
import SwiftUI
import CoreLocation

/// Shown on first launch as a primer before the system location prompt.
/// Accept → triggers system prompt, dismisses. Skip → dismisses without prompt
/// (user can still enable from Settings > Privacy later).
struct LocationPrimerView: View {
    @Environment(LocationManager.self) private var locationManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            // Same ghost map background as Home for visual continuity
            Image("OperatorBackground")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .ignoresSafeArea()
                .opacity(0.18)
                .overlay(Color.black.opacity(0.15).ignoresSafeArea())

            VStack(spacing: 24) {
                Spacer()

                ZStack {
                    Circle()
                        .fill(AppTheme.accent.opacity(0.15))
                        .frame(width: 96, height: 96)
                    LucideIcon.mapPin.sized(40)
                        .foregroundStyle(AppTheme.accent)
                }

                VStack(spacing: 12) {
                    Text(String(localized: "location_primer_title"))
                        .font(.system(size: 26, weight: .bold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .multilineTextAlignment(.center)
                    Text(String(localized: "location_primer_body"))
                        .font(.system(size: 15))
                        .foregroundStyle(AppTheme.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }

                Spacer()

                VStack(spacing: 12) {
                    Button {
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        locationManager.requestPermissionAndStart()
                        dismiss()
                    } label: {
                        Text(String(localized: "location_primer_cta_enable"))
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppTheme.accent, in: RoundedRectangle(cornerRadius: 14))
                    }
                    .accessibilityIdentifier("primer_enable_location")

                    Button {
                        dismiss()
                    } label: {
                        Text(String(localized: "location_primer_cta_skip"))
                            .font(.system(size: 15))
                            .foregroundStyle(AppTheme.textSecondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                    }
                    .accessibilityIdentifier("primer_skip_location")
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
}
```

- [ ] **Step 2: Collegare il primer al primo launch in `HomeTab.swift`**

Aprire `ios/TransitKit/Sources/Views/Home/HomeTab.swift`. Aggiungere tra le `@State` property:

```swift
@AppStorage("hasSeenLocationPrimer") private var hasSeenLocationPrimer = false
@State private var showLocationPrimer = false
```

Nel `body`, dentro `NavigationStack { ... }`, aggiungere come modificatore al ZStack:

```swift
.fullScreenCover(isPresented: $showLocationPrimer) {
    LocationPrimerView()
}
.onAppear {
    locationManager.requestPermissionAndStart()
    if !hasSeenLocationPrimer &&
       locationManager.authorizationStatus == .notDetermined {
        // Delay leggero per far caricare la Home prima del primer
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            showLocationPrimer = true
            hasSeenLocationPrimer = true
        }
    }
}
```

Nota: rimpiazza l'`onAppear` esistente — se c'è già uno, unisci il contenuto.

- [ ] **Step 3: Build + commit (build test finale in Task 12)**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/ios && xcodegen generate
xcodebuild -project TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=4302AFD9-496E-4586-A5D0-D6BAC735FFFD" \
  build OPERATOR_ID=appalcart CODE_SIGNING_ALLOWED=NO 2>&1 | grep -E "error:|Build succeeded"
```

```bash
git add ios/TransitKit/Sources/Views/Home/LocationPrimerView.swift \
        ios/TransitKit/Sources/Views/Home/HomeTab.swift \
        ios/TransitKit.xcodeproj
git commit -m "feat(ios/home): primer screen al primo launch per permesso posizione"
```

---

### Task 6 — iOS: rimuovi hero card, nuovo header + alert chip

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Home/HomeTab.swift`

- [ ] **Step 1: Rimuovere helper hero card**

Aprire `HomeTab.swift` e rimuovere integralmente questi simboli (tutti nelle loro intere definizioni):
- `operatorHeroSection`
- `heroCard(config:)`
- `heroSkeleton`
- `alertBanner(count:severity:)`
- `alertBannerLabel(count:)`
- `initials(from:)`

**Mantenere** (servono ancora): `highestSeverity(_:)` viene riusato dall'alert chip.

- [ ] **Step 2: Aggiungere property `operatorMapBackground` e `colorScheme` env**

Aggiungere tra le `@State`:

```swift
@Environment(\.colorScheme) private var colorScheme
```

Aggiungere come computed property (dopo le property esistenti):

```swift
@ViewBuilder
private var operatorMapBackground: some View {
    TimelineView(.animation(minimumInterval: 1.0 / 24.0)) { ctx in
        let t = Float(ctx.date.timeIntervalSinceReferenceDate)
        let isDark: Float = colorScheme == .dark ? 1.0 : 0.0
        GeometryReader { geo in
            Image("OperatorBackground")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: geo.size.width, height: geo.size.height)
                .clipped()
                .colorEffect(
                    ShaderLibrary.mapGlowEffect(
                        .float2(geo.size),
                        .float(t),
                        .float(isDark)
                    )
                )
        }
    }
    .allowsHitTesting(false)
}
```

- [ ] **Step 3: Aggiungere `homeMinimalHeader`**

```swift
// MARK: - Minimal Header

private var homeMinimalHeader: some View {
    HStack(spacing: 12) {
        if UIImage(named: "OperatorLogo") != nil {
            Image("OperatorLogo")
                .resizable()
                .scaledToFill()
                .frame(width: 32, height: 32)
                .clipShape(Circle())
        }
        if let config {
            VStack(alignment: .leading, spacing: 1) {
                Text(config.name)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                if !config.region.isEmpty {
                    Text(config.region)
                        .font(.system(size: 12))
                        .foregroundStyle(AppTheme.textSecondary)
                }
            }
        }
        Spacer()
    }
    .padding(.horizontal, 20)
    .padding(.top, 16)
    .padding(.bottom, 8)
}
```

- [ ] **Step 4: Aggiungere `alertChip` (sopra header nell'ordine visivo)**

```swift
// MARK: - Alert Chip

private var alertChip: some View {
    Button {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        showAlertList = true
    } label: {
        HStack(spacing: 8) {
            LucideIcon.alertTriangle.sized(13)
                .foregroundStyle(chipAlertColor)
            Text(alertChipLabel)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)
                .lineLimit(1)
            Spacer(minLength: 0)
            LucideIcon.chevronRight.sized(12)
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(chipAlertColor.opacity(0.35), lineWidth: 1)
        )
    }
    .buttonStyle(PressableButtonStyle())
    .accessibilityIdentifier("home_alert_chip")
    .padding(.horizontal, 16)
}

private var chipAlertColor: Color {
    switch highestSeverity(alertStore.activeAlerts) {
    case .severe:  return .red
    case .warning: return .orange
    default:       return AppTheme.accent
    }
}

private var alertChipLabel: String {
    let count = alertStore.activeAlerts.count
    return count == 1
        ? String(localized: "alerts_banner_one")
        : String(format: String(localized: "alerts_banner_many"), count)
}
```

- [ ] **Step 5: Riscrivere il `body`**

Sostituire integralmente il `var body: some View { ... }` con:

```swift
var body: some View {
    NavigationStack {
        ZStack {
            operatorMapBackground.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    // Alert chip sopra l'header (safety-critical)
                    if !alertStore.activeAlerts.isEmpty {
                        alertChip
                            .padding(.top, 8)
                            .padding(.bottom, 4)
                    }
                    homeMinimalHeader

                    VStack(spacing: 20) {
                        favoritesSection
                        nearbyStopsSection
                        footerDisclaimer
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 16)
                    .padding(.bottom, 100)
                }
            }
            .background(.clear)

            // Footer gradient fade per leggibilità disclaimer sullo sfondo
            VStack {
                Spacer()
                LinearGradient(
                    colors: [AppTheme.background.opacity(0), AppTheme.background.opacity(0.9)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 120)
                .allowsHitTesting(false)
            }
            .ignoresSafeArea()
        }
        .background(AppTheme.background.ignoresSafeArea())
        .fullScreenCover(isPresented: $showLocationPrimer) {
            LocationPrimerView()
        }
        .onAppear {
            locationManager.requestPermissionAndStart()
            if !hasSeenLocationPrimer &&
               locationManager.authorizationStatus == .notDetermined {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    showLocationPrimer = true
                    hasSeenLocationPrimer = true
                }
            }
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    showSettings = true
                } label: {
                    LucideIcon.settings.sized(20)
                        .foregroundStyle(AppTheme.textSecondary)
                }
                .accessibilityIdentifier("btn_settings")
                .accessibilityLabel(String(localized: "tab_settings"))
            }
        }
        .sheet(isPresented: $showSettings) { SettingsTab() }
        .sheet(isPresented: $showAlertList) {
            NavigationStack { AlertListView() }
        }
        .navigationDestination(item: $selectedMainStop) { stop in
            StopDetailView(stop: stop)
        }
    }
}
```

- [ ] **Step 6: Build check parziale**

La build fallirà su `footerDisclaimer` (non ancora definito — Task 10). Verificare che **gli altri errori non ci siano**:

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/ios
xcodebuild -project TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=4302AFD9-496E-4586-A5D0-D6BAC735FFFD" \
  build OPERATOR_ID=appalcart CODE_SIGNING_ALLOWED=NO 2>&1 | grep -E "error:" | grep -v "footerDisclaimer"
```

Atteso: nessuna riga diversa da `footerDisclaimer`.

---

### Task 7 — iOS: favoritesSection aggiornata (ordine manuale + emphasis sul next)

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Home/HomeTab.swift`

- [ ] **Step 1: Aggiornare `favoriteStops` — fino a 5, ordine salvato (no urgency sort)**

Sostituire la property `favoriteStops` esistente:

```swift
private var favoriteStops: [ResolvedStop] {
    favoritesManager.favoriteStopIds.prefix(5).compactMap { stopId in
        store.stops.first { $0.id == stopId }
    }
}
```

- [ ] **Step 2: Aggiungere helper `walkingTime(meters:)`**

```swift
private func walkingTime(meters: Double) -> String {
    let minutes = Int((meters / 80.0).rounded(.up))
    if minutes <= 1 { return String(localized: "walking_1_min") }
    if minutes > 10 { return String(localized: "walking_10_plus_min") }
    return String(format: String(localized: "walking_n_min"), minutes)
}
```

- [ ] **Step 3: Ricodificare `stopCard` con emphasis visuale sul "next"**

La card con la partenza più imminente (entro 5 min) riceve uno stroke accent. Le altre sono stroke neutro.

```swift
private func stopCard(_ stop: ResolvedStop, showLiveBadge: Bool, distanceMeters: Double? = nil) -> some View {
    let departures = store.upcomingDepartures(forStopId: stop.id, limit: 3)
    let transitTypeIcon: Image = stopPinIcon(transitTypes: stop.transitTypes).image
    let isImminent = departures.first.map { isWithinFiveMinutes($0) } ?? false

    return VStack(alignment: .leading, spacing: 10) {
        HStack(spacing: 6) {
            transitTypeIcon
                .font(.caption.weight(.semibold))
                .foregroundStyle(AppTheme.textTertiary)
                .frame(width: 14, height: 14)
            Text(stop.name)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(AppTheme.textSecondary)
                .lineLimit(1)
            Spacer()
            if let distanceMeters {
                Text(walkingTime(meters: distanceMeters))
                    .font(.caption.weight(.medium))
                    .foregroundStyle(AppTheme.textTertiary)
            }
        }

        if departures.isEmpty {
            Text(String(localized: "no_departures_today"))
                .font(.caption)
                .foregroundStyle(AppTheme.textTertiary)
        } else {
            VStack(spacing: 0) {
                ForEach(Array(departures.enumerated()), id: \.element.id) { index, dep in
                    HStack(spacing: 8) {
                        LineBadge(departure: dep, size: .medium)
                        Text(dep.headsign)
                            .font(.system(size: 13))
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineLimit(1)
                            .truncationMode(.tail)
                        Spacer()
                        if showLiveBadge && vehicleStore.isLive(tripId: dep.tripId) {
                            LiveBadge()
                        }
                        TimelineView(.periodic(from: .now, by: 30)) { _ in
                            TimeDisplay(departure: dep)
                        }
                    }
                    .padding(.vertical, 6)
                    if index < departures.count - 1 {
                        Divider().overlay(AppTheme.separatorLine)
                    }
                }
            }
        }
    }
    .padding(14)
    .adaptiveGlass(in: RoundedRectangle(cornerRadius: 14), withShadow: true)
    .overlay(
        RoundedRectangle(cornerRadius: 14, style: .continuous)
            .strokeBorder(
                isImminent ? AppTheme.accent.opacity(0.6) : Color.clear,
                lineWidth: isImminent ? 1.5 : 0
            )
    )
}

/// True se la partenza è entro 5 minuti — usato per evidenziare la card.
private func isWithinFiveMinutes(_ dep: Departure) -> Bool {
    let now = Date()
    let cal = Calendar.current
    let comps = dep.time.split(separator: ":").compactMap { Int($0) }
    guard comps.count == 2 else { return false }
    guard let depDate = cal.date(bySettingHour: comps[0], minute: comps[1], second: 0, of: now) else {
        return false
    }
    let delta = depDate.timeIntervalSince(now)
    return delta >= 0 && delta <= 300
}
```

- [ ] **Step 4: Tenere `favoritesSection` esistente** — usa già `stopCard`, nessuna modifica necessaria.

---

### Task 8 — iOS: nearbyStopsSection con fallback chip

Se GPS `notDetermined`: mostra chip "Attiva posizione" (fallback per chi ha skippato primer). Se GPS negato: sezione nascosta (è una scelta dell'utente). Se autorizzato: lista fermate entro 400m.

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Home/HomeTab.swift`

- [ ] **Step 1: Rimuovere `permissionPromptCard` e `permissionDeniedCard` esistenti**

- [ ] **Step 2: Aggiungere `enableLocationChip`**

```swift
private var enableLocationChip: some View {
    Button {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        showLocationPrimer = true
    } label: {
        HStack(spacing: 8) {
            LucideIcon.mapPin.sized(14)
                .foregroundStyle(AppTheme.accent)
            Text(String(localized: "home_enable_location_chip"))
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(AppTheme.textPrimary)
            Spacer(minLength: 0)
            LucideIcon.chevronRight.sized(12)
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
    }
    .buttonStyle(PressableButtonStyle())
    .accessibilityIdentifier("home_enable_location_chip")
}
```

- [ ] **Step 3: Rimpiazzare `nearbyStopsSection`**

```swift
@ViewBuilder
private var nearbyStopsSection: some View {
    switch locationManager.authorizationStatus {
    case .notDetermined:
        enableLocationChip
    case .authorizedWhenInUse, .authorizedAlways:
        let nearby = nearbyStopsWithDistance.filter { $0.1 <= 400 }
        if !nearby.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                sectionHeader(String(localized: "nearby_you"))
                VStack(spacing: 8) {
                    ForEach(nearby, id: \.0.id) { (stop, distance) in
                        Button {
                            selectedMainStop = stop
                        } label: {
                            stopCard(stop, showLiveBadge: true, distanceMeters: distance)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    default:
        EmptyView()
    }
}
```

---

### Task 9 — iOS: empty state preferiti (card glass singola)

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Home/HomeTab.swift`

- [ ] **Step 1: Rimpiazzare `onboardingCard` con nuova empty card coerente**

Il file ha già un `onboardingCard`. Sostituirlo integralmente con:

```swift
private var onboardingCard: some View {
    VStack(spacing: 16) {
        ZStack {
            Circle()
                .fill(AppTheme.accent.opacity(0.12))
                .frame(width: 56, height: 56)
            LucideIcon.bookmark.sized(24)
                .foregroundStyle(AppTheme.accent)
        }

        VStack(spacing: 6) {
            Text(String(localized: "home_empty_favorites_title"))
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
                .multilineTextAlignment(.center)
            Text(String(localized: "home_empty_favorites_body"))
                .font(.system(size: 14))
                .foregroundStyle(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
        }

        Button {
            selectedTab = 1   // tab Orari
        } label: {
            Text(String(localized: "home_empty_favorites_cta"))
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(AppTheme.accent, in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
    .padding(20)
    .adaptiveGlass(in: RoundedRectangle(cornerRadius: 16), withShadow: true)
}
```

---

### Task 10 — iOS: footer disclaimer

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Home/HomeTab.swift`

- [ ] **Step 1: Aggiungere `footerDisclaimer`**

```swift
@ViewBuilder
private var footerDisclaimer: some View {
    if let config {
        Text(String(format: String(localized: "home_footer_disclaimer"), config.name))
            .font(.system(size: 11))
            .foregroundStyle(AppTheme.textTertiary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.top, 8)
    }
}
```

---

### Task 11 — iOS: Settings > Informazioni + Privacy

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Settings/SettingsTab.swift`

- [ ] **Step 1: Aggiungere sezione Privacy (gestione permesso posizione)**

Trovare la `List` principale. Aggiungere questa `Section` (posizione: prima della sezione About/Informazioni esistente):

```swift
Section(String(localized: "settings_location_section")) {
    HStack {
        LucideIcon.mapPin.sized(18)
            .foregroundStyle(AppTheme.accent)
        VStack(alignment: .leading, spacing: 2) {
            Text(String(localized: "settings_location_title"))
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(AppTheme.textPrimary)
            Text(locationStatusDescription)
                .font(.caption)
                .foregroundStyle(AppTheme.textSecondary)
        }
        Spacer()
        locationActionButton
    }
}
```

Aggiungere nello stesso file, come helper privati sulla struct `SettingsTab`:

```swift
@Environment(LocationManager.self) private var locationManager

private var locationStatusDescription: String {
    switch locationManager.authorizationStatus {
    case .authorizedWhenInUse, .authorizedAlways:
        return String(localized: "nearby_enable_subtitle")  // chiave già esistente o adattare
    case .denied, .restricted:
        return String(localized: "nearby_denied_subtitle")
    case .notDetermined:
        return String(localized: "nearby_enable_subtitle")
    @unknown default:
        return ""
    }
}

@ViewBuilder
private var locationActionButton: some View {
    switch locationManager.authorizationStatus {
    case .notDetermined:
        Button(String(localized: "settings_location_enable")) {
            locationManager.requestPermissionAndStart()
        }
        .buttonStyle(.borderedProminent)
        .tint(AppTheme.accent)
    case .denied, .restricted:
        Button(String(localized: "settings_location_open")) {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        }
        .buttonStyle(.bordered)
    default:
        Image(systemName: "checkmark.circle.fill")
            .foregroundStyle(AppTheme.accent)
    }
}
```

- [ ] **Step 2: Aggiungere sezione Informazioni (disclaimer esteso)**

Prima della chiusura della `List`:

```swift
Section(String(localized: "settings_info_section")) {
    VStack(alignment: .leading, spacing: 8) {
        if let config {
            Text(String(format: String(localized: "settings_disclaimer_body"), config.name, config.fullName))
                .font(.system(size: 13))
                .foregroundStyle(AppTheme.textSecondary)
        }
        Text("v\(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "")")
            .font(.system(size: 12))
            .foregroundStyle(AppTheme.textTertiary)
    }
    .padding(.vertical, 4)
}
```

Aggiungere in cima alla struct `SettingsTab`:

```swift
private var config: OperatorConfig? { try? ConfigLoader.load() }
```

(se non già presente)

---

### Task 12 — iOS: build, install, screenshot, commit

- [ ] **Step 1: Build finale**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/ios
xcodebuild -project TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=4302AFD9-496E-4586-A5D0-D6BAC735FFFD" \
  build OPERATOR_ID=appalcart CODE_SIGNING_ALLOWED=NO 2>&1 | grep -E "error:|Build succeeded"
```

Atteso: `Build succeeded`. Se fallisce, delegare a `apple-platform-build-tools:builder`.

- [ ] **Step 2: Install + launch**

```bash
UDID="4302AFD9-496E-4586-A5D0-D6BAC735FFFD"
APP=$(find ~/Library/Developer/Xcode/DerivedData/TransitKit-*/Build/Products/Debug-iphonesimulator \
  -name "TransitKit.app" | head -1)
xcrun simctl uninstall $UDID com.transitkit.appalcart
xcrun simctl install $UDID "$APP"
xcrun simctl launch $UDID com.transitkit.appalcart
```

- [ ] **Step 3: Reset `hasSeenLocationPrimer` per verificare il primer**

```bash
UDID="4302AFD9-496E-4586-A5D0-D6BAC735FFFD"
xcrun simctl privacy $UDID reset location com.transitkit.appalcart
# Il primer apparirà al prossimo launch (fresh install reset già lo stato)
```

- [ ] **Step 4: Screenshot primer + Home**

```bash
sleep 3
UDID="4302AFD9-496E-4586-A5D0-D6BAC735FFFD"
xcrun simctl io $UDID screenshot /tmp/ios_primer.png
sips -Z 800 /tmp/ios_primer.png --out /tmp/ios_primer_s.png

# Tap "Non ora" per skippare
idb ui tap --udid $UDID 200 740

sleep 2
xcrun simctl io $UDID screenshot /tmp/ios_home_v2.png
sips -Z 800 /tmp/ios_home_v2.png --out /tmp/ios_home_v2_s.png
```

Verifica visiva:
- Primer: sfondo visibile, CTA grande, skip link secondario
- Home: sfondo mappa visibile al ~15%, nessun hero verde, alert chip sopra (se alert presenti), card preferiti con glass, chip "Attiva posizione" al posto della sezione vicini

- [ ] **Step 5: Commit iOS**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine
git add ios/
git commit -m "feat(ios/home): redesign con sfondo operatore, onboarding posizione, no urgency sort"
```

---

## PHASE 3 — Android Home

### Task 13 — Android: ViewModel fixes (400m + nearbyDepartures + hasSeenPrimer)

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Aggiungere filtro 400m a `nearbyStops`**

Trovare il combine/flow di `nearbyStops` nel ViewModel (intorno alla riga 83-97 secondo la verifica). Aggiungere `.filter { it.second <= 400.0 }` prima del `.sortedBy`/`.take(3)`.

Esempio finale (adattare al codice esistente se differisce):

```kotlin
val nearbyStops: StateFlow<List<Pair<ResolvedStop, Double>>> = combine(
    _userLocation,
    scheduleRepository.stops
) { location, stops ->
    if (location == null) return@combine emptyList()
    val (userLat, userLon) = location
    stops
        .map { stop ->
            val dLat = stop.lat - userLat
            val dLon = stop.lon - userLon
            val distM = kotlin.math.sqrt(dLat * dLat + dLon * dLon) * 111_320.0
            stop to distM
        }
        .filter { (_, dist) -> dist <= 400.0 }
        .sortedBy { it.second }
        .take(3)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- [ ] **Step 2: Aggiungere flow `nearbyDepartures`**

Sotto il blocco `favoriteDepartures`, aggiungere:

```kotlin
private val _nearbyDepartures = MutableStateFlow<Map<String, List<Departure>>>(emptyMap())
val nearbyDepartures: StateFlow<Map<String, List<Departure>>> = _nearbyDepartures.asStateFlow()
```

Nel blocco `init { ... }`, aggiungere (dopo il caricamento dei favoriti):

```kotlin
viewModelScope.launch {
    nearbyStops.collectLatest { nearby ->
        val result = mutableMapOf<String, List<Departure>>()
        for ((stop, _) in nearby) {
            result[stop.id] = scheduleRepository
                .upcomingDepartures(stop.id, limit = 3)
                .map { it.toDeparture() }
        }
        _nearbyDepartures.value = result
    }
}
```

Importare se necessario: `kotlinx.coroutines.flow.collectLatest`, `kotlinx.coroutines.launch`.

- [ ] **Step 3: Aggiungere state per primer location (SharedPreferences)**

Nel ViewModel:

```kotlin
private val _shouldShowLocationPrimer = MutableStateFlow(false)
val shouldShowLocationPrimer: StateFlow<Boolean> = _shouldShowLocationPrimer.asStateFlow()

fun checkLocationPrimer(prefs: SharedPreferences, currentPermissionGranted: Boolean) {
    val hasSeen = prefs.getBoolean("has_seen_location_primer", false)
    _shouldShowLocationPrimer.value = !hasSeen && !currentPermissionGranted
}

fun markLocationPrimerSeen(prefs: SharedPreferences) {
    prefs.edit().putBoolean("has_seen_location_primer", true).apply()
    _shouldShowLocationPrimer.value = false
}
```

Import: `android.content.SharedPreferences`.

- [ ] **Step 4: Build check**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/android
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD SUCCESSFUL|BUILD FAILED"
```

---

### Task 14 — Android: OperatorMapBackground composable (file separato)

**Files:**
- Create: `android/app/src/main/java/com/transitkit/app/ui/home/OperatorMapBackground.kt`

- [ ] **Step 1: Creare il file**

```kotlin
// android/app/src/main/java/com/transitkit/app/ui/home/OperatorMapBackground.kt
package com.transitkit.app.ui.home

import androidx.compose.animation.core.InfiniteRepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.transitkit.app.R
import kotlin.math.sin

/**
 * Ghost map background per la Home: mostra l'immagine dell'operatore come
 * texture desaturata a ~15% con uno spotlight radiale animato su percorso
 * lissajous lento (~62s per ciclo).
 */
@Composable
fun OperatorMapBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mapSpotlight")

    val phaseX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 62_000, easing = LinearEasing),
            repeatMode = InfiniteRepeatMode.Restart
        ),
        label = "phaseX"
    )
    val phaseY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI * 1.618f).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 62_000, easing = LinearEasing),
            repeatMode = InfiniteRepeatMode.Restart
        ),
        label = "phaseY"
    )

    val isDark = isSystemInDarkTheme()
    val inkAlpha = if (isDark) 0.14f else 0.18f
    val spotAlpha = if (isDark) 0.07f else 0.06f

    val desaturate = remember { ColorMatrix().apply { setToSaturation(0f) } }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.operator_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = inkAlpha,
            colorFilter = ColorFilter.colorMatrix(desaturate),
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lx = (0.5f + 0.40f * sin(phaseX)) * size.width
            val ly = (0.5f + 0.30f * sin(phaseY + 0.9f)) * size.height
            val radius = size.minDimension * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = spotAlpha),
                        Color.Transparent
                    ),
                    center = Offset(lx, ly),
                    radius = radius
                ),
                radius = radius,
                center = Offset(lx, ly)
            )
        }
    }
}
```

Importa `remember`: `import androidx.compose.runtime.remember`.

- [ ] **Step 2: Build check**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/android
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD SUCCESSFUL|BUILD FAILED"
```

---

### Task 15 — Android: Location primer screen

**Files:**
- Create: `android/app/src/main/java/com/transitkit/app/ui/home/LocationPrimerScreen.kt`
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt`
- Modify: `android/app/src/main/java/com/transitkit/app/MainActivity.kt`

- [ ] **Step 1: Creare `LocationPrimerScreen.kt`**

```kotlin
package com.transitkit.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.ui.theme.TransitTheme

/**
 * Primer mostrato al primo launch (se permesso location non ancora determinato).
 * Offre due azioni: attiva posizione (triggera system prompt) o skip.
 */
@Composable
fun LocationPrimerScreen(
    onEnableLocation: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Mappa sfondo in background, desaturata + overlay scuro per contrasto CTA
        Image(
            painter = painterResource(id = R.drawable.operator_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.22f,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(TransitTheme.colors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = TransitTheme.colors.accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.location_primer_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TransitTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.location_primer_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TransitTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onEnableLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TransitTheme.colors.accent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    stringResource(R.string.location_primer_cta_enable),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.location_primer_cta_skip),
                    color = TransitTheme.colors.textSecondary,
                    fontSize = 15.sp
                )
            }
        }
    }
}
```

- [ ] **Step 2: Aggiungere rotta primer in `MainActivity.kt`**

Trovare il `NavHost` e aggiungere una rotta `location_primer` che porta a `LocationPrimerScreen`. La logica di "mostra al primo launch" è nel ViewModel.

Nel composable che mostra la Home (probabilmente `HomeScreen` chiamato dal NavHost), aggiungere il trigger (vedi Task 16).

- [ ] **Step 3: Build check**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/android
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD SUCCESSFUL|BUILD FAILED"
```

---

### Task 16 — Android: ristruttura HomeScreen (no hero, background, chip alert sopra)

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt`

- [ ] **Step 1: Rimuovere helper hero**

Eliminare i composable `HeroSection`, `HeroSectionWrapper`, `HeroAlertBanner`, `SearchBar` (se presente e inutilizzato), `QuickAccessSection`, `QuickCard` se inutilizzati.

- [ ] **Step 2: Aggiungere `HomeMinimalHeader`**

```kotlin
@Composable
private fun HomeMinimalHeader(
    config: OperatorConfig?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val logoId = remember {
            runCatching {
                context.resources.getIdentifier("operator_logo", "drawable", context.packageName)
            }.getOrDefault(0)
        }
        if (logoId != 0) {
            Image(
                painter = painterResource(id = logoId),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        }
        config?.let {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TransitTheme.colors.textPrimary
                )
                if (it.region.isNotEmpty()) {
                    Text(
                        text = it.region,
                        style = MaterialTheme.typography.labelSmall,
                        color = TransitTheme.colors.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.tab_settings),
                tint = TransitTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
```

Import necessari aggiuntivi:
```kotlin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.remember
import com.transitkit.app.ui.theme.TransitTheme
```

- [ ] **Step 3: Aggiungere `HomeAlertChip`** (con tipo corretto `ServiceAlert`)

```kotlin
@Composable
private fun HomeAlertChip(
    alerts: List<ServiceAlert>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highestSeverity = alerts.maxByOrNull { it.severity.raw }?.severity ?: AlertSeverity.UNKNOWN
    val chipColor = when (highestSeverity) {
        AlertSeverity.SEVERE  -> TransitTheme.colors.realtimeRed
        AlertSeverity.WARNING -> TransitTheme.colors.realtimeOrange
        else                  -> TransitTheme.colors.accent
    }
    val label = if (alerts.size == 1)
        stringResource(R.string.alerts_banner_one)
    else
        stringResource(R.string.alerts_banner_many, alerts.size)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(10.dp),
        color = TransitTheme.colors.glassFill,
        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.35f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = chipColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TransitTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = TransitTheme.colors.textTertiary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
```

Import: `com.transitkit.app.data.model.ServiceAlert`, `com.transitkit.app.data.model.AlertSeverity`, `androidx.compose.foundation.BorderStroke`, `androidx.compose.foundation.shape.RoundedCornerShape`, `androidx.compose.material.icons.outlined.Warning`, `androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight`.

- [ ] **Step 4: Ristruttura principale `HomeScreen`**

Sostituire il body del composable `HomeScreen` (la parte dopo le dichiarazioni `val ...` di state collection) con:

```kotlin
val shouldShowPrimer by viewModel.shouldShowLocationPrimer.collectAsState()

// Primer al primo launch
if (shouldShowPrimer) {
    LocationPrimerScreen(
        onEnableLocation = {
            viewModel.markLocationPrimerSeen(context.getSharedPreferences("transitkit_prefs", Context.MODE_PRIVATE))
            // Triggera system prompt tramite activity result launcher esistente
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        },
        onSkip = {
            viewModel.markLocationPrimerSeen(context.getSharedPreferences("transitkit_prefs", Context.MODE_PRIVATE))
        }
    )
    return
}

Box(modifier = Modifier.fillMaxSize()) {
    OperatorMapBackground()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Alert chip sopra l'header
        if (activeAlerts.isNotEmpty()) {
            item {
                HomeAlertChip(
                    alerts = activeAlerts,
                    onClick = { navController.navigate("alerts") },
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
        }

        item {
            HomeMinimalHeader(
                config = uiState.config,
                onSettingsClick = { navController.navigate("settings_from_home") }
            )
        }

        // Preferiti
        item {
            FavoritesSection(
                stops = resolvedFavoriteStops,
                departures = favoriteDepartures,
                liveTripIds = liveTripIds,
                onStopClick = { stop ->
                    navController.navigate("stop/${stop.id}?name=${Uri.encode(stop.name)}")
                },
                onBrowseStops = { navController.navigate("orari") },
                operatorTimezone = uiState.config?.timezone ?: "UTC",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Vicino a te — con fallback chip se notDetermined
        item {
            NearbySection(
                nearbyStops = nearbyStops,
                nearbyDepartures = nearbyDepartures,
                liveTripIds = liveTripIds,
                permissionGranted = hasLocationPermission,
                permissionNotDetermined = isLocationPermissionNotDetermined,
                onEnableLocation = {
                    locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onStopClick = { stop ->
                    navController.navigate("stop/${stop.id}?name=${Uri.encode(stop.name)}")
                },
                operatorTimezone = uiState.config?.timezone ?: "UTC",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Footer disclaimer
        item {
            HomeFooterDisclaimer(
                operatorName = uiState.config?.name ?: "",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }

    // Footer gradient fade
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        TransitTheme.colors.background.copy(alpha = 0.9f)
                    )
                )
            )
    )
}
```

Aggiungere le variabili di stato location sopra:
```kotlin
val hasLocationPermission = remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    )
}
val isLocationPermissionNotDetermined = !hasLocationPermission.value &&
    !ActivityCompat.shouldShowRequestPermissionRationale(
        (context as Activity), android.Manifest.permission.ACCESS_FINE_LOCATION
    )

LaunchedEffect(Unit) {
    val prefs = context.getSharedPreferences("transitkit_prefs", Context.MODE_PRIVATE)
    viewModel.checkLocationPrimer(prefs, hasLocationPermission.value)
}
```

Import: `android.content.Context`, `android.content.pm.PackageManager`, `android.app.Activity`, `androidx.core.content.ContextCompat`, `androidx.core.app.ActivityCompat`, `androidx.compose.runtime.mutableStateOf`, `androidx.compose.runtime.LaunchedEffect`, `androidx.compose.runtime.collectAsState`, `androidx.compose.runtime.getValue`, `androidx.compose.ui.graphics.Brush`, `android.net.Uri`.

---

### Task 17 — Android: `StopCard` composable (tipi corretti)

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt`

- [ ] **Step 1: Rimuovere `FavoriteStopCard` e `NearbyStopCard` esistenti**

Eliminare i due composable duplicati.

- [ ] **Step 2: Aggiungere `StopCard` condiviso con tipi corretti**

```kotlin
@Composable
private fun StopCard(
    stop: ResolvedStop,
    departures: List<Departure>,
    liveTripIds: Set<String>,
    operatorTimezone: String,
    distanceMeters: Double? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isImminent = departures.firstOrNull()?.let { isWithinFiveMinutes(it, operatorTimezone) } ?: false
    val borderColor = if (isImminent) TransitTheme.colors.accent.copy(alpha = 0.6f) else Color.Transparent
    val borderWidth = if (isImminent) 1.5.dp else 0.dp

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = TransitTheme.colors.glassFill,
        tonalElevation = 1.dp,
        border = if (isImminent) BorderStroke(borderWidth, borderColor) else null
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stop name row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = stopIcon(stop.transitTypes)),
                    contentDescription = null,
                    tint = TransitTheme.colors.textTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TransitTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                distanceMeters?.let {
                    Text(
                        text = walkingTimeLabel(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = TransitTheme.colors.textTertiary
                    )
                }
            }

            if (departures.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_departures_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = TransitTheme.colors.textTertiary
                )
            } else {
                val shown = departures.take(3)
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    shown.forEachIndexed { index, dep ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // LineBadge accetta ResolvedDeparture — convertiamo
                            val resolved = dep.toResolvedDeparture()
                            LineBadge(
                                departure = resolved,
                                size = LineBadgeSize.Medium
                            )
                            Text(
                                text = dep.headsign,
                                style = MaterialTheme.typography.bodySmall,
                                color = TransitTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (dep.tripId in liveTripIds) {
                                LiveDot()
                            }
                            val timeState = departureTimeState(dep.time, operatorTimezone)
                            TimeDisplay(state = timeState)
                        }
                        if (index < shown.size - 1) {
                            HorizontalDivider(
                                color = TransitTheme.colors.separator,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Verde inline "live" — riproduce il pattern esistente del codebase.
 * Separato qui perché più piccolo dello LiveBadge nominale che non esiste su Android.
 */
@Composable
private fun LiveDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(TransitTheme.colors.realtimeGreen)
    )
}

@Composable
private fun walkingTimeLabel(meters: Double): String {
    val minutes = kotlin.math.ceil(meters / 80.0).toInt()
    return when {
        minutes <= 1 -> stringResource(R.string.walking_1_min)
        minutes > 10 -> stringResource(R.string.walking_10_plus_min)
        else -> stringResource(R.string.walking_n_min, minutes)
    }
}

private fun isWithinFiveMinutes(dep: Departure, tz: String): Boolean {
    val state = departureTimeState(dep.time, tz)
    return when (state) {
        is DepartureTimeState.Departing -> true
        is DepartureTimeState.Minutes -> state.minutes in 0..5
        else -> false
    }
}
```

**Importanti:**
- `dep.toResolvedDeparture()`: se questo metodo non esiste, convertilo inline costruendo `ResolvedDeparture` dai campi disponibili, oppure crea un overload di `LineBadge` che accetta `Departure`. Verifica il tipo esatto in `LineBadge.kt` prima di procedere.
- `departureTimeState(dep.time, tz)`: funzione già usata nel codebase (riferita nella verifica). Se la firma è diversa, adattare.

Import aggiuntivi:
```kotlin
import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.components.stopIcon
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.TimeDisplay
import com.transitkit.app.ui.components.DepartureTimeState
import com.transitkit.app.ui.components.departureTimeState
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
```

---

### Task 18 — Android: sezioni `FavoritesSection` e `NearbySection` aggiornate

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt`

- [ ] **Step 1: `FavoritesSection`**

```kotlin
@Composable
private fun FavoritesSection(
    stops: List<ResolvedStop>,
    departures: Map<String, List<Departure>>,
    liveTripIds: Set<String>,
    operatorTimezone: String,
    onStopClick: (ResolvedStop) -> Unit,
    onBrowseStops: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (stops.isEmpty()) {
        EmptyFavoritesCard(onBrowseStops = onBrowseStops, modifier = modifier)
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.favorites))
        stops.take(5).forEach { stop ->
            StopCard(
                stop = stop,
                departures = departures[stop.id] ?: emptyList(),
                liveTripIds = liveTripIds,
                operatorTimezone = operatorTimezone,
                onClick = { onStopClick(stop) }
            )
        }
    }
}
```

- [ ] **Step 2: `NearbySection` con fallback chip**

```kotlin
@Composable
private fun NearbySection(
    nearbyStops: List<Pair<ResolvedStop, Double>>,
    nearbyDepartures: Map<String, List<Departure>>,
    liveTripIds: Set<String>,
    permissionGranted: Boolean,
    permissionNotDetermined: Boolean,
    onEnableLocation: () -> Unit,
    onStopClick: (ResolvedStop) -> Unit,
    operatorTimezone: String,
    modifier: Modifier = Modifier
) {
    when {
        permissionNotDetermined -> {
            EnableLocationChip(onClick = onEnableLocation, modifier = modifier)
        }
        permissionGranted && nearbyStops.isNotEmpty() -> {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.nearby_you))
                nearbyStops.forEach { (stop, distance) ->
                    StopCard(
                        stop = stop,
                        departures = nearbyDepartures[stop.id] ?: emptyList(),
                        liveTripIds = liveTripIds,
                        operatorTimezone = operatorTimezone,
                        distanceMeters = distance,
                        onClick = { onStopClick(stop) }
                    )
                }
            }
        }
        // permissionGranted && vuoto → sezione nascosta
        // permission denied → sezione nascosta
        else -> Unit
    }
}

@Composable
private fun EnableLocationChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = TransitTheme.colors.glassFill,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = TransitTheme.colors.accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.home_enable_location_chip),
                style = MaterialTheme.typography.labelMedium,
                color = TransitTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = TransitTheme.colors.textTertiary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
```

Import: `androidx.compose.material.icons.outlined.LocationOn`.

---

### Task 19 — Android: empty state + footer + Settings

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt`
- Modify: `android/app/src/main/java/com/transitkit/app/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: `EmptyFavoritesCard` (sostituisce `EmptyFavoritesState`)**

Rimuovere `EmptyFavoritesState` esistente e aggiungere:

```kotlin
@Composable
private fun EmptyFavoritesCard(
    onBrowseStops: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TransitTheme.colors.glassFill,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TransitTheme.colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = null,
                    tint = TransitTheme.colors.accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = stringResource(R.string.home_empty_favorites_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TransitTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.home_empty_favorites_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TransitTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onBrowseStops,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TransitTheme.colors.accent,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.home_empty_favorites_cta))
            }
        }
    }
}
```

Import: `androidx.compose.material.icons.outlined.Bookmark`, `androidx.compose.ui.text.style.TextAlign`.

- [ ] **Step 2: `HomeFooterDisclaimer`**

```kotlin
@Composable
private fun HomeFooterDisclaimer(
    operatorName: String,
    modifier: Modifier = Modifier
) {
    if (operatorName.isEmpty()) return
    Text(
        text = stringResource(R.string.home_footer_disclaimer, operatorName),
        style = MaterialTheme.typography.labelSmall,
        color = TransitTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}
```

- [ ] **Step 3: Sezione Privacy in `SettingsScreen.kt`**

Trovare il `LazyColumn` principale. Aggiungere (prima delle sezioni esistenti):

```kotlin
item {
    SectionTitle(text = stringResource(R.string.settings_location_section))
}
item {
    LocationAccessRow(
        hasPermission = /* calcolato da PermissionStatus */,
        onEnableLocation = { /* launcher */ },
        onOpenSettings = { /* intent Settings.ACTION_APPLICATION_DETAILS_SETTINGS */ }
    )
}
```

Composable `LocationAccessRow`:

```kotlin
@Composable
private fun LocationAccessRow(
    hasPermission: Boolean,
    canRequest: Boolean,
    onEnableLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = TransitTheme.colors.glassFill
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = TransitTheme.colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_location_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TransitTheme.colors.textPrimary
                )
            }
            when {
                hasPermission -> Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = TransitTheme.colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                canRequest -> Button(onClick = onEnableLocation) {
                    Text(stringResource(R.string.settings_location_enable))
                }
                else -> OutlinedButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.settings_location_open))
                }
            }
        }
    }
}
```

Import: `androidx.compose.material.icons.outlined.CheckCircle`.

- [ ] **Step 4: Sezione Informazioni in `SettingsScreen.kt`**

Alla fine del LazyColumn:

```kotlin
item {
    SectionTitle(text = stringResource(R.string.settings_info_section))
}
item {
    uiState.config?.let { config ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = TransitTheme.colors.glassFill
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_disclaimer_body, config.name, config.fullName),
                    style = MaterialTheme.typography.bodySmall,
                    color = TransitTheme.colors.textSecondary
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TransitTheme.colors.textTertiary
                )
            }
        }
    }
}
```

Import: `com.transitkit.app.BuildConfig`.

---

### Task 20 — Android: build finale + install + screenshot + commit

- [ ] **Step 1: Build**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine/android
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD SUCCESSFUL|BUILD FAILED"
```

- [ ] **Step 2: Install + launch**

```bash
ADB=/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb
ANDROID_SERIAL=$($ADB devices | grep emulator | awk '{print $1}' | while read s; do
  $ADB -s $s emu avd name 2>/dev/null | grep -q "transitkit-dev" && echo $s && break
done)
$ADB -s $ANDROID_SERIAL uninstall com.transitkit.appalcart 2>/dev/null
$ADB -s $ANDROID_SERIAL install android/app/build/outputs/apk/debug/app-debug.apk
$ADB -s $ANDROID_SERIAL shell am start -n com.transitkit.appalcart/.MainActivity
```

- [ ] **Step 3: Screenshot primer + Home**

```bash
sleep 3
$ADB -s $ANDROID_SERIAL shell screencap -p /sdcard/android_primer.png
$ADB -s $ANDROID_SERIAL pull /sdcard/android_primer.png /tmp/android_primer.png
sips -Z 800 /tmp/android_primer.png --out /tmp/android_primer_s.png

# Tap "Non ora"
$ADB -s $ANDROID_SERIAL shell input tap 540 1800

sleep 2
$ADB -s $ANDROID_SERIAL shell screencap -p /sdcard/android_home.png
$ADB -s $ANDROID_SERIAL pull /sdcard/android_home.png /tmp/android_home_v2.png
sips -Z 800 /tmp/android_home_v2.png --out /tmp/android_home_v2_s.png
```

- [ ] **Step 4: Commit Android**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine
git add android/
git commit -m "feat(android/home): redesign con sfondo operatore, onboarding posizione, no urgency sort"
```

---

## PHASE 4 — Verification

### Task 21 — Consistenza + visual QA

- [ ] **Step 1: Checklist visiva**

Confrontare `/tmp/ios_home_v2_s.png` con `/tmp/android_home_v2_s.png`. Verifica:

- [ ] Sfondo mappa visibile in entrambi (~15% opacità)
- [ ] Nessun hero verde in entrambi
- [ ] Header minimalista con logo + nome operatore in entrambi
- [ ] Alert chip sopra l'header (se presenti) in entrambi
- [ ] Card preferiti con 3 partenze e stroke accent sul next imminente
- [ ] Card `walkingTime` localizzata (non "420m" ma "5 min a piedi" / "5 min walk")
- [ ] Chip "Attiva posizione" visibile se GPS `notDetermined`
- [ ] Footer disclaimer presente, leggibile sotto gradient fade

- [ ] **Step 2: Test onboarding flow**

Su entrambi: reset permessi + fresh install → verifica che il primer appaia al primo launch.

- [ ] **Step 3: Test Settings**

Su entrambi: aprire Settings → verifica sezione Privacy (stato posizione + bottone) + sezione Informazioni (disclaimer).

- [ ] **Step 4: Test empty state preferiti**

Rimuovere tutti i preferiti → verifica che appaia la card glass con CTA "Sfoglia fermate" → tap la porta al tab Orari.

- [ ] **Step 5: Commit visuals**

```bash
cd /Users/andreatoffanello/GitHub/transit-engine
# (solo se vogliamo archiviare gli screenshot nel repo; opzionale)
# mkdir -p docs/screenshots/home-redesign
# cp /tmp/ios_home_v2_s.png /tmp/android_home_v2_s.png docs/screenshots/home-redesign/
# git add docs/screenshots/home-redesign/
# git commit -m "docs: screenshot home redesign v2"
```

---

## Self-Review

**Spec coverage:**
- ✅ Opzione C (glass-over-map) implementata
- ✅ Sfondo operatore full-page con opacità percepibile (15%)
- ✅ Hero card rimossa, header minimalista
- ✅ Alert chip sopra header (safety-critical)
- ✅ Ordine manuale preferiti (no urgency sort)
- ✅ Visual emphasis sul "next imminente" via stroke accent
- ✅ 5 preferiti, 3 partenze per card
- ✅ Walking time (non distanza)
- ✅ Onboarding primer screen al primo launch
- ✅ Fallback chip in Home per `notDetermined`
- ✅ Settings > Privacy per gestire permesso
- ✅ Settings > Informazioni con disclaimer esteso
- ✅ Footer disclaimer con gradient fade per leggibilità
- ✅ Empty state preferiti con card glass singola

**Non coperto (deferred, confermato dall'utente):**
- Drag-to-reorder preferiti (feature separata)
- Redesign radicale map-as-interface (scartato)

**Fix vs plan v1:**
- PressableButtonStyle estratto in file proprio (Task 1)
- Stringhe xcstrings con formato JSON esatto (Task 2)
- Tipi Android corretti: `Departure`, `ServiceAlert`, `TransitTheme.colors`, `stopIcon()` come @DrawableRes → `Icon(painterResource(...))`, `DepartureTimeState` via `departureTimeState()`, `LiveDot` inline (non `LiveBadge`)
- `nearbyStops` filtrato a 400m nel ViewModel (Task 13)
- `nearbyDepartures` flow dedicato nel ViewModel (Task 13)
- `walkingTimeLabel` come `@Composable` con `stringResource` (Task 17)
- Image su Android con `alpha` e `colorFilter` come parametri (non Modifier) (Task 14)
- Onboarding primer screen invece di card vuoto-promozionale (Task 5, 15)
- No urgency sort (Task 7)
- Footer gradient fade per leggibilità (Task 6, 16)

**Ambiguità risolte:**
- `dep.toResolvedDeparture()` in Android: Task 17 nota che va verificato il tipo esatto — se non esiste, usare overload `LineBadge(name, colorHex, textColorHex, transitType, size)` con i campi di `Departure`. Verifica in Task 17 Step 2.
- `departureTimeState()` Android: assumiamo esista (menzionato nella verifica del codice). Se firma diversa, adattare in Task 17.
