package com.topaloglu.topalfx.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.TransferDirection
import com.topaloglu.topalfx.ui.calculator.LabeledSwitchRow
import com.topaloglu.topalfx.ui.calculator.ValidatedNumberField
import com.topaloglu.topalfx.ui.theme.TopalFXTheme
import com.topaloglu.topalfx.util.Prefs
import com.topaloglu.topalfx.viewmodel.CalculatorViewModel

@Composable
fun SettingsScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var form by remember { mutableStateOf(SettingsFormState.from(context)) }

    SettingsScreenContent(
        form = form,
        onFormChange = { form = it },
        onSave = {
            Prefs.setDefaults(
                context = context,
                direction = form.direction,
                mode = form.mode,
                pctFee = form.pctFee,
                feeInclusive = form.feeInclusive,
                pctAgentCost = form.pctAgentCost,
                deductionBase = form.deductionBase,
                customerDiscount = CalculatorViewModel.parse(form.customerDiscount)
                    .takeIf { it.isFinite() && it >= 0.0 }
                    ?: Prefs.DEFAULT_CUSTOMER_DISCOUNT,
                downloadFromAmount = form.downloadFromAmount,
            )
            onSaved()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreenContent(
    form: SettingsFormState,
    onFormChange: (SettingsFormState) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pctFeeError = validationMessage(form.pctFee)
    val pctAgentError = validationMessage(form.pctAgentCost)
    val discountError = validationMessage(form.customerDiscount)

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

        // ---- Transfer defaults ----
        SectionHeader(stringResource(R.string.settings_section_transfer))

        Text(
            text = stringResource(R.string.settings_default_direction),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Four labels of the form "€ EUR ➔ $ USD" will not fit a segmented row on a phone.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransferDirection.entries.forEach { direction ->
                FilterChip(
                    selected = form.direction == direction,
                    onClick = { onFormChange(form.copy(direction = direction)) },
                    label = {
                        Text(
                            "${direction.base.symbol} ${direction.base.name} ➔ " +
                                "${direction.target.symbol} ${direction.target.name}"
                        )
                    },
                )
            }
        }

        Text(
            text = stringResource(R.string.settings_default_mode),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CalcMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = form.mode == mode,
                    onClick = { onFormChange(form.copy(mode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = CalcMode.entries.size,
                    ),
                ) {
                    Text(text = modeLabel(mode), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // ---- Customer fees ----
        SectionHeader(stringResource(R.string.settings_section_fees))

        ValidatedNumberField(
            value = form.pctFee,
            onValueChange = { onFormChange(form.copy(pctFee = it)) },
            label = stringResource(R.string.settings_default_pct_fee),
            errorText = pctFeeError,
        )
        LabeledSwitchRow(
            label = stringResource(R.string.settings_default_fee_inclusive),
            checked = form.feeInclusive,
            onCheckedChange = { onFormChange(form.copy(feeInclusive = it)) },
            supportingText = stringResource(R.string.fee_inclusive_hint),
        )

        // ---- Office costs ----
        SectionHeader(stringResource(R.string.settings_section_costs))

        ValidatedNumberField(
            value = form.pctAgentCost,
            onValueChange = { onFormChange(form.copy(pctAgentCost = it)) },
            label = stringResource(R.string.settings_default_pct_agent),
            errorText = pctAgentError,
        )

        Text(
            text = stringResource(R.string.settings_default_deduction),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DeductionBase.entries.forEachIndexed { index, base ->
                SegmentedButton(
                    selected = form.deductionBase == base,
                    onClick = { onFormChange(form.copy(deductionBase = base)) },
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

        ValidatedNumberField(
            value = form.customerDiscount,
            onValueChange = { onFormChange(form.copy(customerDiscount = it)) },
            label = stringResource(R.string.settings_default_customer_discount),
            errorText = discountError,
        )

        SectionHeader(stringResource(R.string.settings_section_download))

        LabeledSwitchRow(
            label = stringResource(R.string.settings_default_download_from_amount),
            checked = form.downloadFromAmount,
            onCheckedChange = { onFormChange(form.copy(downloadFromAmount = it)) },
            supportingText = stringResource(R.string.download_fee_from_amount_hint),
        )

        Button(
            onClick = onSave,
            enabled = pctFeeError == null && pctAgentError == null && discountError == null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    HorizontalDivider()
    Text(text = title, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun modeLabel(mode: CalcMode): String = stringResource(
    when (mode) {
        CalcMode.SEND_EXACT -> R.string.mode_send_exact
        CalcMode.RECEIVE_EXACT -> R.string.mode_receive_exact
        CalcMode.CUSTOM_DEAL -> R.string.mode_custom_deal
    }
)

@Preview(showBackground = true, locale = "ar")
@Composable
private fun SettingsScreenPreview() {
    TopalFXTheme {
        SettingsScreenContent(
            form = SettingsFormState(pctFee = "5", pctAgentCost = "2.5"),
            onFormChange = {},
            onSave = {},
        )
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
