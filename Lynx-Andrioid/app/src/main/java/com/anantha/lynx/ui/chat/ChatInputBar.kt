package com.anantha.lynx.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import com.anantha.lynx.ui.theme.LynxMetrics

/**
 * Ported from Lynx-IOS/Lynx-IOS/Features/Chat/ChatInputBar.swift — auto-
 * growing text field with amber corner-bracket "terminal chrome" decoration.
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Describe what you want to build…",
) {
    val canSend = text.isNotBlank()

    Box(
        modifier = modifier
            .background(LynxColor.panelBg, RoundedCornerShape(LynxMetrics.cornerRadius))
            .border(1.dp, LynxColor.stone800, RoundedCornerShape(LynxMetrics.cornerRadius)),
    ) {
        Column {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = LynxFont.mono(14.sp).copy(color = LynxColor.stone200),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(LynxColor.amberBright),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 20.dp, max = 120.dp),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(placeholder, style = LynxFont.mono(14.sp), color = LynxColor.stone600)
                        }
                        innerTextField()
                    },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 10.dp),
            ) {
                IconButton(onClick = { /* no-op, mirrors iOS placeholder button */ }) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Attach", tint = LynxColor.stone600)
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (canSend) LynxColor.amberBright else Color.Transparent,
                            RoundedCornerShape(LynxMetrics.cornerRadius),
                        )
                        .border(
                            width = if (canSend) 0.dp else 1.dp,
                            color = LynxColor.stone700,
                            shape = RoundedCornerShape(LynxMetrics.cornerRadius),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = { if (canSend) onSend() }, enabled = canSend) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.Black else LynxColor.stone600,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        CornerMarks(modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun CornerMarks(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val mark = 10.dp.toPx()
        val stroke = Stroke(width = 1.dp.toPx())
        val color = LynxColor.amber.copy(alpha = 0.6f)
        for (corner in 0 until 4) {
            val isTop = corner < 2
            val isLeft = corner % 2 == 0
            val x = if (isLeft) 0f else size.width
            val y = if (isTop) 0f else size.height
            val dx = if (isLeft) mark else -mark
            val dy = if (isTop) mark else -mark
            val path = Path().apply {
                moveTo(x, y + dy)
                lineTo(x, y)
                lineTo(x + dx, y)
            }
            drawPath(path, color = color, style = stroke)
        }
    }
}
