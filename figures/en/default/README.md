# Figure Assets Folder Structure

This folder contains the figure assets (SVG images and audio files) used by the AeroJudge Device.

## Folder Hierarchy

```
figures/en/
|-- default/           # Software-managed default sequences
|   |-- season.cfg     # Season year configuration
|   |-- FAIL/          # Fallback folder for missing figures
|   |   `-- fig_missing.svg
|   |-- FS/            # Freestyle figures
|   |   `-- 1B.svg, 1C.svg, etc.
|   |-- SPK_26S/       # Sportsman Known 2026 Standard
|   |-- SPK_26A/       # Sportsman Known 2026 Alternate
|   |-- INK_26S/       # Intermediate Known 2026 Standard
|   `-- ...etc
|-- event/             # Event-specific sequences (user-managed)
|   |-- CUSTOM_UNK1/   # Custom unknown sequences
|   `-- ...etc
|-- audio/             # Score and announcement audio
|   |-- score/         # Score value audio (0.mp3, 0.5.mp3, etc.)
|   |-- now_judging_*.mp3  # Now Judging announcements
|   `-- break.mp3, not_observed.mp3
|-- std/               # [DEPRECATED] Old standard sequence folders
`-- alt/               # [DEPRECATED] Old alternate sequence folders
```

## Folder Name Convention

For KNOWN sequences (can be derived from class + year + variant):
- `{ClassCode}K_{Year}{Variant}`
- Example: `SPK_26S` = Sportsman Known, Year 2026, Standard

Class Codes:
- BA = BASIC
- SP = SPORTSMAN
- IN = INTERMEDIATE
- AD = ADVANCED
- UN = UNLIMITED
- IV = INVITATIONAL

Variants:
- S = Standard
- A = Alternate

For UNKNOWN sequences (must use short_desc from Score):
- Use the exact `short_desc` value from sequences.dat
- Example: `ADVANCED_UNKNOWN_2024`

## Folder Resolution Order

1. Check `event/{short_desc}` - for custom event sequences
2. Check `default/{short_desc}` - for standard sequences
3. Derive from class+year (KNOWN only) - `default/{ClassCode}K_{Year}{Variant}`
4. Fallback to `default/FAIL/` - generic placeholder

## Files Per Folder

Each sequence folder should contain:
- `1B.svg`, `2B.svg`, ... - Figure images (B = left-to-right)
- `1C.svg`, `2C.svg`, ... - Figure images (C = right-to-left)
- `fig1.mp3`, `fig2.mp3`, ... - Short description audio
- `call1.mp3`, `call2.mp3`, ... - Long description audio

## season.cfg

Located at `default/season.cfg`, contains:
```properties
season.year=26
```

This is used to derive folder names when `short_desc` is blank.
