package com.topaloglu.topalfx.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.ui.theme.TopalFXTheme

/**
 * The two actions that have to be reachable at any scroll position, because the customer
 * is standing at the counter while they are used.
 *
 * Reset is outlined and narrower than Calculate on purpose — it wipes the sheet, so it
 * should be the harder of the two to hit by accident.
 */
@Composable
fun CalculatorActionBar(
    onCalculate: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    calculateEnabled: Boolean = true,
) {
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
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_reset))
            }
            Button(
                onClick = onCalculate,
                enabled = calculateEnabled,
                modifier = Modifier.weight(1.6f),
            ) {
                Icon(Icons.Filled.Calculate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_calculate))
            }
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun CalculatorActionBarArabicPreview() {
    TopalFXTheme {
        CalculatorActionBar(onCalculate = {}, onReset = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun CalculatorActionBarDisabledPreview() {
    TopalFXTheme {
        CalculatorActionBar(onCalculate = {}, onReset = {}, calculateEnabled = false)
    }
}
