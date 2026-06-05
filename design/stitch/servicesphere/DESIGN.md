---
name: ServiceSphere
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#4a4455'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#7b7487'
  outline-variant: '#ccc3d8'
  surface-tint: '#732ee4'
  primary: '#630ed4'
  on-primary: '#ffffff'
  primary-container: '#7c3aed'
  on-primary-container: '#ede0ff'
  inverse-primary: '#d2bbff'
  secondary: '#4e45d5'
  on-secondary: '#ffffff'
  secondary-container: '#6860ef'
  on-secondary-container: '#fffbff'
  tertiary: '#474e60'
  on-tertiary: '#ffffff'
  tertiary-container: '#5f6678'
  on-tertiary-container: '#dfe5fa'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#eaddff'
  primary-fixed-dim: '#d2bbff'
  on-primary-fixed: '#25005a'
  on-primary-fixed-variant: '#5a00c6'
  secondary-fixed: '#e3dfff'
  secondary-fixed-dim: '#c3c0ff'
  on-secondary-fixed: '#100069'
  on-secondary-fixed-variant: '#372abf'
  tertiary-fixed: '#dce2f7'
  tertiary-fixed-dim: '#c0c6db'
  on-tertiary-fixed: '#141b2b'
  on-tertiary-fixed-variant: '#404758'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  margin-mobile: 16px
  margin-tablet: 24px
  gutter: 16px
  stack-sm: 4px
  stack-md: 12px
  stack-lg: 24px
---

## Brand & Style
The design system is engineered for efficiency, reliability, and precision. It serves as a high-performance tool for field service professionals who require clarity under varying environmental conditions. The aesthetic follows a **Corporate / Modern** direction with a focus on high-density information management and rapid task completion.

The visual language is rooted in a refined SaaS aesthetic: expansive whitespace to reduce cognitive load, a focused color palette to direct attention, and a tactile feel that suggests stability. The emotional response is one of organized control—transforming complex logistical data into actionable, easy-to-digest interfaces.

## Colors
The color architecture prioritizes action and status. The **Primary Vivid Purple** is reserved for high-intent actions (creating jobs, finishing tasks), while the **Secondary Indigo** supports navigation and secondary grouping. 

The palette utilizes a "Surface-Neutral" strategy:
- **Backgrounds:** Use the cool #F8FAFC to provide a modern, airy feel that differentiates from standard white paper.
- **Surfaces:** Use pure #FFFFFF for cards and containers to create a clear "lift" from the background.
- **Tonal Contrast:** For dark mode or high-emphasis headers, the #111827 surface provides a sophisticated, professional anchor.
- **Semantic Indicators:** Success, Warning, and Danger colors are calibrated for high legibility against both white and light-gray backgrounds, ensuring critical alerts are never missed.

## Typography
This design system utilizes **Plus Jakarta Sans** for its exceptional legibility and modern, open apertures. The type scale is optimized for Android-native environments, ensuring that technical data (part numbers, timestamps, coordinates) is easily scannable.

- **Weight Strategy:** Use Bold (700) for primary page titles, SemiBold (600) for section headers and component labels, and Regular (400) for body text.
- **Hierarchy:** Tighten letter-spacing on larger headlines to maintain a professional, "tight" SaaS feel. Increase letter-spacing on small labels (all-caps or sentence case) to improve readability on mobile displays.

## Layout & Spacing
The layout follows an **8px grid system**, consistent with Jetpack Compose standards. 

- **Grid:** On mobile, use a fluid single-column layout with 16px side margins. On tablets, transition to a 12-column grid with 24px gutters to allow for "Master-Detail" views (e.g., job list on the left, map/details on the right).
- **Rhythm:** Vertical spacing between cards should be 12px to maintain a compact, high-utility density. Internal card padding is fixed at 16px to ensure touch targets for nested elements remain accessible.

## Elevation & Depth
Depth is communicated through **Tonal Layering** supplemented by **Ambient Shadows**. This design system avoids heavy shadows to maintain a clean, modern profile.

- **Level 0 (Background):** #F8FAFC.
- **Level 1 (Cards/Inputs):** White surface with a 1px #E2E8F0 border or a very soft shadow (0px 2px 4px rgba(0,0,0,0.05)).
- **Level 2 (Active/Hover):** Enhanced shadow (0px 4px 12px rgba(0,0,0,0.08)) to indicate interactivity.
- **Level 3 (Modals/Overlays):** Scrim-backed surfaces with a 0px 10px 25px rgba(0,0,0,0.12) shadow.

## Shapes
The shape language is defined by **Rounded (0.5rem / 8px)** base corners, scaling up for larger containers to create a friendly but structured appearance.

- **Standard Elements:** Buttons, Input Fields, and small Chips use an 8px radius.
- **Containers:** Service cards and dashboard widgets use a 12px (rounded-lg) radius.
- **Bottom Sheets:** Large modal containers use a 24px top-only radius to soften the mobile interface.
- **Icons:** Use "Rounded" variant icon sets (Material Symbols Rounded) to match the UI's geometry.

## Components
Components are designed for "gloves-friendly" interaction and high contrast.

- **Buttons:**
    - *Primary:* Solid #7C3AED with white text. 12px vertical padding.
    - *Secondary:* Ghost style with #7C3AED border and text.
- **Cards:** White surfaces with 12px radius. Use a vertical 4px "accent bar" on the left side of the card using semantic colors (Success/Warning) to indicate job status at a glance.
- **Input Fields:** Outlined style with 8px radius. The label should float to the top border on focus (Material 3 style). Use a 2px primary border on focus.
- **Chips:** Soft-filled chips (10% opacity of the semantic color) for status indicators (e.g., "In Progress," "Pending").
- **Lists:** Use divider-less lists with 8px vertical spacing between card-based items for better visual separation of distinct tasks.
- **Navigation:** Standard Android Bottom Navigation Bar with tinted icons and 12px labels using the `label-sm` typography.