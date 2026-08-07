---
name: MediCare+ Design System
colors:
  surface: '#faf9fa'
  surface-dim: '#dadadb'
  surface-bright: '#faf9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f3f4'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e9'
  surface-container-highest: '#e3e2e3'
  on-surface: '#1a1c1d'
  on-surface-variant: '#3f484c'
  inverse-surface: '#2f3131'
  inverse-on-surface: '#f1f0f1'
  outline: '#6f787d'
  outline-variant: '#bfc8cd'
  surface-tint: '#00677f'
  primary: '#004d60'
  on-primary: '#ffffff'
  primary-container: '#00677f'
  on-primary-container: '#98e3ff'
  inverse-primary: '#86d1ec'
  secondary: '#4a6267'
  on-secondary: '#ffffff'
  secondary-container: '#cde7ed'
  on-secondary-container: '#50686d'
  tertiary: '#005146'
  on-tertiary: '#ffffff'
  tertiary-container: '#006b5d'
  on-tertiary-container: '#95e8d6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#b6eaff'
  primary-fixed-dim: '#86d1ec'
  on-primary-fixed: '#001f28'
  on-primary-fixed-variant: '#004e60'
  secondary-fixed: '#cde7ed'
  secondary-fixed-dim: '#b1cbd1'
  on-secondary-fixed: '#051f23'
  on-secondary-fixed-variant: '#334b4f'
  tertiary-fixed: '#9ff2e0'
  tertiary-fixed-dim: '#83d6c4'
  on-tertiary-fixed: '#00201b'
  on-tertiary-fixed-variant: '#005046'
  background: '#faf9fa'
  on-background: '#1a1c1d'
  surface-variant: '#e3e2e3'
typography:
  display-lg:
    fontFamily: Atkinson Hyperlegible Next
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Atkinson Hyperlegible Next
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Atkinson Hyperlegible Next
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: Atkinson Hyperlegible Next
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  title-lg:
    fontFamily: Atkinson Hyperlegible Next
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Atkinson Hyperlegible Next
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: Atkinson Hyperlegible Next
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-lg:
    fontFamily: Atkinson Hyperlegible Next
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  baseline: 8px
  container-padding: 24px
  gutter: 16px
  touch-target-min: 48px
  stack-gap: 12px
---

## Brand & Style

The design system is centered on the "Quiet Confidence" of modern healthcare. It prioritizes the emotional well-being of users—particularly seniors and caregivers—by employing a **Corporate / Modern** style infused with **Minimalist** clarity. The objective is to reduce cognitive load and medical anxiety through a UI that feels organized, spacious, and human-centric.

The aesthetic follows Material Design 3 (MD3) principles, emphasizing tonal surfaces and clear hierarchy. It avoids high-density information patterns in favor of large, breathable layouts that evoke a sense of calm and reliability. Every visual choice is optimized for accessibility, ensuring that critical health information is never more than a glance away.

## Colors

The palette is rooted in "Healing Blues" and "Vitality Greens." The primary color is a deep, accessible teal that provides high contrast against light surfaces. Secondary and tertiary colors are used for subtle categorization and supportive UI elements.

**Color Usage & Semantics:**
- **Primary:** Core actions, active states, and branding.
- **Secondary:** Tonal variations for containers and background layering.
- **Tertiary:** Supportive health accents and alternative categories.
- **Neutral:** Focused on WCAG 2.1 AAA compliance for typography on surface containers.

**Status Indicators:**
- **Taken (Green):** Safe, completed, reassuring.
- **Scheduled (Blue):** Informational, steady.
- **Upcoming (Orange):** Alerting without causing panic.
- **Missed (Red):** Urgent, requires immediate attention.

Both Light and Dark modes utilize the MD3 Tonal Palette system, ensuring that surface colors shift to deep charcoals in dark mode while maintaining the necessary contrast ratios for readability.

## Typography

This design system exclusively uses **Atkinson Hyperlegible Next**. This font was specifically developed to increase legibility for readers with low vision, making it the ideal choice for a senior-focused health application.

**Key Implementation Rules:**
- **Scale:** Body text never drops below 16px to ensure accessibility.
- **Emphasis:** Use font weight (Medium/Bold) rather than italics to highlight important medical instructions.
- **Line Height:** Generous leading (1.5x for body) is used to prevent lines of text from blurring together.
- **Alignment:** Left-aligned text is preferred for all medical instructions to provide a consistent "starting point" for the eye.

## Layout & Spacing

The layout follows a **Fluid Grid** model optimized for Android mobile devices. It utilizes a 4-column grid for mobile handsets with a standard 24px outer margin to keep interactive elements away from screen edges.

**Spacing Philosophy:**
- **Touch Targets:** A strict minimum of 48x48dp for every interactive element (buttons, checkboxes, toggles).
- **Vertical Rhythm:** Elements are stacked using an 8px base unit. Card containers within a list use a 12px or 16px gap to ensure they are visually distinct.
- **Safe Areas:** Generous padding within cards (min 16px) ensures that text does not feel cramped against the container boundaries.

## Elevation & Depth

This design system utilizes **Tonal Layers** as the primary method of showing depth, supplemented by **Ambient Shadows**.

- **Level 0 (Surface):** The lowest layer, using the system's base background color.
- **Level 1 (Cards):** Slightly elevated using a very soft, diffused shadow (Blur: 8px, Y: 2px, Opacity: 8%) to signify interactivity.
- **Level 2 (Active/Selected):** Used for "Upcoming" reminders or currently active tasks. These use a slightly more pronounced shadow and a tonal stroke.
- **Modal Sheets:** Used for adding new medications; these use a scrim to dim the background and a Level 3 elevation (Blur: 16px, Y: 4px) to pull focus.

## Shapes

The shape language is **Rounded**, favoring soft, approachable geometries over sharp edges. This reinforces the "calming" brand pillar.

- **Small Components (Chips, Tags):** Fully rounded (Pill) for a friendly, organic feel.
- **Medium Components (Buttons, Input Fields):** 0.5rem (8px) corner radius to provide a modern, sturdy look.
- **Large Components (Cards, Bottom Sheets):** 1.5rem (24px) corner radius on top edges to create a soft, non-threatening containment of information.

## Components

**Buttons:**
- **Primary:** Filled buttons with high-contrast text. Minimum height 56px for main actions (e.g., "Mark as Taken").
- **Secondary:** Tonal buttons (light blue/teal background) for less urgent tasks.

**Medication Cards:**
- Contain a prominent icon (Pill, Syrup, Injection), medication name (Title LG), and dosage (Body MD).
- Feature a color-coded vertical "Status Bar" on the left edge (Green/Blue/Orange/Red).

**Input Fields:**
- Outlined style with a 2px stroke when focused. 
- Labels are always visible (not floating) to ensure the user knows what information is required at all times.

**Chips:**
- Used for day-of-the-week selection or medication categories. 
- Active state uses a primary color fill; inactive state uses a light neutral stroke.

**Checkboxes & Radios:**
- Enlarged to 24x24dp within a 48x48dp touch zone.
- Use a clear checkmark animation to provide positive reinforcement when a medication is logged.

**Progress Indicators:**
- Circular "Day Progress" rings on the dashboard to show percentage of medications taken, using the "Taken" Green color.