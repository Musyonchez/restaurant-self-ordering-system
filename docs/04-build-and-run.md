# Build and Run

## Project layout

```
restaurant-self-ordering-system/
  settings.gradle, build.gradle        root Gradle build, four subprojects
  gradlew, gradlew.bat, gradle/        self-contained Gradle wrapper (Gradle 9.6.0)
  shared/                               domain model, menu catalog, wire protocol
    src/main/java/school/swe2040/restaurant/shared/
      model/        MenuCategory, Size, SpiceLevel, MenuItem, OrderItem, Order, OrderStatus
      menu/          MenuCatalog (the one fixed menu both ends read)
      protocol/      Message and the six message classes from 03-architecture-and-protocol.md
      NetworkConfig  shared default host/port
    src/test/java/   OrderStatusTest, OrderItemPricingTest
  server/                               socket server
    src/main/java/school/swe2040/restaurant/server/
      store/OrderStore      thread-safe order queue and state-machine logic
      net/                  ConnectionRegistry, ClientConnection, RestaurantServer
      ServerMain            entry point
    src/test/java/   OrderStoreTest (state machine + concurrency, no sockets/JavaFX)
  client-customer/                      JavaFX app, the table's tablet
    src/main/java/school/swe2040/restaurant/customer/
      net/ServerConnection   networking layer only
      ui/                    TableSetupPane, OrderingPane, MenuItemCard, CustomizeDialog, OrderStatusCard
      app/CustomerApp        entry point
    src/main/resources/customer.css
  client-kitchen/                       JavaFX app, the kitchen display
    src/main/java/school/swe2040/restaurant/kitchen/
      net/KitchenConnection  networking layer only
      ui/                    BoardPane, OrderCard
      app/KitchenApp         entry point
    src/main/resources/kitchen.css
  docs/                                  01-04, this file is the last of the set
```

Layering matches 03-architecture-and-protocol.md: in the server and in each client, networking classes
(`net/`) never contain business logic or UI code, the order store/state machine (`store/`) never touches
a socket or a JavaFX class, and UI classes (`ui/`, `app/`) only call methods on the networking classes and
only react to events those classes hand them (via `Platform.runLater`), never opening a socket themselves.

## Prerequisites

- JDK 17.
- No separate Gradle install needed, the project carries its own wrapper (`gradlew`/`gradlew.bat`), already
  bootstrapped against Gradle 9.6.0.
- The JavaFX platform jars (version 17.0.9) are pulled in automatically by the `org.openjfx.javafxplugin`
  Gradle plugin the first time the `client-customer` or `client-kitchen` module is built; this needs
  network access once, after that Gradle's cache has them.

Every command below assumes the current directory is the project root
(`restaurant-self-ordering-system/`) and that `JAVA_HOME` points at a JDK 17 install. On the machine this
was built on, that means setting it explicitly first, since the system default `JAVA_HOME` is JDK 8:

```
set JAVA_HOME=C:\Program Files\Java\jdk-17        (cmd.exe)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"   (PowerShell)
```

## Build

```
gradlew.bat build
```

Compiles all four subprojects, runs the unit tests in `shared` and `server`, and packages each module's
jar (plus run scripts for `server`, `client-customer` and `client-kitchen`, since they all apply the
`application` plugin).

To just compile without running tests:

```
gradlew.bat compileJava
```

## Run

Three separate processes, each in its own terminal, started from the project root. Start the server
first, it must already be listening before either client connects.

```
gradlew.bat :server:run
```

Listens on port 5050 by default (`school.swe2040.restaurant.shared.NetworkConfig.DEFAULT_PORT`). Pass a
different port as an argument if needed, e.g. `gradlew.bat :server:run --args="6000"`.

```
gradlew.bat :client-customer:run
```

Opens the customer tablet app full-screen. It asks for a table number on first launch, then connects to
`localhost:5050` and switches to the ordering screen. Running it more than once (in separate terminals)
simulates multiple tables, each should be given a different table number.

```
gradlew.bat :client-kitchen:run
```

Opens the kitchen display full-screen and connects straight away, no setup screen, showing a status board
with one column per order state.

Press `Esc` to exit full-screen on either client window if you need to see window controls during a demo;
closing the window (or the terminal running its Gradle process) disconnects that client cleanly.

## What was verified, and how

Everything below was actually run on the build machine during development, not assumed:

1. **Build.** `gradlew.bat build` completes with `BUILD SUCCESSFUL`, compiling `shared`, `server`,
   `client-customer` and `client-kitchen` and running every unit test, with no errors.
2. **Server starts and listens.** `gradlew.bat :server:run` was started in the background; its own log
   line ("Restaurant server listening on port 5050.") appeared, and a raw TCP connection attempt to
   `localhost:5050` while it was running succeeded (`Test-NetConnection` reported
   `TcpTestSucceeded: True`). The server was then stopped. It was also confirmed to handle an unexpected
   disconnect (a probe connection that closed without sending a handshake) without crashing, logging
   "Unidentified client disconnected." and continuing to run.
3. **Both JavaFX clients launch without an immediate crash.** This machine has a real desktop session (not
   a headless container), so an attempt to actually open a window is meaningful here. Both
   `gradlew.bat :client-customer:run` and `gradlew.bat :client-kitchen:run` were started, alongside the
   server. The kitchen client's process actually completed its socket handshake with the server: the
   server's own log recorded `Kitchen display connected.`, which only happens after
   `KitchenConnection.connect(...)` succeeds and the kitchen app's UI layer is already running to receive
   the resulting `OrderSnapshotMessage`. The customer client's Gradle `run` task was confirmed to reach
   the `:client-customer:run` task (past all compilation and classpath resolution) and stay running for
   over 40 seconds with no exception or stack trace in its output, consistent with it sitting in its
   JavaFX event loop showing the table-setup screen (which, unlike the kitchen client, doesn't connect to
   the server until a table number is submitted, so no corresponding server-side log line is expected for
   it at this stage). Both processes were then stopped.
4. **Unit tests pass.** The `shared` and `server` test suites (order lifecycle rules, order line pricing,
   and the order store including a concurrent-access test with 8 threads racing to advance the same order)
   all pass as part of step 1's `build` run.
5. **Build artifacts removed.** `build/` (root and all four subprojects) and `.gradle/` were deleted after
   verification, so the committed project carries only source, config and docs.

**What was not, and could not be, verified from this environment:** actual visual rendering of either
JavaFX window. This session has no tool that can screenshot a native desktop window (only a browser-page
screenshot tool, which doesn't apply to a JavaFX desktop app), so "the window opened, didn't crash, and
the kitchen client successfully talked to the server over its socket" is as far as the verification above
goes. Things that depend on actually looking at the rendered UI, correct on-screen layout at full-screen
resolution, CSS rendering as intended, dialog positioning, touch-target feel on an actual touch screen, or
clicking through the full customer flow (choosing a table number, browsing the menu, customizing an item,
placing an order, watching its status update, and the kitchen side advancing that order through its
column) were exercised through code review, not through driving the GUI by hand. Running the three
processes side by side and clicking through that flow once (and comparing what both clients eventually
display) is worth doing before the presentation on 3rd December.

## Known scope and deviations

- No deviation from `01-project-brief.md`, `02-requirements-and-scope.md` or
  `03-architecture-and-protocol.md` was required; the JavaFX plugin resolved normally (the
  `javafx-controls` platform jars fallback described in the task brief was not needed).
- The pricing rules (a flat fee per selected add-on, a price delta per size) and the exact default port
  (5050) are implementation details the three docs left unspecified; they live in `MenuItem`, `OrderItem`
  and `NetworkConfig` respectively and are not meant to be load-bearing design decisions, just values that
  had to be picked for the menu and the handshake to work at all.
