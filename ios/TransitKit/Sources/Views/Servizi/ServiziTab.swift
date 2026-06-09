import SwiftUI

// MARK: - ServiziTab (Services index)

/// Top-level index for the Services tab.
/// Groups the operator's services, fares, accessibility, and contact entries
/// in a single browsable list. Tapping any card navigates to its detail view.
struct ServiziTab: View {
    let config: OperatorConfig
    @Environment(\.dismiss) private var dismiss
    @Environment(\.isPresented) private var isPresented
    @Environment(DeepLinkRouter.self) private var router

    /// Id of the service to auto-push when `transitkit://servizi/<id>` is
    /// consumed. `ServiceInfo` isn't Hashable end-to-end, so we bind the id
    /// and re-resolve the model inside the destination closure.
    @State private var deeplinkServiceId: String? = nil

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 20) {
                    servicesSection(config: config)
                    operatorCard(config: config)
                    faresSection(config: config)
                    accessibilitySection(config: config)
                    contactSection(config: config)
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(String(localized: "services_title"))
            .navigationBarTitleDisplayMode(.large)
            .navigationDestination(item: $deeplinkServiceId) { id in
                if let svc = config.services?.first(where: { $0.id == id }) {
                    ServiceDetailView(service: svc, phone: config.contact?.phone)
                }
            }
            .toolbar {
                if isPresented {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            dismiss()
                        } label: {
                            LucideIcon.x.sized(18)
                                .foregroundStyle(AppTheme.textPrimary)
                        }
                        .accessibilityLabel(String(localized: "action_close"))
                        .accessibilityIdentifier("btn_close_servizi")
                    }
                }
            }
            .onAppear { consumePendingServiziId() }
        }
    }

    /// Honor `transitkit://servizi/<id>` by pushing `ServiceDetailView` for the
    /// matching service. Unknown ids are dropped silently — the user lands on
    /// the index, which is a friendlier failure than a blank push.
    private func consumePendingServiziId() {
        guard let pendingId = router.pendingServiziId else { return }
        router.pendingServiziId = nil
        guard let services = config.services,
              services.contains(where: { $0.id == pendingId }) else { return }
        // Small hop so the push happens after NavigationStack mounts; without
        // this the binding mutation races the initial layout and is dropped.
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(120))
            deeplinkServiceId = pendingId
        }
    }

    // MARK: Services section

    @ViewBuilder
    private func servicesSection(config: OperatorConfig) -> some View {
        if let services = config.services, !services.isEmpty {
            sectionHeader(String(localized: "services_section_title"))
            VStack(spacing: 12) {
                ForEach(services) { service in
                    NavigationLink {
                        ServiceDetailView(service: service, phone: config.contact?.phone)
                    } label: {
                        ServiceRowCard(service: service)
                    }
                    .buttonStyle(PressableCardStyle())
                    .accessibilityIdentifier("service_row_\(service.id)")
                }
            }
        }
    }

    // MARK: Fares section

    @ViewBuilder
    private func faresSection(config: OperatorConfig) -> some View {
        if let fares = config.fares {
            sectionHeader(String(localized: "services_section_fares"))
            NavigationLink {
                FareInfoView(fares: fares, operatorUrl: config.url)
            } label: {
                summaryCard(
                    icon: .ticket,
                    title: String(localized: "services_section_fares"),
                    subtitle: faresSummary(fares)
                )
            }
            .buttonStyle(PressableCardStyle())
            .accessibilityIdentifier("service_row_fares")
        }
    }

    private func faresSummary(_ fares: FareInfo) -> String {
        let freeTokens: Set<String> = ["free", "gratis", "gratuito", "$0", "$0.00"]
        let isFree = fares.types.allSatisfy { t in
            freeTokens.contains(t.price.trimmingCharacters(in: .whitespaces).lowercased())
        }
        if isFree {
            return String(localized: "services_fare_free")
        }
        // Find minimum numeric fare
        let numeric = fares.types.compactMap { t -> (String, Double)? in
            let cleaned = t.price.replacingOccurrences(of: "$", with: "")
                .trimmingCharacters(in: .whitespaces)
            return Double(cleaned).map { (t.price, $0) }
        }
        if let min = numeric.min(by: { $0.1 < $1.1 }) {
            return String(format: String(localized: "services_fare_from %@"), min.0)
        }
        return fares.types.first?.price ?? ""
    }

    // MARK: Accessibility section

    @ViewBuilder
    private func accessibilitySection(config: OperatorConfig) -> some View {
        if let accessibility = config.accessibility {
            sectionHeader(String(localized: "services_section_accessibility"))
            NavigationLink {
                AccessibilityInfoView(info: accessibility)
            } label: {
                summaryCard(
                    icon: .accessibility,
                    title: accessibility.title.resolved(),
                    subtitle: accessibility.description.resolved()
                )
            }
            .buttonStyle(PressableCardStyle())
            .accessibilityIdentifier("service_row_accessibility")
        }
    }

    // MARK: Contact section

    /// Single-row entry card. Tapping opens the full `ContactInfoView`
    /// where phone / email / TDD / address / hours are presented in full.
    /// The index row itself shows only the section title + a compact preview
    /// of the primary channels to avoid duplicating detail content.
    @ViewBuilder
    private func contactSection(config: OperatorConfig) -> some View {
        if let contact = config.contact {
            sectionHeader(String(localized: "services_section_contact"))
            NavigationLink {
                ContactInfoView(contact: contact)
            } label: {
                contactSummaryRow(contact: contact)
            }
            .buttonStyle(PressableCardStyle())
            .accessibilityIdentifier("service_row_contact")
        }
    }

    private func contactSummaryRow(contact: OperatorConfig.ContactConfig) -> some View {
        GlassCard(cornerRadius: 16) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(AppTheme.accent.opacity(0.14))
                    .frame(width: 40, height: 40)
                    .overlay(
                        LucideIcon.phone.sized(20)
                            .foregroundStyle(AppTheme.accent)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "services_section_contact"))
                        .font(.headline)
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                    Text(contactPreview(contact: contact))
                        .font(.footnote)
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
                Spacer(minLength: 0)
                LucideIcon.chevronRight.sized(18)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(.horizontal, 12)
            .frame(height: 56)
        }
    }

    /// Primary-channel preview line: phone + email, separated by middle dot.
    /// Trimmed to the subset present; middle-ellipsis kicks in via the row.
    private func contactPreview(contact: OperatorConfig.ContactConfig) -> String {
        [contact.phone, contact.email]
            .compactMap { $0 }
            .joined(separator: " • ")
    }

    // MARK: Operator card

    /// Standalone about-operator card (no section header). Sits directly
    /// beneath the services list so users quickly see whose services they're
    /// looking at without the redundant top "tagline" header card.
    private func operatorCard(config: OperatorConfig) -> some View {
        NavigationLink {
            OperatorInfoView(config: config)
        } label: {
            GlassCard(cornerRadius: 16) {
                HStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(AppTheme.accent.opacity(0.14))
                        .frame(width: 48, height: 48)
                        .overlay(
                            LucideIcon.busFront.sized(22)
                                .foregroundStyle(AppTheme.accent)
                        )
                    VStack(alignment: .leading, spacing: 2) {
                        Text(config.fullName)
                            .font(.headline)
                            .foregroundStyle(AppTheme.textPrimary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                        Text(operatorSubtitle(config: config))
                            .font(.subheadline)
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 0)
                    LucideIcon.chevronRight.sized(18)
                        .foregroundStyle(AppTheme.textTertiary)
                }
                .padding(16)
            }
        }
        .buttonStyle(PressableCardStyle())
        .accessibilityIdentifier("service_row_operator")
    }

    /// Subtitle for the operator card: prefers an explicit region/city, falls
    /// back to the services tagline so the row never reads with a blank line.
    private func operatorSubtitle(config: OperatorConfig) -> String {
        if let region = extractRegion(from: config.contact?.address), !region.isEmpty {
            return region
        }
        return String(localized: "services_tagline")
    }

    /// Grab a short, human-readable region hint from a full address (the
    /// last comma-separated component, e.g. "Boone, NC" → "Boone, NC").
    /// Falls back to the full string if no split is possible.
    private func extractRegion(from address: String?) -> String? {
        guard let address else { return nil }
        let parts = address.split(separator: ",").map {
            $0.trimmingCharacters(in: .whitespaces)
        }
        if parts.count >= 2 {
            return parts.suffix(2).joined(separator: ", ")
        }
        return parts.first
    }

    // MARK: Shared building blocks

    private func sectionHeader(_ title: String) -> some View {
        HStack {
            Text(title.uppercased())
                .font(.caption.weight(.semibold))
                .kerning(0.6)
                .foregroundStyle(AppTheme.textTertiary)
            Spacer()
        }
        .padding(.horizontal, 4)
        .padding(.top, 8)
    }

    private func summaryCard(icon: LucideIcon, title: String, subtitle: String) -> some View {
        GlassCard(cornerRadius: 16) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(AppTheme.accent.opacity(0.14))
                    .frame(width: 48, height: 48)
                    .overlay(
                        icon.sized(22)
                            .foregroundStyle(AppTheme.accent)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
                LucideIcon.chevronRight.sized(18)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(16)
        }
    }

}

// MARK: - ServiceRowCard

/// Tappable list card: icon badge + title + subtitle + chevron.
/// Used as the label for each service's NavigationLink.
private struct ServiceRowCard: View {
    let service: ServiceInfo

    var body: some View {
        GlassCard(cornerRadius: 16) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(AppTheme.accent.opacity(0.14))
                    .frame(width: 48, height: 48)
                    .overlay(
                        ServiceIcon(key: service.icon)
                            .sized(22)
                            .foregroundStyle(AppTheme.accent)
                    )
                VStack(alignment: .leading, spacing: 3) {
                    Text(service.title.resolved())
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(service.subtitle.resolved())
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
                LucideIcon.chevronRight.sized(18)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(16)
        }
    }
}

// MARK: - ServiceIcon

/// Maps a string key from config JSON to the correct `LucideIcon` asset,
/// falling back to `.info` for unknown keys.
struct ServiceIcon {
    let key: String

    private var icon: LucideIcon {
        switch key {
        case "bus": return .bus
        case "bus-front": return .busFront
        case "accessibility": return .accessibility
        case "map-pin": return .mapPin
        case "map": return .map
        case "route": return .route
        case "ticket": return .ticket
        case "compass": return .compass
        case "users": return .users
        case "phone": return .phone
        case "globe": return .globe
        case "clock": return .clock
        case "navigation": return .navigation
        default: return .info
        }
    }

    func sized(_ pt: CGFloat) -> some View {
        icon.sized(pt)
    }

    var image: Image { icon.image }
}

// MARK: - PressableCardStyle

/// Button style for navigation rows built from `GlassCard`.
/// Provides a subtle press-state scale + opacity shift for tactile feedback.
struct PressableCardStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .contentShape(Rectangle())
            .scaleEffect(configuration.isPressed ? 0.985 : 1.0)
            .opacity(configuration.isPressed ? 0.92 : 1.0)
            .animation(.spring(response: 0.28, dampingFraction: 0.78), value: configuration.isPressed)
    }
}
