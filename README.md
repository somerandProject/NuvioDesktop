# NuvioDev (personal fork of Nuvio Desktop)

> **This is a personal, unofficial fork of [Nuvio Desktop](https://github.com/NuvioMedia/NuvioDesktop).**
> It is not affiliated with, endorsed by, or supported by the original authors.
> All credit for the original app, design, and the vast majority of the code goes to
> **Tapframe and the Nuvio contributors** — see [Credits](#credits) below.

`NuvioDev` is a side-by-side dev build of the Nuvio fork. It installs alongside the
official Nuvio app as a separate app (`NuvioDev`), with its own shortcuts and menu
entry, so installing or updating it will **not** touch your official Nuvio install.

## What's different in this fork

- In-app updates: a **Check for updates** button in `Settings → About`, plus a
  seamless update banner that downloads and installs the newest `NuvioDev` MSI.
  Every Dev build is versioned (`0.2-alpha.<run>`) so updates are always detected.
- **What's new** dialog shown once after updating.
- Update channel picker (Stable / Beta).
- Desktop window options: remember size/position, **always on top**, and
  **minimize to tray**.
- Player extras: **A-B loop** (`Shift+A` / `Shift+C`), frame step (`,` / `.`),
  subtitle delay nudge (`G` / `H`), and rich keyboard shortcuts.
- Diagnostics: in-app **View logs** screen and a crash-reporting opt-out.
- Builds are published only for **Windows x64** (MSI) from the `Dev` branch.
- Assorted personal tweaks (e.g. subtitle track selection on seek, skip
  intro/outro via introdb).
- Uses the debug keystore for release signing when no release keystore is
  configured, so fork builds are installable without your own keystore.

## Player keyboard shortcuts (desktop)

| Key | Action |
| --- | --- |
| `Space` | Play/pause (hold to speed up) |
| `←` / `→` (or `J` / `L`) | Seek 10s (Shift for fine seek) |
| `↑` / `↓` | Volume |
| `+` / `-` / `[` / `]` | Playback speed |
| `=` / `Shift+Z` | Reset speed to 1x |
| `S` | Cycle subtitles |
| `A` | Audio track menu |
| `V` | Toggle subtitles |
| `G` / `H` | Subtitle delay −/+ 100 ms |
| `,` / `.` | Step one frame back/forward |
| `Shift+A` / `Shift+C` | Set A-B loop / clear A-B loop |
| `Enter` / `Z` | Skip intro/outro when the prompt is visible |
| `T` | Stream info |
| `X` | Stop |
| `P` | Picture-in-picture |
| `?` | Show the keyboard shortcut list |

## Get NuvioDev

Grab the latest build from the rolling release:

- [NuvioDev Latest (Windows x64 MSI)](https://github.com/somerandProject/NuvioDesktop/releases/tag/nuvio-dev-latest)
- [NuvioDev Latest (Linux x86_64 AppImage)](https://github.com/somerandProject/NuvioDesktop/releases/tag/nuvio-dev-latest)
- [NuvioDev Latest (Android APKs)](https://github.com/somerandProject/NuvioDesktop/releases/tag/nuvio-dev-latest)

The in-app updater points at this rolling release, so you can update without
leaving the app.

### Linux (Arch and derivatives)

The AppImage needs a few system libraries (already present on most setups):

```sh
sudo pacman -S mpv webkit2gtk-4.1 gst-plugins-good gst-libav libxcomposite glib-networking fuse2
chmod +x NuvioDev-Linux-x86_64-*.AppImage
./NuvioDev-Linux-x86_64-*.AppImage
```

No `fuse2`? Run it with `./NuvioDev-Linux-x86_64-*.AppImage --appimage-extract-and-run`.

To install it as a pacman package (menu entry + `nuvio-dev` command), use the
bundled PKGBUILD — set `_realver`/`pkgver` and the `sha256sums` digest to a
release that ships the Linux AppImage, then:

```sh
cd packaging/arch
makepkg -si
```

### Android

The release also ships sideload APKs built from the same Compose Multiplatform
codebase as the official mobile app (`arm64-v8a`, `armeabi-v7a`, `x86_64`,
`x86` — pick the one matching your phone; most devices want `arm64-v8a`).
The APKs are debug-signed for sideloading.

> **Important:** the APK uses the same app id as the official Nuvio app but a
> different signature, so Android refuses to install it over the official app.
> Uninstall the official app first — your data restores via Nuvio sync after
> you sign in.

On the phone, open the APK asset from the release page, allow
"install unknown apps" for the browser, and install.

## Build from source

```bash
git clone https://github.com/somerandProject/NuvioDesktop.git
cd NuvioDesktop
```

Prerequisites for the Windows MSI:

1. **JDK 21** (Temurin) — its jpackage generates per-product MSI component
   GUIDs; older JDKs collide with the official Nuvio install.
2. **Windows runtime files** are stored in Git LFS. Pull them once:

   ```bash
   git lfs pull --include="composeApp/src/desktopMain/native/windows/runtime/**"
   ```

3. **WebView2 SDK** — install locally with nuget and pass the directory to
   Gradle:

   ```powershell
   nuget install Microsoft.Web.WebView2 -Version 1.0.4078.44 -OutputDirectory "$env:RUNNER_TEMP\nuget"
   .\gradlew.bat :composeApp:packageReleaseMsi `
     "-Pnuvio.webview2.dir=$env:RUNNER_TEMP\nuget\Microsoft.Web.WebView2.1.0.4078.44"
   ```

### `local.properties` keys

Runtime config values are injected at build time from `local.properties` (or
environment variables). Without them the app falls back to the official
backend for sign-in and TMDB, but tracking providers are disabled:

| Key | Purpose |
| --- | --- |
| `NUVIO_SUPABASE_URL` / `NUVIO_SUPABASE_ANON_KEY` | Nuvio account backend (falls back to the official endpoint) |
| `NUVIO_SUPABASE_FALLBACK_URL` | Secondary backend used when the primary origin fails |
| `TRAKT_CLIENT_ID` / `TRAKT_CLIENT_SECRET` | Trakt sign-in (device-code flow) |
| `SIMKL_CLIENT_ID` | Simkl tracking |
| `MDBLIST_CLIENT_ID` | MDBList tracking |
| `PREMIUMIZE_CLIENT_ID` | Premiumize debrid device sign-in |
| `TMDB_API_KEY` | TMDB metadata (official key used as fallback) |
| `SENTRY_DSN` / `SENTRY_DESKTOP_DSN` | Crash reporting |

CI consumes the same file, base64-encoded, via the
`NUVIO_DESKTOP_LOCAL_PROPERTIES_BASE64` repository secret. If it is blank the
release workflow fails with a clear error (sign-in would be baked in blank).

Run the app:

```bash
./gradlew :composeApp:run
```

On Windows PowerShell:

```powershell
.\gradlew.bat :composeApp:run
```

Package the Windows MSI:

```powershell
.\gradlew.bat :composeApp:packageReleaseMsi --rerun-tasks
```

The shared app is built with Kotlin Multiplatform and Compose Multiplatform.

## License

[GNU General Public License v3.0](./LICENSE)

Because this is a fork of a GPL-3.0 project, this fork remains under the same
license, and the original copyright and license notices are preserved.

## Credits

- **Original app (Nuvio): Tapframe and friends / the Nuvio project** —
  <https://github.com/NuvioMedia/NuvioDesktop> · <https://nuvio.tv>
- All original contributors, translators, and upstream maintainers.
- This personal fork and its modifications: maintained for personal use.

If you are one of the original authors and would like this fork changed or taken
down, please open an issue and it will be handled promptly.
