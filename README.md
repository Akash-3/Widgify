# Widgify — Advanced Java Desktop Widget Engine

**Widgify** is a Rainmeter and Nothing OS inspired desktop widget application for Windows and macOS. It allows users to place minimalist, independent widgets directly onto their operating system desktop, drag and resize them fluidly, configure layout presets, and persist their layout across application restarts.

Widgify is built as an **Advanced Java Academic Project**, demonstrating clean architectural separation between an **Apache Tomcat 10.1.x Backend Server** and a **JavaFX Desktop Client App**.

---

## 1. Project Architecture

The architecture cleanly separates presentation and persistence while adhering 100% to Advanced Java standards:

```
┌───────────────────────────────────────────────────────────┐
│              WIDGIFY JAVAFX DESKTOP CLIENT                │
├───────────────────────────────────────────────────────────┤
│ • Independent Transparent Stages (StageStyle.TRANSPARENT)  │
│ • Desktop Widget Manager (WidgetManagerStage)             │
│ • Mouse Dragging & 10px Screen Edge Snapping              │
│ • 300ms Debounced Persistence (WidgetWindowManager)       │
│ • System Tray Integration & App Keyboard Shortcuts        │
└─────────────────────────────┬─────────────────────────────┘
                              │
                    HTTP REST / JSON APIs
                  (Cookie JSESSIONID Session)
                              │
┌─────────────────────────────▼─────────────────────────────┐
│             ADVANCED JAVA TOMCAT BACKEND WAR              │
├───────────────────────────────────────────────────────────┤
│ • Jakarta Servlets & Servlet Filters (AuthenticationFilter)│
│ • HttpSession JSESSIONID Session Cookie Security          │
│ • Service Layer & Business Logic                          │
│ • DAO (Data Access Object) Pattern & PreparedStatement    │
│ • MySQL 8.0 Relational Database                           │
└───────────────────────────────────────────────────────────┘
```

---

## 2. Advanced Java Technologies Implemented

* **Jakarta Servlets**: Handles incoming HTTP GET/POST controllers (`LoginServlet`, `RegisterServlet`, `LogoutServlet`, `WidgetServlet`, `NoteServlet`, `TaskServlet`, `SystemHealthServlet`, `WeatherServlet`).
* **JSP (JavaServer Pages)**: Retained for web interface & admin views (`dashboard.jsp`, `login.jsp`, `register.jsp`).
* **Servlet Filters**: `AuthenticationFilter` intercepts all protected API routes, verifying `HttpSession` state before executing controller logic.
* **HTTP Session Management**: Session authentication via `JSESSIONID` cookies retained by Java 11 `HttpClient` and `CookieManager`.
* **Service Layer**: Decoupled business logic services (`SystemMonitorService`, `WeatherService`, `PasswordUtil` PBKDF2 hashing).
* **DAO Pattern**: Clean separation of data access logic (`UserDAO`, `WidgetDAO`, `NoteDAO`, `TaskDAO`).
* **JDBC (Java Database Connectivity)**: Direct SQL execution using `PreparedStatement` and connection pooling (`DBConnection`).
* **MySQL 8.0**: Relational database persistence for users, widgets, sticky notes, tasks, and settings.
* **Apache Tomcat 10.1.x**: Standard servlet container executing `widgify.war`.
* **JavaFX 17**: Client-side GUI toolkit rendering transparent desktop stages (`StageStyle.TRANSPARENT`).

---

## 3. Desktop Widgets Inventory (8 Production Widgets)

1. **Digital Clock (`clock`)**: Nothing OS styled digital clock (`HH:mm:ss` or `hh:mm:ss a` 12h/24h formats).
2. **Study Timer (`timer`)**: Countdown/stopwatch timer with 5m, 15m, 25m Pomodoro presets.
3. **Local System Monitor (`system`)**: Client hardware telemetry (Local CPU %, RAM MB/GB, Heap Memory).
4. **Weather Forecast (`weather`)**: Real-time forecast data via Open-Meteo API with configurable location.
5. **Sticky Notes (`notes`)**: Persistent desktop sticky notes linked to MySQL `/notes` backend.
6. **Tasks & To-Do (`tasks`)**: Interactive task checklist with strikethrough & completion toggles.
7. **Tomcat Server Health (`server_health`)**: Dedicated JVM server health metrics from Tomcat `/system-health`.
8. **Quick Web Launcher (`launcher`)**: Safe allowlist browser shortcuts (VS Code Web, GitHub, Google, Stack Overflow, ChatGPT).

---

## 4. Prerequisites & Environment Setup

* **Java Development Kit (JDK)**: JDK 17 or higher.
* **Build Tool**: Apache Maven (included via `./mvnw` / `mvnw.cmd`).
* **Database**: MySQL 8.0 Server running on port 3306.
* **Servlet Container**: Apache Tomcat 10.1.x (or external daemon runner).

---

## 5. Quick Start & Execution Guide

For Windows, the included launcher can download Maven, MySQL, and Tomcat into
the project-local `.widgify` directory and start the complete application:
```cmd
run-widgify.bat
```
Run the non-destructive prerequisite check first when setting up another PC:
```cmd
run-widgify.bat --check
```
The launcher requires Windows PowerShell, `curl.exe` (included with Windows
10 and newer), and a JDK 17 or newer. Do not move only the batch file; copy the
complete project folder so the Maven build, database schema, and web
application are available.

### Step 1: Initialize MySQL Database
Execute `database/schema.sql` against your MySQL instance:
```bash
mysql -u root -p < database/schema.sql
```

### Step 2: Build Clean Project & WAR
Execute Maven packaging:
```cmd
.\mvnw.cmd clean package
```
*Creates `target/widgify.war` with **0 JavaFX dependencies** in `WEB-INF/lib`.*

### Step 3: Run Tomcat Backend Server
Run the Tomcat runner daemon:
```cmd
.\mvnw.cmd exec:java "-Dexec.mainClass=com.widgify.runner.TomcatServerRunner"
```
*Server starts at `http://localhost:8080/widgify`.*

### Step 4: Launch Widgify Desktop Client
In a separate terminal window:
```cmd
.\mvnw.cmd exec:java "-Dexec.mainClass=com.widgify.desktop.WidgifyDesktopClient"
```

---

## 6. Layout Presets & Configuration

Widgify supports built-in layout presets:
* **Minimalist**: Clock + Weather
* **Productivity**: Clock + Tasks + Notes
* **Developer**: Clock + System + Server Health + Quick Launcher
* **All Active**: All 8 widgets positioned across the desktop grid.

---

## 7. Troubleshooting

* **Tomcat Port Conflict (8080)**: Ensure no other service is bound to port 8080.
* **MySQL Connection Refused**: Verify MySQL service is running on `localhost:3306` with credentials matching `DBConnection.java`.
* **Session Expiration (HTTP 401)**: The desktop client automatically detects session timeouts and prompts for re-login without closing existing desktop widgets.
