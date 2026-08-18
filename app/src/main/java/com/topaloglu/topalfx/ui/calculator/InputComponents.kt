package com.topaloglu.topalfx.ui.calculator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
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
        trailingIcon = if (locked) {
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

fun formatAmount(value: Double): String = String.format(Locale.US, "%,.2f", value)

fun formatRate(value: Double): String = String.format(Locale.US, "%.4f", value)
