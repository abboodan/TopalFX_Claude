package com.topaloglu.topalfx.ui.calculator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.data.Currency
import java.util.Locale

/** Number input that stays LTR inside RTL layouts and shows localized errors. */
@Composable
fun ValidatedNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    locked: Boolean = false,
    errorText: String? = null,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && !locked,
        singleLine = true,
        isError = errorText != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon ?: if (locked) {
            { Icon(Icons.Filled.Lock, contentDescription = null) }
        } else null,
        supportingText = when {
            errorText != null -> {
                { Text(errorText) }
            }
            supportingText != null -> {
                { Text(supportingText) }
            }
            else -> null
        },
    )
}

/** Circular currency marker shown on monetary fields so the active side is obvious. */
@Composable
fun CurrencyBadge(currency: Currency, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(start = 12.dp).size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

fun formatAmount(value: Double): String = String.format(Locale.US, "%,.2f", value)

fun formatRate(value: Double): String = String.format(Locale.US, "%.4f", value)
