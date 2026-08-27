package com.transitkit.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Vocabolario aptico dell'app. I verbi sono per INTENTO, non per intensità:
 * il call site dice cosa sta succedendo, non quanto deve vibrare. Così la stessa
 * azione ha la stessa sensazione ovunque, e ritarare è una modifica in un file solo.
 *
 * Prima di questo file c'erano 38 call site grezzi con DUE sole costanti, usate
 * a caso: `LongPress` (un tonfo) finiva su tap di navigazione ordinari in 15
 * punti, e `TextHandleMove` su tutto il resto. Non aggiungere
 * `performHapticFeedback` diretti: aggiungi un verbo qui se davvero ne manca uno.
 *
 * Parità iOS: `Components/Haptics.swift` — stessi verbi, stessa semantica.
 *
 * **Perché le costanti di piattaforma e non `HapticFeedbackType` di Compose.**
 * Il BOM in uso (2025.03.01 → compose-ui 1.7.8) espone solo `LongPress` e
 * `TextHandleMove`; il set espressivo è arrivato in 1.8. `HapticFeedbackConstants`
 * ce l'ha da API 30/34 ed è ciò su cui Compose mappa comunque, quindi si prende
 * il vocabolario completo oggi, senza un bump di dipendenza. I guard di
 * `Build.VERSION` degradano su costanti più vecchie sui device più vecchi
 * (minSdk 26), mai su niente.
 *
 * Taratura: sottile per default. L'aptico deve essere notato inconsciamente come
 * segno di cura, non annunciarsi. `LONG_PRESS` compare solo per le azioni
 * distruttive.
 */
class Haptics(private val view: View) {

    /** Tap che porta altrove: riga di lista, card, push di una destinazione,
     *  apertura di un foglio. Il tick più leggero del vocabolario. */
    fun navigate() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) HapticFeedbackConstants.CONTEXT_CLICK
        else HapticFeedbackConstants.KEYBOARD_TAP
    )

    /** Cambio di selezione dentro un set: chip di filtro, segmented, direzione,
     *  riga di un picker, swap origine/destinazione, cambio tab. */
    fun selection() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.SEGMENT_TICK
        else HapticFeedbackConstants.CLOCK_TICK
    )

    /** Detent attraversato durante uno scroll continuo (wheel picker).
     *  Più secco e più fitto di [selection]: ne partono decine di seguito. */
    fun tick() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        else HapticFeedbackConstants.CLOCK_TICK
    )

    /** Azione che si assume una responsabilità: cerca un percorso, conferma una
     *  destinazione, chiama un numero, esce dall'app, riprova dopo un errore. */
    fun commit() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY
    )

    /** Stato che si accende (preferito aggiunto, notifiche on, follow attivato).
     *  Asimmetrico rispetto a [toggleOff] di proposito: aggiungere e togliere
     *  devono sentirsi diversi anche senza guardare. */
    fun toggleOn() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.TOGGLE_ON
        else HapticFeedbackConstants.VIRTUAL_KEY
    )

    /** Stato che si spegne (preferito rimosso, notifiche off, follow disattivato).
     *
     *  Il fallback è `CLOCK_TICK` (API 21) e non `TEXT_HANDLE_MOVE`: quest'ultimo
     *  esiste solo dall'API 27 e il minSdk è 26, quindi su Android 8.0
     *  `performHapticFeedback` lo ignorerebbe e togliere un preferito non
     *  vibrerebbe affatto. Resta comunque distinto dal `VIRTUAL_KEY` di
     *  [toggleOn], che è il punto: acceso e spento devono sentirsi diversi. */
    fun toggleOff() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.TOGGLE_OFF
        else HapticFeedbackConstants.CLOCK_TICK
    )

    /** Azione distruttiva confermata: rimozione di un preferito da swipe. */
    fun destructive() = perform(HapticFeedbackConstants.LONG_PRESS)

    /** Qualcosa è andato storto in risposta a un'azione dell'utente.
     *  NON per eventi automatici: un avviso che arriva da solo non merita una
     *  notifica aptica, l'utente non ha chiesto nulla. */
    fun warn() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS
    )

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

/**
 * `val haptics = rememberHaptics()` in cima al composable, poi `haptics.navigate()`
 * dentro l'onClick. Sostituisce la coppia `LocalHapticFeedback.current` +
 * `performHapticFeedback(HapticFeedbackType.…)`.
 */
@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
