# Build and Run

## Project layout

```
restaurant-self-ordering-system/
  settings.gradle, build.gradle        single-module Gradle build
  gradlew, gradlew.bat, gradle/        self-contained Gradle wrapper (Gradle 9.6.0)
  src/main/java/school/swe2040/restaurant/
    client/        CustomerApp, KitchenApp (JavaFX entry points), ServerConnection (networking layer)
    server/        RestaurantServer, ClientHandler, ConnectionRegistry, OrderManager
    models/        MenuCategory, Size, SpiceLevel, MenuItem, OrderItem, Order, OrderStatus, MenuCatalog
    protocol/       Message and the six message classes from 03-architecture-and-protocol.md
    customer/      TableSetupPane, OrderingPane, MenuItemCard, CustomizeDialog, OrderStatusCard, OrderItemFormatter
    kitchen/       BoardPane, OrderCard
    NetworkConfig  shared default host/port
  src/main/resources/
    customer/      customer.css, bundled Inter font files
    kitchen/       kitchen.css, bundled JetBrains Mono / Manrope font files
  src/test/java/school/swe2040/restaurant/
    models/        OrderStatusTest, OrderItemPricingTest
    server/        OrderManagerTest (state machine + concurrency, no sockets/JavaFX)
    integration/   EndToEndOrderFlowTest (drives the real server and ServerConnection over an
                    actual socket: place order -> kitchen receives it -> status update ->
                    customer receives it; and a late-connecting kitchen gets a full snapshot)
  docs/            01-06, this file is the build/run guide
```

The server, both clients, the shared models/protocol classes, and all tests live in one Gradle
module (not four, as an earlier draft of this project had), so model and protocol classes are never
duplicated or allowed to drift between the server and the clients. Layering still matches
03-architecture-and-protocol.md: `client/ServerConnection` and `server/ClientHandler` never contain
business logic or UI code, `server/OrderManager` never touches a socket or a JavaFX class, and UI
classes (`customer/`, `kitchen/`) only call methods on `ServerConnection` and only react to events it
hands them (via `Platform.runLater`), never opening a socket themselves.

## Prerequisites

- JDK 17.
- No separate Gradle install needed, the project carries its own wrapper (`gradlew`/`gradlew.bat`),
  already bootstrapped against Gradle 9.6.0.
- The JavaFX platform jars (version 17.0.9) are pulled in automatically by the
  `org.openjfx.javafxplugin` Gradle plugin the first time the project is built; this needs network
  access once, after that Gradle's cache has them.

Every command below assumes the current directory is the project root
(`restaurant-self-ordering-system/`) and that `JAVA_HOME` points at a JDK 17 install. On the machine
this was built on, that means setting it explicitly first, since the system default `JAVA_HOME` is
JDK 8:

```
set JAVA_HOME=C:\Program Files\Java\jdk-17        (cmd.exe)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"   (PowerShell)
```

## Build

```
gradlew.bat build
```

Compiles the project, runs every unit and integration test, and packages the jar plus a run script
(since the project applies the `application` plugin).

To just compile without running tests:

```
gradlew.bat compileJava
```

## Run

Three separate processes, each in its own terminal, started from the project root. Start the server
first, it must already be listening before either client connects.

```
gradlew.bat runServer
```

Listens on port 5050 by default (`school.swe2040.restaurant.NetworkConfig.DEFAULT_PORT`). Pass a
different port as an argument if needed, e.g. `gradlew.bat runServer --args="6000"`.

```
gradlew.bat runCustomer
```

Opens the customer tablet app, windowed by default at 1280x800. It asks for a table number on first
launch, then connects to `localhost:5050` and switches to the ordering screen. Running it more than
once (in separate terminals) simulates multiple tables, each should be given a different table
number. Press **F11** to go full-screen, simulating the permanently mounted tablet the brief
describes; press it again (or Esc, while full-screen) to return to windowed.

```
gradlew.bat runKitchen
```

Opens the kitchen display, windowed by default at 1400x850, and connects straight away, no setup
screen, showing a status board with one column per order state. Press **F11** to go full-screen for
an actual kitchen-mounted display.

Closing a client window (or the terminal running its Gradle process) disconnects that client cleanly.

There is also a plain `gradlew.bat run` task, equivalent to `runCustomer` (it is the `application`
plugin's default task and happens to point at `CustomerApp`); prefer the three named tasks above
since they make which process you are starting explicit.

### Note on the three run tasks and JavaFX

`runCustomer` and `runKitchen` each compute and pass their own `--module-path`/`--add-modules` JVM
args in `build.gradle`, rather than relying on the `org.openjfx.javafxplugin` to add them
automatically: that plugin only patches Gradle's own `run` task by name, not custom-named `JavaExec`
tasks, so without this a custom task fails immediately with "JavaFX runtime components are missing"
even though the jars are on the classpath. If a future change renames these tasks or adds a new
JavaFX entry point, it needs the same `jvmArgs` block, not just a new `JavaExec` registration.

## What was verified, and how

Everything below was actually run on the build machine, not assumed:

1. **Build.** `gradlew.bat build` completes with `BUILD SUCCESSFUL`, compiling the project and
   running every unit and integration test, with no errors.
2. **Server starts and listens.** `gradlew.bat runServer` was started in the background; its own log
   line ("Restaurant server listening on port 5050.") appeared, and a raw TCP connection attempt to
   `localhost:5050` while it was running succeeded. The server was also confirmed to handle an
   unexpected disconnect (a probe connection that closed without sending a handshake) without
   crashing, logging "Unidentified client disconnected." and continuing to run.
3. **Both JavaFX clients actually launch and render.** This machine has a real desktop session, so
   `gradlew.bat runCustomer` and `gradlew.bat runKitchen` were each started and confirmed, by
   inspecting the live OS process list, to produce a window with the expected title ("Table
   Ordering" / "Kitchen Display") and the expected windowed (not full-screen) size. This is a
   stronger check than process-didn't-crash: it confirms JavaFX's module-path setup genuinely works
   for all three Gradle tasks, not just the default `run` task.
4. **The full order round trip works, over a real socket, not a mock.** `EndToEndOrderFlowTest`
   starts a real `RestaurantServer`, connects a customer `ServerConnection` and a kitchen
   `ServerConnection` to it, places an order, and asserts the kitchen receives it with a real
   server-assigned id; advances its status from the kitchen side and asserts the customer receives
   that update; and separately confirms a kitchen client connecting after an order already exists
   still receives it in its startup snapshot. This is the one thing unit tests on the model classes
   alone cannot catch: that the customer -> server -> kitchen -> server -> customer path still works
   after a refactor to `ServerConnection`, `ClientHandler`, `ConnectionRegistry`, or the protocol
   classes.
5. **A dropped connection is reported, not silently swallowed.** `ServerConnection.send()` used to
   catch a failed write and just log it to stderr, with no path back to the UI; `OrderingPane`
   unconditionally cleared the customer's cart after calling `placeOrder()` regardless of whether it
   actually reached the server. Fixed: `placeOrder`/`advanceOrder` now return whether the send
   succeeded, a failed send closes the connection and fires the existing disconnect notification
   (guarded so a near-simultaneous failure on the read side can't double-fire it), and the customer's
   cart is only cleared on a confirmed send, with an on-screen error otherwise telling them nothing
   was lost. This was found by actually reasoning through what happens when a send fails, not by
   forcing a real socket failure in a test; the regression suite above confirms the happy path still
   behaves identically.
6. **Unit tests pass.** `OrderStatusTest`, `OrderItemPricingTest`, and `OrderManagerTest` (order
   lifecycle rules, order line pricing, and the order store including a concurrent-access test with
   8 threads racing to advance the same order) all pass as part of the `build` run.
7. **Build artifacts removed.** `build/` and `.gradle/` were deleted after verification, so the
   committed project carries only source, config and docs.

**What was not, and could not be, verified from this environment:** clicking through the full
customer flow by hand (choosing a table number, browsing the menu, customizing an item, placing an
order from the actual UI, and watching the kitchen side advance it through its column), since doing
that reliably requires driving the GUI by mouse/keyboard rather than through code, which risks
interfering with whatever else is happening on the machine it's running on. The networking layer
that flow depends on is covered end-to-end by `EndToEndOrderFlowTest` instead. Running the three
processes side by side and clicking through that flow once (and comparing what both clients
eventually display) is still worth doing by hand before the presentation on 3rd December.

## Known scope and deviations

- No deviation from `01-project-brief.md`, `02-requirements-and-scope.md` or
  `03-architecture-and-protocol.md` was required; the JavaFX plugin resolved normally.
- The pricing rules (a flat fee per selected add-on, a price delta per size) and the exact default
  port (5050) are implementation details the three docs left unspecified; they live in `MenuItem`,
  `OrderItem` and `NetworkConfig` respectively and are not meant to be load-bearing design decisions,
  just values that had to be picked for the menu and the handshake to work at all.
- The project was restructured from an earlier four-module layout (`shared`/`server`/
  `client-customer`/`client-kitchen`) into the single module described above, to match
  `06-group-work-distribution.md`'s recommended structure. Nothing was dropped in that move; see that
  doc and the project's git history for what changed and why.
