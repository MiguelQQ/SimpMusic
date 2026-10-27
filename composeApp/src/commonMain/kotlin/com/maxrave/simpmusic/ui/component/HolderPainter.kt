package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.killmon_gray

/** Killmon's existing monochrome launcher mark, rendered grey for missing artwork. */
@Composable
fun rememberHolderPainter(square: Boolean = false): Painter = painterResource(Res.drawable.killmon_gray)
