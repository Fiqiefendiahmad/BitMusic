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

- **`androidApp/`** — new Android application module (Compose, Material 3, minSdk 26, target 36, compile 36).
- **`shared/`**, **`sharedUi/`** — shared cross-platform modules with Android target (compileSdk 36).
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

- Android SDK with **platform 37** and build-tools 36+ (set `sdk.dir` in `local.properties`, not committed)
- JDK 17+ (Android module uses Java/Kotlin 17; desktop modules remain as configured)
- Gradle wrapper: `./gradlew :androidApp:assembleDebug`

If the full multi-project configuration fights the desktop toolchain on a headless machine, temporarily include only `:shared` and `:androidApp` in `settings.gradle.kts`, build, then restore.

## Shared Data / Playback Integration

The redesign is **not** a sample-data prototype. It reuses the existing BitMusic data layer:

- `:shared` now has an Android target that reuses `src/jvmSharedMain` through a `jvmAndroid` hierarchy, so the same `Innertube`, `YtMusicRepository`, `InnerTubeXResolver`, and `StreamResolver` code runs on Android and desktop.
- `BitMusicApplication` installs the shared config: SharedPreferences-backed InnerTubeX config store, device locale, and `StreamResolver` bitrate ceiling.
- `MusicRepository` maps real Home/Search/Library results to `Song` rows; search calls `YtMusicRepository.search(query, SearchFilter.SONGS)`.
- `AndroidPlaybackController` resolves each track through `StreamResolver.resolve(videoId)` and plays the returned URL in Media3 ExoPlayer with the issuing client's media headers.
- `BitMusicViewModel` owns queue, play/pause, seek, next/previous, progress, buffering, and playback errors.

Remaining production work: optional media-session/notification service for background audio, Android YouTube sign-in if the library should show a signed-in account, and physical-device verification at 360×640dp to 412×915dp.

## License

Same as upstream BitMusic: **GPL-3.0**. See [LICENSE](LICENSE).
