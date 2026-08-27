import SwiftUI
import UIKit

// MARK: - Haptics
//
// Vocabolario aptico dell'app. I verbi sono per INTENTO, non per intensità:
// il call site dice cosa sta succedendo, non quanto deve vibrare. Così la
// stessa azione ha la stessa sensazione ovunque, e ritarare l'intensità è
// una modifica in un file solo.
//
// Prima di questo file c'erano 40 call site grezzi che decidevano lo stile a
// mano: `.light` su un tap di navigazione qui e `.medium` là, un
// `UISelectionFeedbackGenerator` su uno swap e un `.impact(.light)` sullo
// swap gemello. Non aggiungere `UIImpactFeedbackGenerator` nuovi: aggiungi un
// verbo qui se davvero ne manca uno.
//
// Parità Android: `ui/components/Haptics.kt` — stessi verbi, stessa semantica.
//
// Taratura: sottile per default. L'aptico deve essere notato inconsciamente
// come segno di cura, non annunciarsi. Solo `commit` e `toggleOn` superano
// intensità 0.7, e nulla usa `.heavy`.

/// `@MainActor` obbligatorio: i generator di UIKit sono isolati al main actor,
/// e `selectionGenerator` è una stored property — su un default value il
/// compilatore lo tratta come errore fatale, non come warning. Chiamare i verbi
/// da una view SwiftUI è comunque sempre main-actor, quindi non costa nulla.
@MainActor
enum Haptics {

    /// Tap che porta altrove: riga di lista, card, push di una destinazione,
    /// apertura di un foglio. Il tick più leggero del vocabolario.
    static func navigate() {
        impact(.light, intensity: 0.5)
    }

    /// Cambio di selezione dentro un set: chip di filtro, segmented, direzione,
    /// riga di un picker, swap origine/destinazione, cambio tab.
    static func selection() {
        selectionGenerator.selectionChanged()
    }

    /// Detent attraversato durante uno scroll continuo (wheel picker).
    /// Più secco e più fitto di `selection`: ne partono decine di seguito.
    static func tick() {
        impact(.rigid, intensity: 0.35)
    }

    /// Azione che si assume una responsabilità: cerca un percorso, conferma una
    /// destinazione, chiama un numero, esce dall'app verso Safari/Mappe, riprova
    /// dopo un errore. Il tap più pieno del vocabolario.
    static func commit() {
        impact(.medium, intensity: 0.8)
    }

    /// Stato che si accende (preferito aggiunto, notifiche on, follow attivato).
    /// Asimmetrico rispetto a `toggleOff` di proposito: aggiungere e togliere
    /// devono sentirsi diversi anche senza guardare.
    static func toggleOn() {
        impact(.rigid, intensity: 0.7)
    }

    /// Stato che si spegne (preferito rimosso, notifiche off, follow disattivato).
    static func toggleOff() {
        impact(.soft, intensity: 0.55)
    }

    /// Azione distruttiva confermata: rimozione di un preferito da swipe.
    static func destructive() {
        impact(.rigid, intensity: 0.85)
    }

    /// Qualcosa è andato storto in risposta a un'azione dell'utente.
    /// NON per eventi automatici: un avviso che arriva da solo non merita
    /// una notifica aptica, l'utente non ha chiesto nulla.
    static func warn() {
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
    }

    // MARK: - Internals

    private static let selectionGenerator = UISelectionFeedbackGenerator()

    private static func impact(_ style: UIImpactFeedbackGenerator.FeedbackStyle, intensity: CGFloat) {
        UIImpactFeedbackGenerator(style: style).impactOccurred(intensity: intensity)
    }
}

// MARK: - View helpers

extension View {
    /// Aggiunge un tick di navigazione a una `NavigationLink`, che altrimenti
    /// non offre un punto in cui infilarlo. `simultaneousGesture` non ruba il
    /// tap: la push avviene comunque.
    ///
    /// `assumeIsolated` e non un `Task { @MainActor in }`: SwiftUI consegna le
    /// gesture sul main actor, e un hop di runloop fra il dito e il tick lo
    /// renderebbe percepibilmente scollegato dal tocco.
    func hapticNavigationLink() -> some View {
        simultaneousGesture(
            TapGesture().onEnded { _ in
                MainActor.assumeIsolated { Haptics.navigate() }
            }
        )
    }
}
