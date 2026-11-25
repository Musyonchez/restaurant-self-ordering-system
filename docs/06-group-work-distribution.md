# Group Work Distribution: Six Members

The project will consist of two JavaFX desktop applications connected through a central Java server: a full-screen, touch-friendly customer ordering interface installed at each restaurant table, and a kitchen display interface used by kitchen staff to receive and manage orders.

## 1. Allocation of Responsibilities

### PERSON 1 - Project Lead and Server Developer

**Main focus:** Backend development and system integration

- Set up and maintain the shared Gradle project structure.
- Develop the central Java server.
- Implement client-server communication using Java sockets or another agreed networking approach.
- Handle multiple client connections and incoming order requests.
- Ensure orders are transmitted from customer interfaces to the kitchen display.
- Coordinate integration of all components and maintain the shared project repository.

**Deliverables:** Working server, client connection handling, communication functionality and integrated project configuration.

### PERSON 2 - Customer Interface Developer

**Main focus:** Customer-side JavaFX UI/UX

- Develop the customer-facing JavaFX application interface.
- Design the home screen and menu browsing screens.
- Display food categories, menu items, descriptions and prices.
- Create clear navigation and touch-friendly buttons and controls.
- Configure the application to run in full-screen mode to simulate a permanently mounted restaurant tablet.
- Apply consistent styling and an attractive, easy-to-use layout.

**Deliverables:** Customer home screen, menu interface, navigation and full-screen tablet-style experience.

### PERSON 3 - Ordering, Customization and Cart Developer

**Main focus:** Customer-side ordering functionality

- Implement menu-item selection and adding items to the cart.
- Allow customers to increase or decrease item quantities.
- Implement item removal from the cart.
- Provide available customization options such as size, toppings and special instructions.
- Calculate item subtotals and the overall order total.
- Implement order submission to the server using the agreed order format.

**Deliverables:** Functional shopping cart, item customization, quantity management, price calculations and order submission.

### PERSON 4 - Kitchen Display Developer

**Main focus:** Kitchen-side JavaFX UI/UX

- Develop the JavaFX kitchen dashboard.
- Display incoming orders as they are received.
- Show order IDs, table numbers, ordered items, quantities and special instructions.
- Organize orders according to their current status.
- Provide controls for kitchen staff to update order statuses, such as New, Preparing and Ready.
- Ensure the kitchen interface reflects incoming orders and status changes promptly.

**Deliverables:** Kitchen dashboard, incoming-order display and order status management interface.

### PERSON 5 - Data Models and Order Processing Developer

**Main focus:** Data management and business logic

- Create shared Java model classes, including MenuItem, Order, OrderItem and Table.
- Define the structure and fields used to represent an order.
- Implement unique order identification and timestamps.
- Implement order validation and status-transition rules.
- Manage menu information and order data using in-memory storage or a simple persistence solution.
- Work with the server developer and customer-ordering developer to ensure all components use the same data structures.

**Deliverables:** Shared Java model classes, menu data, order validation and order-processing logic.

### PERSON 6 - Quality Assurance, Documentation and Presentation

**Main focus:** Testing, documentation and final delivery

- Prepare test cases for the customer application, kitchen display and server communication.
- Test each component as it becomes available and report errors to the relevant developer.
- Test the complete ordering process from customer selection to kitchen receipt.
- Test multiple orders and multiple connected customer clients.
- Verify that order details, table numbers, totals and status updates are correct.
- Prepare project documentation, screenshots and instructions for running the application.
- Prepare presentation slides and assist with final project packaging and submission checks.

**Deliverables:** Test cases and results, defect reports, project documentation, presentation slides and final submission checklist.

## 2. Shared Project Requirements

- The project must be maintained as one shared Gradle project.
- Both the customer ordering interface and kitchen display must be implemented using JavaFX.
- The customer application must support full-screen operation and a touch-friendly layout.
- The customer application, server and kitchen display must communicate reliably.
- All developers must use the same agreed order data structure and shared model classes.
- The complete application must build and run without errors.
- All members must commit their work to the shared repository and communicate integration issues promptly.

## 3. Recommended System Structure

```
RestaurantOrderingSystem/
├── build.gradle
├── settings.gradle
└── src/main/
    ├── java/com/restaurant/
    │   ├── client/
    │   │   ├── CustomerApp.java
    │   │   ├── KitchenApp.java
    │   │   └── ServerConnection.java
    │   ├── server/
    │   │   ├── RestaurantServer.java
    │   │   ├── ClientHandler.java
    │   │   └── OrderManager.java
    │   ├── models/
    │   │   ├── MenuItem.java
    │   │   ├── Order.java
    │   │   ├── OrderItem.java
    │   │   └── Table.java
    │   ├── customer/
    │   │   ├── CustomerController.java
    │   │   ├── MenuController.java
    │   │   └── CartController.java
    │   └── kitchen/
    │       └── KitchenController.java
    └── resources/
        ├── customer/
        ├── kitchen/
        └── styles/
```

This is a suggested structure. The group may adjust the package and class names, provided the final project remains organized and all components are integrated into the same Gradle project.

## 4. Important Coordination Agreements

- **Agree on the order format.** Every order should contain an order ID, table number, timestamp, items, quantities, customizations, special instructions, total and status.
- **Share model classes early.** Person 5 should make the initial model classes available to the other developers so that the customer application and server use the same data structure.
- **Document the communication protocol.** Person 1 should specify how the customer client submits orders and how the server sends orders and status updates to the kitchen display.
- **Integrate continuously.** Do not wait until every component is complete before connecting the applications. Test communication between components throughout development.
- **Keep network operations off the JavaFX UI thread.** Use background threads for network communication and safely dispatch interface updates to the JavaFX Application Thread.

## 5. Final Functionality Checklist

- [ ] Customer application launches in full-screen mode.
- [ ] Customers can browse menu categories and items.
- [ ] Customers can customize items and add special instructions.
- [ ] Customers can add items to a cart, change quantities and remove items.
- [ ] The system calculates the order total correctly.
- [ ] Customers can submit orders with a table number.
- [ ] The server receives and processes customer orders.
- [ ] The kitchen display receives and displays incoming orders.
- [ ] Kitchen staff can update order statuses.
- [ ] The system handles multiple connected customer clients.
- [ ] Both JavaFX applications and the server build and run without errors.
- [ ] The complete project is packaged as a ZIP for Blackboard submission.

## 6. Assessment Priorities

| Assessment category | Marks | Main focus |
|---|---|---|
| UI and UX Design | 6 | Attractive, consistent and touch-friendly customer and kitchen interfaces. |
| Working Functionality | 10 | Order placement, server communication, kitchen receipt and status updates. |
| Enterprise System Design | 5 | Client-server architecture, modular code, concurrency and data management. |
| Solution Practicality | 4 | Realistic restaurant workflow, table identification, accuracy and reliability. |
| **TOTAL** | **25** | Complete, integrated system. |

Note: Responsibilities are divided by primary ownership. All six members are expected to communicate, support integration and help ensure that the final application works as one complete system.
