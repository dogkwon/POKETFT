package com.poketft.overlay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary      = PokeAccent,
    secondary    = PokeBlue,
    background   = PokeBg,
    surface      = PokeSurface,
    onPrimary    = PokeTextPri,
    onSecondary  = PokeTextPri,
    onBackground = PokeTextPri,
    onSurface    = PokeTextPri
)

@Composable
fun PoketftTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
