import SwiftUI
import UIKit

// MARK: - ContactInfoView

/// Full contact details: phone, email, TDD, address, office hours.
/// Each tappable row routes to the appropriate system handler
/// (tel:, mailto:, or Apple Maps for the address).
struct ContactInfoView: View {
    let contact: OperatorConfig.ContactConfig

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                GlassCard(cornerRadius: 16) {
                    VStack(spacing: 0) {
                        if let phone = contact.phone {
                            row(
                                icon: .phone,
                                title: String(localized: "operator_phone"),
                                subtitle: phone,
                                url: telURL(from: phone),
                                isFirst: true
                            )
                        }

                        if let email = contact.email {
                            divider
                            row(
                                icon: .mail,
                                title: String(localized: "operator_email"),
                                subtitle: email,
                                url: URL(string: "mailto:\(email)")
                            )
                        }

                        if let tdd = contact.tdd {
                            divider
                            row(
                                icon: .headphones,
                                title: String(localized: "services_label_tdd"),
                                subtitle: tdd,
                                url: telURL(from: tdd)
                            )
                        }

                        if let address = contact.address {
                            divider
                            row(
                                icon: .mapPin,
                                title: String(localized: "services_section_contact"),
                                subtitle: address,
                                url: mapsURL(for: address),
                                multiline: true
                            )
                        }

                        if let hours = contact.hours {
                            divider
                            row(
                                icon: .clock,
                                title: String(localized: "services_label_office_hours"),
                                subtitle: hours.resolved(),
                                url: nil,
                                multiline: true
                            )
                        }
                    }
                }

            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(String(localized: "services_section_contact"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar(.hidden, for: .tabBar)
    }

    // MARK: Row

    private var divider: some View {
        Rectangle()
            .fill(AppTheme.separatorLine)
            .frame(height: 0.5)
            .padding(.leading, 52)
    }

    @ViewBuilder
    private func row(
        icon: LucideIcon,
        title: String,
        subtitle: String,
        url: URL?,
        multiline: Bool = false,
        isFirst: Bool = false
    ) -> some View {
        if let url {
            Button {
                UIApplication.shared.open(url)
            } label: {
                rowContent(icon: icon, title: title, subtitle: subtitle, multiline: multiline, tappable: true)
            }
            .buttonStyle(.plain)
        } else {
            rowContent(icon: icon, title: title, subtitle: subtitle, multiline: multiline, tappable: false)
        }
    }

    private func rowContent(
        icon: LucideIcon,
        title: String,
        subtitle: String,
        multiline: Bool,
        tappable: Bool
    ) -> some View {
        HStack(alignment: .top, spacing: 14) {
            icon.image
                .font(.body)
                .foregroundStyle(AppTheme.accent)
                .frame(width: 24)
                .padding(.top, 2)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textTertiary)
                Text(subtitle)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(multiline ? nil : 1)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
            if tappable {
                LucideIcon.chevronRight.sized(18)
                    .foregroundStyle(AppTheme.textTertiary)
                    .padding(.top, 2)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .contentShape(Rectangle())
    }

    // MARK: URL builders

    private func telURL(from phone: String) -> URL? {
        let digits = phone.filter { $0.isNumber || $0 == "+" }
        guard !digits.isEmpty else { return nil }
        return URL(string: "tel:\(digits)")
    }

    private func mapsURL(for address: String) -> URL? {
        guard let encoded = address.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            return nil
        }
        return URL(string: "http://maps.apple.com/?q=\(encoded)")
    }
}
