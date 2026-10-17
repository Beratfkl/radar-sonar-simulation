package com.radarsonar.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Target {

    private static int idCounter = 1;
    private static final int MAX_TRAIL_POINTS = 5;

    private static final java.util.Random RANDOM = new java.util.Random();

    private final int id;
    private double x, y;       // Current position
    private double vx, vy;     // Velocity vector
    private double alpha = 0.0;     // Visibility/fade level (0.0-255.0)
    private boolean detected = false;
    private boolean discovered = false; // Tracks if radar/sonar has ever scanned this target
    private boolean inDangerZone = false;
    private boolean logged = false; // Prevents log spamming

    // Maneuver behavior
    private double turnRate = 0.0;
    private int maneuverCooldown = 30 + RANDOM.nextInt(90);

    // History of past scanned coordinates
    private final List<Point2D.Double> historyTrail = new ArrayList<>();

    public Target(double x, double y, double vx, double vy) {
        this.id = idCounter++;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.alpha = 0.0; // Starts invisible until scanned
        this.discovered = false;
    }

    public void update() {
        // Dynamic course adjustment / maneuver
        maneuverCooldown--;
        if (maneuverCooldown <= 0) {
            // Pick a subtle turn rate (-0.006 to +0.006 rad/tick)
            turnRate = (RANDOM.nextDouble() - 0.5) * 0.010;
            // Hold this turn for 80 to 240 ticks (1.3s to 4s)
            maneuverCooldown = 80 + RANDOM.nextInt(160);
        }

        // Apply heading update
        double speed = Math.sqrt(vx * vx + vy * vy);
        if (speed > 0.01) {
            double currentHeading = Math.atan2(vy, vx);
            currentHeading += turnRate;
            this.vx = speed * Math.cos(currentHeading);
            this.vy = speed * Math.sin(currentHeading);
        }

        this.x += this.vx;
        this.y += this.vy;

        // Fade out target over 270 degrees of sweep (1 deg/tick => 255.0 / 270.0 per tick)
        if (this.alpha > 0) {
            this.alpha = Math.max(0.0, this.alpha - (255.0 / 270.0));
        }
    }

    public void draw(Graphics2D g2d) {
        int currentAlpha = (int) Math.round(alpha);
        if (currentAlpha <= 0) {
            return;
        }

        Stroke originalStroke = g2d.getStroke();

        // 1. Draw historical trail (lines & breadcrumbs from previous scan positions)
        if (!historyTrail.isEmpty()) {
            Point2D.Double prevPoint = historyTrail.get(0);
            for (int i = 0; i < historyTrail.size(); i++) {
                Point2D.Double currentPoint = historyTrail.get(i);

                // Dimmer trail for older points
                int trailAlpha = Math.max(10, (int) (currentAlpha * ((i + 1.0) / (historyTrail.size() + 1.0)) * 0.75));

                if (inDangerZone) {
                    g2d.setColor(new Color(255, 60, 60, trailAlpha));
                } else {
                    g2d.setColor(new Color(255, 160, 40, trailAlpha));
                }

                // Small historical position marker
                g2d.fillOval((int) currentPoint.x - 2, (int) currentPoint.y - 2, 4, 4);

                if (i > 0) {
                    // Dashed line between historical points
                    g2d.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{3.0f, 3.0f}, 0.0f));
                    g2d.drawLine((int) prevPoint.x, (int) prevPoint.y, (int) currentPoint.x, (int) currentPoint.y);
                }
                prevPoint = currentPoint;
            }

            // Connect last historical point to current position
            int lastSegmentAlpha = Math.max(20, (int) (currentAlpha * 0.85));
            if (inDangerZone) {
                g2d.setColor(new Color(255, 80, 80, lastSegmentAlpha));
            } else {
                g2d.setColor(new Color(255, 180, 50, lastSegmentAlpha));
            }
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{4.0f, 3.0f}, 0.0f));
            g2d.drawLine((int) prevPoint.x, (int) prevPoint.y, (int) x, (int) y);
        }

        g2d.setStroke(originalStroke);

        // 2. Draw current target blip
        if (inDangerZone) {
            // Threat alert: red dot with warning ring
            g2d.setColor(new Color(255, 30, 30, currentAlpha));
            g2d.fillOval((int) x - 5, (int) y - 5, 10, 10);
            g2d.setColor(new Color(255, 0, 0, Math.min(255, currentAlpha)));
            g2d.drawOval((int) x - 9, (int) y - 9, 18, 18);
        } else {
            // Normal target blip
            g2d.setColor(new Color(255, 120, 0, currentAlpha));
            g2d.fillOval((int) x - 4, (int) y - 4, 8, 8);
        }
    }

    // Target ID
    public int getId() { return id; }

    // Position (X and Y)
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    // Velocity components
    public double getVx() { return vx; }
    public void setVx(double vx) { this.vx = vx; }
    public double getVy() { return vy; }
    public void setVy(double vy) { this.vy = vy; }

    // Visibility / alpha (fade effect)
    public int getAlpha() { return (int) Math.round(alpha); }
    public void setAlpha(double alpha) { this.alpha = Math.max(0.0, Math.min(255.0, alpha)); }

    // Scan / detection state
    public boolean isDetected() { return detected; }
    public void setDetected(boolean detected) { 
        this.detected = detected; 
        if (detected) {
            recordScan();
        }
    }

    public void recordScan() {
        if (alpha < 220.0 || historyTrail.isEmpty()) {
            historyTrail.add(new Point2D.Double(x, y));
            if (historyTrail.size() > MAX_TRAIL_POINTS) {
                historyTrail.remove(0);
            }
        }
        this.detected = true;
        this.alpha = 255.0;
    }

    public void clearTrail() {
        this.historyTrail.clear();
    }

    // Discovery status (first scan / active tracking)
    public boolean isDiscovered() { return discovered; }
    public void setDiscovered(boolean discovered) { 
        this.discovered = discovered; 
        if (!discovered) {
            clearTrail();
        }
    }

    // Danger zone check
    public boolean isDanger() { return inDangerZone; }
    public void setDanger(boolean inDangerZone) { this.inDangerZone = inDangerZone; }

    // Log flag (prevents spamming the console)
    public boolean isLogged() { return logged; }
    public void setLogged(boolean logged) { this.logged = logged; }
}
