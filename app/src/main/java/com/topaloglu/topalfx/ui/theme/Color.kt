package com.topaloglu.topalfx.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Shield T palette — navy for trust, gold for value.
 *
 * The two brand colours are [Navy] and [Gold]; everything else is a tint or shade of
 * them, so the app reads as one identity with the launcher icon.
 */

// Brand
val Navy = Color(0xFF0B2545)
val NavyLift = Color(0xFF12325C)
val Gold = Color(0xFFC9A227)

// Light scheme
val NavyContainerLight = Color(0xFFD8E3F5)
val OnNavyContainerLight = Color(0xFF071A33)
val GoldDeep = Color(0xFF7D6210)
val GoldContainerLight = Color(0xFFF7EAC4)
val OnGoldContainerLight = Color(0xFF3D2F00)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE3E8F0)
val BackgroundLight = Color(0xFFF6F8FC)
val OnSurfaceLight = Color(0xFF11151B)
val OnSurfaceVariantLight = Color(0xFF4A5462)
val OutlineLight = Color(0xFF7B8697)

// Dark scheme
val NavyTint = Color(0xFFA9C7F0)
val NavyContainerDark = Color(0xFF16324F)
val OnNavyContainerDark = Color(0xFFD8E3F5)
val GoldTint = Color(0xFFE3C35C)
val GoldContainerDark = Color(0xFF5E4A00)
val OnGoldContainerDark = Color(0xFFF7EAC4)
val SurfaceDark = Color(0xFF131A22)
val SurfaceVariantDark = Color(0xFF1E2833)
val BackgroundDark = Color(0xFF0C1117)
val OnSurfaceDark = Color(0xFFE7ECF3)
val OnSurfaceVariantDark = Color(0xFFA5B0BF)
val OutlineDark = Color(0xFF6C7787)

// Semantic — kept apart from the brand hues so they never read as decoration
val ErrorLight = Color(0xFFB3261E)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
val ErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

/** Profit figures. Deliberately outside the brand palette — this is state, not identity. */
val ProfitLight = Color(0xFF1B6B3A)
val ProfitDark = Color(0xFF6FD394)
