package io.github.diarmaidob.anyfin.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.diarmaidob.anyfin.R

@Composable
fun AnyfinIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(108.dp) //
            .clip(RoundedCornerShape(20.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Anyfin Logo",
            modifier = Modifier.matchParentSize()
        )
    }
}