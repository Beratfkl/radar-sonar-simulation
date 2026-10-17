package com.radarsonar.engine;

import com.radarsonar.model.Target;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RadarSonarLogger {

    private static final String LOG_FILE = "radar_sonar_log.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Logs threat telemetry to file with timestamp
    public static synchronized void logThreat(Target target, double distance) {
        if (target == null) {
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format(
                Locale.US,
                "[%s] [ALERT] Target ID: #%d | Coordinates: (X: %.1f, Y: %.1f) | Distance to Base: %.1f px | STATUS: CRITICAL_PROXIMITY",
                timestamp,
                target.getId(),
                target.getX(),
                target.getY(),
                distance
        );

        // Append log entry safely
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(logEntry);
            writer.newLine();
            writer.flush();
            System.out.println("[LOG]: " + logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write radar/sonar log: " + e.getMessage());
        }
    }
}
