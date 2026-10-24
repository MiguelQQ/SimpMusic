package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.theme.killmonGreen
import com.maxrave.simpmusic.ui.theme.killmonOnGreen

/**
 * Solid Killmon green with black text, including while a form is empty or a request is in flight.
 *
 * [enabled] is never used. Material would paint the disabled content gray, and the app typography
 * bakes a gray body color into [androidx.compose.material3.MaterialTheme] so a stock label inherits
 * that instead of the button content color. Validation belongs in the click handler.
 *
 * [loading] keeps the same colors, swaps the label for a black spinner, and drops extra clicks.
 * Set it to true synchronously at the start of [onClick], before any async work.
 */
@Composable
fun KillmonPrimaryButton(
    text: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = {
            if (loading) return@Button
            onClick()
        },
        enabled = true,
        modifier =
            modifier.then(
                if (expanded) Modifier.fillMaxWidth().height(52.dp) else Modifier.height(48.dp),
            ),
        shape = RoundedCornerShape(24.dp),
        colors = killmonPrimaryButtonColors(),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = killmonOnGreen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(
                text = text,
                color = killmonOnGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

/** Enabled and disabled slots share the same solid green and black so nothing can fade the button. */
@Composable
fun killmonPrimaryButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(
        containerColor = killmonGreen,
        contentColor = killmonOnGreen,
        disabledContainerColor = killmonGreen,
        disabledContentColor = killmonOnGreen,
    )
