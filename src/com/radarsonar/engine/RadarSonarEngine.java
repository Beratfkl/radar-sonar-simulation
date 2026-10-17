package com.radarsonar.engine;

import com.radarsonar.model.Target;
import java.util.List;

public class RadarSonarEngine {

    // Angular tolerance in degrees for detecting a target
    private static final double DETECTION_THRESHOLD = 2.5;

    public static void scanTargets(List<Target> targets, double sweepAngle, double centerX, double centerY, double maxRange) {
        if (targets == null) {
            return;
        }

        for (Target target : targets) {
            scanTarget(target, sweepAngle, centerX, centerY, maxRange);
        }
    }

    public static boolean scanTarget(Target target, double sweepAngle, double centerX, double centerY, double maxRange) {
        double dx = target.getX() - centerX;
        double dy = target.getY() - centerY;

        // Calculate distance from sensor center
        double distance = Math.sqrt(dx * dx + dy * dy);

        // If target leaves the sensor range, it's no longer tracked/discovered
        if (distance > maxRange) {
            target.setDiscovered(false);
            target.setDanger(false);
            target.setLogged(false);
            target.setAlpha(0);
            return false;
        }

        // Danger zone check (inner 1/3 of max range)
        double dangerRadius = maxRange / 3.0;
        if (distance <= dangerRadius) {
            target.setDanger(true);
            if (target.isDiscovered() && !target.isLogged()) {
                RadarSonarLogger.logThreat(target, distance);
                target.setLogged(true);
            }
        } else {
            target.setDanger(false);
            target.setLogged(false);
        }

        // Target angle in degrees [0, 360)
        double targetAngle = Math.toDegrees(Math.atan2(dy, dx));
        if (targetAngle < 0) {
            targetAngle += 360.0;
        }

        // Circular angular difference
        double angleDiff = Math.abs(sweepAngle - targetAngle);
        if (angleDiff > 180.0) {
            angleDiff = 360.0 - angleDiff;
        }

        // Flash target when intersected by sweep line
        if (angleDiff <= DETECTION_THRESHOLD) {
            target.setDetected(true);
            target.setDiscovered(true);

            // If it is in the danger zone upon detection, log threat
            if (target.isDanger() && !target.isLogged()) {
                RadarSonarLogger.logThreat(target, distance);
                target.setLogged(true);
            }
            return true;
        }

        return false;
    }

    public static int getActiveTargetCount(List<Target> targets, double centerX, double centerY, double maxRange) {
        if (targets == null) {
            return 0;
        }
        int count = 0;
        for (Target target : targets) {
            double dx = target.getX() - centerX;
            double dy = target.getY() - centerY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (target.isDiscovered() && distance <= maxRange) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasDangerTarget(List<Target> targets) {
        if (targets == null) {
            return false;
        }
        for (Target target : targets) {
            if (target.isDiscovered() && target.isDanger() && target.getAlpha() > 0) {
                return true;
            }
        }
        return false;
    }
}
