# UI Polish Pass

The brief only required the customer client to be full-screen and touch-friendly, and said nothing about visual design beyond that. Everything in this document is a deliberate design pass on top of the working functionality in [04-build-and-run.md](04-build-and-run.md), done in two parts, one per client, so each could be researched and built against its own real-world reference point instead of guessing at what "modern" means.

## Customer ordering client

Researched against general kiosk/food-ordering UI guidance (clear category navigation, prominent price and customization cues, dietary/spice icons, a visible human fallback). Applied as:

- A warm terracotta/amber palette on a cream background, replacing JavaFX's default Modena gray, defined as named custom CSS properties instead of scattered hex values.
- Inter (five weights) bundled locally as real `.ttf` files under `src/main/resources/customer/fonts/`, loaded via `@font-face` and pre-registered in code, replacing the implicit system-font fallback.
- Pill-style category navigation with an icon per category, menu cards showing hint chips for which customizations apply, a redesigned cart panel with an item-count badge and per-row customization summaries, and the order-status screen rebuilt as a real four-step progress stepper (filled circles, checkmarks, a connecting bar) instead of a row of colored labels.
- Fixed a real pre-existing bug found while in there: the customization dialog was never actually attaching `customer.css`, so it rendered in plain default styling regardless of the rest of the app.

## Kitchen display client

Researched against real commercial Kitchen Display Systems (Toast, Lightspeed, Fleksa, Shift4), which consistently use a dark, high-contrast board with color-coded order status plus a separate aging signal tied to how long an order has been waiting. Applied as:

- A dark background with four distinct status colors (PLACED/PREPARING/READY/SERVED), kept visually separate from a separate yellow/red aging highlight that only appears once an order has been sitting too long, computed from the existing order timestamp already in the data model (no protocol or server changes).
- Manrope and JetBrains Mono bundled locally as real `.ttf` files under `src/main/resources/kitchen/fonts/` (compiled from Google's variable-font sources with `fonttools`, since JavaFX cannot load the woff2 files Google Fonts serves directly).
- A live header clock and a one-second refresh so each order card's elapsed timer and aging highlight stay current without needing a new event from the server.
- Order cards restructured for at-a-glance legibility from a few feet away: a large monospace table number, a clear item list with customizations on their own line, and a single full-width action button per card, colored by the status it advances into.

## What was and wasn't verified

Both passes compile cleanly together (`gradlew build`, run after both were done, succeeded with no errors) and both clients were launched individually and confirmed to start without exceptions, with the kitchen client additionally confirmed to complete a real socket handshake with the server while running. Neither pass changed anything in the server or shared model code beyond the one build-config fix below, and neither touched the networking layer (`ServerConnection.java`, then still two separate classes, one per client, later merged into one as part of the project's restructure into a single Gradle module) or the data model.

As with the rest of this project (see [04-build-and-run.md](04-build-and-run.md)), nothing in this session can screenshot a native JavaFX window, so the actual rendered result (colors as perceived, font rendering, layout at full-screen resolution, hover/pressed states) was not visually confirmed, only that the apps build and run without throwing. Click through both clients' full flow at least once before the presentation.

## Incidental build fix

While working on the customer client, a real (not cosmetic) bug surfaced: the UI code uses literal Unicode icon glyphs, and without an explicit compiler encoding, `javac` fell back to this machine's default `windows-1252` encoding, which either failed outright or would have silently corrupted other glyphs. `tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }` was added to `build.gradle` to fix this project-wide, not just for the one source file that hit it. (At the time this was a `subprojects {}` block across four subprojects; the project has since been restructured into a single module, and the same fix now applies directly, see `04-build-and-run.md`.)
