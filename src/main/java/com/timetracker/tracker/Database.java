package com.timetracker.tracker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Database {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final String url;

    public Database() {
        Path dbDir = Paths.get(System.getProperty("user.home"), "TimeTracker");
        try {
            Files.createDirectories(dbDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Path dbFile = dbDir.resolve("timetracker.db");
        this.url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        init();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private synchronized void init() {
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "process_name TEXT NOT NULL," +
                    "window_title TEXT," +
                    "start_time TEXT NOT NULL," +
                    "end_time TEXT NOT NULL," +
                    "duration REAL NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_start_time ON sessions(start_time)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_process ON sessions(process_name)");
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось инициализировать базу данных", e);
        }
    }

    public synchronized void addSession(String processName, String windowTitle,
                                         LocalDateTime start, LocalDateTime end, double duration) {
        String sql = "INSERT INTO sessions (process_name, window_title, start_time, end_time, duration) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, processName);
            ps.setString(2, windowTitle == null ? "" : windowTitle);
            ps.setString(3, start.format(FMT));
            ps.setString(4, end.format(FMT));
            ps.setDouble(5, duration);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Map.Entry<String, Double>> getTotalsByProcess(LocalDateTime since, LocalDateTime until) {
        StringBuilder sql = new StringBuilder(
                "SELECT process_name, SUM(duration) as total FROM sessions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (since != null) {
            sql.append(" AND start_time >= ?");
            params.add(since.format(FMT));
        }
        if (until != null) {
            sql.append(" AND start_time < ?");
            params.add(until.format(FMT));
        }
        sql.append(" GROUP BY process_name ORDER BY total DESC");

        List<Map.Entry<String, Double>> result = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new AbstractMap.SimpleEntry<>(rs.getString("process_name"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public synchronized double getTotalTime(LocalDateTime since, LocalDateTime until) {
        StringBuilder sql = new StringBuilder("SELECT SUM(duration) as total FROM sessions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (since != null) {
            sql.append(" AND start_time >= ?");
            params.add(since.format(FMT));
        }
        if (until != null) {
            sql.append(" AND start_time < ?");
            params.add(until.format(FMT));
        }

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
