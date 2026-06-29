import SwiftUI

/// Riga di chip inline per selezionare quando partire/arrivare. Niente
/// schermata dedicata.
///
/// - Sempre presente: chip mode con Menu "Adesso / Parti alle / Arriva entro"
/// - Quando il mode è `.departAt`/`.arriveBy`, si affiancano 2 chip identiche
///   (stesso style, font, padding) — una mostra l'orario, una la data.
///   Su tap aprono un popover con `DatePicker` wheel/graphical nativo iOS.
struct WhenChipsRow: View {
    @Binding var selection: WhenSelection

    @Environment(\.operatorTimeZone) private var operatorTimeZone
    @State private var showTimePicker = false
    @State private var showDatePicker = false

    private var mode: Mode {
        switch selection {
        case .now:        return .now
        case .departAt:   return .depart
        case .arriveBy:   return .arrive
        }
    }

    private var currentDate: Date {
        switch selection {
        case .now:              return Date()
        case .departAt(let d):  return d
        case .arriveBy(let d):  return d
        }
    }

    var body: some View {
        HStack(spacing: 8) {
            modeChip
            if mode != .now {
                timeChip
                dateChip
            }
        }
    }

    // MARK: - Mode chip (Menu)

    private var modeChip: some View {
        Menu {
            Button { selection = .now } label: {
                Label(String(localized: "planner_now"),
                      systemImage: mode == .now ? "checkmark" : "")
            }
            Button {
                selection = .departAt(seedDate())
            } label: {
                Label(String(localized: "planner_depart_at"),
                      systemImage: mode == .depart ? "checkmark" : "")
            }
            Button {
                selection = .arriveBy(seedDate())
            } label: {
                Label(String(localized: "planner_arrive_by"),
                      systemImage: mode == .arrive ? "checkmark" : "")
            }
        } label: {
            chipLabel(icon: LucideIcon.clock, text: modeLabel, showChevron: true)
        }
        .accessibilityIdentifier("home_planner_when_mode")
    }

    private var modeLabel: String {
        switch mode {
        case .now:    return String(localized: "planner_now")
        case .depart: return String(localized: "planner_depart_at")
        case .arrive: return String(localized: "planner_arrive_by")
        }
    }

    private func seedDate() -> Date {
        // Se la selezione corrente è .now, parti da +15 min; altrimenti
        // mantieni la data già scelta.
        switch selection {
        case .now: return Date().addingTimeInterval(900)
        case .departAt(let d), .arriveBy(let d): return d
        }
    }

    // MARK: - Time chip (popover wheel picker)

    private var timeChip: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showTimePicker = true
        } label: {
            chipLabel(text: timeLabel, showChevron: false)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("home_planner_when_time")
        .popover(isPresented: $showTimePicker) {
            DatePicker(
                "",
                selection: dateBinding,
                in: Date()...,
                displayedComponents: [.hourAndMinute]
            )
            .datePickerStyle(.wheel)
            .labelsHidden()
            .padding(20)
            .presentationCompactAdaptation(.popover)
            .environment(\.timeZone, operatorTimeZone)
        }
    }

    private var timeLabel: String {
        ClockTime.clock(currentDate, timeZone: operatorTimeZone)
    }

    // MARK: - Date chip (popover graphical picker)

    private var dateChip: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showDatePicker = true
        } label: {
            chipLabel(text: dateLabel, showChevron: false)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("home_planner_when_date")
        .popover(isPresented: $showDatePicker) {
            DatePicker(
                "",
                selection: dateBinding,
                in: Date()...,
                displayedComponents: [.date]
            )
            .datePickerStyle(.graphical)
            .labelsHidden()
            .padding(16)
            .frame(minWidth: 320)
            .presentationCompactAdaptation(.popover)
            .environment(\.timeZone, operatorTimeZone)
        }
    }

    private var dateLabel: String {
        var cal = Calendar.current
        cal.timeZone = operatorTimeZone
        // Se il giorno coincide con oggi, mostra "Oggi"; altrimenti formato breve.
        if cal.isDateInToday(currentDate) {
            return String(localized: "today_short")
        }
        if cal.isDateInTomorrow(currentDate) {
            return String(localized: "tomorrow_short")
        }
        let f = DateFormatter()
        f.timeZone = operatorTimeZone
        f.dateFormat = "d MMM"
        return f.string(from: currentDate)
    }

    // MARK: - Shared chip style

    @ViewBuilder
    private func chipLabel(icon: LucideIcon? = nil, text: String, showChevron: Bool) -> some View {
        HStack(spacing: 6) {
            if let icon { icon.sized(13) }
            Text(text)
                .font(.system(size: 13, weight: .semibold))
            if showChevron { LucideIcon.chevronDown.sized(11) }
        }
        .foregroundStyle(AppTheme.textSecondary)
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(Color(.tertiarySystemFill))
        .clipShape(Capsule())
    }

    // MARK: - Binding helpers

    /// Binding che propaga i cambiamenti del DatePicker dentro l'enum
    /// `WhenSelection`, preservando il mode (.depart o .arrive).
    private var dateBinding: Binding<Date> {
        Binding(
            get: { currentDate },
            set: { newDate in
                switch selection {
                case .now:             break
                case .departAt:        selection = .departAt(newDate)
                case .arriveBy:        selection = .arriveBy(newDate)
                }
            }
        )
    }

    private enum Mode { case now, depart, arrive }
}
