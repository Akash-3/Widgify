<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.widgify.model.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    if (currentUser == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Widgify — Web Productivity Workspace</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/dashboard.css">
</head>
<body>
    <div class="dashboard-layout">
        <!-- Sidebar Navigation -->
        <aside class="sidebar">
            <div class="brand">
                <span class="brand-name">W I D G I F Y</span>
            </div>

            <nav class="nav-menu">
                <div class="nav-item active" id="navDashboardItem">
                    <span class="nav-icon">📊</span> Dashboard
                </div>
                <div class="nav-item" id="navWidgetsItem">
                    <span class="nav-icon">🧩</span> Widgets
                </div>
                <div class="nav-item" id="navSettingsItem">
                    <span class="nav-icon">⚙️</span> Settings
                </div>
            </nav>

            <button class="btn-add-widget btn-open-add-widget">
                <span>+</span> Add Widget
            </button>

            <div class="sidebar-footer">
                <div class="user-profile-summary">
                    <div class="user-avatar-circle">
                        <%= currentUser.getName().substring(0, 1).toUpperCase() %>
                    </div>
                    <div class="user-info-text">
                        <span class="user-name-str"><%= currentUser.getName() %></span>
                        <span class="user-email-str"><%= currentUser.getEmail() %></span>
                    </div>
                </div>
                <a href="logout" class="btn-logout-link">
                    <span>🚪</span> Sign out
                </a>
            </div>
        </aside>

        <!-- Main Workspace Area -->
        <main class="main-content">
            <!-- Header Controls & Greeting Banner Container -->
            <header class="desktop-header-container">
                <!-- Row 1: Search & Layout Lock Controls -->
                <div class="header-controls-row">
                    <div class="search-box">
                        <span class="search-icon">🔍</span>
                        <input type="text" id="widgetSearchInput" placeholder="Search widgets, notes, tasks..." />
                    </div>

                    <button id="layoutLockBtn" class="btn-layout-toggle">
                        <span id="layoutLockIcon">🔓</span>
                        <span id="layoutLockText">Edit Layout</span>
                    </button>
                </div>

                <!-- Row 2: Greeting & Date/Quote Banner -->
                <div class="header-banner-row">
                    <div class="greeting-area">
                        <h1 class="greeting-title">Good morning, <%= currentUser.getName() %></h1>
                        <p class="greeting-subtitle">Your workspace at a glance.</p>
                    </div>

                    <div class="quote-area">
                        <div class="banner-date" id="banner-date-str">Wed, Sep 23, 2026</div>
                        <div class="banner-quote">Small steps. Big progress.</div>
                    </div>
                </div>
            </header>

            <!-- Dynamic Widget Grid Canvas Container -->
            <div class="canvas-wrapper">
                <section class="widget-canvas" id="widgetCanvas">
                    <!-- Dynamic widgets populated by dashboard.js -->
                </section>
            </div>

            <!-- Nothing OS Minimal Footer -->
            <footer class="dashboard-footer">
                <span class="footer-version">Widgify v1.0.0</span>
                <span class="footer-tagline">A more productive you. Every day.</span>
            </footer>
        </main>
    </div>

    <!-- Add Widget Modal Panel -->
    <div class="modal-overlay" id="addWidgetModal">
        <div class="modal-card">
            <div class="modal-header">
                <h3>ADD WIDGET</h3>
                <button class="widget-action-btn" id="closeModalBtn">✕</button>
            </div>
            <div class="modal-body">
                <div class="add-widget-option" data-type="clock">
                    <div class="widget-option-icon">🕒</div>
                    <div>
                        <div class="widget-option-name">Digital Clock</div>
                        <div class="widget-option-desc">Live dot-matrix clock</div>
                    </div>
                </div>
                <div class="add-widget-option" data-type="timer">
                    <div class="widget-option-icon">⏱️</div>
                    <div>
                        <div class="widget-option-name">Study Timer</div>
                        <div class="widget-option-desc">Countdown timer</div>
                    </div>
                </div>
                <div class="add-widget-option" data-type="system">
                    <div class="widget-option-icon">💻</div>
                    <div>
                        <div class="widget-option-name">Server Health</div>
                        <div class="widget-option-desc">Server CPU &amp; RAM telemetry</div>
                    </div>
                </div>
                <div class="add-widget-option" data-type="weather">
                    <div class="widget-option-icon">🌤️</div>
                    <div>
                        <div class="widget-option-name">Weather Updates</div>
                        <div class="widget-option-desc">Forecast telemetry</div>
                    </div>
                </div>
                <div class="add-widget-option" data-type="notes">
                    <div class="widget-option-icon">📝</div>
                    <div>
                        <div class="widget-option-name">Sticky Notes</div>
                        <div class="widget-option-desc">Personal notes &amp; memos</div>
                    </div>
                </div>
                <div class="add-widget-option" data-type="tasks">
                    <div class="widget-option-icon">✅</div>
                    <div>
                        <div class="widget-option-name">Tasks &amp; To-Do</div>
                        <div class="widget-option-desc">Checklist &amp; progress</div>
                    </div>
                </div>
                <div class="add-widget-option" data-type="launcher">
                    <div class="widget-option-icon">🚀</div>
                    <div>
                        <div class="widget-option-name">Quick Web Launcher</div>
                        <div class="widget-option-desc">Web shortcut tiles</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Settings Modal Panel -->
    <div class="modal-overlay" id="settingsModal">
        <div class="modal-card">
            <div class="modal-header">
                <h3>SETTINGS</h3>
                <button class="widget-action-btn" id="closeSettingsModalBtn">✕</button>
            </div>
            <div class="modal-body" style="grid-template-columns: 1fr; gap: 16px;">
                <div class="settings-group">
                    <label class="settings-label">LAYOUT MODE</label>
                    <div class="settings-row" style="display:flex; gap:10px; margin-top:6px;">
                        <button id="setFreeformBtn" class="btn-secondary-sm active">Freeform Grid</button>
                        <button id="setLockedBtn" class="btn-secondary-sm">Locked Layout</button>
                    </div>
                </div>
                <div class="settings-group">
                    <label class="settings-label">THEME</label>
                    <div class="settings-value-str" style="font-size:13px; color:var(--text-main); margin-top:4px;">Nothing OS Dark (Default)</div>
                </div>
                <div class="settings-group">
                    <label class="settings-label">ACCOUNT INFO</label>
                    <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">
                        Signed in as <strong><%= currentUser.getName() %></strong> (<%= currentUser.getEmail() %>)
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Application Context Variable -->
    <script>
        window.WIDGIFY_CONTEXT = '${pageContext.request.contextPath}';
    </script>

    <!-- Scripts -->
    <script src="js/widgets.js"></script>
    <script src="js/widgets/clock.js"></script>
    <script src="js/widgets/timer.js"></script>
    <script src="js/widgets/system.js"></script>
    <script src="js/widgets/weather.js"></script>
    <script src="js/widgets/notes.js"></script>
    <script src="js/widgets/tasks.js"></script>
    <script src="js/widgets/launcher.js"></script>
    <script src="js/dashboard.js"></script>
</body>
</html>
