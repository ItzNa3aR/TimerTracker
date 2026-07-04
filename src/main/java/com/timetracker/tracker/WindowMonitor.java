package com.timetracker.tracker;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;


public class WindowMonitor {

    private final Database db;
    private final long pollIntervalMs;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    private String currentProcess;
    private String currentTitle;
    private LocalDateTime sessionStart;

    public WindowMonitor(Database db, long pollIntervalMs) {
        this.db = db;
        this.pollIntervalMs = pollIntervalMs;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            thread = new Thread(this::loop, "window-monitor");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void stop() {
        running.set(false);
        closeCurrentSession();
        if (thread != null) {
            try {
                thread.join(2000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void loop() {
        while (running.get()) {
            ActiveWindowInfo info = getActiveWindow();
            if (info != null) {
                if (!info.processName.equals(currentProcess)) {
                    closeCurrentSession();
                    currentProcess = info.processName;
                    currentTitle = info.title;
                    sessionStart = LocalDateTime.now();
                } else if (!info.title.equals(currentTitle)) {
                    currentTitle = info.title;
                }
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void closeCurrentSession() {
        if (currentProcess != null && sessionStart != null) {
            LocalDateTime end = LocalDateTime.now();
            double duration = Duration.between(sessionStart, end).toMillis() / 1000.0;
            if (duration >= 1) {
                db.addSession(currentProcess, currentTitle, sessionStart, end, duration);
            }
        }
        currentProcess = null;
        currentTitle = null;
        sessionStart = null;
    }

    private ActiveWindowInfo getActiveWindow() {
        try {
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) {
                return null;
            }

            char[] buffer = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
            String title = Native.toString(buffer);

            IntByReference pidRef = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef);
            int pid = pidRef.getValue();
            if (pid == 0) {
                return null;
            }

            String processName = ProcessHandle.of(pid)
                    .map(ProcessHandle::info)
                    .flatMap(ProcessHandle.Info::command)
                    .map(this::extractFileName)
                    .orElse("Неизвестная программа");

            return new ActiveWindowInfo(processName, title);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFileName(String fullPath) {
        int idx = Math.max(fullPath.lastIndexOf('\\'), fullPath.lastIndexOf('/'));
        return idx >= 0 ? fullPath.substring(idx + 1) : fullPath;
    }

    private static class ActiveWindowInfo {
        final String processName;
        final String title;

        ActiveWindowInfo(String processName, String title) {
            this.processName = processName;
            this.title = title == null ? "" : title;
        }
    }
}
