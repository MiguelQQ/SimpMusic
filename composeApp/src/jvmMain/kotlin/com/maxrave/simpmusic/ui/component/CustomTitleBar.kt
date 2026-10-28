package com.maxrave.simpmusic.ui.component
import org.jetbrains.compose.resources.stringResource

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.maxrave.simpmusic.extension.DesktopWindowChrome
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Remove
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.UnfoldLess
import com.maxrave.simpmusic.ui.icon.UnfoldMore
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.theme.windowCloseButton
import com.maxrave.simpmusic.ui.theme.windowCloseButtonHover
import com.maxrave.simpmusic.ui.theme.windowMaximiseButton
import com.maxrave.simpmusic.ui.theme.windowMaximiseButtonHover
import com.maxrave.simpmusic.ui.theme.windowMinimiseButton
import com.maxrave.simpmusic.ui.theme.windowMinimiseButtonHover
import java.awt.MouseInfo
import java.awt.Window
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cd_close
import simpmusic.composeapp.generated.resources.cd_minimize

/** Windows uses caption buttons on the right. macOS keeps traffic lights. Linux stays on the native bar. */
internal enum class DesktopTitleBarStyle {
    Windows,
    Mac,
}

internal fun desktopTitleBarStyle(osName: String): DesktopTitleBarStyle {
    val os = osName.lowercase()
    return if (os.contains("win")) DesktopTitleBarStyle.Windows else DesktopTitleBarStyle.Mac
}

/**
 * Custom title bar for JVM desktop application.
 * Provides minimize, maximize/restore, and close buttons with drag-to-move functionality.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomTitleBar(
    title: String,
    windowState: WindowState,
    window: Window,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    // Passed in rather than read from MaterialTheme: this bar is drawn outside AppTheme, so
    // MaterialTheme here would hand back the default light scheme no matter what the user picked.
    containerColor: Color = Color.Black,
    titleColor: Color = Color.White,
    accentColor: Color = Color.Transparent,
) {
    var isMaximized by remember { mutableStateOf(windowState.placement == WindowPlacement.Maximized) }
    var dragStartX by remember { mutableStateOf(0) }
    var dragStartY by remember { mutableStateOf(0) }
    val style = desktopTitleBarStyle(System.getProperty("os.name").orEmpty())

    LaunchedEffect(windowState.placement) {
        isMaximized = windowState.placement == WindowPlacement.Maximized
    }

    val toggleMaximize = {
        windowState.placement =
            if (windowState.placement == WindowPlacement.Maximized) {
                WindowPlacement.Floating
            } else {
                WindowPlacement.Maximized
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DesktopWindowChrome.TITLE_BAR_HEIGHT_DP.dp)
                .background(containerColor)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { toggleMaximize() })
                }.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            val mouseLocation = MouseInfo.getPointerInfo().location
                            dragStartX = mouseLocation.x - window.x
                            dragStartY = mouseLocation.y - window.y
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val mouseLocation = MouseInfo.getPointerInfo().location
                            if (windowState.placement == WindowPlacement.Maximized) {
                                windowState.placement = WindowPlacement.Floating
                                dragStartX = (windowState.size.width.value / 2).toInt()
                                dragStartY = 20
                            }
                            window.setLocation(
                                mouseLocation.x - dragStartX,
                                mouseLocation.y - dragStartY,
                            )
                        },
                    )
                },
    ) {
        if (style == DesktopTitleBarStyle.Windows) {
            WindowsCaption(
                title = title,
                titleColor = titleColor,
                isMaximized = isMaximized,
                onMinimize = { windowState.isMinimized = true },
                onMaximize = toggleMaximize,
                onClose = onCloseRequest,
            )
        } else {
            MacCaption(
                title = title,
                titleColor = titleColor,
                isMaximized = isMaximized,
                onMinimize = { windowState.isMinimized = true },
                onMaximize = toggleMaximize,
                onClose = onCloseRequest,
            )
        }
        if (accentColor.alpha > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(accentColor),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WindowsCaption(
    title: String,
    titleColor: Color,
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = typo().labelSmall.copy(letterSpacing = 0.2.sp),
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp),
        )
        WindowsCaptionButton(
            kind = WindowControlIcon.Minimize,
            contentColor = titleColor,
            onClick = onMinimize,
            contentDescription = stringResource(Res.string.cd_minimize),
        )
        WindowsCaptionButton(
            kind = if (isMaximized) WindowControlIcon.Restore else WindowControlIcon.Maximize,
            contentColor = titleColor,
            onClick = onMaximize,
            contentDescription = stringResource(Res.string.cd_minimize),
        )
        WindowsCaptionButton(
            kind = WindowControlIcon.Close,
            contentColor = titleColor,
            danger = true,
            onClick = onClose,
            contentDescription = stringResource(Res.string.cd_close),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MacCaption(
    title: String,
    titleColor: Color,
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WindowControlButton(
                onClick = onClose,
                backgroundColor = windowCloseButton,
                hoverColor = windowCloseButtonHover,
                icon = WindowControlIcon.Close,
            )
            Spacer(modifier = Modifier.width(8.dp))
            WindowControlButton(
                onClick = onMinimize,
                backgroundColor = windowMinimiseButton,
                hoverColor = windowMinimiseButtonHover,
                icon = WindowControlIcon.Minimize,
            )
            Spacer(modifier = Modifier.width(8.dp))
            WindowControlButton(
                onClick = onMaximize,
                backgroundColor = windowMaximiseButton,
                hoverColor = windowMaximiseButtonHover,
                icon = if (isMaximized) WindowControlIcon.Restore else WindowControlIcon.Maximize,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = title,
            style = typo().labelSmall.copy(letterSpacing = 0.6.sp),
            color = titleColor,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

private enum class WindowControlIcon {
    Minimize,
    Maximize,
    Restore,
    Close,
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WindowsCaptionButton(
    kind: WindowControlIcon,
    contentColor: Color,
    onClick: () -> Unit,
    contentDescription: String,
    danger: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val background =
        when {
            danger && isHovered -> Color(0xFFE81123)
            isHovered -> contentColor.copy(alpha = 0.12f)
            else -> Color.Transparent
        }
    val glyph = if (danger && isHovered) Color.White else contentColor
    Box(
        modifier =
            Modifier
                .width(46.dp)
                .fillMaxHeight()
                .background(background)
                .hoverable(interactionSource)
                .semantics { this.contentDescription = contentDescription }
                .pointerInput(onClick) {
                    detectTapGestures(onTap = { onClick() })
                },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(10.dp)) {
            val stroke = 1.1.dp.toPx()
            when (kind) {
                WindowControlIcon.Minimize ->
                    drawLine(
                        color = glyph,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = stroke,
                    )
                WindowControlIcon.Maximize ->
                    drawRect(color = glyph, style = Stroke(width = stroke))
                WindowControlIcon.Restore -> {
                    val inset = size.width * 0.22f
                    drawRect(
                        color = glyph,
                        topLeft = Offset(inset, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width - inset, size.height - inset),
                        style = Stroke(width = stroke),
                    )
                    drawRect(
                        color = glyph,
                        topLeft = Offset(0f, inset),
                        size = androidx.compose.ui.geometry.Size(size.width - inset, size.height - inset),
                        style = Stroke(width = stroke),
                    )
                }
                WindowControlIcon.Close -> {
                    drawLine(glyph, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = stroke)
                    drawLine(glyph, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = stroke)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    hoverColor: Color,
    icon: WindowControlIcon,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier =
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (isHovered) hoverColor else backgroundColor)
                .hoverable(interactionSource)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                },
        contentAlignment = Alignment.Center,
    ) {
        if (isHovered) {
            Box(modifier = Modifier.padding(1.dp)) {
                when (icon) {
                    WindowControlIcon.Minimize -> {
                        Icon(SimpIcons.Remove, tint = Color.DarkGray, contentDescription = stringResource(Res.string.cd_minimize))
                    }
                    WindowControlIcon.Maximize -> {
                        Icon(
                            modifier = Modifier.rotate(45f),
                            imageVector = SimpIcons.UnfoldMore,
                            tint = Color.DarkGray,
                            contentDescription = stringResource(Res.string.cd_minimize),
                        )
                    }
                    WindowControlIcon.Restore -> {
                        Icon(
                            modifier = Modifier.rotate(45f),
                            imageVector = SimpIcons.UnfoldLess,
                            tint = Color.DarkGray,
                            contentDescription = stringResource(Res.string.cd_minimize),
                        )
                    }
                    WindowControlIcon.Close -> {
                        Icon(
                            imageVector = SimpIcons.Close,
                            tint = Color.DarkGray,
                            contentDescription = stringResource(Res.string.cd_close),
                        )
                    }
                }
            }
        }
    }
}
