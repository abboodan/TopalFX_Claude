package com.topaloglu.topalfx.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.ui.calculator.ValidatedNumberField
import com.topaloglu.topalfx.ui.calculator.formatRate
import com.topaloglu.topalfx.util.Prefs
import com.topaloglu.topalfx.viewmodel.CalculatorViewModel

@Composable
fun SettingsScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pctAgentCost by remember { mutableStateOf(Prefs.getDefaultPctAgentCost(context)) }
    var flatAgentCost by remember { mutableStateOf(Prefs.getDefaultFlatAgentCost(context)) }
    var deductionBase by remember { mutableStateOf(Prefs.getDefaultDeductionBase(context)) }
    var customerDiscount by remember {
        mutableStateOf(formatRate(Prefs.getDefaultCustomerDiscount(context)))
    }

    val pctError = validationMessage(pctAgentCost)
    val flatError = validationMessage(flatAgentCost)
    val discountError = validationMessage(customerDiscount)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_defaults_section),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_defaults_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ValidatedNumberField(
            value = pctAgentCost,
            onValueChange = { pctAgentCost = it },
            label = stringResource(R.string.settings_default_pct_agent),
            errorText = pctError,
        )
        ValidatedNumberField(
            value = flatAgentCost,
            onValueChange = { flatAgentCost = it },
            label = stringResource(R.string.settings_default_flat_agent),
            errorText = flatError,
        )
        ValidatedNumberField(
            value = customerDiscount,
            onValueChange = { customerDiscount = it },
            label = stringResource(R.string.settings_default_customer_discount),
            errorText = discountError,
        )

        Text(
            text = stringResource(R.string.settings_default_deduction),
            style = MaterialTheme.typography.titleSmall,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DeductionBase.entries.forEachIndexed { index, base ->
                SegmentedButton(
                    selected = deductionBase == base,
                    onClick = { deductionBase = base },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = DeductionBase.entries.size,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            if (base == DeductionBase.ON_RECEIVED) R.string.deduction_on_received
                            else R.string.deduction_on_delivered
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Button(
            onClick = {
                Prefs.setDefaults(
                    context = context,
                    pctAgentCost = pctAgentCost,
                    flatAgentCost = flatAgentCost,
                    deductionBase = deductionBase,
                    customerDiscount = CalculatorViewModel.parse(customerDiscount)
                        .takeIf { it.isFinite() && it >= 0.0 }
                        ?: Prefs.DEFAULT_CUSTOMER_DISCOUNT,
                )
                onSaved()
            },
            enabled = pctError == null && flatError == null && discountError == null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

@Composable
private fun validationMessage(raw: String): String? {
    if (raw.isBlank()) return null
    val parsed = CalculatorViewModel.parse(raw)
    return when {
        parsed.isNaN() -> stringResource(R.string.error_invalid_number)
        parsed < 0.0 -> stringResource(R.string.error_negative_value)
        else -> null
    }
}
