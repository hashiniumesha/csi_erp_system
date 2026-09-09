# Running this as a real desktop app (no terminal)

Day-to-day development still uses `mvnw spring-boot:run` / `mvnw javafx:run`
in two terminals, same as always - nothing about that changed.

For a demo (or anyone who just wants to double-click an icon instead of
running Maven commands), there's now a proper packaged version.

## Build it

From the project root (needs a JDK installed, same one you already build
with):

```bash
./build-desktop-app.ps1
```

This takes a minute or two and produces:

```
frontend/target/dist/
  "Ceylon Sweets Island ERP"/
    Ceylon Sweets Island ERP.exe   <- double-click this
    runtime/                       <- its own bundled Java, nothing extra to install
    app/
  backend/
    erp-backend.jar
```

`target/` is git-ignored, so this build output never goes into the repo -
everyone regenerates it locally by running the script.

## Run it

Just double-click `Ceylon Sweets Island ERP.exe`. No terminal window opens
- if the backend isn't already running, the app starts it invisibly by
itself, shows a short "Starting…" screen while it comes up, then goes
straight to the login screen. Closing the app also shuts the backend back
down if the app was the one that started it.

**This still needs MySQL already installed, the MySQL80 service running,
and the `csi_erp_db` database set up** (schema + `seed_roles.sql` +
`seed_demo_users.sql` + the expiry-batch migration - see the other .sql
files in `backend/src/main/resources/`). Packaging the app doesn't touch
that requirement - if MySQL isn't ready, the app now shows a plain-language
message explaining that, instead of a raw error stack trace.

## Why this exists / how it works

- The `.exe` is built with `jpackage` (comes with the JDK) and bundles a
  full Java runtime inside it, so the machine running it doesn't need Java
  installed separately.
- `Launcher.java` is a small workaround for a JavaFX quirk: a packaged
  app's main class can't directly be the `Application` subclass
  (`MainApp`) or JavaFX refuses to start with "JavaFX runtime components
  are missing" - `Launcher` just hands off to `MainApp.main()` immediately.
- `BackendLauncher.java` is what makes it one icon instead of two: it only
  activates when running from the packaged `.exe` (it checks for a system
  property jpackage sets automatically, `jpackage.app-path` - a plain
  `mvnw javafx:run` never has that set, so this is fully inert during
  normal development), and starts `backend/erp-backend.jar` using the
  app's own bundled Java, with no console window.

## What this doesn't solve

A completely fresh, untouched PC that has never had MySQL or this project
on it - packaging the frontend doesn't install MySQL or seed the database.
If the plan is to demo on a laptop that already has this project's MySQL
set up (the normal case), the above is enough. Getting it running from
zero on a brand new machine is a separate, bigger piece of work (bundling
or auto-installing MySQL, auto-creating the schema and data) - ask if that
actually turns out to be needed.
