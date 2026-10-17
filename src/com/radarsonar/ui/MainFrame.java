package com.radarsonar.ui;

import javax.swing.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        this.setTitle("Radar & Sonar Target Detection Simulation System");
        this.setSize(800, 800);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add main radar/sonar canvas
        RadarSonarPanel radarSonarPanel = new RadarSonarPanel();
        add(radarSonarPanel);

        this.setLocationRelativeTo(null); // Center window on screen
    }
}
