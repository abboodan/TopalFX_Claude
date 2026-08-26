package com.topaloglu.topalfx.ui.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.CalcResult
import com.topaloglu.topalfx.data.Currency
import com.topaloglu.topalfx.data.TransferDirection
import com.topaloglu.topalfx.ui.theme.ProfitDark
import com.topaloglu.topalfx.ui.theme.ProfitLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultsCard(
    result: CalcResult,
    direction: TransferDirection,
    mode: CalcMode,
    lastCalculatedAt: Long,
    modifier: Modifier = Modifier,
) {
    val baseCode = direction.base.name
    val targetCode = direction.target.name
    // The office fund is accounted in EUR; profit rows convert when the base is USD.
    val toEur = result.eurConversionRate
    val profitCode = if (toEur != null) Currency.EUR.name else baseCode
    fun profit(value: Double): Double = if (toEur != null) value * toEur else value

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.results_title),
                style = MaterialTheme.typography.titleMedium,
            )

            MoneyFlow(result = result, baseCode = baseCode, targetCode = targetCode)

            HorizontalDivider()
            Text(
                text = stringResource(R.string.result_profit_section, profitCode),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (mode != CalcMode.CUSTOM_DEAL) {
                    ResultRow(
                        stringResource(R.string.result_pct_fee),
                        profit(result.customerFeeBase),
                        profitCode,
                        Sign.REVENUE,
                    )
                    ResultRow(
                        stringResource(R.string.result_delivery_fee_base),
                        profit(result.deliveryFeeBase),
                        profitCode,
                        Sign.COST,
                    )
                }
                ResultRow(
                    stringResource(R.string.result_hidden_spread),
                    profit(result.hiddenSpread),
                    profitCode,
                    Sign.REVENUE,
                )
                ResultRow(
                    stringResource(R.string.result_agent_pct),
                    profit(result.agentCostBase),
                    profitCode,
                    Sign.COST,
                )
            }

            if (toEur == null) {
                Text(
                    text = stringResource(R.string.result_eur_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider()
            val netProfit = result.netProfitEur ?: result.netProfitBase
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.result_net_profit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${formatAmount(netProfit)} $profitCode",
                    // Forced LTR, or a losing figure renders its minus sign on the wrong
                    // side in an Arabic paragraph.
                    style = MaterialTheme.typography.titleMedium
                        .copy(textDirection = TextDirection.Ltr),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (netProfit >= 0.0) profitGreen() else MaterialTheme.colorScheme.error,
                )
            }
            // Keep the base-currency figure visible when it differs from the EUR total.
            if (toEur != null && direction.base != Currency.EUR) {
                Text(
                    text = stringResource(
                        R.string.result_net_profit_base,
                        formatAmount(result.netProfitBase),
                        baseCode,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // The red figure above says how much is being lost; this says what to do
            // about it, which is the number needed to re-quote the customer on the spot.
            if (netProfit < 0.0) {
                NegativeProfitWarning(result.breakEvenPctFee)
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

/**
 * The three amounts that matter, laid out as the journey the money actually takes:
 * cash in ➔ what is left to transfer ➔ what the beneficiary collects.
 *
 * Each step carries its own colour and icon so the counter number and the payout number
 * are told apart at a glance, without reading the labels.
 */
@Composable
private fun MoneyFlow(
    result: CalcResult,
    baseCode: String,
    targetCode: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowStep(
            icon = Icons.AutoMirrored.Filled.CallReceived,
            label = stringResource(R.string.result_cash_received),
            value = result.cashReceived,
            currencyCode = baseCode,
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        FlowArrow()
        // Outlined rather than filled: the middle step is a way-station, not an endpoint,
        // and separating it by SHAPE keeps the two filled endpoints unmistakable even
        // where two tints would sit too close together.
        FlowStep(
            icon = Icons.Filled.SwapHoriz,
            label = stringResource(R.string.result_transfer_amount),
            value = result.transferAmount,
            currencyCode = baseCode,
            container = Color.Transparent,
            onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.weight(1f),
        )
        FlowArrow()
        FlowStep(
            icon = Icons.AutoMirrored.Filled.CallMade,
            label = stringResource(R.string.result_target_delivered),
            value = result.targetDelivered,
            currencyCode = targetCode,
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FlowStep(
    icon: ImageVector,
    label: String,
    value: Double,
    currencyCode: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    Surface(
        modifier = modifier,
        color = container,
        contentColor = onContainer,
        shape = MaterialTheme.shapes.small,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = formatAmount(value),
                style = MaterialTheme.typography.titleSmall
                    .copy(textDirection = TextDirection.Ltr),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
            Text(
                text = currencyCode,
                style = MaterialTheme.typography.labelSmall
                    .copy(textDirection = TextDirection.Ltr),
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/** AutoMirrored so the flow runs right-to-left in Arabic. */
@Composable
private fun FlowArrow() {
    Icon(
        Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.outline,
    )
}

/** Whether a breakdown line adds to the profit or eats it. */
private enum class Sign { REVENUE, COST }

@Composable
private fun NegativeProfitWarning(breakEvenPctFee: Double?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null)
            Text(
                text = if (breakEvenPctFee != null && breakEvenPctFee.isFinite()) {
                    stringResource(R.string.result_break_even, formatPercent(breakEvenPctFee))
                } else {
                    stringResource(R.string.result_negative_profit)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ResultRow(label: String, value: Double, currencyCode: String, sign: Sign) {
    val tint = when (sign) {
        Sign.REVENUE -> profitGreen()
        Sign.COST -> MaterialTheme.colorScheme.error
    }
    // The sign is what makes the breakdown scannable: everything green is coming in,
    // everything red is going out, and the bold total at the bottom is the difference.
    val prefix = if (sign == Sign.REVENUE) "+" else "−"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "$prefix ${formatAmount(value)} $currencyCode",
            style = MaterialTheme.typography.bodyMedium
                .copy(textDirection = TextDirection.Ltr),
            fontFamily = FontFamily.Monospace,
            color = tint,
        )
    }
}

@Composable
private fun profitGreen(): Color = if (isSystemInDarkTheme()) ProfitDark else ProfitLight

private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US)
