# Changelog

All notable changes to this personal NuvioDev fork are documented here.
This fork tracks the upstream [Nuvio Desktop](https://github.com/NuvioMedia/NuvioDesktop)
project; all original work is credited there.

## 0.3-alpha

- **Android**: the release workflow now also builds **sideload APKs** (arm64-v8a,
  armeabi-v7a, x86_64, x86) from the same Compose Multiplatform codebase as the
  official mobile app. The mobile `cmp-rewrite` branch was merged (server
  library fixes through 0.5.9-beta), and the player runtime was hardened
  against force closes: the player coroutine scope is now a supervised scope
  with an exception handler (an uncaught player-coroutine failure toasts and
  logs instead of killing the app), and the next-episode handoff and preload
  paths catch their own failures. Debug-signed for sideloading; uninstall the
  official app first (same app id, different signature).
- **Linux (Arch-friendly)**: the release workflow now also builds a portable
  **x86_64 AppImage** (best-effort — never blocks the Windows release) and
  ships a `packaging/arch/PKGBUILD` that installs it as a pacman package with
  menu entry and `nuvio-dev` launcher. Runtime deps: `mpv`,
  `webkit2gtk-4.1`, `gst-plugins-good`, `gst-libav`, `libxcomposite`,
  `fuse2`.
- **Performance**: second pass after the polling overhaul — the native
  controls bridge now also skips WebView pushes entirely while paused with
  nothing changed, log lines are queued to a background writer thread (no
  more disk I/O on playback/UI threads), GIF card animations share a global
  decode budget of 4 concurrent loops, and the profile store preloads with a
  bounded 150 ms wait instead of blocking window creation.
- **Volume boost**: new **Settings → Playback → Volume Boost** toggle (off by
  default) raises the player volume ceiling from 100% to 200% for the wheel,
  `Shift`+arrows, and the slider.
- **Seek step**: new **Settings → Playback → Seek Step** (5 / 10 / 15 / 30
  seconds, default 10) applies to the arrow keys, `J`/`L`, the seek buttons,
  and the seek toasts.
- **Player**: press `?` for an in-player keyboard shortcut cheat-sheet.
- **Performance**: playback polling overhaul — the native controls bridge only
  rebuilds and re-injects track lists when they change, the Kotlin audio-track
  poll is throttled, position ticks no longer rebuild the full player controls
  state (no more full-UI recomposition or controls re-sync twice per second),
  the WebView chrome skips DOM writes whose values are unchanged, and seek-drag
  events no longer run full controls syncs or per-event disk logging.
- **Performance (picked from upstream NuvioDesktop, credited)**: fast video
  profile on integrated GPUs, player shaders kept between opens, bounded image
  cache, player files no longer read into memory at launch, player preload
  released after startup, lower desktop buffer with dropped-stream reconnect,
  fallback to the older video renderer, BT.2390 HDR tone mapping.
- **Player**: `Z` (like `Enter`) skips intro/outro while the skip prompt is
  visible.
- **Subtitles**: streams with built-in subtitle tracks auto-load the first
  non-forced embedded track when no preferred subtitle language is configured;
  explicit choices and language preferences still take precedence.

## 0.2-alpha

- **In-app updates**: `Settings → About → Check for updates` plus a seamless
  update banner that downloads and installs the newest NuvioDev MSI. The rolling
  release carries a monotonic version so every Dev build is detected.
- **Versioned releases**: each Dev push also publishes a downloadable
  `0.2-alpha.<run>` prerelease for history and rollback.
- **What's new dialog**: shown once after the app updates to a new version.
- **Update channel**: STABLE / BETA channel picker on desktop.
- **Window behavior**: remember size/position (existing), new **always on top**
  and **minimize to tray** toggles.
- **Player**: A-B loop (`Shift+A` set, `Shift+C` clear), frame step (`,` / `.`),
  and the existing subtitle delay nudge (`G` / `H`), skip (`Enter` / `Z`), speed,
  seek and volume shortcuts.
- **Player**: built-in subtitle tracks embedded in the video stream are now
  loaded automatically (first non-forced track) when no preferred subtitle
  language is configured; picking a language or turning subtitles off still
  takes precedence.
- **Diagnostics**: in-app **View logs** screen and a crash-reporting opt-out.
- **Skip intro/outro/recap** works out of the box (introdb default endpoint).
- **Fixed**: MSIs are now built with a jpackage whose installer components are
  per-product, so updating the official Nuvio app no longer deletes NuvioDev's
  files. NuvioDev's updater settings are also stored separately from the
  official app's. If an older NuvioDev install ever loses its files again,
  repair it with: `msiexec /f {3EAC7727-DC4D-32A0-A86B-C10ADA878BFA}`.
- **Fixed updater flow**: finishing a download no longer closes the app — the
  banner switches to "A new version is ready to install." with an **Install**
  button, and the app only restarts once you click it.
- **Fixed updater flow**: finishing a download no longer closes the app — the
  banner switches to "A new version is ready to install." with an **Install**
  button, and the app only restarts once you click it.
- **Fixed MSI upgrades**: every Dev build now carries a distinct Windows
  Installer product version (run-numbered, e.g. `1.2.11`). Previously all
  `0.2-alpha.X` builds shared one version, so msiexec "reconfigured" the
  installed product instead of upgrading and the in-app update silently failed
  ("Configuration failed").
- **Fixed sign-in**: release builds now ship the full backend config (Supabase,
  TMDB, Trakt, Simkl, MDBList, Premiumize) — previously the fork's CI built
  with a blank config, so sign-in failed with "connection refused" and NuvioDev
  could only pick up sessions signed in from the official app.
- **Updater hardening**: downloads retry with backoff on transient CDN
  failures, verify the GitHub release asset SHA-256, and offer an
  **Open page** shortcut (release page in browser) when a download keeps
  failing.
- **Single instance**: launching NuvioDev twice focuses the running window and
  forwards its `nuvio://` deep links instead of starting a second app. The
  `nuvio://` protocol is registered per-user so browser links open the app.
- **Primary profile by default**: the app boots straight into the primary
  (unlocked) profile instead of showing the profile selector every launch.
  Switch Profile in Settings still shows the selector.
- **Real-Debrid** is now available in `Settings → Connected Services` (API key).
- **Fixed**: stale debrid device-auth prompts are cleared once their approval
  window has passed instead of waiting forever.

## 0.1

- Initial NuvioDev fork: side-by-side install, OTA updates, in-app naming and
  credits, `S` cycles subtitles, Windows MSI builds.
