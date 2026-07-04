# Time Tracker (Java)

A Java desktop app that tracks how much time you spend in each program, and shows
statistics for today / yesterday / week / month / year / all time.

Data is stored **locally only**:
`C:\Users\<your_name>\TimeTracker\timetracker.db`
Nothing is ever sent over the network.

## How it works

- `WindowMonitor` — a background thread that checks, once a second via WinAPI
  (through the JNA library), which window is currently active and which process
  owns it. This works regardless of how many monitors you have — Windows always
  considers exactly one window "active" (the one in focus), no matter which
  screen it's on.
- When you switch programs, the previous session is closed and written to the
  local SQLite database (`Database`).
- `DashboardApp` (JavaFX) reads that database and builds statistics for the
  selected period.
- `ProcessIconCache` fetches the standard Windows icon for each running program
  and shows it in the list instead of a colored dot.
- `TrayManager` minimizes the app to the system tray instead of closing it
  completely.

**Clicking the close button** hides the window to the tray (bottom right, near
the clock), while the tracking service **keeps running** in the background.
To fully stop the program, right-click the tray icon and choose
"Exit (stop tracking)". Double-clicking the tray icon reopens the window.

Periods: **week** — the last 7 days including today; **month** and **year** —
from the start of the current calendar month/year.

## One-time setup

1. **JDK 17 or newer** — https://adoptium.net (this gives you both `java` and
   `jpackage`). During installation, check "Add to PATH".
2. **Maven** — https://maven.apache.org/download.cgi
   Download the archive, extract it anywhere (e.g. `C:\maven`), then add the
   `C:\maven\bin` folder to your PATH environment variable (Control Panel →
   System → Advanced system settings → Environment Variables → Path → Edit →
   New).

Verify everything is installed by running in a terminal:
```
java -version
mvn -version
jpackage --version
```

## Building the .exe

1. Build the fat jar (with all dependencies bundled):
```
mvn clean package
```
This produces `target\time-tracker-1.0.0.jar`.

2. Package it into a standalone Windows app with `jpackage` (Java is fully
   bundled — the machine running the .exe does **not** need Java installed):
```
jpackage --input target --name TimeTracker --main-jar time-tracker-1.0.0.jar --main-class com.timetracker.Main --type app-image --icon assets\icon.ico --dest release
```

This creates `release\TimeTracker\TimeTracker.exe`.

> **Tip:** if the app fails to start and the window just doesn't appear, add
> `--win-console` to the command above to get a console window with the full
> error/stack trace. Once everything works, rebuild **without** that flag to
> get the clean version with no black window.

## Running without building an exe (for development)

```
mvn clean javafx:run
```
(this requires the `javafx-maven-plugin` in `pom.xml`), or simply build the jar
and run it directly:
```
java -jar target\time-tracker-1.0.0.jar
```

## Auto-start on Windows boot (optional)

1. `Win + R` → `shell:startup` → Enter.
2. Copy a shortcut to `release\TimeTracker\TimeTracker.exe` into that folder.

Alternatively, the app can register itself in
`HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Run` on first launch and start
automatically (hidden, minimized to tray) on the next reboot — see
`enableAutoStart()` in `DashboardApp.java`.

## Project structure

```
TimeTrackerJava/
├── pom.xml
├── assets/
│   └── icon.ico
├── src/main/java/com/timetracker/
│   ├── Main.java
│   ├── gui/
│   │   ├── DashboardApp.java     # application window (JavaFX)
│   │   ├── TrayManager.java      # system tray integration
│   │   └── ProcessIconCache.java # per-process icon lookup
│   └── tracker/
│       ├── Database.java         # local SQLite database
│       ├── WindowMonitor.java    # background active-window tracking service
│       └── Period.java           # date range calculations
```
