package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The reference decks lean on one typographic trick: an oversized number paired
 * with a small, wide, uppercase label. [SwType] holds that pairing plus the few
 * other roles the design system actually uses, so screens stop hand-rolling
 * fontSize/fontWeight on every Text.
 */
object SwType {
  /** The one number a screen is about — "1,213", "62", "8k+". */
  val StatHuge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    letterSpacing = (-1.4).sp,
  )

  /** Secondary stats inside a tile grid. */
  val StatLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.8).sp,
  )

  /** Inline figures — a row's amount, a percentage on a bar. */
  val StatSmall = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.3).sp,
  )

  /** The small wide caption that sits under or above a stat. */
  val Label = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 10.5.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.9.sp,
  )

  /** Screen and section headings. */
  val Headline = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 29.sp,
    letterSpacing = (-0.5).sp,
  )

  /** Card titles. */
  val Title = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 21.sp,
    letterSpacing = (-0.1).sp,
  )

  /** Row titles, list items. */
  val Body = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  )

  /** Metadata under a row title. */
  val Caption = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  )
}

val Typography =
  Typography(
    headlineLarge = SwType.Headline,
    headlineMedium = SwType.Headline.copy(fontSize = 20.sp, lineHeight = 25.sp),
    titleLarge = SwType.Title.copy(fontSize = 18.sp, lineHeight = 23.sp),
    titleMedium = SwType.Title,
    bodyLarge = SwType.Body.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = SwType.Body,
    bodySmall = SwType.Caption,
    labelSmall = SwType.Label,
  )
