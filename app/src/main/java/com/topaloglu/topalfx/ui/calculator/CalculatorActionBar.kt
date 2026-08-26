package com.topaloglu.topalfx.ui.calculator

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.ui.theme.ProfitDark
import com.topaloglu.topalfx.ui.theme.ProfitLight
import com.topaloglu.topalfx.ui.theme.TopalFXTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What a button is showing right now. */
enum class ActionStatus { IDLE, BUSY, SUCCESS, ERROR }

/**
 * The two actions that have to be reachable at any scroll position, because the customer
 * is standing at the counter while they are used.
 *
 * Each button reports its own outcome inside itself, so the owner can see the run
 * happened without looking anywhere else. The two do it differently, on purpose:
 *
 * - [onReset] refreshes rates over the network, so it genuinely waits — it spins for
 *   however long that takes, then shows the outcome.
 * - [onCalculate] is pure arithmetic and returns instantly, so it never spins. Material's
 *   own guidance is that a loading indicator must reflect an ongoing process and never be
 *   decorative, and a spinner padded out to be visible on work that already finished
 *   would be exactly that. It jumps straight to the tick or the cross.
 *
 * The signatures carry the distinction: one is suspend, the other is not.
 *
 * Both return true when the action succeeded — for Calculate that means real figures came
 * out, for Reset that means the rates came back.
 *
 * Reset is outlined and narrower than Calculate because it wipes the sheet, so it should
 * be the harder of the two to hit by accident.
 */
@Composable
fun CalculatorActionBar(
    onCalculate: () -> Boolean,
    onReset: suspend () -> Boolean,
    modifier: Modifier = Modifier,
    calculateEnabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var calculateStatus by remember { mutableStateOf(ActionStatus.IDLE) }
    var resetStatus by remember { mutableStateOf(ActionStatus.IDLE) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResetButton(
                status = resetStatus,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (resetStatus == ActionStatus.IDLE) {
                        scope.launch {
                            resetStatus = ActionStatus.BUSY
                            val ok = runCatching { onReset() }.getOrDefault(false)
                            resetStatus = outcomeOf(ok)
                            delay(SETTLE_MS)
                            resetStatus = ActionStatus.IDLE
                        }
                    }
                },
            )
            CalculateButton(
                status = calculateStatus,
                // Stay pressable while showing an outcome, so the button does not go grey
                // mid-confirmation when the sheet is empty.
                enabled = calculateEnabled || calculateStatus != ActionStatus.IDLE,
                modifier = Modifier.weight(1.6f),
                onClick = {
                    if (calculateStatus == ActionStatus.IDLE) {
                        val ok = runCatching { onCalculate() }.getOrDefault(false)
                        calculateStatus = outcomeOf(ok)
                        scope.launch {
                            delay(SETTLE_MS)
                            calculateStatus = ActionStatus.IDLE
                        }
                    }
                },
            )
        }
    }
}

private fun outcomeOf(ok: Boolean) = if (ok) ActionStatus.SUCCESS else ActionStatus.ERROR

@Composable
private fun CalculateButton(
    status: ActionStatus,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = when (status) {
            ActionStatus.SUCCESS -> successGreen()
            ActionStatus.ERROR -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        label = "calculateContainer",
    )
    val content = when (status) {
        ActionStatus.SUCCESS -> MaterialTheme.colorScheme.surface
        ActionStatus.ERROR -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onPrimary
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
    ) {
        ActionContent(
            status = status,
            idleIcon = Icons.Filled.Calculate,
            label = stringResource(R.string.action_calculate),
            spinnerColor = content,
        )
    }
}

@Composable
private fun ResetButton(
    status: ActionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content by animateColorAsState(
        targetValue = when (status) {
            ActionStatus.SUCCESS -> successGreen()
            ActionStatus.ERROR -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        label = "resetContent",
    )
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
    ) {
        ActionContent(
            status = status,
            idleIcon = Icons.Filled.RestartAlt,
            label = stringResource(R.string.action_reset),
            spinnerColor = content,
        )
    }
}

/**
 * Swaps the button's inside between its label, a spinner, and the outcome mark. The label
 * is dropped while the outcome shows so the row never reflows mid-press.
 */
@Composable
private fun ActionContent(
    status: ActionStatus,
    idleIcon: ImageVector,
    label: String,
    spinnerColor: Color,
) {
    when (status) {
        ActionStatus.BUSY -> CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = spinnerColor,
        )
        ActionStatus.SUCCESS -> Icon(
            Icons.Filled.Check,
            contentDescription = stringResource(R.string.action_done),
            modifier = Modifier.size(20.dp),
        )
        ActionStatus.ERROR -> Icon(
            Icons.Filled.Close,
            contentDescription = stringResource(R.string.action_failed),
            modifier = Modifier.size(20.dp),
        )
        ActionStatus.IDLE -> {
            Icon(idleIcon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
private fun successGreen(): Color = if (isSystemInDarkTheme()) ProfitDark else ProfitLight

/** How long the tick or cross stays before the button returns to its label. */
private const val SETTLE_MS = 1300L

@Preview(showBackground = true, locale = "ar")
@Composable
private fun CalculatorActionBarArabicPreview() {
    TopalFXTheme {
        CalculatorActionBar(onCalculate = { true }, onReset = { true })
    }
}

@Preview(showBackground = true)
@Composable
private fun CalculatorActionBarDisabledPreview() {
    TopalFXTheme {
        CalculatorActionBar(
            onCalculate = { true },
            onReset = { true },
            calculateEnabled = false,
        )
    }
}
