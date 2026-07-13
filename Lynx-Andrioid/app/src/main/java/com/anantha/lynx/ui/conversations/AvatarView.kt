package com.anantha.lynx.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anantha.lynx.auth.AuthState
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import com.anantha.lynx.ui.theme.LynxMetrics

/**
 * User avatar — mirrors ConversationListView.swift's AvatarView: the Auth0
 * profile picture, falling back to an amber-tinted initial badge.
 */
@Composable
fun AvatarView(authState: AuthState, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(LynxMetrics.cornerRadius * 0.7f)
    val user = (authState as? AuthState.SignedIn)?.user
    val pictureUrl = user?.pictureUrl

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(LynxColor.amber.copy(alpha = 0.2f))
            .border(1.dp, LynxColor.amber.copy(alpha = 0.3f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (pictureUrl != null) {
            AsyncImage(
                model = pictureUrl.toString(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                (user?.name ?: "U").take(1).uppercase(),
                style = LynxFont.mono((size.value * 0.42f).sp),
                color = LynxColor.amberBright,
            )
        }
    }
}
