# Architecture and Protocol

## Modules

The project is a single Gradle build with four subprojects:

- **`shared`**: the domain model both ends must agree on (`MenuItem`, `OrderItem`, `Order`, `OrderStatus`), the fixed menu catalog, and the message classes sent over the wire. Putting the menu here means the customer client and the server read the exact same item names, prices and customization options instead of two copies that could drift apart.
- **`server`**: accepts socket connections from both kinds of client, holds the live order queue, and broadcasts events when orders are created or change status.
- **`client-customer`**: the JavaFX app that runs full-screen on a table's tablet, for browsing the menu, customizing items, placing an order, and watching that order's status update live.
- **`client-kitchen`**: the JavaFX app for the kitchen, showing every order on a board grouped by status, with a button per order to advance it to the next status.

> **Diagram:** a box labeled "shared" at the top, with arrows pointing down into three boxes below it labeled "server", "client-customer", and "client-kitchen" (all three depend on shared). A separate pair of arrows connects "client-customer" and "client-kitchen" to "server" directly, labeled "TCP socket", showing the clients only ever talk to the server, never to each other.

## Why plain sockets and Java serialization

The clients and the server are all JVM processes, so there's no need for a text format like JSON or a cross-language framework. A `java.net.Socket` with an `ObjectOutputStream`/`ObjectInputStream` pair lets whole message objects be sent and received directly, with no extra library and no hand-written parsing to get wrong. This is the "keep it practical" choice from [02-requirements-and-scope.md](02-requirements-and-scope.md): it is the simplest thing that actually satisfies the requirement, not the simplest thing that merely looks simple.

## Order lifecycle

```
PLACED -> PREPARING -> READY -> SERVED
```

Each arrow is one step a kitchen staff member triggers by tapping a button on that order's card. There is no way to skip a step or go backwards; this matches a real kitchen workflow and avoids a bigger state machine than the brief asks for.

## Wire protocol

All messages are `Serializable` objects defined in `shared`, sent one per `writeObject` call:

| Message | Direction | Purpose |
|---|---|---|
| `HandshakeMessage(role, tableNumber)` | client -> server, first message on connect | Identifies the connection as a `CUSTOMER` (with its table number) or a `KITCHEN` display. |
| `OrderSnapshotMessage(orders)` | server -> client, right after handshake | Gives a newly connected kitchen display the full current order board, so it isn't blank until the next event. |
| `PlaceOrderMessage(order)` | customer client -> server | Submits a new order for that table. |
| `OrderCreatedEvent(order)` | server -> all kitchen displays | A new order has been placed and should appear on the board. |
| `UpdateStatusMessage(orderId, newStatus)` | kitchen client -> server | Staff advanced an order to its next status. |
| `OrderStatusChangedEvent(order)` | server -> all kitchen displays, and -> the owning table's customer client | The order's status changed; kitchen displays refresh that card, and the customer client updates its own status screen if it's the table that placed it. |

> **Sequence:** A customer at table 4 builds an order and taps "Place Order". The client sends a `PlaceOrderMessage` to the server over its socket. The server's order store assigns an order id, sets its status to `PLACED`, stores it, then sends an `OrderCreatedEvent` to every connected kitchen display and an `OrderStatusChangedEvent` back to table 4's own client so it can show "Order received." When kitchen staff tap "Start Preparing" on that order's card, the kitchen client sends `UpdateStatusMessage(orderId, PREPARING)`. The server updates the stored order and sends `OrderStatusChangedEvent` to every kitchen display and to table 4's client, which updates its status screen to "Preparing" without the customer doing anything.

## Concurrency

The server runs a connection-accept loop on the main thread; each accepted socket gets its own thread reading messages from that client in a loop. The order store is a thread-safe map keyed by order id, and the list of connected clients (used for broadcasting) is a thread-safe collection too, so one table placing an order and another table's status update can happen at the same time without corrupting either client's view or blocking either connection.

## Layering

Within both the server and each client, networking code (reading/writing sockets), business/domain logic (the order store and its state transitions), and UI code (JavaFX scenes and controls) are kept in separate classes rather than mixed together. The UI layer only ever reacts to events handed to it by the networking layer (via `Platform.runLater`, since JavaFX requires UI updates to happen on its own application thread) and only ever triggers actions through the networking layer, never touching sockets directly. This separation is what the "Enterprise System Design" mark component is aimed at: a change to the wire protocol shouldn't require rewriting the UI, and a change to the UI shouldn't require touching the socket code.
