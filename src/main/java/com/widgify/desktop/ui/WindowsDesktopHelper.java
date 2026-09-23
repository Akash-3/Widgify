package com.widgify.desktop.ui;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.logging.Logger;

/**
 * WindowsDesktopHelper — applies Windows native extended window styles to
 * make a JavaFX DesktopWidgetStage behave as a real desktop widget:
 *
 *   WS_EX_NOACTIVATE   — window never steals keyboard focus
 *   WS_EX_TOOLWINDOW   — removes from Alt+Tab list and taskbar
 *   HWND_BOTTOM         — placed behind all normal application windows
 *
 * This is the correct Win32 approach for Rainmeter-style desktop widgets.
 * The JavaFX stage itself is NOT replaced — only its native HWND properties
 * are modified after show().
 *
 * Falls back gracefully on non-Windows platforms or if JNA is unavailable.
 */
public final class WindowsDesktopHelper {

    private static final Logger LOG = Logger.getLogger(WindowsDesktopHelper.class.getName());

    // Extended window style constants
    private static final int GWL_EXSTYLE      = -20;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int WS_EX_NOACTIVATE = 0x08000000;
    private static final int WS_EX_LAYERED    = 0x00080000;
    private static final int WS_EX_TRANSPARENT= 0x00000020;

    // SetWindowPos constants
    private static final WinDef.HWND HWND_BOTTOM   = new WinDef.HWND(new Pointer(1));
    private static final WinDef.HWND HWND_NOTOPMOST= new WinDef.HWND(new Pointer(-2));

    private static final int SWP_NOMOVE        = 0x0002;
    private static final int SWP_NOSIZE        = 0x0001;
    private static final int SWP_NOACTIVATE    = 0x0010;
    private static final int SWP_NOZORDER      = 0x0004;
    private static final int SWP_FRAMECHANGED  = 0x0020;

    /** User32 interface narrowed to what we need. */
    interface User32Ex extends StdCallLibrary {
        User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

        WinDef.HWND FindWindowA(String lpClassName, String lpWindowName);
        int GetWindowLong(WinDef.HWND hWnd, int nIndex);
        int SetWindowLong(WinDef.HWND hWnd, int nIndex, int dwNewLong);
        boolean SetWindowPos(WinDef.HWND hWnd, WinDef.HWND hWndInsertAfter,
                             int X, int Y, int cx, int cy, int uFlags);
        WinDef.HWND GetForegroundWindow();
    }

    private WindowsDesktopHelper() {}

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Finds the HWND for a JavaFX Stage window by its title.
     * JavaFX renders each Stage as a native top-level Win32 window.
     */
    private static WinDef.HWND findHwnd(String windowTitle) {
        if (!isWindows() || windowTitle == null) return null;
        try {
            return User32Ex.INSTANCE.FindWindowA(null, windowTitle);
        } catch (Throwable t) {
            LOG.fine("FindWindowA failed: " + t.getMessage());
            return null;
        }
    }

    /**
     * Applies desktop-widget window properties to the stage identified by title.
     * Must be called AFTER stage.show() on the JavaFX Application thread.
     *
     * Result: widget appears behind normal windows, does not steal focus,
     * does not appear in Alt+Tab or taskbar.
     *
     * @param windowTitle the exact title of the JavaFX Stage
     */
    public static void applyDesktopWidgetStyle(String windowTitle) {
        if (!isWindows()) return;
        try {
            User32Ex u32 = User32Ex.INSTANCE;

            // Small retry loop — the native HWND may not be immediately available
            WinDef.HWND hwnd = null;
            for (int attempt = 0; attempt < 5 && hwnd == null; attempt++) {
                hwnd = findHwnd(windowTitle);
                if (hwnd == null) {
                    Thread.sleep(60);
                }
            }

            if (hwnd == null) {
                LOG.fine("Could not find HWND for window: " + windowTitle);
                return;
            }

            // Get current extended style
            int exStyle = u32.GetWindowLong(hwnd, GWL_EXSTYLE);

            // Add: NOACTIVATE (never steal focus) + TOOLWINDOW (no Alt+Tab / taskbar)
            // Keep LAYERED for transparency
            int newExStyle = exStyle | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW | WS_EX_LAYERED;

            u32.SetWindowLong(hwnd, GWL_EXSTYLE, newExStyle);

            // Place behind all normal windows (HWND_BOTTOM = desktop z-order layer)
            u32.SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE | SWP_FRAMECHANGED);

            LOG.info("[WindowsDesktopHelper] Applied desktop widget style to: " + windowTitle);

        } catch (Throwable t) {
            // Non-fatal: widget still works, just without native desktop behavior
            LOG.fine("[WindowsDesktopHelper] Native style not applied: " + t.getMessage());
        }
    }

    /**
     * Call this when the widget is dragged or resized to re-assert HWND_BOTTOM
     * (Windows may sometimes push the window up after user interaction).
     */
    public static void reapplyZOrder(String windowTitle) {
        if (!isWindows()) return;
        try {
            WinDef.HWND hwnd = findHwnd(windowTitle);
            if (hwnd != null) {
                User32Ex.INSTANCE.SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0,
                        SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
            }
        } catch (Throwable ignored) {}
    }
}
