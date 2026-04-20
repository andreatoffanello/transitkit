import SwiftUI
import UIKit

// MARK: - ServiceDetailView

/// Full detail screen for a single service.
/// Layout: hero header + description, then one card per optional section
/// (audience, how to ride, hours, fare, service area, notes, links),
/// with a pinned primary CTA at the bottom when available.
struct ServiceDetailView: View {
    let service: ServiceInfo
    let phone: String?
    @Environment(DeepLinkRouter.self) private var router

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                hero

                descriptionCard

                if let audience = service.audience, !audience.resolved().isEmpty {
                    labeledCard(
                        icon: .users,
                        label: String(localized: "services_label_audience"),
                        body: audience.resolved()
                    )
                }

                if let steps = service.steps, !steps.isEmpty {
                    stepsCard(steps: steps)
                }

                if let hours = service.hours, !hours.resolved().isEmpty {
                    labeledCard(
                        icon: .clock,
                        label: String(localized: "services_label_hours"),
                        body: hours.resolved()
                    )
                }

                if let fare = service.fare, !fare.resolved().isEmpty {
                    labeledCard(
                        icon: .ticket,
                        label: String(localized: "services_label_fare"),
                        body: fare.resolved()
                    )
                }

                if let area = service.serviceArea, !area.resolved().isEmpty {
                    labeledCard(
                        icon: .map,
                        label: String(localized: "services_label_area"),
                        body: area.resolved()
                    )
                }

                if let notes = service.notes, !notes.isEmpty {
                    notesCard(notes: notes)
                }

                if let links = service.links, !links.isEmpty {
                    linksCard(links: links)
                }

                // Skip the secondary questions-call strip when the pinned CTA
                // is already a phone action — avoid offering the same phone
                // action twice (e.g. Paratransit / Rural services).
                if let phone, !phone.isEmpty, service.cta?.type != "phone" {
                    contactStrip(phone: phone)
                        .padding(.top, 4)
                }

                Spacer(minLength: service.cta == nil ? 24 : 32)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { EmptyView() }
        }
        .toolbar(.hidden, for: .tabBar)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if let cta = service.cta {
                ctaDockedBar(cta: cta)
            }
        }
    }

    // MARK: Hero

    private var hero: some View {
        VStack(alignment: .leading, spacing: 0) {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(AppTheme.accent.opacity(0.14))
                .frame(width: 72, height: 72)
                .overlay(
                    ServiceIcon(key: service.icon).sized(34)
                        .foregroundStyle(AppTheme.accent)
                )
                .padding(.bottom, 16)
            Text(service.title.resolved())
                .font(.system(size: 34, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.bottom, 4)
            Text(service.subtitle.resolved())
                .font(.title3)
                .foregroundStyle(AppTheme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
        .padding(.top, 4)
    }

    // MARK: Description

    private var descriptionCard: some View {
        GlassCard(cornerRadius: 16) {
            Text(service.description.resolved())
                .font(.subheadline)
                .foregroundStyle(AppTheme.textPrimary)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
        }
    }

    // MARK: Labeled text card

    private func labeledCard(icon: LucideIcon, label: String, body: String) -> some View {
        GlassCard(cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 10) {
                sectionLabel(icon: icon, text: label)
                Text(body)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineSpacing(4)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
    }

    // MARK: Steps card

    private func stepsCard(steps: [LocalizedText]) -> some View {
        GlassCard(cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 12) {
                sectionLabel(icon: .listOrdered, text: String(localized: "services_label_how"))
                VStack(spacing: 0) {
                    ForEach(Array(steps.enumerated()), id: \.offset) { idx, step in
                        HStack(alignment: .top, spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(AppTheme.accent)
                                    .frame(width: 24, height: 24)
                                Text("\(idx + 1)")
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(.white)
                            }
                            Text(step.resolved())
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.textPrimary)
                                .lineSpacing(3)
                                .fixedSize(horizontal: false, vertical: true)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding(.vertical, 10)

                        if idx < steps.count - 1 {
                            Rectangle()
                                .fill(AppTheme.separatorLine)
                                .frame(height: 0.5)
                                .padding(.leading, 36)
                        }
                    }
                }
            }
            .padding(16)
        }
    }

    // MARK: Notes card

    private func notesCard(notes: [LocalizedText]) -> some View {
        GlassCard(cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 12) {
                sectionLabel(icon: .info, text: String(localized: "services_label_notes"))
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(Array(notes.enumerated()), id: \.offset) { _, note in
                        HStack(alignment: .top, spacing: 10) {
                            Circle()
                                .fill(AppTheme.accent)
                                .frame(width: 6, height: 6)
                                .padding(.top, 7)
                            Text(note.resolved())
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.textPrimary)
                                .lineSpacing(3)
                                .fixedSize(horizontal: false, vertical: true)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                }
            }
            .padding(16)
        }
    }

    // MARK: Links card

    /// Single-link variant renders one prominent button row (no section header).
    /// Multi-link variant keeps the section header but styles every row like
    /// the single one — so the list reads as real actions, not footnotes.
    @ViewBuilder
    private func linksCard(links: [ServiceLink]) -> some View {
        if links.count == 1, let link = links.first {
            linkRow(link: link)
        } else {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 8) {
                    LucideIcon.externalLink.image
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.accent)
                    Text(String(localized: "services_label_links").uppercased())
                        .font(.caption.weight(.semibold))
                        .kerning(0.6)
                        .foregroundStyle(AppTheme.textTertiary)
                }
                .padding(.horizontal, 4)

                VStack(spacing: 8) {
                    ForEach(Array(links.enumerated()), id: \.offset) { _, link in
                        linkRow(link: link)
                    }
                }
            }
        }
    }

    private func linkRow(link: ServiceLink) -> some View {
        Button {
            if let url = URL(string: link.url) {
                UIApplication.shared.open(url)
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
            }
        } label: {
            HStack(spacing: 14) {
                LucideIcon.externalLink.sized(20)
                    .foregroundStyle(AppTheme.accent)
                Text(link.label.resolved())
                    .font(.headline)
                    .foregroundStyle(AppTheme.textPrimary)
                    .multilineTextAlignment(.leading)
                    .lineLimit(2)
                Spacer(minLength: 0)
                LucideIcon.chevronRight.sized(18)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(.horizontal, 16)
            .frame(minHeight: 52)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(AppTheme.bgSecondary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
            )
            .contentShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(PressableButtonStyle())
    }

    // MARK: Contact strip

    /// Pill-button with accent tint. Reads as tappable at a glance so users
    /// don't miss the phone-call affordance buried under content.
    private func contactStrip(phone: String) -> some View {
        HStack {
            Spacer(minLength: 0)
            Button {
                if let url = telURL(from: phone) {
                    UIApplication.shared.open(url)
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                }
            } label: {
                HStack(spacing: 8) {
                    LucideIcon.phone.sized(14)
                        .foregroundStyle(AppTheme.accent)
                    Text(String(format: String(localized: "services_label_questions %@"), phone))
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(AppTheme.accent)
                        .lineLimit(1)
                }
                .padding(.horizontal, 16)
                .frame(height: 40)
                .background(
                    Capsule(style: .continuous)
                        .fill(AppTheme.accent.opacity(0.10))
                )
                .contentShape(Capsule(style: .continuous))
            }
            .buttonStyle(PressableButtonStyle())
            .accessibilityIdentifier("services_questions_call")
            Spacer(minLength: 0)
        }
    }

    // MARK: CTA button

    /// Solid-fill primary button. Kept separate from the dock so we can
    /// reuse the button without the wrapping bar chrome if needed.
    private func ctaButton(cta: ServiceCTA) -> some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            if handleInternalCTA(cta) { return }
            guard let url = resolveCTA(cta) else { return }
            UIApplication.shared.open(url)
        } label: {
            HStack(spacing: 10) {
                ctaIcon(for: cta).sized(18)
                Text(cta.label.resolved())
                    .font(.headline)
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(AppTheme.accent)
            )
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityIdentifier("service_detail_cta")
    }

    private func ctaIcon(for cta: ServiceCTA) -> LucideIcon {
        switch cta.type {
        case "phone":    return .phone
        case "internal": return .map
        default:         return .externalLink
        }
    }

    /// Dispatches `transitkit://` CTAs through the in-app router instead of
    /// bouncing through `UIApplication.shared.open`, which can surface a
    /// system confirmation when reopening the app's own scheme.
    /// Returns true if the CTA was handled internally.
    private func handleInternalCTA(_ cta: ServiceCTA) -> Bool {
        guard cta.type == "internal",
              let url = URL(string: cta.value),
              url.scheme == "transitkit"
        else { return false }
        switch url.host {
        case "map":
            router.pendingMapOpen = UUID()
            return true
        default:
            return false
        }
    }

    /// Docked bar at the bottom edge: solid background so scrolling content
    /// no longer bleeds through, a 0.5pt hairline for separation, and a short
    /// 12pt fade above the hairline so content softens into the dock rather
    /// than clipping abruptly.
    private func ctaDockedBar(cta: ServiceCTA) -> some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color(uiColor: UIColor { tc in
                    tc.userInterfaceStyle == .dark
                        ? UIColor(white: 1, alpha: 0.14)
                        : UIColor(white: 0, alpha: 0.12)
                }))
                .frame(height: 1)
            ctaButton(cta: cta)
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 8)
        }
        .background(AppTheme.background)
        .overlay(alignment: .top) {
            LinearGradient(
                colors: [AppTheme.background.opacity(0), AppTheme.background],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 16)
            .offset(y: -16)
            .allowsHitTesting(false)
        }
    }

    // MARK: Helpers

    private func sectionLabel(icon: LucideIcon, text: String) -> some View {
        HStack(spacing: 8) {
            icon.image
                .font(.subheadline)
                .foregroundStyle(AppTheme.accent)
            Text(text.uppercased())
                .font(.caption.weight(.semibold))
                .kerning(0.6)
                .foregroundStyle(AppTheme.textTertiary)
        }
    }

    private func telURL(from phone: String) -> URL? {
        let digits = phone.filter { $0.isNumber || $0 == "+" }
        guard !digits.isEmpty else { return nil }
        return URL(string: "tel:\(digits)")
    }

    private func resolveCTA(_ cta: ServiceCTA) -> URL? {
        if cta.type == "phone" {
            return telURL(from: cta.value)
        }
        return URL(string: cta.value)
    }
}

