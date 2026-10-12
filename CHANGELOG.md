# Changelog

All notable changes to this personal NuvioDev fork are documented here.
This fork tracks the upstream [Nuvio Desktop](https://github.com/NuvioMedia/NuvioDesktop)
project; all original work is credited there.

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
  and the existing subtitle delay nudge (`G` / `H`), skip (`Enter`), speed, seek
  and volume shortcuts.
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
