# EcoNest — Java Swing Desktop Application

A complete educational desktop solution for the **Higher Diploma in Information Technology — Object Oriented Programming (Java)** EcoNest group project specification. Built with **Java Swing, core Java OOP and local file persistence**. No external libraries, database installation, Spring Boot, Maven or internet are required for the regular features.

## Quick start (Windows + VS Code)

1. Extract `EcoNest_Java_Swing.zip`.
2. Install **JDK 17 or newer** (a JRE alone is not sufficient). Confirm in Command Prompt: `java -version` and `javac -version`.
3. Open the extracted `EcoNest_Java_Swing` folder in **VS Code**. You can use the *Extension Pack for Java* for editing.
4. Double-click `run-windows.bat` (or run it from VS Code terminal: `.\run-windows.bat`). The script compiles the Java source and launches the desktop application.
5. For macOS/Linux run `./run-mac-linux.sh` in Terminal.

**Manual commands in VS Code Terminal (Windows PowerShell):**

```powershell
mkdir build -ErrorAction SilentlyContinue
javac -encoding UTF-8 -d build src/EcoNestApp.java
java -cp build EcoNestApp
```

## Modules and assignment tasks

| Task | Feature | Where to find it |
|---|---|---|
| 1 | Product CRUD, category, LKR pricing, 1–5 eco-rating | Products |
| 2 | Household / office client CRUD and contact details | Clients |
| 3 | Quantity, reorder level, supplier stock-in, stock history | Inventory |
| 4 | Delivery agent CRUD and vehicle information | Delivery Agents |
| 5 | Create client orders, select multiple products, automatic totals, stock deduction | Orders |
| 6 | Assign agent, schedule delivery, track status | Deliveries |
| 7 | Monthly revenue, orders, inventory / low-stock report, CSV export | Reports |
| 8 | Queue dispatch email to customer when order is marked Dispatched or Out for Delivery | Deliveries → Notifications |
| 9 | Queue delivery assignment email to agent when agent is assigned | Deliveries → Notifications |

Other features: a responsive-to-resizing desktop dashboard, automatic saving, demo product records, confirmation before deletion, data validation, no deleting related clients/products/agents, automatic stock restoration on order cancellation, and order detail view.

## Important: saving data

Changes are **automatically persisted** in a Java serialization file:

- Windows: `C:\Users\YOUR_USERNAME\.econest\econest-data.ser`
- macOS / Linux: `~/.econest/econest-data.ser`

This is a **local-file storage implementation**, not a relational database. The specification requires a Java Swing application and does not explicitly specify MySQL, JDBC, or a DBMS. If your lecturer separately requires MySQL or JDBC, this app would need a persistence-layer upgrade before submission. Back up this `.ser` file to keep your records. Demo data is created only the first time you start the app. Demo email addresses ending in `example.com` are placeholders, not real inboxes.

## Sending real email

The Notifications page records new assignment and dispatch notifications automatically. By default they stay **Pending** until SMTP credentials are configured; email is not silently sent without a mail account.

1. Click **Email Settings** at the top right.
2. Enter your SMTP **SSL/TLS (implicit TLS)** server address, port (for example, `smtp.gmail.com`, port `465`), sender email address and your mail provider's **app-specific password**.
3. In **Notifications**, select a pending notification and click **Send Selected** or **Send All Pending**.
4. When credentials have already been configured for the current session, new assignment / dispatch notifications are attempted immediately. Failed messages remain queued for a retry.

**Security:** SMTP credentials are held in memory for the current session only, never saved to the database. Email sending requires internet and an SMTP provider that supports implicit SSL and AUTH PLAIN. Gmail typically requires 2-step verification and an app password; normal Google account passwords may be rejected. Ensure actual client/agent email addresses are used in place of sample addresses.

## Project architecture / OOP

- UI: `EcoNestApp extends JFrame`, with Java Swing panels, dialogs, data tables and navigation.
- Domain classes: `Product`, `Client`, `Agent`, `Order`, `OrderLine`, `Delivery`, `StockIn`, `Notification`.
- Persistence: `Store` serializes domain objects and handles IDs, demo data and reload.
- Email integration: `MailSettings` and `Mail` use the JDK's `SSLSocket` for SMTP over SSL.
- OOP: classes and objects, composition (Order contains OrderLine list), encapsulation through class member state and methods, inheritance (`JFrame` and `Serializable`), polymorphism via Swing interfaces and listeners.

**Development note:** This version deliberately uses one organized Java source file with nested domain classes to make manual compilation easy. For a final group submission, each member can refactor their allocated module into independent Java files/packages and add unit tests.

## Group submission checklist

- Register your group of **5–8 members**, allocate **tasks 1–9**, and complete the group registration form on the course web.
- Prepare individual **Use Case Diagrams, Class Diagrams, and ER Diagrams** as requested in the assignment.
- Test all features with your own records and real email recipients before the final project demonstration.
- Refer to `docs/REQUIREMENTS_AND_TESTS.md` for a feature-by-feature demonstration checklist.

## Limitations

This is a single-user educational desktop app rather than a multiuser server system. There is no login/access control in the provided specification. Financial totals reflect order creation month, not payment settlement. File persistence is not suitable for simultaneous editing by multiple computers. Reports export CSV files, not PDF. The email integration supports SMTP SSL on port 465, not STARTTLS on port 587.
