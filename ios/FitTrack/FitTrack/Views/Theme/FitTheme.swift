import SwiftUI
import UIKit

// MARK: - Palette
//
// Dark-first athletic palette, mirroring the Android `Color.kt`: Volt lime
// primary, Sky secondary, Coral tertiary, near-black layered surfaces. Every
// color is dynamic so the light theme stays readable without a second set
// of view code.

private func dyn(_ dark: UInt32, _ light: UInt32) -> Color {
    Color(uiColor: UIColor { traits in
        let hex = traits.userInterfaceStyle == .dark ? dark : light
        return UIColor(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    })
}

extension Color {
    /// Primary accent. Electric lime on dark, forest green on light.
    static let fitPrimary = dyn(0xC6FF4A, 0x1F7A1F)
    /// Text drawn on top of `fitPrimary`.
    static let fitOnPrimary = dyn(0x0B1A00, 0xFFFFFF)
    static let fitPrimaryContainer = dyn(0x2E3F0E, 0xD7FFB0)
    static let fitOnPrimaryContainer = dyn(0xD9FF8A, 0x0B2A00)

    static let fitSecondary = dyn(0x7FB3FF, 0x2F6FDB)
    static let fitSecondaryContainer = dyn(0x1C3458, 0xDCE8FF)
    static let fitTertiary = dyn(0xFF8A73, 0xD9452B)
    static let fitTertiaryContainer = dyn(0x5C1F10, 0xFFDAD3)

    static let fitBackground = dyn(0x0B0C10, 0xF4F5F8)
    static let fitOnBackground = dyn(0xF2F3F7, 0x15161C)
    /// Tiles / cards.
    static let fitSurfaceLow = dyn(0x15161C, 0xFFFFFF)
    /// Nav bar, sheets.
    static let fitSurface = dyn(0x1A1B22, 0xFFFFFF)
    /// Inputs, unselected pills.
    static let fitSurfaceHigh = dyn(0x21222B, 0xE2E4EB)
    static let fitOnSurfaceVariant = dyn(0xA2A6B5, 0x5B5F6E)
    /// Hairline tile border.
    static let fitOutline = dyn(0x262833, 0xDADCE3)
}

// MARK: - Theme mode

enum ThemeMode: String, CaseIterable, Identifiable {
    case system, dark, light
    var id: String { rawValue }

    var label: String {
        switch self {
        case .system: return "System"
        case .dark: return "Dark"
        case .light: return "Light"
        }
    }

    /// nil lets the system decide.
    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .dark: return .dark
        case .light: return .light
        }
    }

    static let storageKey = "theme_mode"
    /// The app is designed dark-first, so a fresh install opens dark.
    static let defaultMode: ThemeMode = .dark
}

// MARK: - Layout constants

enum Fit {
    static let gutter: CGFloat = 20
    static let tileRadius: CGFloat = 20
    static let smallRadius: CGFloat = 12
}

// MARK: - Building blocks

/// Rounded surface with a hairline border — the basic card of the design.
struct Tile<Content: View>: View {
    var padding: CGFloat = 16
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.fitSurfaceLow)
            .clipShape(RoundedRectangle(cornerRadius: Fit.tileRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: Fit.tileRadius, style: .continuous)
                    .stroke(Color.fitOutline, lineWidth: 1)
            )
    }
}

/// Small uppercase section label.
struct Overline: View {
    let text: String
    var body: some View {
        Text(text.uppercased())
            .font(.caption2.weight(.semibold))
            .tracking(1.2)
            .foregroundStyle(Color.fitOnSurfaceVariant)
    }
}

/// Circular tinted icon.
struct IconBadge: View {
    let systemName: String
    var tint: Color = .fitPrimary
    var size: CGFloat = 40

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: size * 0.45, weight: .semibold))
            .foregroundStyle(tint)
            .frame(width: size, height: size)
            .background(tint.opacity(0.16))
            .clipShape(Circle())
    }
}

/// Big number with a small label under it.
struct StatValue: View {
    let value: String
    let label: String
    var tint: Color = .primary

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.system(size: 26, weight: .bold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(tint)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Overline(text: label)
        }
    }
}

/// Selectable pill used for filters and inline actions.
struct PillChip: View {
    let label: String
    var systemImage: String? = nil
    var selected: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let systemImage { Image(systemName: systemImage).font(.caption.weight(.semibold)) }
                Text(label).font(.subheadline.weight(.semibold))
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selected ? Color.fitPrimary : Color.fitSurfaceHigh)
            .foregroundStyle(selected ? Color.fitOnPrimary : Color.primary)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

/// Full-width filled call to action.
struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.fitPrimary.opacity(configuration.isPressed ? 0.8 : 1))
            .foregroundStyle(Color.fitOnPrimary)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

/// Full-width tonal (muted) action.
struct TonalButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.fitSurfaceHigh.opacity(configuration.isPressed ? 0.7 : 1))
            .foregroundStyle(Color.primary)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

/// Row with icon, title, subtitle and optional trailing view; used in
/// Settings-style lists inside a Tile.
struct ListRow<Trailing: View>: View {
    let title: String
    var subtitle: String? = nil
    var systemImage: String? = nil
    var tint: Color = .fitPrimary
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 14) {
            if let systemImage { IconBadge(systemName: systemImage, tint: tint, size: 36) }
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.semibold)).foregroundStyle(Color.primary)
                if let subtitle {
                    Text(subtitle).font(.caption).foregroundStyle(Color.fitOnSurfaceVariant)
                }
            }
            Spacer(minLength: 8)
            trailing()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

extension ListRow where Trailing == EmptyView {
    init(title: String, subtitle: String? = nil, systemImage: String? = nil, tint: Color = .fitPrimary) {
        self.init(title: title, subtitle: subtitle, systemImage: systemImage, tint: tint) { EmptyView() }
    }
}

/// Thin separator aligned with ListRow content.
struct RowDivider: View {
    var body: some View {
        Rectangle().fill(Color.fitOutline).frame(height: 1).padding(.leading, 66)
    }
}

struct SmallChevron: View {
    var body: some View {
        Image(systemName: "chevron.right")
            .font(.caption.weight(.semibold))
            .foregroundStyle(Color.fitOnSurfaceVariant)
    }
}

// MARK: - Global chrome

extension View {
    /// Dark-first screen background behind scrolling content.
    func fitScreenBackground() -> some View {
        background(Color.fitBackground.ignoresSafeArea())
    }
}

/// Applies the dark-first chrome to UIKit-backed bars once per process.
enum FitAppearance {
    static func apply() {
        let tab = UITabBarAppearance()
        tab.configureWithOpaqueBackground()
        tab.backgroundColor = UIColor(Color.fitSurface)
        tab.shadowColor = UIColor(Color.fitOutline)
        UITabBar.appearance().standardAppearance = tab
        UITabBar.appearance().scrollEdgeAppearance = tab

        let nav = UINavigationBarAppearance()
        nav.configureWithOpaqueBackground()
        nav.backgroundColor = UIColor(Color.fitBackground)
        nav.shadowColor = .clear
        nav.largeTitleTextAttributes = [.font: UIFont.systemFont(ofSize: 32, weight: .heavy)]
        UINavigationBar.appearance().standardAppearance = nav
        UINavigationBar.appearance().scrollEdgeAppearance = nav
        UINavigationBar.appearance().compactAppearance = nav
    }
}
