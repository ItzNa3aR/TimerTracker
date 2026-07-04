package com.timetracker.gui;

import com.timetracker.tracker.Database;
import com.timetracker.tracker.Period;
import com.timetracker.tracker.WindowMonitor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DashboardApp extends Application {

    private static final String[] CHART_COLORS = {
            "#7c5cff", "#00d4b5", "#ff6b9d", "#ffb84d",
            "#4dc9ff", "#ff5c5c", "#8bc34a", "#c77dff"
    };
    private static final String BG = "#0f1117";
    private static final String CARD = "#181b25";
    private static final String TEXT_PRIMARY = "#f5f5f7";
    private static final String TEXT_SECONDARY = "#9096a8";
    private static final String ACCENT = "#7c5cff";

    private Database db;
    private WindowMonitor monitor;
    private Period currentPeriod = Period.TODAY;

    private Label totalTitle;
    private Label totalValue;
    private PieChart pieChart;
    private StackPane chartContainer;
    private VBox listBox;
    private final Map<Period, Button> periodButtons = new EnumMap<>(Period.class);
    private final ProcessIconCache iconCache = new ProcessIconCache();

    @Override
    public void start(Stage stage) {
        db = new Database();
        monitor = new WindowMonitor(db, 1000);
        monitor.start();


        List<String> params = getParameters().getRaw();
        boolean isHidden = params.contains("--hidden");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");
        root.setLeft(buildSidebar());
        root.setCenter(buildMain());

        Scene scene = new Scene(root, 980, 640);
        stage.setTitle("Time Tracker");
        stage.setScene(scene);
        stage.setMinWidth(860);
        stage.setMinHeight(560);

        Image appIcon = new Image(getClass().getResourceAsStream("/icon.png"));
        stage.getIcons().add(appIcon);

        if (!isHidden) {
            stage.show();
        } else {
            Platform.setImplicitExit(false);
        }

        TrayManager tray = new TrayManager(stage, monitor::stop);
        boolean trayInstalled = tray.install();

        if (trayInstalled) {
            Platform.setImplicitExit(false);
            stage.setOnCloseRequest(e -> {
                e.consume();
                stage.hide();
                tray.notifyMinimized();
            });
        } else {
            stage.setOnCloseRequest(e -> {
                monitor.stop();
                Platform.exit();
            });
        }

        refreshData();

        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshData()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();

        enableAutoStart();
    }
    public void enableAutoStart() {
        try {
            String exePath = ProcessHandle.current().info().command().orElse(null);
            if (exePath == null) {
                System.err.println("Failed to determine the path to the executable");
                return;
            }
            String command = String.format(
                    "reg add \"HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\" /v \"TimeTracker\" /t REG_SZ /d \"\\\"%s\\\" --hidden\" /f",
                    exePath
            );
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: " + CARD + ";");
        sidebar.setPadding(new Insets(30, 0, 20, 0));

        Label title = new Label("⏱ Time Tracker");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(TEXT_PRIMARY));
        VBox.setMargin(title, new Insets(0, 0, 20, 20));

        Label periodLabel = new Label("Period");
        periodLabel.setFont(Font.font("Segoe UI", 12));
        periodLabel.setTextFill(Color.web(TEXT_SECONDARY));
        VBox.setMargin(periodLabel, new Insets(0, 0, 5, 20));

        sidebar.getChildren().addAll(title, periodLabel);

        for (Period p : Period.values()) {
            Button btn = new Button(p.label);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setFont(Font.font("Segoe UI", 14));
            btn.setStyle(buttonStyle(false));
            btn.setOnAction(e -> selectPeriod(p));
            VBox.setMargin(btn, new Insets(3, 12, 0, 12));
            periodButtons.put(p, btn);
            sidebar.getChildren().add(btn);
        }
        highlightPeriod();

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Label status = new Label("● The application collects statistics");
        status.setFont(Font.font("Segoe UI", 11));
        status.setTextFill(Color.web("#00d4b5"));
        status.setMaxWidth(Double.MAX_VALUE);
        status.setAlignment(Pos.CENTER);
        status.setWrapText(true);
        status.setStyle("-fx-text-alignment: center;");
        VBox.setMargin(status, new Insets(0, 10, 10, 10));
        sidebar.getChildren().add(status);

        return sidebar;
    }

    private String buttonStyle(boolean active) {
        if (active) {
            return "-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;";
        }
        return "-fx-background-color: transparent; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-background-radius: 8; -fx-cursor: hand;";
    }

    private void highlightPeriod() {
        for (Map.Entry<Period, Button> entry : periodButtons.entrySet()) {
            entry.getValue().setStyle(buttonStyle(entry.getKey() == currentPeriod));
        }
    }

    private void selectPeriod(Period p) {
        currentPeriod = p;
        highlightPeriod();
        refreshData();
    }

    private VBox buildMain() {
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));

        VBox totalCard = new VBox(4);
        totalCard.setStyle("-fx-background-color: " + CARD + "; -fx-background-radius: 16;");
        totalCard.setPadding(new Insets(20, 24, 20, 24));

        totalTitle = new Label("Today");
        totalTitle.setFont(Font.font("Segoe UI", 14));
        totalTitle.setTextFill(Color.web(TEXT_SECONDARY));

        totalValue = new Label("0 min");
        totalValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 38));
        totalValue.setTextFill(Color.web(TEXT_PRIMARY));

        totalCard.getChildren().addAll(totalTitle, totalValue);

        VBox listCard = buildCard("App activity");
        listBox = new VBox(14);
        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        listCard.getChildren().add(scrollPane);
        VBox.setVgrow(listCard, Priority.ALWAYS);

        main.getChildren().addAll(totalCard, listCard);
        return main;
    }

    private void refreshData() {
        LocalDateTime since = currentPeriod.since();
        LocalDateTime until = currentPeriod.until();
        double total = db.getTotalTime(since, until);
        List<Map.Entry<String, Double>> rows = db.getTotalsByProcess(since, until);

        totalTitle.setText(currentPeriod.label);
        totalValue.setText(formatDuration(total));

        renderList(rows, total);
    }

    private VBox buildCard(String headerText) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: " + CARD + "; -fx-background-radius: 16;");
        card.setPadding(new Insets(16, 20, 16, 20));

        Label header = new Label(headerText);
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        header.setTextFill(Color.web(TEXT_PRIMARY));
        card.getChildren().add(header);

        return card;
    }
    private void renderChart(List<Map.Entry<String, Double>> rows) {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        int limit = Math.min(rows.size(), 8);
        for (int i = 0; i < limit; i++) {
            data.add(new PieChart.Data(rows.get(i).getKey(), rows.get(i).getValue()));
        }
        pieChart.setData(data);

        chartContainer.widthProperty().addListener((obs, oldVal, newVal) -> updateIcons(data));
        chartContainer.heightProperty().addListener((obs, oldVal, newVal) -> updateIcons(data));

        updateIcons(data);
    }
    private void updateIcons(ObservableList<PieChart.Data> data) {
        Platform.runLater(() -> {
            chartContainer.getChildren().removeIf(n -> n instanceof ImageView);

            double total = data.stream().mapToDouble(PieChart.Data::getPieValue).sum();
            if (total <= 0) return;

            double currentAngle = pieChart.getStartAngle();

            for (int i = 0; i < data.size(); i++) {
                PieChart.Data d = data.get(i);
                double sliceAngle = (d.getPieValue() / total) * 360;

                double midAngle = Math.toRadians(currentAngle + sliceAngle / 2);

                double radius = Math.min(pieChart.getWidth(), pieChart.getHeight()) * 0.25;

                double x = Math.cos(midAngle) * radius;
                double y = -Math.sin(midAngle) * radius;

                Image icon = iconCache.getIcon(d.getName());
                if (icon != null) {
                    ImageView iv = new ImageView(icon);
                    iv.setFitWidth(20);
                    iv.setFitHeight(20);
                    iv.setTranslateX(x);
                    iv.setTranslateY(y);
                    iv.setMouseTransparent(true);
                    chartContainer.getChildren().add(iv);
                }
                currentAngle += sliceAngle;
            }
        });
    }

    private void renderList(List<Map.Entry<String, Double>> rows, double total) {
        listBox.getChildren().clear();
        if (rows.isEmpty()) {
            Label empty = new Label("No data available for this period");
            empty.setTextFill(Color.web(TEXT_SECONDARY));
            listBox.getChildren().add(empty);
            return;
        }

        int i = 0;
        for (Map.Entry<String, Double> row : rows) {
            String color = CHART_COLORS[i % CHART_COLORS.length];

            HBox line = new HBox(8);
            line.setAlignment(Pos.CENTER_LEFT);

            Node marker = buildMarker(row.getKey(), color);

            Label name = new Label(row.getKey());
            name.setTextFill(Color.web(TEXT_PRIMARY));
            name.setFont(Font.font("Segoe UI", 13));
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);

            Label time = new Label(formatDuration(row.getValue()));
            time.setTextFill(Color.web(TEXT_SECONDARY));
            time.setFont(Font.font("Segoe UI", 13));

            line.getChildren().addAll(marker, name, time);

            ProgressBar bar = new ProgressBar(total > 0 ? row.getValue() / total : 0);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(6);
            bar.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: " + BG + ";");

            VBox entry = new VBox(4, line, bar);
            listBox.getChildren().add(entry);
            i++;
        }
    }

    private Node buildMarker(String processName, String fallbackColor) {
        Image icon = iconCache.getIcon(processName);
        if (icon != null) {
            ImageView iv = new ImageView(icon);
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        }
        Label dot = new Label("●");
        dot.setTextFill(Color.web(fallbackColor));
        return dot;
    }

    private static String formatDuration(double seconds) {
        long s = (long) seconds;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) return h + " h " + m + " m";
        if (m > 0) return m + " m " + sec + " sec";
        return sec + " sec";
    }

    public static void launchApp(String[] args) {
        launch(args);
    }
}