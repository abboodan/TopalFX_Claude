package com.topaloglu.topalfx.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.CalcError
import com.topaloglu.topalfx.data.CashDownloadResult
import com.topaloglu.topalfx.data.Currency
import com.topaloglu.topalfx.ui.calculator.CurrencyBadge
import com.topaloglu.topalfx.ui.calculator.ValidatedNumberField
import com.topaloglu.topalfx.ui.calculator.formatAmount
import com.topaloglu.topalfx.viewmodel.CalculatorViewModel
import com.topaloglu.topalfx.viewmodel.CashDownloadUiState
import com.topaloglu.topalfx.viewmodel.CashDownloadViewModel
import com.topaloglu.topalfx.viewmodel.DownloadField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * إعادة تنزيل مبلغ — how much cash to hand over so a wanted balance reaches the wallet.
 */
@Composable
fun CashDownloadScreen(
    state: CashDownloadUiState,
    viewModel: CashDownloadViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.download_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.download_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Currency.entries.forEachIndexed { index, currency ->
                SegmentedButton(
                    selected = state.currency == currency,
                    onClick = { viewModel.onCurrencyChanged(currency) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = Currency.entries.size,
                    ),
                ) {
                    Text("${currency.symbol} ${currency.name}")
                }
            }
        }

        DownloadNumberField(
            state, viewModel, DownloadField.DESIRED_DIGITAL,
            R.string.download_desired_digital,
            currency = state.currency,
        )
        DownloadNumberField(
            state, viewModel, DownloadField.PCT_RATE,
            R.string.download_pct_rate,
        )

        if (state.result?.error == CalcError.FEE_OVERFLOW) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.download_rate_overflow),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        state.result?.takeIf { it.error == null }?.let { result ->
            DownloadResultsCard(
                result = result,
                currency = state.currency,
                lastCalculatedAt = state.lastCalculatedAt,
            )
        }
    }
}

@Composable
private fun DownloadResultsCard(
    result: CashDownloadResult,
    currency: Currency,
    lastCalculatedAt: Long,
    modifier: Modifier = Modifier,
) {
    val code = currency.name
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.results_title),
                style = MaterialTheme.typography.titleMedium,
            )

            DownloadRow(
                stringResource(R.string.download_result_digital),
                result.desiredDigital,
                code,
            )
            DownloadRow(stringResource(R.string.download_result_cost), result.cost, code)

            HorizontalDivider()
            // The headline figure: this is the number counted out at the counter.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.download_result_cash),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${formatAmount(result.cashRequired)} $code",
                    style = MaterialTheme.typography.titleMedium
                        .copy(textDirection = TextDirection.Ltr),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (lastCalculatedAt > 0L) {
                Text(
                    text = stringResource(
                        R.string.result_last_calculated,
                        TIME_FORMAT.format(Date(lastCalculatedAt)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DownloadRow(label: String, value: Double, currencyCode: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${formatAmount(value)} $currencyCode",
            style = MaterialTheme.typography.bodyMedium
                .copy(textDirection = TextDirection.Ltr),
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun DownloadNumberField(
    state: CashDownloadUiState,
    viewModel: CashDownloadViewModel,
    field: DownloadField,
    labelRes: Int,
    currency: Currency? = null,
) {
    val raw = state.field(field)
    val parsed = CalculatorViewModel.parse(raw)
    val fieldError = when {
        raw.isBlank() -> null
        parsed.isNaN() -> stringResource(R.string.error_invalid_number)
        parsed < 0.0 -> stringResource(R.string.error_negative_value)
        else -> null
    }
    ValidatedNumberField(
        value = raw,
        onValueChange = { viewModel.onFieldChanged(field, it) },
        label = stringResource(labelRes),
        errorText = fieldError,
        leadingIcon = currency?.let { { CurrencyBadge(it) } },
    )
}

private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US)
