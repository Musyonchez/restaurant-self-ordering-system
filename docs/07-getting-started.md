# Getting Started

A plain-language walkthrough for anyone opening this project for the first time: where to get it,
what to install, how to run it, and what you'll actually see once it's running. For deeper detail on
any of this, see the other docs linked at the bottom.

## 1. Getting the code

```
git clone https://github.com/Musyonchez/restaurant-self-ordering-system.git
cd restaurant-self-ordering-system
```

This project lives in its own repository, separate from any other coursework repo, so this clone is
all you need.

## 2. What you need installed

- **JDK 17.** Check with `java -version`. If you have a different version installed too, make sure
  `JAVA_HOME` points at the 17 install before running any command below:
  ```
  set JAVA_HOME=C:\Program Files\Java\jdk-17        (cmd.exe)
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"   (PowerShell)
  ```
- **Nothing else to install.** The project carries its own Gradle wrapper (`gradlew`/`gradlew.bat`),
  and the JavaFX libraries are downloaded automatically the first time you build, which needs
  internet access once. After that, everything is cached locally.

## 3. Running it

The system is three separate programs: a server, a customer app, and a kitchen app. Open three
terminals in the project folder and start them in this order, since the server has to already be
listening before either app connects to it.

```
gradlew.bat runServer
```

```
gradlew.bat runCustomer
```

```
gradlew.bat runKitchen
```

Each one keeps running in its own terminal. To stop one, close its window or press Ctrl+C in its
terminal. If something looks wrong on first run (a build error, a port already in use), see the
troubleshooting notes in [04-build-and-run.md](04-build-and-run.md).

## 4. Navigating the customer app

This simulates the tablet mounted at a restaurant table.

1. **Table setup screen.** Type a table number and press **Start Ordering**. (If you run the
   customer app more than once at a time, give each a different table number to simulate several
   tables at once.)
2. **Browse the menu.** Tabs across the top split items into Starters, Mains, Drinks and Desserts.
   Each item card shows its description, price, and small icons for whichever customizations it
   supports (size, spice level, add-ons).
3. **Customize and add an item.** Press **+ Add to Order** on any card to open a dialog where you
   pick size/spice/add-ons (only shown if that item supports them), set a quantity, and optionally
   add a note, then press **Add to Order**.
4. **Review the cart.** The panel on the right lists everything added so far, with a running total
   at the bottom. Each line can be removed with its ✕ button.
5. **Place the order.** Once the cart isn't empty, press **Place Order**. The cart clears and the
   order appears below, in the **My Orders** strip, as a card with a live step tracker (Placed →
   Preparing → Ready → Served) that updates on its own as the kitchen advances it, no refresh needed.

The window opens at a normal size; press **F11** to go full-screen (simulating the permanently
mounted tablet described in the brief), and F11 again, or Esc while full-screen, to return to a
normal window.

## 5. Navigating the kitchen app

This simulates the display kitchen staff watch.

1. It connects automatically on launch, no setup screen, straight to the board.
2. **Four columns**, one per status: Placed, Preparing, Ready, Served. Every order sits in exactly
   one column at a time.
3. Each order's card shows its table number, every item with its customizations and any note, the
   total, and how long it's been waiting (turning yellow, then red, if it sits too long).
4. Press the button at the bottom of a card to advance it to its next status. Once an order reaches
   Served, it just shows "Served" with no further action.

Press **F11** here too to go full-screen for an actual kitchen-mounted display.

## Where to go next

- [01-project-brief.md](01-project-brief.md) and [02-requirements-and-scope.md](02-requirements-and-scope.md): what was actually asked for.
- [03-architecture-and-protocol.md](03-architecture-and-protocol.md): how the pieces talk to each other.
- [04-build-and-run.md](04-build-and-run.md): the full command reference, what's been verified, and troubleshooting.
- [05-ui-polish.md](05-ui-polish.md): the design reasoning behind how each app looks.
- [06-group-work-distribution.md](06-group-work-distribution.md): who owns which part of this.
