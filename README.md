# Restaurant Self-Ordering System

A networked restaurant ordering system built for SWE2040A (Networked Application Design):
a touch-friendly JavaFX app for customers to order from their table, a JavaFX kitchen display that
receives those orders live, and a plain Java socket server connecting the two.

## What it does

- **Customer app**: pick a table number, browse the menu by category, customize an item (size,
  spice level, add-ons, notes), build a cart, place the order, then watch it move through
  Placed → Preparing → Ready → Served in real time as the kitchen works on it.
- **Kitchen app**: a live board with one column per order status, showing every item, customization
  and note the kitchen needs, how long each order has been waiting, and a single button to advance
  it to its next status.
- **Server**: accepts both kinds of client over plain TCP sockets, holds the live order state, and
  broadcasts every new order and status change to whoever needs to see it.

## Tech stack

Java 17, JavaFX 17 for both desktop clients, `java.net.Socket` with Java serialization for the wire
protocol (no HTTP/JSON needed since every process is a JVM), Gradle (wrapper included, no separate
install needed), JUnit 5 for the test suite including a real socket-level end-to-end test.

## Quick start

```
git clone https://github.com/Musyonchez/restaurant-self-ordering-system.git
cd restaurant-self-ordering-system
gradlew.bat runServer      # in its own terminal, start this first
gradlew.bat runCustomer    # in a second terminal
gradlew.bat runKitchen     # in a third terminal
```

Needs JDK 17; see [docs/07-getting-started.md](docs/07-getting-started.md) for the full walkthrough,
including what you'll actually see in each app.

## Documentation

| Doc | What's in it |
|---|---|
| [01-project-brief.md](docs/01-project-brief.md) | The assignment brief as given |
| [02-requirements-and-scope.md](docs/02-requirements-and-scope.md) | What was actually required vs. left open |
| [03-architecture-and-protocol.md](docs/03-architecture-and-protocol.md) | How the pieces are organized and talk to each other |
| [04-build-and-run.md](docs/04-build-and-run.md) | Full command reference, what's been verified, troubleshooting |
| [05-ui-polish.md](docs/05-ui-polish.md) | The design reasoning behind how each app looks |
| [06-group-work-distribution.md](docs/06-group-work-distribution.md) | Who owns which part of the project |
| [07-getting-started.md](docs/07-getting-started.md) | Plain-language walkthrough for anyone opening this cold |

## Status

Builds and runs cleanly (`gradlew.bat build`), full test suite passing, including an automated
end-to-end test that drives a real server and both clients' networking code over an actual socket.
See 04-build-and-run.md for exactly what has and hasn't been verified.
