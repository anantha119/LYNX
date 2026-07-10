import MarkdownUI
import SwiftUI

/// Renders assistant message markdown with a theme matching the web app's
/// `MarkdownContent` (chat-messages.tsx): mono body text, amber inline code
/// and `›` bullets, dark fenced code blocks, amber blockquote rule.
struct MarkdownMessage: View {
    let content: String
    var streaming: Bool = false

    var body: some View {
        HStack(alignment: .bottom, spacing: 4) {
            Markdown(content)
                .markdownTheme(.lynx)
            if streaming {
                Rectangle()
                    .fill(LynxColor.amberBright)
                    .frame(width: 2, height: 14)
            }
        }
    }
}

private extension Theme {
    static let lynx = Theme()
        .text {
            ForegroundColor(LynxColor.stone200)
            FontFamilyVariant(.monospaced)
            FontSize(14)
        }
        .code {
            FontFamilyVariant(.monospaced)
            FontSize(.em(0.85))
            ForegroundColor(LynxColor.amberBright)
            BackgroundColor(LynxColor.stone800)
        }
        .link {
            ForegroundColor(LynxColor.amberBright)
            UnderlineStyle(.single)
        }
        .strong {
            FontWeight(.bold)
            ForegroundColor(LynxColor.stone100)
        }
        .heading1 { configuration in
            configuration.label
                .markdownTextStyle {
                    FontWeight(.bold)
                    FontSize(17)
                    ForegroundColor(LynxColor.stone100)
                }
                .markdownMargin(top: 16, bottom: 8)
        }
        .heading2 { configuration in
            configuration.label
                .markdownTextStyle {
                    FontWeight(.bold)
                    FontSize(15)
                    ForegroundColor(LynxColor.stone100)
                }
                .markdownMargin(top: 16, bottom: 8)
        }
        .heading3 { configuration in
            configuration.label
                .markdownTextStyle {
                    FontWeight(.semibold)
                    FontSize(14)
                    ForegroundColor(LynxColor.stone200)
                }
                .markdownMargin(top: 12, bottom: 6)
        }
        .paragraph { configuration in
            configuration.label
                .relativeLineSpacing(.em(0.3))
                .markdownMargin(top: 0, bottom: 10)
        }
        .blockquote { configuration in
            configuration.label
                .markdownTextStyle {
                    ForegroundColor(LynxColor.stone400)
                    FontStyle(.italic)
                }
                .padding(.leading, 12)
                .overlay(alignment: .leading) {
                    Rectangle()
                        .fill(LynxColor.amber.opacity(0.6))
                        .frame(width: 2)
                }
                .markdownMargin(top: 8, bottom: 8)
        }
        .bulletedListMarker { _ in
            Text("›")
                .font(LynxFont.mono(13))
                .foregroundStyle(LynxColor.amber)
        }
        .codeBlock { configuration in
            ScrollView(.horizontal, showsIndicators: false) {
                Text(configuration.content)
                    .font(LynxFont.mono(12))
                    .foregroundStyle(LynxColor.stone200)
                    .padding(12)
            }
            .background(LynxColor.codeBg)
            .overlay(
                RoundedRectangle(cornerRadius: 2)
                    .strokeBorder(LynxColor.stone800, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 2))
            .markdownMargin(top: 8, bottom: 8)
        }
        .table { configuration in
            configuration.label
                .fixedSize(horizontal: false, vertical: true)
                .markdownMargin(top: 8, bottom: 8)
        }
        .tableCell { configuration in
            configuration.label
                .markdownTextStyle {
                    FontSize(.em(0.85))
                    ForegroundColor(LynxColor.stone300)
                }
                .padding(8)
        }
}
