package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ═════════════════════════════════════════════════════════════════════════════
//  SMART WORKER — dark-first design tokens
//
//  Blended from three references:
//    • bold fitness stats     → orange hero + electric blue, oversized numerals
//    • colour-coded task cards → lime / sun / coral blocks on near-black
//    • bento tile dashboard    → mixed tile sizes, giant numbers, calm accents
//
//  Two rules hold the system together:
//    1. The SURFACE is dark and quiet. COLOUR carries the meaning.
//    2. Text on a bright tile is always ink, never white. Text on a dark
//       surface is OnDark. Never put OnDark on Lime/Sun/Cream.
// ═════════════════════════════════════════════════════════════════════════════

// ── Surfaces ─────────────────────────────────────────────────────────────────
/** App canvas — everything sits on this. */
val Ink = Color(0xFF0B0B0D)
/** Standard card. */
val Surface1 = Color(0xFF17181C)
/** Raised / pressed card, bottom sheets. */
val Surface2 = Color(0xFF212329)
/** Chips, inputs, progress tracks. */
val Surface3 = Color(0xFF2B2E36)
/** 1dp borders and dividers. */
val Hairline = Color(0xFF32353F)
/** Dimmer behind dialogs and sheets. */
val Scrim = Color(0xCC000000)

// ── Text ─────────────────────────────────────────────────────────────────────
/** Primary text on a dark surface. */
val OnDark = Color(0xFFF6F6F8)
/** Body text on a dark surface. */
val OnDarkBody = Color(0xFFC9CCD6)
/** Labels, captions, metadata. */
val OnDarkMuted = Color(0xFF9CA0AD)
/** Disabled / tertiary. */
val OnDarkFaint = Color(0xFF6B6F7C)
/** Text on Lime / Sun / Coral / Cream / white tiles. */
val OnBright = Color(0xFF0B0B0D)

// ── Brand accents ────────────────────────────────────────────────────────────
/** Hero. Primary actions, the one stat that matters most on a screen. */
val Flame = Color(0xFFFF4A1C)
val FlameDeep = Color(0xFFD63208)
/** Secondary stat colour — pairs with Flame without competing. */
val Volt = Color(0xFF2F5BF0)
/** Positive, completed, present. */
val Lime = Color(0xFFC8F23C)
/** Attention, in progress, late. */
val Sun = Color(0xFFFFC93C)
/** Urgent, negative, absent. */
val Coral = Color(0xFFFF5F52)
/** Calm informational tile. */
val Mint = Color(0xFFB8E6DC)
/** Deep accent tile. */
val Forest = Color(0xFF0F4D32)
/** Light tile — the "paper" in a bento grid. */
val Cream = Color(0xFFF7F3EA)
val White = Color(0xFFFFFFFF)

// ── Status (semantic) ────────────────────────────────────────────────────────
val Success = Lime
val Warning = Sun
val Danger = Coral
val Purple = Color(0xFFA78BFA)

/** Status backgrounds — the accent sunk into the canvas, not a pale tint. */
val SuccessBg = Color(0xFF1E2A12)
val WarningBg = Color(0xFF2E2512)
val DangerBg = Color(0xFF2E1816)
val PurpleBg = Color(0xFF1F1B2E)
val FlameBg = Color(0xFF2B1812)
val VoltBg = Color(0xFF141C33)

// ── Icon / avatar wells ──────────────────────────────────────────────────────
val AvatarBlueBg = Color(0xFF16203F)
val IconBlueBg = Color(0xFF141A2E)
val IconGreenBg = Color(0xFF13241A)
val IconRedBg = Color(0xFF2A1513)
val IconOrangeBg = Color(0xFF2B1A12)
val IconPurpleBg = Color(0xFF1D1830)

// ═════════════════════════════════════════════════════════════════════════════
//  Compatibility aliases
//
//  The screens still refer to the old light-theme names in ~600 places. Mapping
//  them here flips the whole app to the dark system in one move and keeps every
//  call site compiling. Screens are being migrated to the names above screen by
//  screen; prefer the tokens above in new code.
//
//  NOTE: `PrimaryBlue` now resolves to the orange hero and `Navy` to near-white
//  text. The names are historical — trust the value, not the word.
// ═════════════════════════════════════════════════════════════════════════════
val PrimaryBlue = Flame
val PrimaryBlueDark = FlameDeep
val DarkBlue = Color(0xFF121318)
val Navy = OnDark
val TextPrimary = OnDark
val TextBody = OnDarkBody
val TextSecondary = OnDarkMuted

val BackgroundColor = Ink
val CardBackground = Surface1
val DividerColor = Hairline
val SubtleDivider = Color(0xFF24262D)
val SelectedCardBackground = FlameBg
val SecurityBannerBackground = VoltBg
