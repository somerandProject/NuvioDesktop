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
| `Enter` | Skip intro/outro when the prompt is visible |
| `T` | Stream info |
| `X` | Stop |
| `P` | Picture-in-picture |

## Get NuvioDev

Grab the latest build from the rolling release:

- [NuvioDev Latest (Windows x64 MSI)](https://github.com/somerandProject/NuvioDesktop/releases/tag/nuvio-dev-latest)

The in-app updater points at this rolling release, so you can update without
leaving the app.

## Build from source

```bash
git clone https://github.com/somerandProject/NuvioDesktop.git
cd NuvioDesktop
```

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
