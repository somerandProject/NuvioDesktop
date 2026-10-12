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

## 0.1

- Initial NuvioDev fork: side-by-side install, OTA updates, in-app naming and
  credits, `S` cycles subtitles, Windows MSI builds.
