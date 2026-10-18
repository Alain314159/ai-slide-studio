package com.aislide.studio.data.model

import kotlinx.serialization.Serializable

/**
 * Predefined presentation templates — inspired by Canva's template gallery
 * but using open-source color palettes and font pairings.
 *
 * Sources for palettes and typography:
 * - Color palettes: coolors.co (CC0) and LOL Colors public domain
 * - Font pairings: Google Fonts (OFL) — Poppins+Inter, Playfair+Source Sans, etc.
 * - Layout inspirations: reveal.js open-source themes (MIT)
 *
 * Each template defines:
 * - palette: 7 hex colors (primary, secondary, accent, bg, surface, on variants)
 * - typography: heading + body font pair from Google Fonts
 * - defaultLayouts: suggested layout per slide position (the AI fills content)
 * - promptHint: extra instructions for the LLM to match the template's vibe
 *
 * Templates are pure data — no rendering, no external downloads. The AI
 * generates content matching the template's vibe, then the renderer applies
 * the palette and typography.
 */
@Serializable
data class PresentationTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val description: String,
    val palette: Palette,
    val typography: Typography,
    val promptHint: String,
    val previewEmoji: String  // for visual variety in the picker
)

enum class TemplateCategory {
    EDUCATIVO,
    CORPORATIVO,
    CREATIVO,
    MINIMALISTA,
    OSCURO,
    VIVO,
    ACADEMICO,
    STARTUP
}

object PresentationTemplates {

    /**
     * All available templates — open source palettes + Google Fonts.
     *
     * Color sources (all CC0/public domain):
     * - Tailwind color palette (MIT)
     * - Coolors public palettes
     * - Material Design color system (Apache 2.0)
     *
     * Font pairings (all OFL licensed, free to use commercially):
     * - Poppins + Inter (modern sans)
     * - Playfair Display + Source Sans Pro (editorial serif)
     * - Montserrat + Lato (corporate)
     * - Bebas Neue + Open Sans (impactful display)
     * - Caveat + Quicksand (handwritten warm)
     */
    val all: List<PresentationTemplate> = listOf(
        // === EDUCATIVO ===
        PresentationTemplate(
            id = "edu-vibrant",
            name = "Educativo Vibrante",
            category = TemplateCategory.EDUCATIVO,
            description = "Para clase, con colores cálidos y disposición didáctica.",
            palette = Palette(
                primary = "#FF6B35",      // naranja vibrante
                secondary = "#004E64",    // azul profundo
                accent = "#FFC857",       // amarillo cálido
                background = "#FAF3E0",   // crema
                surface = "#FFFFFF",
                onPrimary = "#FFFFFF",
                onBackground = "#1A1A2E"
            ),
            typography = Typography(headingFont = "Poppins", bodyFont = "Inter", headingWeight = 700, bodyWeight = 400),
            promptHint = "Tono educativo, didáctico y motivador. Usa ejemplos de la vida real. Cada slide debe ser fácil de entender para un estudiante de secundaria.",
            previewEmoji = "🎓"
        ),
        PresentationTemplate(
            id = "edu-clean",
            name = "Educativo Limpio",
            category = TemplateCategory.EDUCATIVO,
            description = "Minimalista, mucho espacio en blanco, ideal para primaria.",
            palette = Palette(
                primary = "#06A77D",
                secondary = "#D5C67B",
                accent = "#F1A208",
                background = "#FFFFFF",
                surface = "#F8F9FA",
                onPrimary = "#FFFFFF",
                onBackground = "#1A1A1A"
            ),
            typography = Typography(headingFont = "Quicksand", bodyFont = "Nunito", headingWeight = 600, bodyWeight = 400),
            promptHint = "Para niños de primaria. Lenguaje simple, oraciones cortas, máximo 3 bullets por slide.",
            previewEmoji = "🌿"
        ),

        // === CORPORATIVO ===
        PresentationTemplate(
            id = "corp-blue",
            name = "Corporativo Azul",
            category = TemplateCategory.CORPORATIVO,
            description = "Profesional, formal, ideal para reportes y reuniones.",
            palette = Palette(
                primary = "#1E3A8A",
                secondary = "#60A5FA",
                accent = "#F59E0B",
                background = "#F3F4F6",
                surface = "#FFFFFF",
                onPrimary = "#FFFFFF",
                onBackground = "#111827"
            ),
            typography = Typography(headingFont = "Montserrat", bodyFont = "Lato", headingWeight = 600, bodyWeight = 400),
            promptHint = "Tono profesional y formal. Usa datos cuantitativos cuando sea posible. Estructura clara con secciones: introducción, análisis, conclusiones.",
            previewEmoji = "💼"
        ),
        PresentationTemplate(
            id = "corp-elegant",
            name = "Corporativo Elegante",
            category = TemplateCategory.CORPORATIVO,
            description = "Tipografía serif para presentaciones ejecutivas.",
            palette = Palette(
                primary = "#2D2D2D",
                secondary = "#8B7355",
                accent = "#C9A66B",
                background = "#FAF7F2",
                surface = "#FFFFFF",
                onPrimary = "#FAF7F2",
                onBackground = "#2D2D2D"
            ),
            typography = Typography(headingFont = "Playfair Display", bodyFont = "Source Sans Pro", headingWeight = 700, bodyWeight = 400),
            promptHint = "Tono ejecutivo, sobrio y elegante. Ideal para juntas directivas. Lenguaje formal y preciso.",
            previewEmoji = "🥃"
        ),

        // === CREATIVO ===
        PresentationTemplate(
            id = "creative-bold",
            name = "Creativo Audaz",
            category = TemplateCategory.CREATIVO,
            description = "Colores saturados, tipografía display para campañas.",
            palette = Palette(
                primary = "#EC4899",
                secondary = "#8B5CF6",
                accent = "#FBBF24",
                background = "#0F0F0F",
                surface = "#1A1A1A",
                onPrimary = "#FFFFFF",
                onBackground = "#FFFFFF"
            ),
            typography = Typography(headingFont = "Bebas Neue", bodyFont = "Open Sans", headingWeight = 700, bodyWeight = 400),
            promptHint = "Tono creativo, provocador y memorable. Ideal para campañas de marketing o pitch creativo. Mensajes cortos y de alto impacto.",
            previewEmoji = "🎨"
        ),
        PresentationTemplate(
            id = "creative-warm",
            name = "Creativo Cálido",
            category = TemplateCategory.CREATIVO,
            description = "Paleta tierra y tipografía handwritten para proyectos artesanales.",
            palette = Palette(
                primary = "#A0522D",
                secondary = "#DEB887",
                accent = "#FF7F50",
                background = "#FFF8DC",
                surface = "#FFE4B5",
                onPrimary = "#FFFFFF",
                onBackground = "#3E2723"
            ),
            typography = Typography(headingFont = "Caveat", bodyFont = "Quicksand", headingWeight = 700, bodyWeight = 400),
            promptHint = "Tono cálido, cercano y artesanal. Ideal para proyectos sostenibles, emprendimiento social, marca personal.",
            previewEmoji = "🍂"
        ),

        // === MINIMALISTA ===
        PresentationTemplate(
            id = "minimal-mono",
            name = "Minimalista Monocromo",
            category = TemplateCategory.MINIMALISTA,
            description = "Blanco y negro absoluto, tipografía sans para diseño gráfico.",
            palette = Palette(
                primary = "#000000",
                secondary = "#666666",
                accent = "#FF0000",
                background = "#FFFFFF",
                surface = "#F5F5F5",
                onPrimary = "#FFFFFF",
                onBackground = "#000000"
            ),
            typography = Typography(headingFont = "Inter", bodyFont = "Inter", headingWeight = 800, bodyWeight = 400),
            promptHint = "Diseño minimalista extremo. Mucho espacio en blanco. Una idea por slide. Sin adornos. Estilo Dieter Rams.",
            previewEmoji = "⚪"
        ),
        PresentationTemplate(
            id = "minimal-pastel",
            name = "Minimalista Pastel",
            category = TemplateCategory.MINIMALISTA,
            description = "Paleta pastel suave, ideal para lifestyle y wellness.",
            palette = Palette(
                primary = "#A8DADC",
                secondary = "#F1FAEE",
                accent = "#E9C46A",
                background = "#F1FAEE",
                surface = "#FFFFFF",
                onPrimary = "#1D3557",
                onBackground = "#1D3557"
            ),
            typography = Typography(headingFont = "Poppins", bodyFont = "Inter", headingWeight = 500, bodyWeight = 300),
            promptHint = "Tono suave, lifestyle, wellness, mindfulness. Mensajes inspiradores y breves.",
            previewEmoji = "🌸"
        ),

        // === OSCURO ===
        PresentationTemplate(
            id = "dark-tech",
            name = "Tech Oscuro",
            category = TemplateCategory.OSCURO,
            description = "Modo oscuro tech, ideal para presentaciones de software.",
            palette = Palette(
                primary = "#5B6CFF",
                secondary = "#7DD3C0",
                accent = "#FFC857",
                background = "#0F1117",
                surface = "#1A1D26",
                onPrimary = "#FFFFFF",
                onBackground = "#E8E9F0"
            ),
            typography = Typography(headingFont = "Poppins", bodyFont = "JetBrains Mono", headingWeight = 600, bodyWeight = 400),
            promptHint = "Tono técnico, para desarrolladores. Usa diagramas de arquitectura, métricas de performance, comparativas de lenguajes. Apropiado para tech talks.",
            previewEmoji = "💻"
        ),
        PresentationTemplate(
            id = "dark-premium",
            name = "Premium Oscuro",
            category = TemplateCategory.OSCURO,
            description = "Negro y dorado, ideal para pitch decks de startups.",
            palette = Palette(
                primary = "#FFD700",
                secondary = "#B8860B",
                accent = "#FFFFFF",
                background = "#000000",
                surface = "#0A0A0A",
                onPrimary = "#000000",
                onBackground = "#FFFFFF"
            ),
            typography = Typography(headingFont = "Playfair Display", bodyFont = "Inter", headingWeight = 700, bodyWeight = 400),
            promptHint = "Tono premium, exclusivo. Ideal para pitch decks de startups financieras, lujo, o inversión. Mensajes de alto valor.",
            previewEmoji = "💎"
        ),

        // === VIVO ===
        PresentationTemplate(
            id = "vibrant-pop",
            name = "Pop Art Vivo",
            category = TemplateCategory.VIVO,
            description = "Colores pop saturados, ideal para campañas juveniles.",
            palette = Palette(
                primary = "#FF006E",
                secondary = "#FFBE0B",
                accent = "#3A86FF",
                background = "#FFD60A",
                surface = "#FFFFFF",
                onPrimary = "#FFFFFF",
                onBackground = "#000000"
            ),
            typography = Typography(headingFont = "Bebas Neue", bodyFont = "Archivo Black", headingWeight = 800, bodyWeight = 400),
            promptHint = "Tono pop art, juvenil, enérgico. Ideal para Gen Z, productos de consumo masivo. Lenguaje directo y con emojis.",
            previewEmoji = "💥"
        ),
        PresentationTemplate(
            id = "vibrant-tropical",
            name = "Tropical Vivo",
            category = TemplateCategory.VIVO,
            description = "Verde tropical + naranja, ideal para viajes y naturaleza.",
            palette = Palette(
                primary = "#06A77D",
                secondary = "#FF6B35",
                accent = "#F1A208",
                background = "#E8F1E1",
                surface = "#FFFFFF",
                onPrimary = "#FFFFFF",
                onBackground = "#1A1A1A"
            ),
            typography = Typography(headingFont = "Poppins", bodyFont = "Inter", headingWeight = 600, bodyWeight = 400),
            promptHint = "Tono tropical, fresco y aventurero. Ideal para viajes, ecología, naturaleza. Con referencias a lugares y experiencias.",
            previewEmoji = "🌴"
        ),

        // === ACADEMICO ===
        PresentationTemplate(
            id = "academic-classic",
            name = "Académico Clásico",
            category = TemplateCategory.ACADEMICO,
            description = "Serif + blanco, ideal para tesis y defensas académicas.",
            palette = Palette(
                primary = "#1A1A1A",
                secondary = "#4A4A4A",
                accent = "#8B0000",
                background = "#FFFFFF",
                surface = "#F8F8F8",
                onPrimary = "#FFFFFF",
                onBackground = "#1A1A1A"
            ),
            typography = Typography(headingFont = "Times New Roman", bodyFont = "Georgia", headingWeight = 700, bodyWeight = 400),
            promptHint = "Tono académico formal. Incluye referencias bibliográficas, citas, datos cuantitativos. Estructura: introducción, hipótesis, metodología, resultados, conclusiones.",
            previewEmoji = "📚"
        ),
        PresentationTemplate(
            id = "academic-modern",
            name = "Académico Moderno",
            category = TemplateCategory.ACADEMICO,
            description = "Sans-serif + azul, para conferencias y papers.",
            palette = Palette(
                primary = "#1E40AF",
                secondary = "#3B82F6",
                accent = "#EF4444",
                background = "#FFFFFF",
                surface = "#F3F4F6",
                onPrimary = "#FFFFFF",
                onBackground = "#1F2937"
            ),
            typography = Typography(headingFont = "IBM Plex Sans", bodyFont = "IBM Plex Serif", headingWeight = 600, bodyWeight = 400),
            promptHint = "Tono de conferencia académica. Estructura IMRaD. Cita autores relevantes. Tono neutral y evidence-based.",
            previewEmoji = "🔬"
        ),

        // === STARTUP ===
        PresentationTemplate(
            id = "startup-pitch",
            name = "Startup Pitch",
            category = TemplateCategory.STARTUP,
            description = "Diseño moderno con acentos, ideal para pitch a inversionistas.",
            palette = Palette(
                primary = "#6366F1",
                secondary = "#8B5CF6",
                accent = "#10B981",
                background = "#0F172A",
                surface = "#1E293B",
                onPrimary = "#FFFFFF",
                onBackground = "#F1F5F9"
            ),
            typography = Typography(headingFont = "Inter", bodyFont = "Inter", headingWeight = 700, bodyWeight = 400),
            promptHint = "Tono de pitch deck para startup. Estructura: problema, solución, mercado, tracción, modelo de negocio, equipo, financials, ask. Datos cuantitativos. Storytelling.",
            previewEmoji = "🚀"
        ),
        PresentationTemplate(
            id = "startup-ycombinator",
            name = "YC-Style Minimal",
            category = TemplateCategory.STARTUP,
            description = "Estilo Y Combinator: máxima claridad, mínimo diseño.",
            palette = Palette(
                primary = "#FF6600",
                secondary = "#FF8C00",
                accent = "#000000",
                background = "#FFFFFF",
                surface = "#FAFAFA",
                onPrimary = "#FFFFFF",
                onBackground = "#000000"
            ),
            typography = Typography(headingFont = "Inter", bodyFont = "Inter", headingWeight = 800, bodyWeight = 400),
            promptHint = "Estilo Y Combinator: máximo 5 bullets por slide, frases directas, sin emojis. Una idea por slide. Estructura: problema, solución, demo, mercado, equipo, tracción.",
            previewEmoji = "🎯"
        )
    )

    fun byCategory(category: TemplateCategory): List<PresentationTemplate> =
        all.filter { it.category == category }

    fun byId(id: String): PresentationTemplate? = all.find { it.id == id }
}
