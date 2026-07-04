package com.timetracker.gui;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.net.URL;

public class TrayManager {

    private final Stage stage;
    private final Runnable onExit;
    private TrayIcon trayIcon;

    public TrayManager(Stage stage, Runnable onExit) {
        this.stage = stage;
        this.onExit = onExit;
    }

    public boolean install() {
        if (!SystemTray.isSupported()) {
            return false;
        }
        try {
            URL iconUrl = getClass().getResource("/icon.png");
            if (iconUrl == null) {
                return false;
            }
            Image image = Toolkit.getDefaultToolkit().getImage(iconUrl);

            PopupMenu menu = new PopupMenu();

            MenuItem openItem = new MenuItem("Open");
            openItem.addActionListener(e -> Platform.runLater(this::showStage));
            menu.add(openItem);

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                SystemTray.getSystemTray().remove(trayIcon);
                Platform.runLater(() -> {
                    onExit.run();
                    Platform.exit();
                });
            });
            menu.add(exitItem);

            trayIcon = new TrayIcon(image, "Time Tracker", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(this::showStage));

            SystemTray.getSystemTray().add(trayIcon);
            return true;
        } catch (AWTException e) {
            return false;
        }
    }

    public void showStage() {
        stage.show();
        stage.toFront();
        stage.setIconified(false);
    }

    public void notifyMinimized() {
        if (trayIcon != null) {
            trayIcon.displayMessage(
                    "Time Tracker",
                    "Tracking your activity in the background",
                    TrayIcon.MessageType.INFO
            );
        }
    }
}
