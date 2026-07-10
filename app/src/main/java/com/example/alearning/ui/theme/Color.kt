package com.example.alearning.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors
val ElectricBlue = Color(0xFF0052FF) // Vibrant Primary
val DynamicOrange = Color(0xFFFF5E00) // Energetic Secondary
val AquaCyan = Color(0xFF00D1FF) // Lively Tertiary
val NavyVariant = Color(0xFF1B263B) // For dark cards if needed

// Legacy mappings for existing components
val NavyPrimary = ElectricBlue
val SportBlue = ElectricBlue
val VibrantBlue = ElectricBlue
val BlueAccent = AquaCyan
val SportOrange = DynamicOrange
val SportOrangeVariant = Color(0xFFE65100) 
val SportOrangeContainer = Color(0xFFFFF4EC)

// Neutral Colors
val SurfaceWhite = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF7F9FA) // Very clean, cool off-white
val TextPrimary = Color(0xFF111827) // Almost black
val TextSecondary = Color(0xFF6B7280) // Soft gray
val OutlineGrey = Color(0xFFE5E7EB)

// Performance zone colors — used across all screens consistently
// Rule: Green ≥ 60th percentile, Yellow 30–59th, Red < 30th, Grey = no data
val PerformanceGreen = Color(0xFFE8F5E9)
val PerformanceGreenText = Color(0xFF1B5E20)
val PerformanceGreenBorder = Color(0xFFA5D6A7)

val PerformanceYellow = Color(0xFFFFFDE7)
val PerformanceYellowText = Color(0xFFF57F17)
val PerformanceYellowBorder = Color(0xFFFFF59D)

val PerformanceRed = Color(0xFFFFEBEE)
val PerformanceRedText = Color(0xFFB71C1C)
val PerformanceRedBorder = Color(0xFFEF9A9A)

val PerformanceGrey = Color(0xFFFAFAFA)
val PerformanceGreyText = Color(0xFF757575)
val PerformanceGreyBorder = Color(0xFFEEEEEE)
