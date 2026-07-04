package com.timetracker.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Достаёт стандартную иконку Windows из .exe файла программы и кэширует её
 * по имени процесса, чтобы не читать диск на каждое обновление списка.
 */
public class ProcessIconCache {

    private final Map<String, Image> cache = new ConcurrentHashMap<>();

    /** Возвращает иконку 16x16 для процесса или null, если найти не удалось. */
    public Image getIcon(String processName) {
        return cache.computeIfAbsent(processName, this::loadIcon);
    }

    private Image loadIcon(String processName) {
        String path = findExecutablePath(processName);
        if (path == null) {
            return null;
        }
        try {
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }
            Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
            BufferedImage buffered = new BufferedImage(
                    icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(null, buffered.getGraphics(), 0, 0);
            return SwingFXUtils.toFXImage(buffered, null);
        } catch (Exception e) {
            return null;
        }
    }

    private String findExecutablePath(String processName) {
        return ProcessHandle.allProcesses()
                .filter(ph -> ph.info().command()
                        .map(cmd -> cmd.endsWith("\\" + processName) || cmd.endsWith("/" + processName))
                        .orElse(false))
                .findFirst()
                .flatMap(ph -> ph.info().command())
                .orElse(null);
    }
}
