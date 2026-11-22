package com.inthehiggsfield.workerant.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.inthehiggsfield.workerant.R

/** Section headers in the Settings theme picker, in display order. */
enum class ThemeCategory(val label: String) {
    Basic("Basic colors"),
    CuteGirly("Cute & girly"),
    Fandom("Fandom-inspired"),
    Aesthetic("Aesthetics"),
    BoldRetro("Bold & retro"),
    Seasonal("Seasonal"),
}

/** Corner style for cards, buttons, and dialogs. */
enum class ShapeStyle { Default, Cut, Square, Rounded, Soft, Sharp }

/** Bundled fonts (res/font, all SIL Open Font License / Apache 2.0) plus Android's built-in families. */
enum class FontChoice {
    Default, Sans, Serif, Mono,
    Orbitron, Rye, Michroma, BebasNeue, Pacifico, Fredoka, Playfair, Righteous,
    Cinzel, Bangers, PressStart, Audiowide, Fraktur, SpecialElite, Creepster, Christmas,
}

/**
 * Everything a styled theme changes.
 * [display] is used for headlines/titles, [body] for paragraphs and labels.
 * [displayScale] shrinks or grows the display font (pixel fonts are huge, so they use ~0.6).
 */
class ThemeSpec(
    val dark: Boolean,
    val colors: ColorScheme,
    val display: FontChoice,
    val body: FontChoice = FontChoice.Default,
    val shapes: ShapeStyle = ShapeStyle.Default,
    val displayScale: Float = 1f,
)

/**
 * Every theme the user can pick. Basic themes (spec = null) only change colors and follow
 * the phone's light/dark mode; all others also change fonts and shapes and are fixed light or dark.
 *
 * Names describe the aesthetic rather than using brand/franchise names.
 * Add a theme by adding one entry here — Settings picks it up automatically.
 * Don't rename existing entries: the saved choice is stored by name.
 */
enum class AppTheme(
    val label: String,
    val tagline: String,
    val category: ThemeCategory,
    val spec: ThemeSpec? = null,
) {
    // ---------------- Basic ----------------
    Wallpaper("Match wallpaper", "Colors pulled from your wallpaper", ThemeCategory.Basic),
    Grape("Grape", "Classic Material purple", ThemeCategory.Basic),
    Forest("Forest", "Calm greens", ThemeCategory.Basic),
    Ocean("Ocean", "Deep blues", ThemeCategory.Basic),
    Sunset("Sunset", "Warm oranges", ThemeCategory.Basic),

    // ---------------- Cute & girly ----------------
    BubblegumGlam("Bubblegum Glam", "Hot-pink fashion-doll glam", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFFE0218A, 0xFFFF8CC6, 0xFF4FC3D9, 0xFFFFE6F2, 0xFFFFC2DF, 0xFF4A0D2E),
        FontChoice.Pacifico, FontChoice.Fredoka, ShapeStyle.Soft)),
    Coquette("Coquette", "Blush bows & lace", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFFB5566E, 0xFFE8A7B5, 0xFF8C6A5D, 0xFFFFF6F4, 0xFFF6DADB, 0xFF4A2A30),
        FontChoice.Playfair, FontChoice.Serif, ShapeStyle.Rounded)),
    KawaiiPastel("Kawaii Pastel", "Pastel rainbow cuteness", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFFD94590, 0xFF7EC8F2, 0xFF9BDB7A, 0xFFFFF8FC, 0xFFEFE3FF, 0xFF3D2C4E),
        FontChoice.Fredoka, FontChoice.Fredoka, ShapeStyle.Soft)),
    StrawberryMilk("Strawberry Milk", "Strawberry pink & cream", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFFE8577E, 0xFFF4A6B7, 0xFF7BC67E, 0xFFFFF1F3, 0xFFFFD9E0, 0xFF4D2730),
        FontChoice.Fredoka, FontChoice.Fredoka, ShapeStyle.Soft)),
    SakuraBloom("Sakura Bloom", "Cherry blossoms in spring", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFFD45D8A, 0xFF8FA87B, 0xFF7C6AA8, 0xFFFFF5F8, 0xFFF8DCE6, 0xFF3E2633),
        FontChoice.Playfair, FontChoice.Sans, ShapeStyle.Rounded)),
    Fairycore("Fairycore", "Lavender, moss & pearls", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFF8E7CC3, 0xFF8DB596, 0xFFD9A441, 0xFFF6F3FB, 0xFFE3DCF2, 0xFF2F2840),
        FontChoice.Playfair, FontChoice.Serif, ShapeStyle.Soft)),
    Mermaidcore("Mermaidcore", "Seafoam & iridescent scales", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFF0E9AA7, 0xFFA07CE0, 0xFFF59BD8, 0xFFE9FBF9, 0xFFC9EFEC, 0xFF0D3B40),
        FontChoice.Pacifico, FontChoice.Sans, ShapeStyle.Soft)),
    MagicalGirl("Magical Girl", "Sparkles, stars & transformations", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFFDB3A88, 0xFF3D7BEB, 0xFFFFC93C, 0xFFFFF2F8, 0xFFFFD6EA, 0xFF3A1430),
        FontChoice.Fredoka, FontChoice.Fredoka, ShapeStyle.Soft)),
    Y2KPop("Y2K Pop", "Butterfly clips & chrome lilac", ThemeCategory.CuteGirly, ThemeSpec(
        false, palette(false, 0xFF9B5DE5, 0xFF00A6E8, 0xFFF15BB5, 0xFFF3ECFF, 0xFFDCCFFF, 0xFF2A1A4A),
        FontChoice.Righteous, FontChoice.Sans, ShapeStyle.Soft)),
    PopDiva("Pop Diva", "Glitter, sequins & stadium lights", ThemeCategory.CuteGirly, ThemeSpec(
        true, palette(true, 0xFFD58CFF, 0xFFFFD166, 0xFF7BDFF2, 0xFF1C1028, 0xFF34224A, 0xFFF5EAFF),
        FontChoice.Playfair, FontChoice.Sans, ShapeStyle.Rounded)),

    // ---------------- Fandom-inspired ----------------
    WizardSchool("Wizard School", "Candlelit castle, scarlet & gold", ThemeCategory.Fandom, ThemeSpec(
        true, palette(true, 0xFFB23A48, 0xFFD4AF37, 0xFF3E7C59, 0xFF1C1416, 0xFF33262A, 0xFFF2E6D8),
        FontChoice.Cinzel, FontChoice.Serif, ShapeStyle.Square)),
    GalacticSaga("Galactic Saga", "Starships, lasers & hyperspace", ThemeCategory.Fandom, ThemeSpec(
        true, palette(true, 0xFF4FC3F7, 0xFFE53935, 0xFFFFD54F, 0xFF0A0E14, 0xFF1C2430, 0xFFE3EAF2),
        FontChoice.Orbitron, FontChoice.Sans, ShapeStyle.Cut)),
    ElvenRealm("Elven Realm", "Ancient forests & elven gold", ThemeCategory.Fandom, ThemeSpec(
        false, palette(false, 0xFF4F7A4A, 0xFFB08D3C, 0xFF6B7FA8, 0xFFF2EFE3, 0xFFDCD8C2, 0xFF2B2A20),
        FontChoice.Cinzel, FontChoice.Serif, ShapeStyle.Square)),
    ComicHero("Comic Hero", "POW! Bold primary colors", ThemeCategory.Fandom, ThemeSpec(
        false, palette(false, 0xFFE23636, 0xFF1F4FBF, 0xFFFFC300, 0xFFFFF8E1, 0xFFFFE9A8, 0xFF141414),
        FontChoice.Bangers, FontChoice.Sans, ShapeStyle.Sharp, displayScale = 1.1f)),
    NightVigilante("Night Vigilante", "Rooftops & signal lights", ThemeCategory.Fandom, ThemeSpec(
        true, palette(true, 0xFFFFD000, 0xFF5A6B7D, 0xFF8E8E8E, 0xFF0D0F12, 0xFF1F2329, 0xFFE6E6E6),
        FontChoice.BebasNeue, FontChoice.Sans, ShapeStyle.Sharp)),
    ShonenHero("Shonen Hero", "Anime battle energy", ThemeCategory.Fandom, ThemeSpec(
        true, palette(true, 0xFFFF7A00, 0xFF2F80ED, 0xFFF2C94C, 0xFF1A1A1A, 0xFF2E2E2E, 0xFFF5F5F5),
        FontChoice.Bangers, FontChoice.Sans, ShapeStyle.Sharp, displayScale = 1.1f)),
    IdolStage("Idol Stage", "Lightsticks & concert neon", ThemeCategory.Fandom, ThemeSpec(
        true, palette(true, 0xFFFF4FD8, 0xFF6EE7F9, 0xFFFFE14D, 0xFF140F24, 0xFF2A2140, 0xFFF3EEFF),
        FontChoice.Righteous, FontChoice.Sans, ShapeStyle.Soft)),
    BlockBuilder("Block Builder", "Grass blocks, dirt & diamonds", ThemeCategory.Fandom, ThemeSpec(
        false, palette(false, 0xFF5B8C32, 0xFF8B5A2B, 0xFF4FA3D9, 0xFFEAF2E1, 0xFFCFE0BC, 0xFF1F2A14),
        FontChoice.PressStart, FontChoice.Sans, ShapeStyle.Sharp, displayScale = 0.62f)),
    PirateAdventure("Pirate Adventure", "Treasure maps & the high seas", ThemeCategory.Fandom, ThemeSpec(
        true, palette(true, 0xFFC9A227, 0xFFB04A3A, 0xFF2E6F8E, 0xFF1B1611, 0xFF332A20, 0xFFEFE3C8),
        FontChoice.SpecialElite, FontChoice.Serif, ShapeStyle.Square)),

    // ---------------- Aesthetics ----------------
    DarkAcademia("Dark Academia", "Old libraries & oxblood leather", ThemeCategory.Aesthetic, ThemeSpec(
        true, palette(true, 0xFFB5624F, 0xFFB89B72, 0xFF7D8B6A, 0xFF1E1A16, 0xFF352E27, 0xFFE8DFD0),
        FontChoice.Playfair, FontChoice.Serif, ShapeStyle.Square)),
    LightAcademia("Light Academia", "Sunlit libraries & linen", ThemeCategory.Aesthetic, ThemeSpec(
        false, palette(false, 0xFF8A6D4B, 0xFF6F7F68, 0xFF9C6B5E, 0xFFF7F2E8, 0xFFE6DCC8, 0xFF33291E),
        FontChoice.Playfair, FontChoice.Serif, ShapeStyle.Square)),
    Cottagecore("Cottagecore", "Wildflowers & fresh bread", ThemeCategory.Aesthetic, ThemeSpec(
        false, palette(false, 0xFF6B8F5E, 0xFFD98E73, 0xFFC9A94D, 0xFFFAF6EC, 0xFFE8E3CF, 0xFF2E3326),
        FontChoice.Playfair, FontChoice.Serif, ShapeStyle.Soft)),
    Gothic("Gothic", "Black lace & cathedral red", ThemeCategory.Aesthetic, ThemeSpec(
        true, palette(true, 0xFFC0213C, 0xFF8E7AA0, 0xFF8C8C8C, 0xFF0E0B0D, 0xFF221B20, 0xFFE8DDE2),
        FontChoice.Fraktur, FontChoice.Serif, ShapeStyle.Square)),
    Witchy("Witchy", "Potions, herbs & moonlight", ThemeCategory.Aesthetic, ThemeSpec(
        true, palette(true, 0xFF9B6DD6, 0xFF6FA36B, 0xFFE0B84E, 0xFF16121C, 0xFF2A2333, 0xFFECE5F5),
        FontChoice.Cinzel, FontChoice.Serif, ShapeStyle.Rounded)),
    Celestial("Celestial", "Midnight sky & gold stars", ThemeCategory.Aesthetic, ThemeSpec(
        true, palette(true, 0xFFE3C16F, 0xFF8FA6D9, 0xFFC9A3E6, 0xFF0D1330, 0xFF1E2750, 0xFFEDEFFF),
        FontChoice.Cinzel, FontChoice.Sans, ShapeStyle.Rounded)),
    Steampunk("Steampunk", "Brass gears & copper pipes", ThemeCategory.Aesthetic, ThemeSpec(
        true, palette(true, 0xFFC08A3E, 0xFFB0704C, 0xFF5F8A8B, 0xFF1F1812, 0xFF3A2D22, 0xFFF0E2C8),
        FontChoice.SpecialElite, FontChoice.Serif, ShapeStyle.Square)),
    LofiCafe("Lo-fi Café", "Rainy-day coffee & beats", ThemeCategory.Aesthetic, ThemeSpec(
        false, palette(false, 0xFF8C6A55, 0xFFB5838D, 0xFF6D8B74, 0xFFF5EFE9, 0xFFE5D9CF, 0xFF3A2E27),
        FontChoice.Fredoka, FontChoice.Sans, ShapeStyle.Soft)),
    Matcha("Matcha", "Whisked green tea & oat milk", ThemeCategory.Aesthetic, ThemeSpec(
        false, palette(false, 0xFF5F8D4E, 0xFFA4BE7B, 0xFFC7A27C, 0xFFF6F8EF, 0xFFDFE8CF, 0xFF263021),
        FontChoice.Fredoka, FontChoice.Sans, ShapeStyle.Soft)),
    TropicalBeach("Tropical Beach", "Turquoise water & coral sunsets", ThemeCategory.Aesthetic, ThemeSpec(
        false, palette(false, 0xFF008787, 0xFFFF7F50, 0xFFF4C430, 0xFFF0FBFA, 0xFFCFF1EE, 0xFF0F3533),
        FontChoice.Pacifico, FontChoice.Sans, ShapeStyle.Soft)),
    Grunge("Grunge", "Flannel, fuzz & photocopies", ThemeCategory.Aesthetic, ThemeSpec(
        true, palette(true, 0xFF9AA562, 0xFFB86A50, 0xFF7C7C7C, 0xFF181816, 0xFF2C2C28, 0xFFDCD8CC),
        FontChoice.SpecialElite, FontChoice.Mono, ShapeStyle.Sharp)),
    Minimalist("Minimalist", "Black, white & nothing extra", ThemeCategory.Aesthetic, ThemeSpec(
        false, palette(false, 0xFF111111, 0xFF555555, 0xFF999999, 0xFFFFFFFF, 0xFFEDEDED, 0xFF111111),
        FontChoice.Sans, FontChoice.Sans, ShapeStyle.Sharp)),

    // ---------------- Bold & retro ----------------
    Cyberpunk("Cyberpunk", "Neon on night-city black", ThemeCategory.BoldRetro, ThemeSpec(
        true, CyberpunkColors, FontChoice.Orbitron, FontChoice.Mono, ShapeStyle.Cut)),
    OldWestern("Old Western", "Parchment, rust & saloon type", ThemeCategory.BoldRetro, ThemeSpec(
        false, WesternColors, FontChoice.Rye, FontChoice.Serif, ShapeStyle.Square)),
    Metallic("Metallic", "Brushed steel & chrome", ThemeCategory.BoldRetro, ThemeSpec(
        false, MetallicColors, FontChoice.Michroma, FontChoice.Sans, ShapeStyle.Rounded)),
    MuscleCar("Muscle Car", "Racing red & stripe yellow", ThemeCategory.BoldRetro, ThemeSpec(
        true, MuscleCarColors, FontChoice.BebasNeue, FontChoice.Sans, ShapeStyle.Sharp)),
    Synthwave("Synthwave", "Neon sunsets & chrome grids", ThemeCategory.BoldRetro, ThemeSpec(
        true, palette(true, 0xFFFF3CAC, 0xFFFF9A3C, 0xFF2BD2FF, 0xFF140A2E, 0xFF2A1B4D, 0xFFF7E9FF),
        FontChoice.Audiowide, FontChoice.Sans, ShapeStyle.Cut)),
    Vaporwave("Vaporwave", "Ａｅｓｔｈｅｔｉｃ pastels", ThemeCategory.BoldRetro, ThemeSpec(
        true, palette(true, 0xFFFF71CE, 0xFF01CDFE, 0xFFB967FF, 0xFF1E1A3A, 0xFF332C5C, 0xFFFFFBFF),
        FontChoice.Righteous, FontChoice.Mono, ShapeStyle.Square)),
    RetroArcade("Retro Arcade", "Insert coin to continue", ThemeCategory.BoldRetro, ThemeSpec(
        true, palette(true, 0xFFFFD600, 0xFF00E5FF, 0xFFFF1744, 0xFF000000, 0xFF1A1A1A, 0xFFFFFFFF),
        FontChoice.PressStart, FontChoice.Mono, ShapeStyle.Sharp, displayScale = 0.62f)),
    RetroDiner("Retro Diner", "Checkered floors & milkshakes", ThemeCategory.BoldRetro, ThemeSpec(
        false, palette(false, 0xFFD7263D, 0xFF2EC4B6, 0xFFF4D35E, 0xFFFFF9EC, 0xFFF1E4CC, 0xFF1F1F1F),
        FontChoice.Pacifico, FontChoice.Sans, ShapeStyle.Rounded)),

    // ---------------- Seasonal ----------------
    SpookySeason("Spooky Season", "Pumpkins, bats & candy corn", ThemeCategory.Seasonal, ThemeSpec(
        true, palette(true, 0xFFFF7518, 0xFFA45FC9, 0xFF7CFC00, 0xFF120E0A, 0xFF2A211A, 0xFFF5E9DC),
        FontChoice.Creepster, FontChoice.Sans, ShapeStyle.Rounded, displayScale = 1.05f)),
    HolidayCheer("Holiday Cheer", "Candy canes & evergreen", ThemeCategory.Seasonal, ThemeSpec(
        false, palette(false, 0xFFC62828, 0xFF2E7D32, 0xFFD4AF37, 0xFFFDFBF7, 0xFFEFE7DC, 0xFF2A1A1A),
        FontChoice.Christmas, FontChoice.Serif, ShapeStyle.Rounded)),
    SnowDay("Snow Day", "Fresh snow & icy blues", ThemeCategory.Seasonal, ThemeSpec(
        false, palette(false, 0xFF3D7EA6, 0xFF7E9DB8, 0xFF9B92CC, 0xFFF4F8FC, 0xFFDCE7F0, 0xFF1C2833),
        FontChoice.Playfair, FontChoice.Sans, ShapeStyle.Soft)),
    Sweetheart("Sweetheart", "Hearts, roses & love notes", ThemeCategory.Seasonal, ThemeSpec(
        false, palette(false, 0xFFD81B60, 0xFFF48FB1, 0xFFB71C1C, 0xFFFFF0F4, 0xFFFFD3DF, 0xFF4A1024),
        FontChoice.Pacifico, FontChoice.Sans, ShapeStyle.Soft)),
    SpringGarden("Spring Garden", "Tulips & new growth", ThemeCategory.Seasonal, ThemeSpec(
        false, palette(false, 0xFF4A8A45, 0xFFE57A9E, 0xFFE0B83A, 0xFFF7FBF2, 0xFFE0EED6, 0xFF223020),
        FontChoice.Fredoka, FontChoice.Sans, ShapeStyle.Soft)),
    AutumnHarvest("Autumn Harvest", "Falling leaves & pumpkin spice", ThemeCategory.Seasonal, ThemeSpec(
        false, palette(false, 0xFFC0582B, 0xFFC9952F, 0xFF7A5C3E, 0xFFFBF3E6, 0xFFEFDDC2, 0xFF3A2414),
        FontChoice.Playfair, FontChoice.Serif, ShapeStyle.Rounded));

    /** true/false = always dark/light; null = follow the phone's setting. */
    val forcedDark: Boolean? get() = spec?.dark
}

// =====================================================================
// Colors
// =====================================================================

@Composable
fun AppTheme.colorScheme(systemDark: Boolean): ColorScheme {
    spec?.let { return it.colors }
    return when (this) {
        AppTheme.Wallpaper ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (systemDark) GrapeDark else GrapeLight
        AppTheme.Forest -> if (systemDark) ForestDark else ForestLight
        AppTheme.Ocean -> if (systemDark) OceanDark else OceanLight
        AppTheme.Sunset -> if (systemDark) SunsetDark else SunsetLight
        else -> if (systemDark) GrapeDark else GrapeLight
    }
}

/**
 * Builds a full Material color scheme from a handful of colors, deriving the rest
 * (containers, outlines, surface tones) by blending. Colors are 0xAARRGGBB.
 */
private fun palette(
    dark: Boolean,
    primary: Long,
    secondary: Long,
    tertiary: Long,
    background: Long,
    surfaceVariant: Long,
    onBackground: Long,
): ColorScheme {
    val p = Color(primary)
    val s = Color(secondary)
    val t = Color(tertiary)
    val bg = Color(background)
    val sv = Color(surfaceVariant)
    val onBg = Color(onBackground)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p, onPrimary = contentOn(p),
        primaryContainer = lerp(bg, p, if (dark) 0.35f else 0.25f),
        onPrimaryContainer = if (dark) lerp(p, Color.White, 0.6f) else lerp(p, Color.Black, 0.55f),
        inversePrimary = lerp(p, if (dark) Color.Black else Color.White, 0.4f),
        secondary = s, onSecondary = contentOn(s),
        secondaryContainer = lerp(bg, s, 0.3f), onSecondaryContainer = onBg,
        tertiary = t, onTertiary = contentOn(t),
        tertiaryContainer = lerp(bg, t, 0.3f), onTertiaryContainer = onBg,
        background = bg, onBackground = onBg,
        surface = lerp(bg, sv, 0.2f), onSurface = onBg,
        surfaceVariant = sv, onSurfaceVariant = lerp(onBg, sv, 0.3f),
        surfaceTint = p,
        inverseSurface = onBg, inverseOnSurface = bg,
        outline = lerp(onBg, bg, 0.5f), outlineVariant = lerp(onBg, bg, 0.78f),
        surfaceContainerLowest = bg,
        surfaceContainerLow = lerp(bg, sv, 0.15f),
        surfaceContainer = lerp(bg, sv, 0.35f),
        surfaceContainerHigh = lerp(bg, sv, 0.55f),
        surfaceContainerHighest = lerp(bg, sv, 0.75f),
    )
}

/** Picks near-black or white text, whichever has more contrast against [c]. */
private fun contentOn(c: Color): Color {
    val l = c.luminance()
    val darkContrast = (l + 0.05f) / (0.0103f + 0.05f)   // vs #1A1A1A
    val lightContrast = 1.05f / (l + 0.05f)               // vs white
    return if (darkContrast >= lightContrast) Color(0xFF1A1A1A) else Color.White
}

private val GrapeLight = lightColorScheme(
    primary = Color(0xFF6750A4), primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71), tertiary = Color(0xFF7D5260),
)
private val GrapeDark = darkColorScheme(
    primary = Color(0xFFD0BCFF), primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8),
)
private val ForestLight = lightColorScheme(
    primary = Color(0xFF2E6B30), primaryContainer = Color(0xFFAFF3A9), onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF52634F), tertiary = Color(0xFF38656A),
)
private val ForestDark = darkColorScheme(
    primary = Color(0xFF94D78F), primaryContainer = Color(0xFF13521B), onPrimaryContainer = Color(0xFFAFF3A9),
    secondary = Color(0xFFB9CCB4), tertiary = Color(0xFFA0CFD4),
)
private val OceanLight = lightColorScheme(
    primary = Color(0xFF00629D), primaryContainer = Color(0xFFCFE5FF), onPrimaryContainer = Color(0xFF001D34),
    secondary = Color(0xFF50606E), tertiary = Color(0xFF67587A),
)
private val OceanDark = darkColorScheme(
    primary = Color(0xFF9CCBFF), primaryContainer = Color(0xFF004A78), onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Color(0xFFB8C8D9), tertiary = Color(0xFFD3BFE6),
)
private val SunsetLight = lightColorScheme(
    primary = Color(0xFF9C4400), primaryContainer = Color(0xFFFFDBC9), onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF765848), tertiary = Color(0xFF636032),
)
private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB68C), primaryContainer = Color(0xFF773300), onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFE6BEAB), tertiary = Color(0xFFCDC991),
)

private val CyberpunkColors = darkColorScheme(
    primary = Color(0xFFFF2A6D), onPrimary = Color(0xFF0B0B14),
    primaryContainer = Color(0xFF3A0F24), onPrimaryContainer = Color(0xFFFF8AB0),
    secondary = Color(0xFF05D9E8), onSecondary = Color(0xFF00151A),
    tertiary = Color(0xFFF9F002), onTertiary = Color(0xFF1A1900),
    background = Color(0xFF0B0B14), onBackground = Color(0xFFE8E8FF),
    surface = Color(0xFF12121E), onSurface = Color(0xFFE8E8FF),
    surfaceVariant = Color(0xFF1E1E30), onSurfaceVariant = Color(0xFFA0A0C8),
    surfaceContainer = Color(0xFF161626),
    outline = Color(0xFF3D3D5C), outlineVariant = Color(0xFF2A2A40),
    error = Color(0xFFFF5370), errorContainer = Color(0xFF3A1018),
)
private val WesternColors = lightColorScheme(
    primary = Color(0xFF8B3A1A), onPrimary = Color(0xFFFFF3E0),
    primaryContainer = Color(0xFFE7B98F), onPrimaryContainer = Color(0xFF3B1606),
    secondary = Color(0xFF6B5B2E), tertiary = Color(0xFF2F4F4F),
    background = Color(0xFFF3E5C8), onBackground = Color(0xFF3B2416),
    surface = Color(0xFFF7ECD6), onSurface = Color(0xFF3B2416),
    surfaceVariant = Color(0xFFE6D2AA), onSurfaceVariant = Color(0xFF6B4A32),
    surfaceContainer = Color(0xFFEBDAB8),
    outline = Color(0xFF9C7B55), outlineVariant = Color(0xFFCDB38A),
    errorContainer = Color(0xFFF2C1A8),
)
private val MetallicColors = lightColorScheme(
    primary = Color(0xFF3A4A5C), onPrimary = Color.White,
    primaryContainer = Color(0xFFB9C6D4), onPrimaryContainer = Color(0xFF0F1C29),
    secondary = Color(0xFF7A848F), tertiary = Color(0xFFB08D57),
    background = Color(0xFFE4E7EB), onBackground = Color(0xFF1C2127),
    surface = Color(0xFFEEF0F3), onSurface = Color(0xFF1C2127),
    surfaceVariant = Color(0xFFCDD2D8), onSurfaceVariant = Color(0xFF4A525C),
    surfaceContainer = Color(0xFFD9DDE2),
    outline = Color(0xFF8A939D), outlineVariant = Color(0xFFB7BEC6),
)
private val MuscleCarColors = darkColorScheme(
    primary = Color(0xFFD62828), onPrimary = Color.White,
    primaryContainer = Color(0xFF5A0F0F), onPrimaryContainer = Color(0xFFFFB3B3),
    secondary = Color(0xFFF7B32B), onSecondary = Color(0xFF111111),
    tertiary = Color(0xFFBDBDBD),
    background = Color(0xFF111111), onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1A1A1A), onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF2A2A2A), onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceContainer = Color(0xFF1F1F1F),
    outline = Color(0xFF555555), outlineVariant = Color(0xFF333333),
)

// =====================================================================
// Fonts
// =====================================================================

private fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.Default -> FontFamily.Default
    FontChoice.Sans -> FontFamily.SansSerif
    FontChoice.Serif -> FontFamily.Serif
    FontChoice.Mono -> FontFamily.Monospace
    FontChoice.Orbitron -> FontFamily(Font(R.font.orbitron))
    FontChoice.Rye -> FontFamily(Font(R.font.rye))
    FontChoice.Michroma -> FontFamily(Font(R.font.michroma))
    FontChoice.BebasNeue -> FontFamily(Font(R.font.bebas_neue))
    FontChoice.Pacifico -> FontFamily(Font(R.font.pacifico))
    FontChoice.Fredoka -> FontFamily(Font(R.font.fredoka))
    FontChoice.Playfair -> FontFamily(Font(R.font.playfair_display))
    FontChoice.Righteous -> FontFamily(Font(R.font.righteous))
    FontChoice.Cinzel -> FontFamily(Font(R.font.cinzel))
    FontChoice.Bangers -> FontFamily(Font(R.font.bangers))
    FontChoice.PressStart -> FontFamily(Font(R.font.press_start_2p))
    FontChoice.Audiowide -> FontFamily(Font(R.font.audiowide))
    FontChoice.Fraktur -> FontFamily(Font(R.font.unifraktur_maguntia))
    FontChoice.SpecialElite -> FontFamily(Font(R.font.special_elite))
    FontChoice.Creepster -> FontFamily(Font(R.font.creepster))
    FontChoice.Christmas -> FontFamily(Font(R.font.mountains_of_christmas))
}

fun AppTheme.typography(): Typography {
    val s = spec ?: return Typography
    return themedTypography(s.display.family(), s.body.family(), s.displayScale)
}

/** Headlines/titles get the theme's display font; body text and labels get a readable font. */
private fun themedTypography(display: FontFamily, body: FontFamily, scale: Float): Typography {
    val b = Typography()
    fun TextStyle.d() = copy(fontFamily = display, fontSize = fontSize * scale, lineHeight = lineHeight * scale)
    fun TextStyle.t() = copy(fontFamily = body)
    return Typography(
        displayLarge = b.displayLarge.d(), displayMedium = b.displayMedium.d(), displaySmall = b.displaySmall.d(),
        headlineLarge = b.headlineLarge.d(), headlineMedium = b.headlineMedium.d(), headlineSmall = b.headlineSmall.d(),
        titleLarge = b.titleLarge.d(), titleMedium = b.titleMedium.d(), titleSmall = b.titleSmall.d(),
        bodyLarge = b.bodyLarge.t(), bodyMedium = b.bodyMedium.t(), bodySmall = b.bodySmall.t(),
        labelLarge = b.labelLarge.t(), labelMedium = b.labelMedium.t(), labelSmall = b.labelSmall.t(),
    )
}

// =====================================================================
// Shapes
// =====================================================================

fun AppTheme.shapes(): Shapes = when (spec?.shapes ?: ShapeStyle.Default) {
    ShapeStyle.Default -> Shapes()
    ShapeStyle.Cut -> Shapes(  // angled sci-fi corners
        extraSmall = CutCornerShape(4.dp), small = CutCornerShape(6.dp), medium = CutCornerShape(10.dp),
        large = CutCornerShape(14.dp), extraLarge = CutCornerShape(20.dp),
    )
    ShapeStyle.Square -> Shapes(  // nearly square, like printed signs and book covers
        extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(2.dp), medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(6.dp), extraLarge = RoundedCornerShape(8.dp),
    )
    ShapeStyle.Rounded -> Shapes(  // smooth curves
        extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp),
    )
    ShapeStyle.Soft -> Shapes(  // extra round and bubbly
        extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(22.dp),
        large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp),
    )
    ShapeStyle.Sharp -> Shapes(  // hard edges
        extraSmall = RoundedCornerShape(0.dp), small = RoundedCornerShape(2.dp), medium = RoundedCornerShape(2.dp),
        large = RoundedCornerShape(4.dp), extraLarge = RoundedCornerShape(4.dp),
    )
}

// =====================================================================
// Saving the choice
// =====================================================================

object ThemePrefs {
    private fun prefs(context: Context) = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    fun load(context: Context): AppTheme {
        val name = prefs(context).getString("theme", null) ?: return AppTheme.Wallpaper
        return AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.Wallpaper
    }

    fun save(context: Context, theme: AppTheme) {
        prefs(context).edit { putString("theme", theme.name) }
    }
}
