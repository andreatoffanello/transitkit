# Android ↔ iOS Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allineare TransitKit Android alle modifiche iOS delle ultime sessioni (mappa unificata, hero map detail screens, departure row con live dot inline, home con sezioni differenziate, planner picker fix).

**Architecture:** Monorepo white-label. Modifiche limitate a `android/app/src/main/java/com/transitkit/app/ui/**` + `res/values/strings.xml`. Riusiamo i pattern già consolidati in Movete (`/Users/andreatoffanello/GitHub/movete/android/app/src/main/java/com/movete/roma/`) come riferimento di stile per i Composable nuovi (MapCameraControlsPill, LiveIndicator inline).

**Tech Stack:** Jetpack Compose · Material 3 · Mapbox Maps SDK Android v11 · Hilt · Coroutines · Lucide Icons (drawables).

**Convenzioni globali da rispettare in ogni task:**
- Touch target ≥ 48dp
- Sentence case (mai `.toUpperCase()`, mai `letterSpacing` per UPPERCASE)
- Icone solo da `LucideIcons.*` (drawable resource) — mai pathData inventato
- Nessuna `animateFloat(repeatMode=Restart/Reverse, iterations=Infinite)` dentro `LazyColumn` items
- Tint user location: blu di sistema (default Mapbox `createDefault2DPuck()`), mai operator accent
- `.semantics { testTag = "..." }` su ogni nuovo elemento interattivo

**Project pinned values** (dal CLAUDE.md):
- Emulator: `transitkit-dev` (serial: `emulator-5556`)
- Package: `com.transitkit.appalcart`
- Java: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- adb path: `/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb`

**Build & install (riferimento ricorrente):**
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug -PoperatorId=appalcart 2>&1 | tail -20
/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb -s emulator-5556 \
  install -r app/build/outputs/apk/debug/app-debug.apk
/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb -s emulator-5556 \
  shell am start -n com.transitkit.appalcart/com.transitkit.app.MainActivity
```

---

## Fase 1 — Componenti foundation

Modifiche ai building block usati da Home, StopDetail, LineDetail, LineeScreen, MappaScreen. Vanno prima di tutto perché toccano le API.

### Task 1.1: TimeDisplay — nuovo case `HoursMinutes` + wrap-around + threshold + liveDot inline

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/components/TimeDisplay.kt`

**Riferimento:** vedi struttura `Movete/components/CountdownLabel.kt` per il pattern "1h23'" e `LiveIndicator` 5dp inline.

- [ ] **Step 1: Aggiungere case `HoursMinutes` e parametro `liveDot` al sealed class**

```kotlin
// In TimeDisplay.kt — sealed class DepartureTimeState
sealed class DepartureTimeState {
    abstract val clockHHmm: String
    data class Absolute(override val clockHHmm: String) : DepartureTimeState()
    data class Departing(override val clockHHmm: String) : DepartureTimeState()
    data class Minutes(val minutes: Int, override val clockHHmm: String) : DepartureTimeState()
    /** Nuovo: render "1h 23'" per partenze fra 60 min e 24h. */
    data class HoursMinutes(val hours: Int, val minutes: Int, override val clockHHmm: String) : DepartureTimeState()
    data class Hours(val hours: Int, override val clockHHmm: String) : DepartureTimeState()
    data class Passed(override val clockHHmm: String) : DepartureTimeState()
}
```

- [ ] **Step 2: Refactor `computeDepartureTimeState` con wrap-around + soglia configurabile**

Sostituisci interamente la funzione esistente:

```kotlin
/**
 * @param departureMinutes minuti del giorno della partenza
 * @param nowMinutes minuti del giorno correnti
 * @param clockHHmm stringa "HH:mm" della partenza
 * @param relativeThreshold soglia oltre cui mostrare orario assoluto (default 60).
 *   Passa 1440 per "sempre relativo".
 */
internal fun computeDepartureTimeState(
    departureMinutes: Int,
    nowMinutes: Int,
    clockHHmm: String,
    relativeThreshold: Int = 60,
): DepartureTimeState {
    var diff = departureMinutes - nowMinutes
    // Wrap-around: partenze "passate" da più di 1h → trattate come prossima occorrenza il giorno dopo
    if (diff < -60) diff += 1440
    return when {
        diff < 0 -> DepartureTimeState.Passed(clockHHmm)
        diff == 0 -> DepartureTimeState.Departing(clockHHmm)
        diff < 60 -> DepartureTimeState.Minutes(diff, clockHHmm)
        diff < relativeThreshold -> {
            val h = diff / 60
            val m = diff % 60
            if (m == 0) DepartureTimeState.Hours(h, clockHHmm)
            else DepartureTimeState.HoursMinutes(h, m, clockHHmm)
        }
        else -> DepartureTimeState.Absolute(clockHHmm)
    }
}
```

- [ ] **Step 3: Aggiungere param `relativeThreshold` a `departureTimeState(timeStr, tz)`**

```kotlin
fun departureTimeState(
    timeStr: String,
    operatorTimezoneId: String,
    relativeThreshold: Int = 60,
): DepartureTimeState {
    // ... parsing invariato …
    return runCatching {
        val parts = timeStr.split(":")
        val depMin = parts[0].toInt() * 60 + parts[1].toInt()
        val cal = Calendar.getInstance(TimeZone.getTimeZone(operatorTimezoneId))
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        computeDepartureTimeState(depMin, nowMin, timeStr.take(5), relativeThreshold)
    }.getOrElse { DepartureTimeState.Absolute(timeStr.take(5)) }
}
```

- [ ] **Step 4: Refactor Composable `TimeDisplay` con param `liveDot` e nuovo case**

```kotlin
@Composable
fun TimeDisplay(
    state: DepartureTimeState,
    modifier: Modifier = Modifier,
    isEmphasis: Boolean = false,
    liveDot: Boolean = false,
) {
    val colors = LocalAppColors.current
    val primaryFontSize = if (isEmphasis) 20.sp else 17.sp
    Column(modifier = modifier.defaultMinSize(minWidth = 52.dp), horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (liveDot && state !is DepartureTimeState.Departing && state !is DepartureTimeState.Passed) {
                Box(Modifier.size(7.dp).background(colors.realtimeGreen, CircleShape))
            }
            when (state) {
                is DepartureTimeState.Departing -> DepartingPulse(isEmphasis)
                is DepartureTimeState.Minutes -> Text(
                    text = "${state.minutes}'",
                    fontSize = primaryFontSize, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (state.minutes <= 5) colors.realtimeGreen else colors.textPrimary,
                )
                is DepartureTimeState.Hours -> Text(
                    text = "${state.hours}h",
                    fontSize = primaryFontSize, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = colors.textPrimary,
                )
                is DepartureTimeState.HoursMinutes -> Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.hours}h",
                        fontSize = primaryFontSize, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = colors.textPrimary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${state.minutes}'",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace, color = colors.textSecondary,
                    )
                }
                is DepartureTimeState.Absolute -> Text(
                    text = state.clockHHmm, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace, color = colors.textPrimary,
                )
                is DepartureTimeState.Passed -> Text(
                    text = state.clockHHmm, fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace, color = colors.textTertiary,
                )
            }
        }
        // Clock secondario solo quando NON in modalità "always relative"
        when (state) {
            is DepartureTimeState.Departing, is DepartureTimeState.Minutes,
            is DepartureTimeState.Hours, is DepartureTimeState.HoursMinutes -> ClockText(state.clockHHmm, 11.sp, colors.textSecondary)
            else -> Unit
        }
    }
}
```

- [ ] **Step 5: Build + verifica compilazione**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:compileDebugKotlin -PoperatorId=appalcart 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL. Se ci sono errori `Unresolved reference DepartureTimeState.HoursMinutes` in call site, sono attesi e verranno risolti in Task 1.3.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/components/TimeDisplay.kt
git commit -m "feat(android): TimeDisplay supports HoursMinutes case, configurable threshold, wrap-around, liveDot inline"
```

---

### Task 1.2: DepartureRow — live dot inline (rimuove colonna pulse separata)

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailDepartureRow.kt`

- [ ] **Step 1: Rimuovi la Box pulse separata e passa `liveDot = departure.isRealtime` a TimeDisplay**

Sostituisci il blocco interno del `Row` principale (era: LineBadge + Column weight + Box pulse 14dp + Column End). Diventa:

```kotlin
Row(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
) {
    LineBadge(
        name = badgeName,
        colorHex = departure.routeColor,
        textColorHex = departure.routeTextColor,
        size = LineBadgeSize.Large,
    )
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = departure.headsign,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (showStopSequence) {
            Text(
                text = stopSequence,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Column(horizontalAlignment = Alignment.End) {
        TimeDisplay(
            state = departureTimeState(displayTime, operatorTimezone),
            isEmphasis = isNext,
            liveDot = departure.isRealtime,
        )
        if (delaySec != null && delaySec != 0) {
            Text(
                text = if (delaySec > 0) "+${delaySec / 60}m" else "${delaySec / 60}m",
                color = if (delaySec > 0) colors.realtimeRed else colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
```

- [ ] **Step 2: Rimuovi import non più usati** (`InfiniteRepeatableSpec`, `keyframes` se usati solo dalla pulse rimossa).

- [ ] **Step 3: Build**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:compileDebugKotlin -PoperatorId=appalcart 2>&1 | tail -10
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailDepartureRow.kt
git commit -m "feat(android): DepartureRow live dot inline via TimeDisplay (no floating column)"
```

---

### Task 1.3: Migrate tutti i call site di `TimeDisplay` / `departureTimeState`

**Files (scan e aggiorna):**
- `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt` (StopCardDepartureRow)
- `android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailDeparturesList.kt`
- `android/app/src/main/java/com/transitkit/app/ui/orari/LineDetailStopsTimeline.kt`
- `android/app/src/main/java/com/transitkit/app/ui/mappa/StopPreviewContent.kt` (se esiste)
- `android/app/src/main/java/com/transitkit/app/ui/mappa/VehiclePreviewContent.kt` (se esiste)
- `android/app/src/main/java/com/transitkit/app/ui/planner/JourneyCard.kt` (se usa TimeDisplay)

- [ ] **Step 1: Scan dei call site**

```bash
grep -rn "TimeDisplay\|departureTimeState\|computeDepartureTimeState" \
  /Users/andreatoffanello/GitHub/transit-engine/android/app/src/main/java/com/transitkit/app/ \
  | grep -v "/TimeDisplay.kt"
```

Per ciascun risultato, leggi il contesto e decidi se passare `liveDot` e `relativeThreshold`.

- [ ] **Step 2: Home `StopCardDepartureRow` → `relativeThreshold = 1440` (sempre relativo) + `liveDot` se realtime**

In `HomeScreen.kt`, dentro `StopCardDepartureRow`, sostituisci la chiamata a `TimeDisplay` con:

```kotlin
TimeDisplay(
    state = departureTimeState(displayTime, operatorTimezone, relativeThreshold = 1440),
    liveDot = departure.isRealtime,
)
```

- [ ] **Step 3: StopDetail full schedule + line detail timeline → default threshold 60, no liveDot**

Lasciano i comportamenti attuali (orario assoluto per partenze > 1h).

- [ ] **Step 4: Build + lint warnings check**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug -PoperatorId=appalcart 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL, zero warning nuovi.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/
git commit -m "refactor(android): migrate TimeDisplay call sites to liveDot + threshold params"
```

---

### Task 1.4: LineBadge — fix clipping nomi 4-5 char

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/components/LineBadge.kt`

**Riferimento iOS:** il bug era che `parent` con `weight` schiacciava il badge. Su Compose il pattern equivalente è `weight(1f).fillMaxWidth()` che impone width al figlio. Fix: badge usa `wrapContentWidth(unbounded = true)` sull'esterno del background.

- [ ] **Step 1: Wrappa il `Box`/`Row` esterno del badge con `wrapContentWidth(unbounded = true)`**

Nel Composable low-level `LineBadge(name, colorHex, ...)`, modifica il modifier del Box esterno:

```kotlin
Box(
    modifier = modifier
        .wrapContentWidth(unbounded = false)         // < importantissimo
        .defaultMinSize(minWidth = d.minW)
        .background(bg, RoundedCornerShape(d.radius))
        .padding(horizontal = d.padH, vertical = d.padV)
        .semantics { contentDescription = name },
    contentAlignment = Alignment.Center,
) {
    Text(
        text = name,
        fontSize = d.fontSize,
        fontWeight = FontWeight.Bold,
        color = fg,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,    // < non clippare nel parent
        style = LocalTextStyle.current.copy(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeight = 1.em,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        ),
    )
}
```

- [ ] **Step 2: Verifica i call site dove `LineBadge` è dentro `Row` con `weight`**

```bash
grep -rn "LineBadge" /Users/andreatoffanello/GitHub/transit-engine/android/app/src/main/java/com/transitkit/app/ \
  --include="*.kt" -B1 -A3 | grep -E "weight|LineBadge"
```

Nei call site dove vedi pattern tipo:
```kotlin
LineBadge(..., modifier = Modifier.weight(...))  // SBAGLIATO
```
Rimuovi `weight`. Il badge non deve mai espandersi a riempire spazio. I genitori (`Column(weight(1f))` con headsign) devono prendere il resto, il badge resta intrinseco.

- [ ] **Step 3: Build + smoke visivo**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug -PoperatorId=appalcart 2>&1 | tail -10
```

Install e screenshot. Apri una fermata con linea a 5 char (es. cerca route con `name.length >= 5` in `LineeScreen`):

```bash
ADB=/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb
$ADB -s emulator-5556 exec-out screencap -p > /tmp/badge_after.png && sips -Z 800 /tmp/badge_after.png --out /tmp/badge_after_s.png
```

Verifica visivamente: testo non clippato, background coerente.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/components/LineBadge.kt
git commit -m "fix(android): LineBadge non si comprime in parent con weight (no clipping su 4-5 char)"
```

---

### Task 1.5: Strings — nuove chiavi sentence case + map controls i18n

**Files:**
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/main/res/values-en/strings.xml` (se esiste)
- Modify: `android/app/src/main/res/values-it/strings.xml` (se esiste)

- [ ] **Step 1: Lista chiavi da aggiungere**

```xml
<!-- Home sections (sentence case) -->
<string name="home_section_favorites">Fermate preferite</string>
<string name="home_section_nearby">Fermate vicino a te</string>

<!-- Map controls -->
<string name="map_switch_to_2d">Passa a 2D</string>
<string name="map_switch_to_3d">Passa a 3D</string>
<string name="map_recenter">Centra sulla mia posizione</string>
<string name="map_reset_bearing">Reimposta orientamento</string>
<string name="map_expand">Espandi mappa</string>
<string name="map_close_expanded">Chiudi mappa</string>

<!-- Line detail live count -->
<plurals name="lines_live_count">
    <item quantity="one">%d in tempo reale</item>
    <item quantity="other">%d in tempo reale</item>
</plurals>

<!-- Favorite star -->
<string name="cd_aggiungi_preferiti">Aggiungi ai preferiti</string>
<string name="cd_rimuovi_preferiti">Rimuovi dai preferiti</string>
```

- [ ] **Step 2: Versione inglese** (se file `values-en/strings.xml` esiste)

```xml
<string name="home_section_favorites">Favorite stops</string>
<string name="home_section_nearby">Stops near you</string>
<string name="map_switch_to_2d">Switch to 2D</string>
<string name="map_switch_to_3d">Switch to 3D</string>
<string name="map_recenter">Center on my location</string>
<string name="map_reset_bearing">Reset bearing</string>
<string name="map_expand">Expand map</string>
<string name="map_close_expanded">Close map</string>
<plurals name="lines_live_count">
    <item quantity="one">%d live</item>
    <item quantity="other">%d live</item>
</plurals>
```

- [ ] **Step 3: Build (verifica niente duplicati)**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:processDebugResources -PoperatorId=appalcart 2>&1 | tail -15
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/res/values/strings.xml android/app/src/main/res/values-*/strings.xml
git commit -m "i18n(android): aggiunge keys per home sections sentence case e map controls"
```

---

### Task 1.6: Star drawable — verifica path Lucide ufficiale

**Files:**
- Verify: `android/app/src/main/res/drawable/ic_lucide_star.xml`
- Verify: `android/app/src/main/res/drawable/ic_lucide_star_filled.xml`

- [ ] **Step 1: Leggi entrambi i drawable e confronta i pathData con Lucide ufficiale**

Reference path Lucide v0.456:
- `star` (outline): `M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z`
- `star-filled` (versione filled di lucide): stesso `d` ma con `fill="currentColor"` invece di `fill="none"` e niente `stroke`.

Se i path differiscono o `ic_lucide_star_filled.xml` è solo `ic_lucide_star.xml` con colore diverso (non filled), riscrivi il drawable filled.

- [ ] **Step 2: Riscrivi `ic_lucide_star_filled.xml` se serve**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M11.525,2.295a.53,.53 0 0 1 .95,0l2.31,4.679a2.123,2.123 0 0 0 1.595,1.16l5.166,.756a.53,.53 0 0 1 .294,.904l-3.736,3.638a2.123,2.123 0 0 0 -0.611,1.878l.882,5.14a.53,.53 0 0 1 -0.771,.56l-4.618,-2.428a2.122,2.122 0 0 0 -1.973,0L6.396,21.01a.53,.53 0 0 1 -0.77,-0.56l.881,-5.139a2.122,2.122 0 0 0 -0.611,-1.879L2.16,9.795a.53,.53 0 0 1 .294,-0.906l5.165,-0.755a2.122,2.122 0 0 0 1.597,-1.16z" />
</vector>
```

- [ ] **Step 3: Build resources**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:processDebugResources -PoperatorId=appalcart 2>&1 | tail -10
```

- [ ] **Step 4: Commit (se modificato)**

```bash
git add android/app/src/main/res/drawable/ic_lucide_star_filled.xml
git commit -m "fix(android): ic_lucide_star_filled usa il pathData filled ufficiale Lucide"
```

---

## Fase 2 — Mappa unificata

Sostituisce i FAB Material di MappaScreen con un singolo pill verticale stile Movete + estende lo stesso pill all'overlay mappa espansa (StopDetail + LineDetail in Fase 3).

### Task 2.1: Crea `UnifiedMapControlsPill` (sostituisce `MappaFabColumn`)

**Files:**
- Create: `android/app/src/main/java/com/transitkit/app/ui/mappa/UnifiedMapControlsPill.kt`
- Delete: `android/app/src/main/java/com/transitkit/app/ui/mappa/MappaFabColumn.kt` (a fine task 2.2)

**Riferimento Movete:** `/Users/andreatoffanello/GitHub/movete/android/app/src/main/java/com/movete/roma/components/MapCameraControlsPill.kt` — usa la stessa struttura ma con `RoundedCornerShape(percent=50)`. Noi usiamo `RoundedCornerShape(22.dp)` come iOS.

- [ ] **Step 1: Scrivi il Composable**

```kotlin
package com.transitkit.app.ui.mappa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.ui.theme.LocalAppColors
import kotlin.math.abs

/**
 * Pill verticale unico per controlli mappa.
 * Bottoni in ordine top→bottom: 2D/3D · Recenter · Reset bearing (opt) · Expand/Close (opt).
 * 44dp per cella, divider 28dp × 0.5dp tra celle.
 */
@Composable
fun UnifiedMapControlsPill(
    is3D: Boolean,
    onToggle3D: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
    currentBearing: Double = 0.0,
    onResetBearing: (() -> Unit)? = null,
    expanded: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val showResetBearing = onResetBearing != null && abs(currentBearing) > 1.0
    Surface(
        modifier = modifier
            .width(44.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = colors.glassFill,
        tonalElevation = 3.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 2D/3D
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onToggle3D)
                    .semantics {
                        contentDescription = if (is3D) "Passa a 2D" else "Passa a 3D"
                    }
                    .testTag("btn_map_toggle_3d"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (is3D) "2D" else "3D",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (is3D) colors.accent else colors.textPrimary,
                )
            }
            PillDivider()

            // Recenter (navigation arrow)
            IconButton(
                onClick = onRecenter,
                modifier = Modifier.size(44.dp).testTag("btn_map_recenter"),
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Navigation),
                    contentDescription = "Centra sulla mia posizione",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Reset bearing (compass) — condizionale
            AnimatedVisibility(
                visible = showResetBearing,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f),
            ) {
                Column {
                    PillDivider()
                    IconButton(
                        onClick = { onResetBearing?.invoke() },
                        modifier = Modifier.size(44.dp).testTag("btn_map_reset_bearing"),
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Compass),
                            contentDescription = "Reimposta orientamento",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Expand / Close — condizionale (solo se passato)
            if (onExpandToggle != null) {
                PillDivider()
                IconButton(
                    onClick = onExpandToggle,
                    modifier = Modifier.size(44.dp).testTag("btn_map_expand"),
                ) {
                    Icon(
                        painter = painterResource(
                            if (expanded) LucideIcons.Minimize2 else LucideIcons.Maximize2
                        ),
                        contentDescription = if (expanded) "Chiudi mappa" else "Espandi mappa",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PillDivider() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(0.5.dp)
            .background(colors.textPrimary.copy(alpha = 0.08f)),
    )
}
```

- [ ] **Step 2: Verifica icone in `LucideIcons.kt`**

Apri `android/app/src/main/java/com/transitkit/app/config/LucideIcons.kt` e verifica che esistano:
- `Navigation` (alias di `navigation`)
- `Compass`
- `Maximize2`
- `Minimize2`

Se mancanti, aggiungi i drawable corrispondenti da Lucide ufficiale.

- [ ] **Step 3: Build**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:compileDebugKotlin -PoperatorId=appalcart 2>&1 | tail -10
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/mappa/UnifiedMapControlsPill.kt
git add android/app/src/main/java/com/transitkit/app/config/LucideIcons.kt
git add android/app/src/main/res/drawable/ic_lucide_*.xml
git commit -m "feat(android): UnifiedMapControlsPill — controlli mappa pill verticale"
```

---

### Task 2.2: Wire `UnifiedMapControlsPill` in `MappaScreen`

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/mappa/MappaScreen.kt`
- Delete: `android/app/src/main/java/com/transitkit/app/ui/mappa/MappaFabColumn.kt`

- [ ] **Step 1: Sostituisci `MappaFabColumn` con `UnifiedMapControlsPill` in `MappaScreen`**

In `MappaScreen.kt` trova il blocco:
```kotlin
MappaFabColumn(
    is3D = is3D,
    onResetView = ...,
    onRecenter = ...,
    onToggle3D = ...,
    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
        .windowInsetsPadding(WindowInsets.navigationBars),
)
```

Sostituisci con:
```kotlin
// In MappaScreen, leggi bearing dalla camera viewport
val currentBearing by remember { derivedStateOf { viewportState.cameraState?.bearing ?: 0.0 } }

UnifiedMapControlsPill(
    is3D = is3D,
    onToggle3D = { /* flyTo pitch toggle, identico a prima */ },
    onRecenter = { /* flyTo center, identico a prima */ },
    currentBearing = currentBearing,
    onResetBearing = {
        viewportState.flyTo(
            cameraOptions {
                bearing(0.0)
            },
            MapAnimationOptions.mapAnimationOptions { duration(500L) },
        )
    },
    modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 16.dp)
        .windowInsetsPadding(WindowInsets.navigationBars),
)
```

In MappaTab NON passare `onExpandToggle`: l'expand serve solo dentro StopDetail/LineDetail map hero overlay.

- [ ] **Step 2: Elimina `MappaFabColumn.kt`**

```bash
rm /Users/andreatoffanello/GitHub/transit-engine/android/app/src/main/java/com/transitkit/app/ui/mappa/MappaFabColumn.kt
```

- [ ] **Step 3: Cerca import residui**

```bash
grep -rn "MappaFabColumn" /Users/andreatoffanello/GitHub/transit-engine/android/
```
Deve restituire zero risultati.

- [ ] **Step 4: Build + smoke visivo**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug -PoperatorId=appalcart 2>&1 | tail -10
ADB=/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb
$ADB -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s emulator-5556 shell am start -n com.transitkit.appalcart/com.transitkit.app.MainActivity
# Apri tab Mappa
$ADB -s emulator-5556 exec-out screencap -p > /tmp/mappa.png && sips -Z 800 /tmp/mappa.png --out /tmp/mappa_s.png
```

Verifica: pill verticale singolo a destra centro, no FAB Material rotondi. Bearing != 0 → appare il pulsante compass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(android): MappaScreen usa UnifiedMapControlsPill (rimuove MappaFabColumn)"
```

---

### Task 2.3: User location puck — verifica z-order sopra veicoli

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/mappa/UserLocationPuck.kt`
- Modify: `android/app/src/main/java/com/transitkit/app/ui/mappa/MapAnnotations.kt`

Mapbox Maps SDK Android v11 renderizza il puck come **layer separato sopra gli annotation manager** per default. Ma se i `PointAnnotation` veicoli vengono renderizzati DOPO l'inizializzazione del puck, possono coprirlo. Forza il puck come ultimo layer.

- [ ] **Step 1: Aggiungi `setLayerPosition` o ricrea puck dopo veicoli**

In `UserLocationPuck.kt`, dopo `mapView.location.updateSettings { ... }`, aggiungi `MapEffect` che riporta il puck sopra:

```kotlin
MapEffect(Unit) { map ->
    map.mapboxMap.getStyle { style ->
        // I layer creati da location plugin: "mapbox-location-indicator-layer"
        // Spostalo sopra tutto.
        runCatching {
            style.moveStyleLayer(
                "mapbox-location-indicator-layer",
                LayerPosition(null, null, null), // null = top
            )
        }
    }
}
```

Documenta che il blu del puck è quello nativo Mapbox e NON va tinted con accent operatore.

- [ ] **Step 2: Smoke visivo**

Build + install + screenshot con un veicolo sopra la posizione utente: il pallino blu deve essere sopra.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/mappa/UserLocationPuck.kt
git commit -m "fix(android): user location puck sempre sopra layer veicoli/stop"
```

---

### Task 2.4: VehicleAnnotationView — fix gap badge/tail/ring

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/mappa/MapAnnotations.kt` (composable `VehicleAnnotationView`)

**Riferimento iOS:** in host 100×100, con badge pill ~20pt sopra + tail 5pt + dot, math richiede `centerY = ringRadius` quando badge presente, altrimenti `size.height/2`. Spacer condizionale di balance.

- [ ] **Step 1: Audita VehicleAnnotationView**

Apri `MapAnnotations.kt` e leggi il composable `VehicleAnnotationView`. Identifica:
- Dimensione host (Box outer)
- Posizione Y del badge, tail, halo+ring+dot

- [ ] **Step 2: Applica correzione per tier `Street` (badge visibile)**

Pattern target:

```kotlin
@Composable
fun VehicleAnnotationView(/* ... */) {
    val showBadge = tier == MapZoomTier.Street
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showBadge) {
                // Badge pill ~20dp height
                Box(/* pill with route name */)
                // Tail triangolo 8x5dp, immediatamente sotto, no spacer
                Canvas(modifier = Modifier.size(width = 8.dp, height = 5.dp)) {
                    val path = Path().apply {
                        moveTo(0f, 0f); lineTo(size.width, 0f)
                        lineTo(size.width / 2, size.height); close()
                    }
                    drawPath(path, color = lineColor)
                }
            }
            // Halo + ring + dot — Canvas singolo
            Canvas(modifier = Modifier.size(haloSize)) { /* … */ }
            if (showBadge) {
                // Balance spacer per bilanciare centro coordinata
                Spacer(Modifier.height(11.dp))
            }
        }
    }
}
```

**Verifica matematica:** in Box 100×100 il centro è y=50. La Column è centrata. Senza badge: dot=50 (corretto). Con badge: pill 20 + tail 5 + halo (es. 24) + balance 11 = 60 → centro Column a y=20, dot al centro halo = y=20+5+24/2=37. Aggiusta `balance` a `(pillH + tailH + spacer = ?)` finché il centro del dot cade a y=50. Empiricamente, partendo da iOS spec, `balance = pill_height - 9 = 11dp`.

- [ ] **Step 3: Build + smoke**

Avvia simulatore, naviga a tier Street, screenshot, misura visivamente: badge → tail → dot devono essere flush (0 gap).

```bash
$ADB -s emulator-5556 exec-out screencap -p > /tmp/vehicle_pin.png && sips -Z 800 /tmp/vehicle_pin.png --out /tmp/vehicle_pin_s.png
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/mappa/MapAnnotations.kt
git commit -m "fix(android): VehicleAnnotationView elimina gap badge/tail/ring a tier Street"
```

---

### Task 2.5: PlannerLocationPickerMap — halo background + stem stretch

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/planner/LocationPickerMapScreen.kt`

**Riferimento iOS:** halo `Circle()` 48×48 era dentro VStack → occupava layout. Spostato come `.background`. Stem 12 → 20dp quando camera in movimento.

- [ ] **Step 1: Estrai pin centrale in Composable con halo come `drawBehind` overlay**

Sostituisci righe 270–287 con:

```kotlin
// State della camera movement
var isCameraMoving by remember { mutableStateOf(false) }
LaunchedEffect(currentLat, currentLon) {
    isCameraMoving = true
    delay(300)
    isCameraMoving = false
}
val stemHeight by animateDpAsState(
    targetValue = if (isCameraMoving) 20.dp else 12.dp,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "stemHeight",
)
val dotLift by animateDpAsState(
    targetValue = if (isCameraMoving) 8.dp else 0.dp,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "dotLift",
)

Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .offset(y = -dotLift)
                .size(22.dp)
                .drawBehind {
                    // Halo come background — NON influenza layout
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.20f),
                        radius = 24.dp.toPx(),
                    )
                }
                .background(colors.accent, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = stemHeight)
                .background(colors.accent.copy(alpha = 0.35f)),
        )
        Spacer(Modifier.height(6.dp))
    }
}
```

- [ ] **Step 2: Build + smoke**

Apri planner picker map, panna la mappa: il dot si solleva di 8dp, lo stem si allunga a 20dp, restano flush. Senza movimento, dot a y=0, stem 12dp.

```bash
$ADB -s emulator-5556 exec-out screencap -p > /tmp/picker_idle.png && sips -Z 800 /tmp/picker_idle.png --out /tmp/picker_idle_s.png
$ADB -s emulator-5556 shell input swipe 500 700 500 400 200
$ADB -s emulator-5556 exec-out screencap -p > /tmp/picker_moving.png && sips -Z 800 /tmp/picker_moving.png --out /tmp/picker_moving_s.png
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/planner/LocationPickerMapScreen.kt
git commit -m "fix(android): planner picker map — halo background no-gap + stem stretch su camera move"
```

---

## Fase 3 — Detail screens (Stop & Line)

Aggiunge ex-novo la hero map a `StopDetailScreen` e `LineDetailScreen` (Android attualmente non ce l'ha), wire dell'overlay espanso con il `UnifiedMapControlsPill`, star toolbar con drawable filled, live count badge nella lista linee.

### Task 3.1: StopDetailScreen — hero map sopra le partenze

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailScreen.kt`
- Create: `android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailMapHero.kt`

- [ ] **Step 1: Crea Composable hero map**

```kotlin
package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.dsl.cameraOptions
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.ui.mappa.applyTransitKitStandardStyleConfig
import com.transitkit.app.ui.theme.LocalAppColors
import com.transitkit.app.ui.theme.LocalIsDarkTheme

@Composable
fun StopDetailMapHero(
    stopLat: Double,
    stopLon: Double,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val isDark = LocalIsDarkTheme.current
    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(com.mapbox.geojson.Point.fromLngLat(stopLon, stopLat))
            zoom(16.0)
            pitch(0.0)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onExpand),
    ) {
        MapboxMap(
            mapViewportState = viewport,
            style = { Style.STANDARD },
            compass = {},
            scaleBar = {},
            modifier = Modifier.fillMaxSize(),
        ) {
            MapEffect(isDark) { mapView ->
                mapView.mapboxMap.getStyle { style ->
                    applyTransitKitStandardStyleConfig(style, isDark)
                }
            }
            // Stop pin nel centro
            StopHeroPin(stopLat, stopLon)
        }
        // Expand FAB in alto a destra
        IconButton(
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(colors.glassFill, RoundedCornerShape(50)),
        ) {
            Icon(
                painter = painterResource(LucideIcons.Maximize2),
                contentDescription = "Espandi mappa",
                tint = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun StopHeroPin(lat: Double, lon: Double) {
    // Renderizza un singolo PointAnnotation per la fermata.
    // Implementazione: usa PointAnnotationManager con icona ic_stop_dot.
    // [VEDI MapAnnotations.kt per pattern esistente]
}
```

- [ ] **Step 2: Wire la hero map in `StopDetailScreen`**

In `StopDetailScreen.kt`, dentro lo `Scaffold` body, prima di `DeparturesList`:

```kotlin
val resolvedStop = viewModel.resolvedStop.collectAsState().value
resolvedStop?.let { stop ->
    StopDetailMapHero(
        stopLat = stop.lat,
        stopLon = stop.lon,
        onExpand = { showExpandedMap = true },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
```

Aggiungi `var showExpandedMap by remember { mutableStateOf(false) }`.

- [ ] **Step 3: Build (verifica solo compile)**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:compileDebugKotlin -PoperatorId=appalcart 2>&1 | tail -10
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailMapHero.kt
git add android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailScreen.kt
git commit -m "feat(android): StopDetailScreen aggiunge hero map sopra partenze (universal, no API gate)"
```

---

### Task 3.2: StopDetail — `ExpandedMapOverlay` con `UnifiedMapControlsPill`

**Files:**
- Create: `android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailExpandedMap.kt`
- Modify: `android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailScreen.kt`

- [ ] **Step 1: Crea Composable overlay full-screen**

```kotlin
package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.geojson.Point
import com.transitkit.app.ui.mappa.UnifiedMapControlsPill
import com.transitkit.app.ui.theme.LocalAppColors

@Composable
fun StopDetailExpandedMap(
    stopLat: Double,
    stopLon: Double,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(stopLon, stopLat))
            zoom(15.0)
            pitch(45.0) // entry 3D
        }
    }
    var is3D by remember { mutableStateOf(true) }
    val currentBearing by remember { derivedStateOf { viewport.cameraState?.bearing ?: 0.0 } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
            MapboxMap(
                mapViewportState = viewport,
                style = { Style.STANDARD },
                compass = {},
                scaleBar = {},
                modifier = Modifier.fillMaxSize(),
            ) {
                // StopHeroPin riuso
            }

            // Pill verticale: 2D/3D + Recenter + ResetBearing (cond.) + Close
            UnifiedMapControlsPill(
                is3D = is3D,
                onToggle3D = { /* flyTo pitch toggle */ is3D = !is3D },
                onRecenter = { /* flyTo center */ },
                currentBearing = currentBearing,
                onResetBearing = { /* flyTo bearing=0 */ },
                expanded = true,
                onExpandToggle = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            )
        }
    }
}
```

**Importante:** non aggiungere bottone "Apri in mappe" né "X" inline. Il close è il bottone expand del pill (icona `Minimize2` quando `expanded=true`).

- [ ] **Step 2: Wire in `StopDetailScreen.kt`**

```kotlin
if (showExpandedMap && resolvedStop != null) {
    StopDetailExpandedMap(
        stopLat = resolvedStop.lat,
        stopLon = resolvedStop.lon,
        onDismiss = { showExpandedMap = false },
    )
}
```

- [ ] **Step 3: Build + smoke**

Build + install + tap su hero map → si apre overlay full-screen, tap su Minimize2 → si chiude.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailExpandedMap.kt
git add android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailScreen.kt
git commit -m "feat(android): StopDetail expanded map overlay con UnifiedMapControlsPill (no open-in-maps inline)"
```

---

### Task 3.3: StopDetail toolbar — star filled + accent + crossfade

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailScreen.kt`

- [ ] **Step 1: Sostituisci il toggle star con `AnimatedContent` su drawable**

Nel `TopAppBar.actions`, trova il pulsante Star e sostituisci:

```kotlin
val isFavorite by viewModel.isFavorite.collectAsState()
IconButton(
    onClick = { viewModel.toggleFavorite() },
    modifier = Modifier.semantics {
        contentDescription = if (isFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti"
    },
) {
    AnimatedContent(
        targetState = isFavorite,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
        label = "favoriteStar",
    ) { fav ->
        Icon(
            painter = painterResource(
                if (fav) LucideIcons.StarFilled else LucideIcons.Star
            ),
            contentDescription = null,
            tint = if (fav) colors.accent else colors.textSecondary,
        )
    }
}
```

- [ ] **Step 2: Build + smoke**

Tap su star → crossfade outline grigio → filled accent.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/orari/StopDetailScreen.kt
git commit -m "feat(android): toolbar favorite star usa StarFilled + accent con crossfade"
```

---

### Task 3.4: LineDetailScreen — hero map sopra timeline + singolo expand nel toolbar

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/orari/LineDetailScreen.kt`
- Create: `android/app/src/main/java/com/transitkit/app/ui/orari/LineDetailMapHero.kt`

- [ ] **Step 1: Crea hero map con polyline route + stop pins**

Composable analogo a `StopDetailMapHero` ma:
- Bounds = bbox di tutti gli stop della direction selezionata
- Polyline color = route color
- Pin piccoli per ogni stop
- Niente bottone expand inline sull'overlay della card
- Aggiungi `LiveBadge` chip leading (top-start) condizionale a `liveCount > 0`

```kotlin
@Composable
fun LineDetailMapHero(
    route: ScheduleRoute,
    stops: List<ResolvedStop>,
    polyline: List<Point>?,
    liveCount: Int,
    modifier: Modifier = Modifier,
) {
    // ... mapbox map, polyline layer, stop pins
    // Top-start: se liveCount > 0, LiveBadge chip
    if (liveCount > 0) {
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                .background(realtimeGreen.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).background(realtimeGreen, CircleShape))
            Text(
                text = pluralStringResource(R.plurals.lines_live_count, liveCount, liveCount),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = realtimeGreen,
            )
        }
    }
    // NIENTE bottone Maximize2 sull'overlay — solo nel toolbar
}
```

- [ ] **Step 2: Wire in `LineDetailScreen`: hero sopra LazyColumn, expand SOLO nel toolbar**

In `LineDetailScreen.kt`:
- Rimuovi la chip "Open in map" attuale (riga con `Icon(Map)` e chevron)
- Aggiungi `LineDetailMapHero` come item della `LazyColumn` (in alto)
- Aggiungi nella `TopAppBar` un'azione `IconButton(Maximize2)` che chiama `onNavigateToMap(routeId)`

- [ ] **Step 3: Build + smoke**

Apri LineDetail, verifica: hero map in alto con polyline route, niente chip extra, chip live (se applicabile), expand solo nel toolbar in alto.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/orari/LineDetailMapHero.kt
git add android/app/src/main/java/com/transitkit/app/ui/orari/LineDetailScreen.kt
git commit -m "feat(android): LineDetail hero map universal + singolo expand nel toolbar + LiveBadge chip"
```

---

### Task 3.5: LineDetailHeader — adaptive content colors (no hard white)

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/orari/LineDetailHeader.kt`

Il problema iOS era forzare white in tutta la nav bar. Su Android `LineDetailHeader` ha già `lineTextColor` calcolato per contrasto con `routeLineColor` background — questo va bene perché il background è solido colorato. Verifichiamo che NON ci sia un hard-coded `Color.White` per tinta icone quando lo sfondo è trasparente / sopra la mappa.

- [ ] **Step 1: Audit colori header**

Verifica che `IconButton(ChevronLeft)` e `Text(route name)` usino `lineTextColor` derivato da `routeBadgeContrast(routeLineColor)`. Se ci sono `Color.White` letterali, sostituiscili con il valore derivato.

- [ ] **Step 2: Caso translucido** (se l'header viene riusato sopra la mappa)

Se in futuro l'header viene messo sopra la map hero, sostituisci il gradient verticale solid con `colors.surface.copy(alpha = 0.92f)` e `lineTextColor = colors.onSurface`. Per ora, l'header rimane solido — niente cambio necessario, ma documenta in commento.

- [ ] **Step 3: Commit (anche solo cosmetico/comment)**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/orari/LineDetailHeader.kt
git commit -m "chore(android): LineDetailHeader documenta uso di colori adattivi (no hard white)"
```

---

### Task 3.6: LineeScreen — live count badge per linea (statico, no animazione)

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/linee/LineeScreen.kt`
- Modify: `android/app/src/main/java/com/transitkit/app/ui/linee/OrariViewModel.kt` (espone `liveCountByRoute: StateFlow<Map<String, Int>>`)

**Importante:** badge STATICO. Nessuna `infiniteRepeatable` qui dentro `LazyColumn` con rerender ogni 15s.

- [ ] **Step 1: Espone `liveCountByRoute` nel VM**

```kotlin
// In OrariViewModel.kt
val liveCountByRoute: StateFlow<Map<String, Int>> = vehicleStore.vehicles
    .map { vehicles -> vehicles.groupingBy { it.routeId }.eachCount() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
```

- [ ] **Step 2: Aggiungi `LiveCountStaticBadge` Composable**

In LineeScreen.kt (private fun):

```kotlin
@Composable
private fun LiveCountStaticBadge(count: Int) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .background(colors.realtimeGreen.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(5.dp).background(colors.realtimeGreen, CircleShape))
        Text(
            text = "$count live",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.realtimeGreen,
        )
    }
}
```

- [ ] **Step 3: Inseriscilo in `RouteListItem` prima della chevron**

```kotlin
Row(...) {
    LineBadge(route, size = LineBadgeSize.Large)
    Column(modifier = Modifier.weight(1f)) { /* longName + subtitle */ }
    val liveCount = liveCountByRoute[route.id] ?: 0
    if (liveCount > 0) {
        LiveCountStaticBadge(liveCount)
    }
    Icon(LucideIcons.ChevronRight, ...)
}
```

Passa `liveCountByRoute` come parametro di `LineeContent` e `RouteListItem`.

- [ ] **Step 4: Build + smoke**

Apri tab linee con feed realtime attivo, verifica chip verde tenue prima della chevron solo se ci sono veicoli live.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/linee/LineeScreen.kt
git add android/app/src/main/java/com/transitkit/app/ui/linee/OrariViewModel.kt
git commit -m "feat(android): LineeScreen live count badge per linea (statico, no pulse)"
```

---

## Fase 4 — Home tab

### Task 4.1: Section header con icona inline + sentence case

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt`

- [ ] **Step 1: Crea Composable `SectionHeader`**

```kotlin
@Composable
private fun SectionHeader(
    text: String,
    @DrawableRes icon: Int? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}
```

- [ ] **Step 2: Sostituisci i due header esistenti**

In `FavoritesSection` (o equivalente) sostituisci l'header "PREFERITI" con:
```kotlin
SectionHeader(
    text = stringResource(R.string.home_section_favorites),
    icon = LucideIcons.Star,
)
```

In `NearbySection`:
```kotlin
SectionHeader(
    text = stringResource(R.string.home_section_nearby),
    icon = LucideIcons.MapPin,
)
```

Niente `.toUpperCase()` o `letterSpacing` ovunque.

- [ ] **Step 3: Build + smoke**

Apri Home, verifica: header sentence case con icona inline 14dp.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt
git commit -m "feat(android): Home section headers sentence case con icona inline (14dp)"
```

---

### Task 4.2: Home — sezione "vicine" come singola card glass con righe compatte

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt`

- [ ] **Step 1: Crea `NearbyStopRow` Composable compatto**

```kotlin
@Composable
private fun NearbyStopRow(
    stop: ResolvedStop,
    distanceMeters: Double,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.BusFront),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stop.name,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDistance(distanceMeters),
            fontSize = 12.sp,
            color = colors.textSecondary,
        )
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}
```

- [ ] **Step 2: Wrap delle righe in singola card glass**

Sostituisci la `items(nearbyStops)` → `StopCard(...)` esistente con:

```kotlin
item {
    if (nearbyStops.isNotEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            color = colors.glassFill,
            border = BorderStroke(0.5.dp, colors.glassBorder),
        ) {
            Column {
                nearbyStops.forEachIndexed { idx, (stop, dist) ->
                    NearbyStopRow(stop, dist) { onNavigateToStop(stop.id, stop.name) }
                    if (idx < nearbyStops.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
                    }
                }
            }
        }
    }
}
```

Rimuovi il rendering di `nearbyDepartures` per le stops nearby (resta solo per `resolvedFavoriteStops`). Aggiorna il VM se necessario per non popolare partenze di nearby (ottimizzazione).

- [ ] **Step 3: Build + smoke**

Home dopo location grant: preferite con card piena + lista partenze, nearby con singola card glass + righe compatte (icona + nome + distanza + chevron).

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt
git add android/app/src/main/java/com/transitkit/app/ui/home/HomeViewModel.kt
git commit -m "feat(android): Home nearby = single glass card con righe compatte shortcut (no partenze)"
```

---

### Task 4.3: PlannerHomeBox — WhenChipRow `Arrangement.Start`

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt` (cerca `WhenChipRow` o `PlannerHomeBox`)

- [ ] **Step 1: Trova e modifica l'allineamento**

```bash
grep -n "WhenChipRow\|PlannerHomeBox" /Users/andreatoffanello/GitHub/transit-engine/android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt
```

Nel `Row` contenitore della WhenChipRow, sostituisci `horizontalArrangement = Arrangement.End` (o `Arrangement.Center`) con `Arrangement.Start`. Rimuovi eventuali `Spacer(Modifier.weight(1f))` prima della chip.

- [ ] **Step 2: Build + smoke**

Home: chip "Ora" appare a leading edge sotto il box origin/destination.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt
git commit -m "ui(android): PlannerHomeBox WhenChipRow allineata a sinistra"
```

---

### Task 4.4: OperatorReferenceCard → logo reale + `config.name`

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt`
- Verify: `android/app/src/main/res/drawable/operator_logo.png` (già esistente)

- [ ] **Step 1: Sostituisci initials avatar con Image del logo**

Nella `OperatorReferenceCard`, trova il blocco gradient + initials e sostituiscilo con:

```kotlin
val context = LocalContext.current
val logoRes = remember {
    context.resources.getIdentifier("operator_logo", "drawable", context.packageName)
}
if (logoRes != 0) {
    Image(
        painter = painterResource(logoRes),
        contentDescription = config.name,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
} else {
    // Fallback initials esistente
}
```

Verifica che `config.name` venga usato come testo principale della card (NON un `brandName`, che non esiste lato Android).

- [ ] **Step 2: Build + smoke**

Home, scroll fino in fondo: vedi card operatore con logo reale (es. AppalCART) + nome operatore + region.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/home/HomeScreen.kt
git commit -m "feat(android): OperatorReferenceCard usa logo reale + config.name"
```

---

## Fase 5 — Location primer

### Task 5.1: LocationPrimerScreen — background leggibile (radial fade + ghost map 8%)

**Files:**
- Modify: `android/app/src/main/java/com/transitkit/app/ui/home/LocationPrimerScreen.kt`

- [ ] **Step 1: Sostituisci layer di background**

Rimuovi:
- `Image(operator_background, alpha=0.18f, ContentScale.Crop)`
- `Box.background(Brush.verticalGradient(... dark overlay ...))`

Aggiungi:
```kotlin
val colors = LocalAppColors.current
Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
    // Layer 1: ghost map molto attenuato
    Image(
        painter = painterResource(operatorBackgroundRes),
        contentDescription = null,
        modifier = Modifier.fillMaxSize().alpha(0.08f),
        contentScale = ContentScale.Crop,
    )
    // Layer 2: radial gradient di leggibilità (center → fade out)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.background.copy(alpha = 0.85f),
                        colors.background.copy(alpha = 0f),
                    ),
                    radius = 360.dp.toPx(LocalDensity.current),
                )
            ),
    )
    // ... il resto del Column con icona, titolo, body, button
}
```

(Usa `with(LocalDensity.current) { 360.dp.toPx() }` se serve `radius` numerico.)

- [ ] **Step 2: Verifica contrast**

Lancia il primer in dark mode e light mode. Testo titolo e body devono essere chiaramente leggibili.

- [ ] **Step 3: Build + smoke**

```bash
# Reset SharedPrefs per riveder e il primer
$ADB -s emulator-5556 shell pm clear com.transitkit.appalcart
$ADB -s emulator-5556 shell am start -n com.transitkit.appalcart/com.transitkit.app.MainActivity
$ADB -s emulator-5556 exec-out screencap -p > /tmp/primer.png && sips -Z 800 /tmp/primer.png --out /tmp/primer_s.png
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/transitkit/app/ui/home/LocationPrimerScreen.kt
git commit -m "fix(android): LocationPrimerScreen — ghost map 8% + radial fade per leggibilità testo"
```

---

## Verifica finale & cleanup

### Task 99: End-to-end smoke + screenshots per UX review

- [ ] **Step 1: Build release-grade + install pulito**

```bash
$ADB -s emulator-5556 shell pm uninstall com.transitkit.appalcart
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug -PoperatorId=appalcart 2>&1 | tail -5
$ADB -s emulator-5556 install app/build/outputs/apk/debug/app-debug.apk
$ADB -s emulator-5556 shell am start -n com.transitkit.appalcart/com.transitkit.app.MainActivity
```

- [ ] **Step 2: Screenshot di tutte le schermate modificate**

| # | Screen | Comando |
|---|--------|---------|
| 1 | Home (primer dismissed) | `exec-out screencap -p > /tmp/01_home.png` |
| 2 | StopDetail (con map hero) | … |
| 3 | StopDetail expanded map | … |
| 4 | LineDetail (con map hero) | … |
| 5 | LineeScreen (con live badge) | … |
| 6 | MappaScreen (pill verticale) | … |
| 7 | Planner picker (idle + moving) | … |
| 8 | LocationPrimer (reset prefs) | … |

Comprimi tutti a `sips -Z 800`.

- [ ] **Step 3: Esegui smoke Maestro**

```bash
maestro test android/flows/smoke_test.yaml
```

(Se `android/flows/` non esiste ancora con un flow generale, salta lo step e nota in TODO.)

- [ ] **Step 4: Verifica logcat — no crash**

```bash
$ADB -s emulator-5556 logcat -d AndroidRuntime:E '*:S' | tail -50
```

Expected: vuoto o solo errori pre-esistenti non correlati.

- [ ] **Step 5: Commit finale (se servono screenshots in repo)**

```bash
# Solo se gli screenshot devono essere committati per documentazione UX
git add docs/screenshots/2026-05-14-android-alignment/
git commit -m "docs(android): screenshot di allineamento iOS→Android"
```

---

## Spec coverage — checklist

| # iOS spec | Task plan | Status |
|------------|-----------|--------|
| 1. Mappa controlli unificati pill | 2.1, 2.2, 3.2 | ✓ |
| 2. Expanded map no open-in-maps | 3.2 | ✓ |
| 3. User location blu sopra tutto | 2.3 | ✓ (verifica + z-order) |
| 4. Planner picker halo+stem | 2.5 | ✓ |
| 5. Map hero universal (Stop+Line) | 3.1, 3.2, 3.4 | ✓ (aggiunta ex-novo) |
| 6. Vehicle pin gap | 2.4 | ✓ |
| 7. Adaptive nav LineDetail | 3.5 | ✓ (audit + doc) |
| 8. Singolo expand LineDetail | 3.4 | ✓ |
| 9. WhenChipRow leading | 4.3 | ✓ |
| 10. Section header icon + sentence | 4.1 + 1.5 (strings) | ✓ |
| 11. Differenziazione fav/nearby | 4.2 | ✓ |
| 12. TimeDisplay hours+min + threshold + wrap | 1.1, 1.3 | ✓ |
| 13. DepartureRow live dot inline | 1.2 | ✓ |
| 14. LineBadge fixedSize esterno | 1.4 | ✓ |
| 15. Operator card logo + name | 4.4 | ✓ |
| 16. LocationPrimer background | 5.1 | ✓ |
| 17. LineeScreen live badge statico | 3.6 | ✓ |
| 18. Star filled + accent + animation | 1.6, 3.3 | ✓ |

---

## Note per l'esecutore

- **Movete reference paths**: ogni task con pattern UI cita `Movete/...` come reference. Lette le 3 prime non-trivial e usate come fonte di stile. NON copiare codice cieco, ma adatta al codebase TransitKit.
- **Commit per task**: una task = un commit (occasionalmente più piccoli se la task ha sub-deliverable indipendenti come Task 1.3 multi-file migration).
- **Build incrementale**: ogni 2-3 task lancia `./gradlew :app:assembleDebug` per scoprire breakage presto.
- **Definition of done globale (per ogni task UI)**:
  - Build zero error / zero warning nuovi
  - `.semantics { testTag = ... }` su nuovi elementi interattivi
  - Screenshot prima/dopo allegati
  - Niente animazioni infinite in LazyColumn
  - Touch target ≥ 48dp
  - No uppercase forzato
