# gc Copy — JetBrains plugin

Adds **Copy with gc** to the Project-view and editor right-click menus. It runs a
configurable command (default `gc -e cs -z \\ \n\n`) in the selected folder using
`bash -lc`, so your login `PATH` plus `~/.local/bin` is available, and `gc` copies
the result to the clipboard. A balloon reports success or shows the captured error.

Change the command without recompiling in **Settings → Tools → gc Copy**. Whatever
you type there is handed to bash exactly as if you typed it in a terminal.

## Prerequisites

- **JDK 17 or newer** on `PATH` (the build uses a JDK 21 toolchain).
- **Internet on first build.** The wrapper fetches Gradle 9.5; the IntelliJ Platform
  plugin fetches the target IDE (~1 GB, cached afterward).
- The **`gc` CLI** working from your terminal.

You do not need an IDE pre-installed to test, and you do not need to install Gradle.

## Test it live

```bash
./gradlew runIde
```

A sandbox IDE launches with the plugin loaded. Right-click a folder in the Project
tool window → **Copy with gc**.

## Build the installable zip

```bash
./gradlew buildPlugin
```

Output: `build/distributions/gc-copy-2.0.0.zip`. Install via
**Settings → Plugins → ⚙ → Install Plugin from Disk…**, then **restart the IDE**.

## If the menu item does not appear

1. **Settings → Plugins** — confirm *gc Copy* is present and enabled (not greyed,
   no error marker). If it shows an error, open it for the load failure reason.
2. Confirm you actually rebuilt and reinstalled the new zip, then restarted.
3. This plugin loads on IDE build 233 (2023.3) and newer. On anything older, lower
   `sinceBuild` in `build.gradle.kts`.
4. If you rename the package or any class, update the matching name in
   `src/main/resources/META-INF/plugin.xml` too — a stale class name there makes the
   whole plugin fail to load, and the action silently disappears.

## Assign a keyboard shortcut (optional)

**Settings → Keymap**, search *Copy with gc*, right-click → Add Keyboard Shortcut.

## Notes

- Uses `bash`, so it targets Linux/macOS. Windows needs the `ProcessBuilder` line
  in `GcCopyAction.kt` swapped for a Windows shell.
- If a pinned version fails to resolve, bump `intellijIdea(...)` in `build.gradle.kts`
  and the plugin versions in `settings.gradle.kts` to current releases.
