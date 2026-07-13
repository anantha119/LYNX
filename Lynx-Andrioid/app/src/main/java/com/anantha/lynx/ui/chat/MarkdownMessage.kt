package com.anantha.lynx.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/**
 * Renders assistant message markdown. Ported from
 * Lynx-IOS/Lynx-IOS/Features/Chat/MarkdownMessage.swift.
 *
 * Colors/typography are themed to match Lynx's mono/amber palette (body,
 * headings, code, links, blockquote text). The amber blockquote left-rule
 * and the `›` bullet glyph iOS draws by hand aren't exposed as clean
 * override points in this renderer's public API — left as a known gap
 * rather than guessing at undocumented internals without a device to
 * visually verify against.
 */
@Composable
fun MarkdownMessage(content: String, streaming: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom) {
        Markdown(
            content,
            colors = markdownColor(
                text = LynxColor.stone200,
                codeBackground = LynxColor.stone800,
                inlineCodeBackground = LynxColor.stone800,
                dividerColor = LynxColor.stone800,
                tableBackground = LynxColor.codeBg,
            ),
            typography = markdownTypography(
                text = LynxFont.mono(14.sp),
                code = LynxFont.mono(12.sp),
                inlineCode = LynxFont.mono(12.sp, FontWeight.Medium).copy(color = LynxColor.amberBright),
                h1 = LynxFont.display(17.sp, FontWeight.Bold).copy(color = LynxColor.stone100),
                h2 = LynxFont.display(15.sp, FontWeight.Bold).copy(color = LynxColor.stone100),
                h3 = LynxFont.display(14.sp, FontWeight.SemiBold).copy(color = LynxColor.stone200),
                h4 = LynxFont.mono(14.sp),
                h5 = LynxFont.mono(13.sp),
                h6 = LynxFont.mono(12.sp),
                quote = LynxFont.mono(14.sp).copy(color = LynxColor.stone400, fontStyle = FontStyle.Italic),
                paragraph = LynxFont.mono(14.sp).copy(color = LynxColor.stone200),
                ordered = LynxFont.mono(14.sp).copy(color = LynxColor.stone200),
                bullet = LynxFont.mono(14.sp).copy(color = LynxColor.stone200),
                list = LynxFont.mono(14.sp).copy(color = LynxColor.stone200),
                textLink = TextLinkStyles(
                    style = SpanStyle(color = LynxColor.amberBright, textDecoration = TextDecoration.Underline),
                ),
                table = LynxFont.mono(12.sp).copy(color = LynxColor.stone300),
            ),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (streaming) {
            Box(modifier = Modifier.width(2.dp).height(14.dp).background(LynxColor.amberBright))
        }
    }
}
