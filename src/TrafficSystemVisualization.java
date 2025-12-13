import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficSystemVisualization extends JFrame {
    private MapPanel mapPanel;
    private JTextArea logArea;
    private JPanel statsPanel;
    private Map<String, AgentInfo> agents;
    private Map<String, EmergencyStatus> emergencies;
    private static TrafficSystemVisualization instance;
    
    private int totalEmergencies = 0;
    private int resolvedEmergencies = 0;
    
    public static TrafficSystemVisualization getInstance() {
        if (instance == null) {
            instance = new TrafficSystemVisualization();
        }
        return instance;
    }
    
    private TrafficSystemVisualization() {
        agents = new ConcurrentHashMap<>();
        emergencies = new ConcurrentHashMap<>();
        
        setTitle("🚦 Multi-Agent Traffic & Emergency System");
        setSize(1500, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 245, 250));
        
        // Top stats panel
        statsPanel = createStatsPanel();
        add(statsPanel, BorderLayout.NORTH);
        
        // Main content area
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 245, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Map in center
        mapPanel = new MapPanel();
        mapPanel.setBackground(new Color(250, 250, 252));
        mapPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 210), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        mainPanel.add(mapPanel, BorderLayout.CENTER);
        
        // Log on right
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBackground(new Color(245, 245, 250));
        logPanel.setPreferredSize(new Dimension(400, 0));
        
        JLabel logTitle = new JLabel("📋 Activity Log");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logTitle.setForeground(new Color(50, 50, 70));
        logTitle.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        logPanel.add(logTitle, BorderLayout.NORTH);
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 35, 45));
        logArea.setForeground(new Color(100, 255, 150));
        logArea.setMargin(new Insets(10, 10, 10, 10));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 210), 2));
        logPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(logPanel, BorderLayout.EAST);
        add(mainPanel, BorderLayout.CENTER);
        
        // Update timer
        new javax.swing.Timer(150, e -> {
            mapPanel.repaint();
            updateStats();
        }).start();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 15, 0));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return panel;
    }
    
    private void updateStats() {
        statsPanel.removeAll();
        
        int ambulances = 0, ambulancesAvailable = 0;
        int hospitals = 0, hospitalBeds = 0, hospitalUsed = 0;
        int police = 0, policeAvailable = 0;
        int vehicles = 0;
        int lights = 0;
        
        for (AgentInfo agent : agents.values()) {
            switch (agent.type) {
                case "ambulance":
                    ambulances++;
                    if (agent.available) ambulancesAvailable++;
                    break;
                case "hospital":
                    hospitals++;
                    hospitalBeds += agent.capacity;
                    hospitalUsed += agent.currentLoad;
                    break;
                case "police":
                    police++;
                    if (agent.available) policeAvailable++;
                    break;
                case "vehicle":
                    vehicles++;
                    break;
                case "traffic-light":
                    lights++;
                    break;
            }
        }
        
        // Emergency card
        statsPanel.add(createStatCard(
            "🚨 Emergencies",
            String.valueOf(emergencies.size()),
            "Active",
            String.format("%d resolved", resolvedEmergencies),
            emergencies.size() > 0 ? new Color(244, 67, 54) : new Color(76, 175, 80)
        ));
        
        // Ambulance card
        statsPanel.add(createStatCard(
            "🚑 Ambulances",
            String.format("%d/%d", ambulancesAvailable, ambulances),
            "Available",
            ambulances > 0 ? String.format("%.0f%% ready", (ambulancesAvailable * 100.0 / ambulances)) : "N/A",
            new Color(233, 30, 99)
        ));
        
        // Hospital card
        statsPanel.add(createStatCard(
            "🏥 Hospitals",
            String.valueOf(hospitals),
            "Facilities",
            String.format("%d/%d beds free", (hospitalBeds - hospitalUsed), hospitalBeds),
            new Color(0, 150, 136)
        ));
        
        // Police card
        statsPanel.add(createStatCard(
            "👮 Police",
            String.format("%d/%d", policeAvailable, police),
            "On Patrol",
            police > 0 ? String.format("%.0f%% available", (policeAvailable * 100.0 / police)) : "N/A",
            new Color(33, 150, 243)
        ));
        
        // Traffic card
        statsPanel.add(createStatCard(
            "🚦 Traffic",
            String.valueOf(vehicles),
            "Vehicles",
            String.format("%d lights", lights),
            new Color(255, 152, 0)
        ));
        
        statsPanel.revalidate();
        statsPanel.repaint();
    }
    
    private JPanel createStatCard(String title, String mainValue, String mainLabel, String subValue, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 3, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(80, 80, 100));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel valueLabel = new JLabel(mainValue);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel mainLabelComp = new JLabel(mainLabel);
        mainLabelComp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mainLabelComp.setForeground(new Color(100, 100, 120));
        mainLabelComp.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subLabel = new JLabel(subValue);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(new Color(150, 150, 170));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(mainLabelComp);
        card.add(Box.createVerticalStrut(5));
        card.add(subLabel);
        
        return card;
    }
    
    public void updateAgent(String name, String type, double x, double y, String status, 
                           boolean available, int capacity, int currentLoad) {
        AgentInfo info = agents.computeIfAbsent(name, k -> new AgentInfo(name, type));
        info.x = x;
        info.y = y;
        info.status = status;
        info.available = available;
        info.capacity = capacity;
        info.currentLoad = currentLoad;
    }
    
    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = String.format("[%tH:%<tM:%<tS] ", System.currentTimeMillis());
            logArea.append(timestamp + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void addEmergency(String id, String type, String location, double x, double y) {
        emergencies.put(id, new EmergencyStatus(id, type, location, x, y));
        totalEmergencies++;
    }
    
    public void updateEmergency(String id, String status) {
        EmergencyStatus emergency = emergencies.get(id);
        if (emergency != null) {
            emergency.status = status;
        }
    }
    
    public void updateCoordination(String emergencyId, String ambulance, String hospital, 
                                  int policeUnits, int trafficLights) {
        // Simplified
    }
    
    public void removeEmergency(String id) {
        emergencies.remove(id);
        resolvedEmergencies++;
    }
    
    class MapPanel extends JPanel {
        private Map<String, Point2D> locations;
        
        public MapPanel() {
            locations = new HashMap<>();
            initLocations();
        }
        
        private void initLocations() {
            String[] names = {
                "Downtown", "Suburb_A", "Industrial_Zone", "Highway_Exit",
                "Station1", "ResidentialArea", "Station2", "Zone1",
                "Intersection1", "Intersection2", "Intersection3", "Zone2"
            };
            
            for (int i = 0; i < names.length; i++) {
                int col = i % 4;
                int row = i / 3;
                locations.put(names[i], new Point2D.Double(
                    120 + col * 220,
                    120 + row * 200
                ));
            }
            
            for (int i = 0; i <= 9; i++) {
                locations.put("Location" + i, new Point2D.Double(
                    80 + (i % 5) * 190,
                    80 + (i / 5) * 270
                ));
            }
            
            locations.put("ControlCenter", new Point2D.Double(500, 50));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            drawRoads(g2);
            drawLocationLabels(g2);
            drawEmergencies(g2);
            drawAgents(g2);
            drawLegend(g2);
        }
        
        private void drawRoads(Graphics2D g2) {
            // Modern road style
            g2.setColor(new Color(220, 220, 230));
            g2.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            // Horizontal roads
            for (int i = 1; i <= 4; i++) {
                g2.drawLine(30, i * 170, getWidth() - 30, i * 170);
            }
            
            // Vertical roads
            for (int i = 1; i <= 5; i++) {
                g2.drawLine(i * 190, 30, i * 190, getHeight() - 30);
            }
            
            // Road markings (dashed lines)
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
                         0, new float[]{20, 20}, 0));
            for (int i = 1; i <= 4; i++) {
                g2.drawLine(30, i * 170, getWidth() - 30, i * 170);
            }
            for (int i = 1; i <= 5; i++) {
                g2.drawLine(i * 190, 30, i * 190, getHeight() - 30);
            }
        }
        
        private void drawLocationLabels(Graphics2D g2) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(new Color(120, 120, 140));
            
            for (Map.Entry<String, Point2D> entry : locations.entrySet()) {
                if (entry.getKey().startsWith("Location")) continue;
                
                Point2D pos = entry.getValue();
                String name = entry.getKey().replace("_", " ");
                int width = g2.getFontMetrics().stringWidth(name);
                
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillRoundRect((int)pos.getX() - width/2 - 5, (int)pos.getY() + 45, 
                               width + 10, 18, 8, 8);
                g2.setColor(new Color(100, 100, 120));
                g2.drawString(name, (int)pos.getX() - width/2, (int)pos.getY() + 58);
            }
        }
        
        private void drawEmergencies(Graphics2D g2) {
            for (EmergencyStatus emergency : emergencies.values()) {
                Point2D pos = locations.getOrDefault(emergency.location, 
                    new Point2D.Double(emergency.x, emergency.y));
                
                // Animated pulsing effect
                long time = System.currentTimeMillis();
                float pulse = (float)(Math.sin(time / 120.0) * 0.25 + 0.75);
                int size = (int)(80 * pulse);
                
                // Outer glow
                g2.setColor(new Color(255, 0, 0, 60));
                g2.fillOval((int)pos.getX() - size/2, (int)pos.getY() - size/2, size, size);
                
                // Inner circle
                g2.setColor(new Color(244, 67, 54));
                g2.fillOval((int)pos.getX() - 25, (int)pos.getY() - 25, 50, 50);
                
                // White exclamation mark
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 36));
                g2.drawString("!", (int)pos.getX() - 9, (int)pos.getY() + 12);
                
                // Emergency type label with shadow
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                String label = emergency.type.toUpperCase();
                int width = g2.getFontMetrics().stringWidth(label);
                
                // Shadow
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect((int)pos.getX() - width/2 - 7, (int)pos.getY() + 27, 
                               width + 14, 22, 11, 11);
                
                // Label background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect((int)pos.getX() - width/2 - 6, (int)pos.getY() + 26, 
                               width + 12, 20, 10, 10);
                
                // Label text
                g2.setColor(new Color(244, 67, 54));
                g2.drawString(label, (int)pos.getX() - width/2, (int)pos.getY() + 41);
            }
        }
        
        private void drawAgents(Graphics2D g2) {
            for (AgentInfo agent : agents.values()) {
                if ("traffic-light".equals(agent.type)) {
                    drawTrafficLight(g2, agent);
                } else {
                    drawAgent(g2, agent);
                }
            }
        }
        
        private void drawTrafficLight(Graphics2D g2, AgentInfo agent) {
            Point2D pos = getPosition(agent);
            
            // Modern traffic light box with gradient
            GradientPaint gradient = new GradientPaint(
                (int)pos.getX() - 15, (int)pos.getY() - 40,
                new Color(60, 60, 70),
                (int)pos.getX() + 15, (int)pos.getY() + 40,
                new Color(40, 40, 50)
            );
            g2.setPaint(gradient);
            g2.fillRoundRect((int)pos.getX() - 15, (int)pos.getY() - 40, 30, 80, 10, 10);
            
            // Light housings
            g2.setColor(new Color(30, 30, 35));
            g2.fillOval((int)pos.getX() - 10, (int)pos.getY() - 30, 20, 20);
            g2.fillOval((int)pos.getX() - 10, (int)pos.getY() - 10, 20, 20);
            g2.fillOval((int)pos.getX() - 10, (int)pos.getY() + 10, 20, 20);
            
            // Active light with glow
            if (agent.status != null) {
                Color lightColor = new Color(60, 60, 60);
                int yOffset = 0;
                
                if (agent.status.contains("RED")) {
                    lightColor = new Color(244, 67, 54);
                    yOffset = -30;
                } else if (agent.status.contains("YELLOW")) {
                    lightColor = new Color(255, 193, 7);
                    yOffset = -10;
                } else if (agent.status.contains("GREEN")) {
                    lightColor = new Color(76, 175, 80);
                    yOffset = 10;
                }
                
                // Glow effect
                g2.setColor(new Color(lightColor.getRed(), lightColor.getGreen(), 
                                     lightColor.getBlue(), 80));
                g2.fillOval((int)pos.getX() - 14, (int)pos.getY() + yOffset - 4, 28, 28);
                
                // Bright light
                g2.setColor(lightColor);
                g2.fillOval((int)pos.getX() - 8, (int)pos.getY() + yOffset, 16, 16);
                
                // Highlight
                g2.setColor(new Color(255, 255, 255, 150));
                g2.fillOval((int)pos.getX() - 5, (int)pos.getY() + yOffset + 3, 6, 6);
            }
            
            // Priority mode indicator
            if (agent.status != null && agent.status.contains("PRIORITY")) {
                g2.setColor(new Color(244, 67, 54));
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect((int)pos.getX() - 20, (int)pos.getY() - 45, 40, 90, 15, 15);
                
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.drawString("PRIORITY", (int)pos.getX() - 22, (int)pos.getY() + 60);
            }
        }
        
        private void drawAgent(Graphics2D g2, AgentInfo agent) {
            Point2D pos = getPosition(agent);
            Color color = getAgentColor(agent);
            String label = getAgentLabel(agent);
            
            // Shadow
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillOval((int)pos.getX() - 23, (int)pos.getY() - 21, 46, 46);
            
            // Main circle with gradient
            GradientPaint gradient = new GradientPaint(
                (int)pos.getX(), (int)pos.getY() - 25,
                color.brighter(),
                (int)pos.getX(), (int)pos.getY() + 25,
                color.darker()
            );
            g2.setPaint(gradient);
            g2.fillOval((int)pos.getX() - 25, (int)pos.getY() - 25, 50, 50);
            
            // Status border (available/busy)
            g2.setStroke(new BasicStroke(4));
            if (agent.available) {
                g2.setColor(new Color(76, 175, 80));
            } else {
                g2.setColor(new Color(244, 67, 54));
            }
            g2.drawOval((int)pos.getX() - 25, (int)pos.getY() - 25, 50, 50);
            
            // Inner highlight
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillOval((int)pos.getX() - 20, (int)pos.getY() - 20, 20, 20);
            
            // Label with shadow
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            
            // Text shadow
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawString(label, (int)pos.getX() - labelWidth/2 + 1, (int)pos.getY() + 6);
            
            // Text
            g2.setColor(Color.WHITE);
            g2.drawString(label, (int)pos.getX() - labelWidth/2, (int)pos.getY() + 5);
            
            // Name label below
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String name = agent.name;
            int nameWidth = g2.getFontMetrics().stringWidth(name);
            
            // Label background
            g2.setColor(Color.WHITE);
            g2.fillRoundRect((int)pos.getX() - nameWidth/2 - 6, (int)pos.getY() + 32, 
                            nameWidth + 12, 18, 9, 9);
            
            // Label border
            g2.setStroke(new BasicStroke(2));
            g2.setColor(color);
            g2.drawRoundRect((int)pos.getX() - nameWidth/2 - 6, (int)pos.getY() + 32, 
                            nameWidth + 12, 18, 9, 9);
            
            // Name text
            g2.setColor(new Color(60, 60, 80));
            g2.drawString(name, (int)pos.getX() - nameWidth/2, (int)pos.getY() + 45);
        }
        
        private void drawLegend(Graphics2D g2) {
            int x = 25;
            int y = getHeight() - 160;
            
            // Modern card style
            g2.setColor(new Color(255, 255, 255, 250));
            g2.fillRoundRect(x, y, 240, 140, 15, 15);
            g2.setColor(new Color(200, 200, 220));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, 240, 140, 15, 15);
            
            // Title
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(new Color(60, 60, 80));
            g2.drawString("🗺️ LEGEND", x + 15, y + 25);
            
            // Separator line
            g2.setColor(new Color(220, 220, 235));
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(x + 15, y + 32, x + 225, y + 32);
            
            String[] labels = {"Ambulance", "Hospital", "Police", "Vehicle"};
            String[] emojis = {"🚑", "🏥", "👮", "🚗"};
            Color[] colors = {
                new Color(220, 20, 60), 
                new Color(0, 150, 0), 
                new Color(0, 100, 200), 
                new Color(100, 100, 100)
            };
            
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (int i = 0; i < labels.length; i++) {
                // Colored circle
                g2.setColor(colors[i]);
                g2.fillOval(x + 20, y + 45 + i * 22, 16, 16);
                g2.setColor(new Color(76, 175, 80));
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(x + 20, y + 45 + i * 22, 16, 16);
                
                // Text
                g2.setColor(new Color(80, 80, 100));
                g2.drawString(emojis[i] + " " + labels[i], x + 45, y + 58 + i * 22);
            }
            
            // Status legend at bottom
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 10));
            g2.setColor(new Color(76, 175, 80));
            g2.drawString("● Available", x + 20, y + 128);
            g2.setColor(new Color(244, 67, 54));
            g2.drawString("● Busy", x + 130, y + 128);
        }
        
        private Point2D getPosition(AgentInfo agent) {
            if (agent.x > 0 && agent.y > 0) {
                return new Point2D.Double(agent.x, agent.y);
            }
            
            if (agent.status != null) {
                for (String loc : locations.keySet()) {
                    if (agent.status.contains(loc)) {
                        return locations.get(loc);
                    }
                }
            }
            
            int hash = agent.name.hashCode();
            return new Point2D.Double(
                80 + Math.abs(hash % 800),
                80 + Math.abs((hash / 100) % 500)
            );
        }
        
        private Color getAgentColor(AgentInfo agent) {
            switch (agent.type) {
                case "ambulance": return new Color(220, 20, 60);
                case "hospital": return new Color(0, 150, 0);
                case "vehicle": return new Color(100, 100, 100);
                case "police": return new Color(0, 100, 200);
                case "tcc": return new Color(103, 58, 183);
                default: return Color.GRAY;
            }
        }
        
        private String getAgentLabel(AgentInfo agent) {
            switch (agent.type) {
                case "ambulance": return "🚑";
                case "hospital": return "🏥";
                case "vehicle": return "🚗";
                case "police": return "👮";
                case "tcc": return "🎛";
                default: return "?";
            }
        }
    }
    
    static class AgentInfo {
        String name, type, status;
        double x, y;
        boolean available = true;
        int capacity = 0, currentLoad = 0;
        
        AgentInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
    
    static class EmergencyStatus {
        String id, type, location, status;
        double x, y;
        
        EmergencyStatus(String id, String type, String location, double x, double y) {
            this.id = id;
            this.type = type;
            this.location = location;
            this.x = x;
            this.y = y;
            this.status = "active";
        }
    }
}