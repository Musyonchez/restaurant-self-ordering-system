# Requirements and Scope Decisions

The brief in [01-project-brief.md](01-project-brief.md) states what the system must do and how it will be marked, but it does not spell out several implementation details a real build needs. This document records the decisions made to fill those gaps, so the rest of the project has a single source of truth instead of each part guessing independently.

## Explicit requirements (from the brief)

- Separate client and server parts.
- Desktop client built with JavaFX.
- Gradle build.
- Runs without raising errors.
- Customer interface simulates a permanently mounted tablet: full-screen, touch-friendly.
- Kitchen Display shows incoming orders in real time and lets staff track progress.
- Communication between customer interfaces and the kitchen display must stay reliable and responsive under high demand (many tables ordering at once).

## Decisions filling the gaps the brief leaves open

**Table identity.** Each table's tablet is physically fixed, so a real deployment would hardcode one table number per device. Since this project runs several "tablets" on one development machine for demonstration, the customer client asks for a table number on a small setup screen the first time it launches, then proceeds straight to full-screen ordering. This keeps the one-tablet-per-table reality of the brief while staying runnable as multiple instances on one machine.

**Menu content.** The brief does not supply a menu, so a fixed, realistic menu (starters, mains, drinks, desserts) with a few customizable options per item (size, spice level, add-ons, a free-text note) is defined once and shared by both the customer client and the server, so neither can drift out of sync with the other.

**Order lifecycle.** Orders move forward through four states: `PLACED` (customer submitted it), `PREPARING` (kitchen started on it), `READY` (kitchen finished it), `SERVED` (a staff member marked it delivered). Kitchen staff advance an order one step at a time from the Kitchen Display; there is no reject/cancel step, since the brief doesn't ask for one and adding it would be scope beyond what's marked.

**Real-time communication.** Each client keeps a persistent TCP socket connection open to the server rather than polling, since polling would add latency and the brief specifically calls for "reliable and responsive" behaviour under load. The server pushes order and status events to clients as they happen instead of waiting to be asked. See [03-architecture-and-protocol.md](03-architecture-and-protocol.md) for the exact message flow.

**Customer-side status feedback.** Nothing in the brief required showing the customer their order's progress, but the brief's own stated goal is to "enhance the customer experience," and a tablet that stays silent after the order is placed doesn't deliver that. The server also pushes status changes back to the table that placed the order, so the customer client can show "Preparing", "Ready" and so on. This is a small addition in direct service of a goal the brief already states, not scope creep.

**Concurrency under load.** The server accepts each client connection on its own thread and keeps the shared order store behind thread-safe collections, so one table placing an order can't block another table's connection or corrupt the kitchen's view of the queue.

**What is deliberately left out of scope.** No persistence (orders live in memory only and reset when the server restarts), no authentication or login, no admin screen for editing the menu, no payment handling, no support for multiple restaurant locations. None of these are asked for in the brief or covered by the mark scheme, and adding them would be effort spent outside what's being graded rather than inside it.

## How the scope maps to the mark scheme

| Mark component | How it's addressed |
|---|---|
| UI and UX Design (6) | Full-screen, large-touch-target customer ordering flow; a clear status-board Kitchen Display grouped by order state. |
| Working Functionality (10) | End-to-end flow: browse menu, customize, place order, kitchen sees it live, kitchen advances status, customer sees status update live. |
| Enterprise System Design (5) | Client/server split with a shared domain model, a defined wire protocol, and separation between networking, business logic, and UI layers (see [03-architecture-and-protocol.md](03-architecture-and-protocol.md)). |
| Solution Practicality (4) | Plain Java sockets and Java's built-in object serialization for the protocol, no external messaging framework or database, kept proportionate to a class project rather than over-built. |
