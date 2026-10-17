package com.radarsonar.ui;

import com.radarsonar.engine.RadarSonarEngine;
import com.radarsonar.model.Target;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class RadarSonarPanel extends JPanel {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private double angle = 0;
    private final Timer timer;
    private final List<Target> targets = new ArrayList<>();
    private final Random random = new Random();
    private int spawnCooldown = 400 + random.nextInt(300); // Initial cooldown (6-11s) before first new spawn

    public RadarSonarPanel() {
        this.setBackground(Color.BLACK);

        int baseCenterX = 400;
        int baseCenterY = 400;

        // 1) Initial targets distributed across different quadrants
        for (int q = 0; q < 4; q++) {
            double quadrantStart = q * (Math.PI / 2.0);
            double spawnAngle = quadrantStart + (random.nextDouble() * (Math.PI / 2.0));
            double spawnDistance = 170 + random.nextDouble() * 170; // Outer region (170px - 340px)

            double startX = baseCenterX + spawnDistance * Math.cos(spawnAngle);
            double startY = baseCenterY + spawnDistance * Math.sin(spawnAngle);

            // Inward-ish initial velocity
            double heading = spawnAngle + Math.PI + (random.nextDouble() - 0.5) * (Math.PI / 3.0);
            double speed = 0.18 + random.nextDouble() * 0.18;

            targets.add(new Target(startX, startY, speed * Math.cos(heading), speed * Math.sin(heading)));
        }

        // 2) 1 target inside the red danger zone (< 120px)
        double innerAngle = random.nextDouble() * 2 * Math.PI;
        double innerDistance = 40 + random.nextDouble() * 60;
        double innerX = baseCenterX + innerDistance * Math.cos(innerAngle);
        double innerY = baseCenterY + innerDistance * Math.sin(innerAngle);
        double innerVx = (random.nextDouble() - 0.5) * 0.3;
        double innerVy = (random.nextDouble() - 0.5) * 0.3;
        targets.add(new Target(innerX, innerY, innerVx, innerVy));

        timer = new Timer(16, e -> {
            angle += 1;
            if (angle >= 360) {
                angle = 0;
            }

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int rmax = Math.min(centerX, centerY) - 20;

            if (rmax > 50) {
                // 1. Remove targets that navigated far beyond boundary
                targets.removeIf(target -> {
                    double dx = target.getX() - centerX;
                    double dy = target.getY() - centerY;
                    return Math.sqrt(dx * dx + dy * dy) > (rmax + 60);
                });

                // 2. Dynamic asynchronous spawning (every 8 to 18 seconds, 1-2 targets)
                spawnCooldown--;
                if (spawnCooldown <= 0) {
                    spawnCooldown = 500 + random.nextInt(600); // Next spawn in ~8 to 18 seconds

                    // Spawn 1 target typically, or 2 if target count is low
                    int countToSpawn = (targets.size() <= 2) ? (1 + random.nextInt(2)) : 1;

                    // Keep a balanced tactical density (max 6-7 active targets)
                    if (targets.size() + countToSpawn <= 7) {
                        for (int i = 0; i < countToSpawn; i++) {
                            spawnIncomingTarget(centerX, centerY, rmax);
                        }
                    }
                }
            }

            RadarSonarEngine.scanTargets(targets, angle, centerX, centerY, rmax);

            for (Target target : targets) {
                target.update();
            }
            repaint();
        });
        timer.start();
    }

    private void spawnIncomingTarget(int centerX, int centerY, int rmax) {
        if (rmax <= 50) return;
        // Spawn right at the outer edge of the sensor field
        double spawnDist = rmax + 5 + random.nextDouble() * 20;
        double spawnAngle = random.nextDouble() * 2 * Math.PI;

        double startX = centerX + spawnDist * Math.cos(spawnAngle);
        double startY = centerY + spawnDist * Math.sin(spawnAngle);

        // Heading directed inward across the detection field with realistic variance
        double heading = spawnAngle + Math.PI + (random.nextDouble() - 0.5) * (Math.PI / 2.0);
        double speed = 0.15 + random.nextDouble() * 0.25;

        targets.add(new Target(startX, startY, speed * Math.cos(heading), speed * Math.sin(heading)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int rmax = Math.min(centerX, centerY) - 20;

        // Background grid
        g2d.setColor(new Color(15, 65, 22));
        int gridSize = 40;
        for (int x = 0; x < width; x += gridSize) {
            g2d.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += gridSize) {
            g2d.drawLine(0, y, width, y);
        }

        // Radar/Sonar background circle
        g2d.setColor(new Color(22, 75, 30, 170));
        g2d.fillOval(centerX - rmax, centerY - rmax, 2 * rmax, 2 * rmax);

        // Sweep beam trail
        for (int i = 0; i < 45; i++) {
            double trailRad = Math.toRadians(angle - i);
            int alphaBeam = (int) ((45 - i) / 45.0 * 65);
            g2d.setColor(new Color(60, 255, 90, alphaBeam));
            int tx = centerX + (int) (rmax * Math.cos(trailRad));
            int ty = centerY + (int) (rmax * Math.sin(trailRad));
            g2d.drawLine(centerX, centerY, tx, ty);
        }

        // Distance range rings
        g2d.setColor(new Color(0, 200, 50));
        for (int i = 1; i <= 4; i++) {
            int r = i * rmax / 4;
            g2d.drawOval(centerX - r, centerY - r, 2 * r, 2 * r);
        }

        // Danger zone ring (inner 1/3)
        int dangerRadius = (int) (rmax / 3.0);
        g2d.setColor(new Color(200, 50, 50));
        g2d.drawOval(centerX - dangerRadius, centerY - dangerRadius, 2 * dangerRadius, 2 * dangerRadius);

        // Axis lines
        g2d.setColor(new Color(0, 200, 50));
        g2d.drawLine(centerX - rmax, centerY, centerX + rmax, centerY);
        g2d.drawLine(centerX, centerY - rmax, centerX, centerY + rmax);

        // Sweep line
        double radians = Math.toRadians(angle);
        int endX = centerX + (int) (rmax * Math.cos(radians));
        int endY = centerY + (int) (rmax * Math.sin(radians));

        g2d.setColor(new Color(170, 255, 170));
        g2d.drawLine(centerX, centerY, endX, endY);

        // Targets
        for (Target target : targets) {
            target.draw(g2d);
        }

        // CRT scanlines
        g2d.setColor(new Color(0, 0, 0, 35));
        for (int y = 0; y < height; y += 3) {
            g2d.drawLine(0, y, width, y);
        }

        // Telemetry HUD (top-left)
        Font hudFont = new Font("Consolas", Font.BOLD, 14);
        Font alertFont = new Font("Consolas", Font.BOLD, 16);
        Color neonGreen = new Color(50, 255, 50);

        g2d.setFont(hudFont);
        g2d.setColor(neonGreen);

        int activeTargetsCount = RadarSonarEngine.getActiveTargetCount(targets, centerX, centerY, rmax);
        g2d.drawString("=== RADAR & SONAR TELEMETRY ===", 20, 30);
        g2d.drawString(String.format("SWEEP ANGLE   : %05.1f°", angle), 20, 50);
        g2d.drawString(String.format("ACTIVE TARGETS: %02d", activeTargetsCount), 20, 70);
        g2d.drawString(String.format("RANGE LIMIT   : %d NM", rmax), 20, 90);

        // Threat status HUD (top-right)
        int rightX = width - 230;
        boolean isThreatPresent = RadarSonarEngine.hasDangerTarget(targets);
        boolean isThreatBlinking = (System.currentTimeMillis() / 400) % 2 == 0;

        if (isThreatPresent) {
            if (isThreatBlinking) {
                g2d.setFont(alertFont);
                g2d.setColor(new Color(255, 40, 40));
                g2d.drawString("STATUS: [!] THREAT!", rightX, 30);

                g2d.setFont(hudFont);
                g2d.drawString("PROXIMITY ALERT ACTIVE", rightX, 50);
            }
        } else {
            g2d.setFont(alertFont);
            g2d.setColor(neonGreen);
            g2d.drawString("STATUS: SECURE [CLEAR]", rightX, 30);

            g2d.setFont(hudFont);
            g2d.setColor(new Color(0, 180, 50));
            g2d.drawString("NO INTRUDER IN ZONE", rightX, 50);
        }

        // Danger warning banner (bottom center)
        if (isThreatPresent && isThreatBlinking) {
            g2d.setColor(new Color(255, 40, 40));
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            String warningText = ">>> WARNING: TARGET IN DANGER ZONE <<<";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(warningText);
            g2d.drawString(warningText, centerX - textWidth / 2, height - 20);
        }

        // System time (bottom-left)
        g2d.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2d.setColor(new Color(0, 180, 50));
        String timeString = LocalTime.now().format(TIME_FORMATTER);
        g2d.drawString("SYS TIME: " + timeString, 20, height - 20);
    }
}
