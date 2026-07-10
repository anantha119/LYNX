import SwiftUI

/// Design tokens mirroring the Lynx web app's dark "terminal" aesthetic
/// (see lynx/src/app/globals.css and the stone/amber palette used across
/// chat-app.tsx, chat-sidebar.tsx, chat-messages.tsx, v0-ai-chat.tsx).
enum LynxColor {
    static let amber = Color(red: 0.961, green: 0.620, blue: 0.043)       // #F59E0B
    static let amberBright = Color(red: 0.988, green: 0.749, blue: 0.141) // amber-400

    static let bg = Color(red: 0x08 / 255.0, green: 0x08 / 255.0, blue: 0x08 / 255.0)         // #080808
    static let sidebarBg = Color(red: 0x06 / 255.0, green: 0x06 / 255.0, blue: 0x06 / 255.0)  // #060606
    static let panelBg = Color(red: 0x0e / 255.0, green: 0x0e / 255.0, blue: 0x0e / 255.0)    // #0e0e0e
    static let codeBg = Color(red: 0x0a / 255.0, green: 0x0a / 255.0, blue: 0x0a / 255.0)     // #0a0a0a

    // stone-* approximations (Tailwind stone scale)
    static let stone100 = Color(white: 0.96)
    static let stone200 = Color(white: 0.90)
    static let stone300 = Color(white: 0.80)
    static let stone400 = Color(white: 0.65)
    static let stone500 = Color(white: 0.50)
    static let stone600 = Color(white: 0.38)
    static let stone700 = Color(red: 0.28, green: 0.26, blue: 0.24)
    static let stone800 = Color(red: 0.19, green: 0.18, blue: 0.16)
    static let stone900 = Color(red: 0.11, green: 0.10, blue: 0.09)
}

enum LynxFont {
    /// Named instances resolved from the bundled variable fonts
    /// (see Resources/Fonts — Syne-Variable.ttf, JetBrainsMono-Variable.ttf).
    enum Display {
        static let regular = "Syne-Regular"
        static let medium = "Syne-Medium"
        static let semibold = "Syne-SemiBold"
        static let bold = "Syne-Bold"
        static let extrabold = "Syne-ExtraBold"
    }
    enum Mono {
        static let light = "JetBrainsMonoRoman-Light"
        static let regular = "JetBrainsMonoRoman-Regular"
        static let medium = "JetBrainsMonoRoman-Medium"
    }

    static func display(_ size: CGFloat, weight: String = Display.bold) -> Font {
        .custom(weight, size: size)
    }

    static func mono(_ size: CGFloat, weight: String = Mono.regular) -> Font {
        .custom(weight, size: size)
    }
}

enum LynxMetrics {
    static let cornerRadius: CGFloat = 3
    static let cardCornerRadius: CGFloat = 4
}
