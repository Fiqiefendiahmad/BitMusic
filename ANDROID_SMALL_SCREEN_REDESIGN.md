# BitMusic — Android Small-Screen Redesign (Fork)

This branch adds an **Android small-screen redesign** to [1999AZZAR/BitMusic](https://github.com/1999AZZAR/BitMusic) (a desktop Compose Multiplatform YouTube Music client).

## Fork / Push

The repo is cloned from upstream. To publish your fork:

```bash
# 1) Create a fork on GitHub: https://github.com/1999AZZAR/BitMusic/fork
# 2) Point origin to YOUR fork (replace <you>):
git remote set-url origin git@github.com:<you>/BitMusic.git
# keep upstream for syncing:
git remote -v   # should show upstream -> 1999AZZAR/BitMusic

# 3) Push the redesign branch:
git push -u origin android/small-screen-redesign
```

Or with GitHub CLI: `gh repo fork 1999AZZAR/BitMusic --clone=false` then set remotes and push.

## What Changed

- **`androidApp/`** — new Android application module (Compose, Material 3, minSdk 26, target/compile 36).
- **Small-screen-first UI redesign** (portrait/compact width focus):
  - **Bottom navigation** (`Home`, `Search`, `Library`) for thumb reach.
  - **Persistent mini-player** above navigation with progress, artwork, play/pause, and tap-to-expand.
  - **Full-screen Now Playing** overlay: large centered artwork (viewport-constrained), prominent transport controls in the lower thumb zone, seek bar, and an “Up next” queue preview — no desktop side panes.
  - **Single-column, touch-friendly lists** (>=48dp targets), status/navigation bar insets, IME padding, compact spacing tokens.
  - **Adaptive shell** via `WindowSizeClass`; compact width uses the mobile bottom-nav layout.
- **Gradle** — root plugins include AGP 8.7.3 + Kotlin Android; `settings.gradle.kts` includes `:androidApp` when present.

## Design Notes (Small Screen)

| Area | Desktop (original) | Android small-screen redesign |
|---|---|---|
| Navigation | Window/desktop chrome & large layouts | Bottom navigation bar |
| Player | Desktop player panes / large viewport controls | Mini-player + portrait Now Playing overlay |
| Layout | Multi-pane/wide grids | Single column, constrained artwork, no horizontal overflow |
| Touch | Pointer-first | 48dp+ targets, thumb-zone primary actions |
| System UI | Desktop windowing | Edge-to-edge, status/nav bar padding, IME-safe |

## Build Requirements

- Android SDK (set `sdk.dir` in `local.properties`, not committed)
- JDK 17+ (Android module uses Java/Kotlin 17; desktop modules remain as configured)
- Gradle wrapper: `./gradlew :androidApp:assembleDebug`

> This environment may not include the Android SDK. The module is scaffolded and ready to build on a machine with Android Studio / SDK installed.

## Integration Path (Next Steps)

The redesign UI is currently a **standalone prototype** with sample data to validate small-screen UX. To fully integrate with BitMusic’s shared playback/data stack:

1. Add Android targets to `:shared`/UI modules or create a KMP `androidMain` source set strategy.
2. Replace `SampleData`/`PlayerUiState` wiring with the existing player host, stream resolver, and library repositories.
3. Add media session/notification playback service for background audio.
4. Validate on physical small-screen devices (e.g., 360×640dp to 412×915dp) and add accessibility tests.

## License

Same as upstream BitMusic: **GPL-3.0**. See [LICENSE](LICENSE).
