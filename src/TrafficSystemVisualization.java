import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficSystemVisualization extends JFrame {
    private MapPanel mapPanel;
    private JTextArea logArea;
    private JPanel statsPanel;
    private JPanel coordinationPanel;
    private Map<String, AgentInfo> agents;
    private Map<String, EmergencyStatus> emergencies;
    private static TrafficSystemVisualization instance;
    
    private int totalEmergencies = 0;
    private int resolvedEmergencies = 0;
    private long systemStartTime;
    private Map<String, CoordinationInfo> activeCoordinations;
    private List<String> dispatchProtocols;
    private Map<String, TrafficControlAction> trafficActions;
    
    public static TrafficSystemVisualization getInstance() {
        if (instance == null) {
            instance = new TrafficSystemVisualization();
        }
        return instance;
    }
    
    private TrafficSystemVisualization() {
        agents = new ConcurrentHashMap<>();
        emergencies = new ConcurrentHashMap<>();
        activeCoordinations = new ConcurrentHashMap<>();
        dispatchProtocols = Collections.synchronizedList(new ArrayList<>());
        trafficActions = new ConcurrentHashMap<>();
        systemStartTime = System.currentTimeMillis();
        
        setTitle("Multi-Agent Traffic & Emergency Management System - Enhanced Monitoring");
        setSize(1900, 1050);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 240, 245));
        
        // Top stats panel
        statsPanel = createStatsPanel();
        add(statsPanel, BorderLayout.NORTH);
        
        // Center: Map + Right panels
        JPanel centerContainer = new JPanel(new BorderLayout(10, 0));
        centerContainer.setBackground(new Color(240, 240, 245));
        centerContainer.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        
        // Map
        mapPanel = new MapPanel();
        mapPanel.setPreferredSize(new Dimension(1150, 850));
        mapPanel.setBackground(Color.WHITE);
        mapPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));
        centerContainer.add(mapPanel, BorderLayout.CENTER);
        
        // Right side with coordination and log
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setPreferredSize(new Dimension(700, 850));
        rightPanel.setBackground(new Color(240, 240, 245));
        
        // Coordination panel (top right)
        coordinationPanel = createCoordinationPanel();
        rightPanel.add(coordinationPanel, BorderLayout.NORTH);
        
        // Log panel (bottom right)
        JPanel logPanel = createLogPanel();
        rightPanel.add(logPanel, BorderLayout.CENTER);
        
        centerContainer.add(rightPanel, BorderLayout.EAST);
        add(centerContainer, BorderLayout.CENTER);
        
        // Update timer
        javax.swing.Timer timer = new javax.swing.Timer(150, e -> {
            mapPanel.repaint();
            updateStats();
            updateCoordinationPanel();
        });
        timer.start();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 245));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        panel.setPreferredSize(new Dimension(1900, 120));
        return panel;
    }
    
    private JPanel createCoordinationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(240, 240, 245));
        panel.setPreferredSize(new Dimension(700, 400));
        
        JLabel title = new JLabel("MULTI-AGENT COORDINATION & DISPATCH PROTOCOLS", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 13));
        title.setForeground(new Color(80, 80, 80));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        
        return panel;
    }
    
    private void updateCoordinationPanel() {
        Component titleComp = coordinationPanel.getComponent(0);
        coordinationPanel.removeAll();
        coordinationPanel.add(titleComp);
        
        // Show active coordinations
        if (!emergencies.isEmpty()) {
            for (EmergencyStatus emergency : emergencies.values()) {
                JPanel emergencyCard = createEmergencyCoordinationCard(emergency);
                coordinationPanel.add(emergencyCard);
                coordinationPanel.add(Box.createVerticalStrut(10));
            }
        } else {
            JPanel noEmergency = new JPanel();
            noEmergency.setBackground(Color.WHITE);
            noEmergency.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));
            noEmergency.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            
            JLabel label = new JLabel("System Stable - No Active Emergencies");
            label.setFont(new Font("Arial", Font.BOLD, 14));
            label.setForeground(new Color(76, 175, 80));
            noEmergency.add(label);
            
            coordinationPanel.add(noEmergency);
        }
        
        // Show recent dispatch protocols
        if (!dispatchProtocols.isEmpty()) {
            coordinationPanel.add(Box.createVerticalStrut(10));
            JPanel protocolPanel = createDispatchProtocolPanel();
            coordinationPanel.add(protocolPanel);
        }
        
        coordinationPanel.revalidate();
        coordinationPanel.repaint();
    }
    
    private JPanel createEmergencyCoordinationCard(EmergencyStatus emergency) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(244, 67, 54), 3),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        
        // Emergency header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        header.setBackground(Color.WHITE);
        
        JLabel idLabel = new JLabel(emergency.id);
        idLabel.setFont(new Font("Arial", Font.BOLD, 14));
        idLabel.setForeground(new Color(244, 67, 54));
        
        JLabel typeLabel = new JLabel(emergency.type.toUpperCase() + " @ " + emergency.location);
        typeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        typeLabel.setForeground(new Color(100, 100, 100));
        
        header.add(idLabel);
        header.add(new JLabel(" | "));
        header.add(typeLabel);
        card.add(header);
        card.add(Box.createVerticalStrut(10));
        
        // Coordination status
        CoordinationInfo coord = activeCoordinations.get(emergency.id);
        if (coord != null) {
            card.add(createCoordItem("Ambulance:", coord.ambulance, coord.ambulanceStatus));
            card.add(createCoordItem("Hospital:", coord.hospital, coord.hospitalStatus));
            card.add(createCoordItem("Police Support:", coord.policeUnits, coord.policeStatus));
            card.add(createCoordItem("Traffic Control:", coord.trafficLights + " lights", coord.trafficStatus));
        } else {
            card.add(createCoordItem("Status:", "Dispatching units...", "INITIATING"));
        }
        
        return card;
    }
    
    private JPanel createCoordItem(String label, String value, String status) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setBackground(Color.WHITE);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Arial", Font.BOLD, 11));
        labelComp.setForeground(new Color(80, 80, 80));
        labelComp.setPreferredSize(new Dimension(120, 20));
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Arial", Font.PLAIN, 11));
        
        JLabel statusComp = new JLabel(status);
        statusComp.setFont(new Font("Arial", Font.BOLD, 10));
        Color statusColor = Color.GRAY;
        if (status.contains("DISPATCHED") || status.contains("ACTIVE")) {
            statusColor = new Color(255, 152, 0);
        } else if (status.contains("COMPLETE") || status.contains("SECURED")) {
            statusColor = new Color(76, 175, 80);
        } else if (status.contains("EN ROUTE")) {
            statusColor = new Color(33, 150, 243);
        }
        statusComp.setForeground(statusColor);
        
        item.add(labelComp, BorderLayout.WEST);
        item.add(valueComp, BorderLayout.CENTER);
        item.add(statusComp, BorderLayout.EAST);
        
        return item;
    }
    
    private JPanel createDispatchProtocolPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(255, 248, 225));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 152, 0), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        JLabel title = new JLabel("RECENT DISPATCH PROTOCOLS");
        title.setFont(new Font("Arial", Font.BOLD, 11));
        title.setForeground(new Color(255, 152, 0));
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        
        int count = 0;
        for (int i = dispatchProtocols.size() - 1; i >= 0 && count < 4; i--, count++) {
            JLabel protocol = new JLabel("• " + dispatchProtocols.get(i));
            protocol.setFont(new Font("Arial", Font.PLAIN, 10));
            protocol.setForeground(new Color(80, 80, 80));
            panel.add(protocol);
            panel.add(Box.createVerticalStrut(3));
        }
        
        return panel;
    }
    
    private void updateStats() {
        statsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Count agents by type
        int ambulances = 0, ambulancesAvailable = 0;
        int hospitals = 0, hospitalBeds = 0, hospitalUsed = 0;
        int vehicles = 0;
        int police = 0, policeAvailable = 0;
        int lights = 0, lightsGreen = 0, lightsPriority = 0;
        
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
                case "vehicle":
                    vehicles++;
                    break;
                case "police":
                    police++;
                    if (agent.available) policeAvailable++;
                    break;
                case "traffic-light":
                    lights++;
                    if (agent.status != null) {
                        if (agent.status.contains("GREEN")) lightsGreen++;
                        if (agent.status.contains("PRIORITY")) lightsPriority++;
                    }
                    break;
            }
        }
        
        // System uptime
        long uptime = (System.currentTimeMillis() - systemStartTime) / 1000;
        String uptimeStr = String.format("%02d:%02d:%02d", uptime / 3600, (uptime % 3600) / 60, uptime % 60);
        
        // Coordination efficiency
        double coordEfficiency = totalEmergencies > 0 ? (resolvedEmergencies * 100.0 / totalEmergencies) : 100.0;
        
        // Add stat cards
        gbc.gridx = 0;
        statsPanel.add(createStatCard("SYSTEM UPTIME", uptimeStr, 
            "Multi-Agent System Active", new Color(103, 58, 183)), gbc);
        
        gbc.gridx = 1;
        statsPanel.add(createStatCard("EMERGENCIES", 
            emergencies.size() + " Active", 
            totalEmergencies + " total | " + resolvedEmergencies + " resolved", 
            emergencies.size() > 0 ? new Color(244, 67, 54) : new Color(76, 175, 80)), gbc);
        
        gbc.gridx = 2;
        statsPanel.add(createStatCard("AMBULANCES", 
            ambulancesAvailable + "/" + ambulances + " Ready", 
            "Dispatch Protocol: Active", 
            new Color(233, 30, 99)), gbc);
        
        gbc.gridx = 3;
        statsPanel.add(createStatCard("HOSPITALS", 
            (hospitalBeds - hospitalUsed) + "/" + hospitalBeds + " Beds Free", 
            "Coordination: " + hospitals + " facilities", 
            new Color(0, 150, 136)), gbc);
        
        gbc.gridx = 4;
        statsPanel.add(createStatCard("POLICE UNITS", 
            policeAvailable + "/" + police + " On Patrol", 
            "Traffic Support: Active", 
            new Color(33, 150, 243)), gbc);
        
        gbc.gridx = 5;
        statsPanel.add(createStatCard("TRAFFIC CONTROL", 
            lightsPriority > 0 ? lightsPriority + " PRIORITY" : lightsGreen + "/" + lights + " Green", 
            "Monitoring: " + vehicles + " vehicles", 
            lightsPriority > 0 ? new Color(244, 67, 54) : new Color(255, 152, 0)), gbc);
        
        gbc.gridx = 6;
        statsPanel.add(createStatCard("COORDINATION", 
            String.format("%.0f%%", coordEfficiency), 
            "System Efficiency", 
            coordEfficiency >= 90 ? new Color(76, 175, 80) : new Color(255, 152, 0)), gbc);
        
        statsPanel.revalidate();
        statsPanel.repaint();
    }
    
    private JPanel createStatCard(String title, String mainValue, String subValue, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 3),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 10));
        titleLabel.setForeground(new Color(120, 120, 120));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel mainLabel = new JLabel(mainValue);
        mainLabel.setFont(new Font("Arial", Font.BOLD, 20));
        mainLabel.setForeground(accentColor);
        mainLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subLabel = new JLabel(subValue);
        subLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        subLabel.setForeground(new Color(150, 150, 150));
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(mainLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(subLabel);
        
        return card;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(240, 240, 245));
        
        JLabel title = new JLabel("REAL-TIME STATUS MONITORING", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 13));
        title.setForeground(new Color(80, 80, 80));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(30, 30, 35));
        logArea.setForeground(new Color(0, 255, 100));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
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
        info.lastUpdate = System.currentTimeMillis();
    }
    
    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = String.format("[%tT] ", System.currentTimeMillis());
            logArea.append(timestamp + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
            
            // Track dispatch protocols
            if (message.contains("DISPATCHED") || message.contains("selected") || 
                message.contains("PRIORITY") || message.contains("route secured")) {
                dispatchProtocols.add(message);
                if (dispatchProtocols.size() > 10) {
                    dispatchProtocols.remove(0);
                }
            }
        });
    }
    
    public void addEmergency(String id, String type, String location, double x, double y) {
        emergencies.put(id, new EmergencyStatus(id, type, location, x, y));
        totalEmergencies++;
        
        // Initialize coordination tracking
        CoordinationInfo coord = new CoordinationInfo();
        coord.emergencyId = id;
        activeCoordinations.put(id, coord);
    }
    
    public void updateEmergency(String id, String status) {
        EmergencyStatus emergency = emergencies.get(id);
        if (emergency != null) {
            emergency.status = status;
        }
        
        // Update coordination info
        CoordinationInfo coord = activeCoordinations.get(id);
        if (coord != null && status != null) {
            if (status.contains("Ambulance dispatched")) {
                coord.ambulanceStatus = "DISPATCHED";
            } else if (status.contains("Hospital assigned")) {
                coord.hospitalStatus = "READY";
            }
        }
    }
    
    public void updateCoordination(String emergencyId, String ambulance, String hospital, 
                                  int policeUnits, int trafficLights) {
        CoordinationInfo coord = activeCoordinations.get(emergencyId);
        if (coord != null) {
            coord.ambulance = ambulance;
            coord.hospital = hospital;
            coord.policeUnits = policeUnits + " units";
            coord.trafficLights = trafficLights;
            coord.ambulanceStatus = "EN ROUTE";
            coord.hospitalStatus = "READY";
            coord.policeStatus = "SECURING ROUTE";
            coord.trafficStatus = "PRIORITY MODE";
        }
    }
    
    public void removeEmergency(String id) {
        emergencies.remove(id);
        activeCoordinations.remove(id);
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
                int row = i / 4;
                locations.put(names[i], new Point2D.Double(
                    180 + col * 240,
                    180 + row * 240
                ));
            }
            
            for (int i = 0; i <= 9; i++) {
                locations.put("Location" + i, new Point2D.Double(
                    140 + (i % 5) * 220,
                    140 + (i / 5) * 300
                ));
            }
            
            locations.put("ControlCenter", new Point2D.Double(575, 90));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            drawRoads(g2);
            drawCoordinationLines(g2);
            drawEmergencies(g2);
            drawAgents(g2);
            drawLegend(g2);
            drawCoordinationIndicators(g2);
        }
        
        private void drawRoads(Graphics2D g2) {
            g2.setColor(new Color(230, 230, 230));
            g2.setStroke(new BasicStroke(16));
            
            for (int i = 1; i <= 3; i++) {
                g2.drawLine(50, i * 240, getWidth() - 50, i * 240);
            }
            
            for (int i = 1; i <= 4; i++) {
                g2.drawLine(i * 240, 50, i * 240, getHeight() - 50);
            }
            
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
                         0, new float[]{25, 25}, 0));
            for (int i = 1; i <= 3; i++) {
                g2.drawLine(50, i * 240, getWidth() - 50, i * 240);
            }
            for (int i = 1; i <= 4; i++) {
                g2.drawLine(i * 240, 50, i * 240, getHeight() - 50);
            }
        }
        
        private void drawCoordinationLines(Graphics2D g2) {
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                         0, new float[]{10, 10}, 0));
            
            for (EmergencyStatus emergency : emergencies.values()) {
                Point2D emergencyPos = locations.getOrDefault(emergency.location,
                    new Point2D.Double(emergency.x, emergency.y));
                
                for (AgentInfo agent : agents.values()) {
                    if ((agent.type.equals("ambulance") && !agent.available) ||
                        (agent.type.equals("police") && !agent.available)) {
                        
                        Point2D agentPos = getPosition(agent);
                        
                        Color lineColor = agent.type.equals("ambulance") ? 
                            new Color(220, 20, 60, 150) : new Color(33, 150, 243, 150);
                        
                        g2.setColor(lineColor);
                        g2.drawLine((int)emergencyPos.getX(), (int)emergencyPos.getY(),
                                   (int)agentPos.getX(), (int)agentPos.getY());
                    }
                }
            }
        }
        
        private void drawCoordinationIndicators(Graphics2D g2) {
            int y = 20;
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            
            // Count active coordinations
            int priorityRoutes = 0;
            for (AgentInfo agent : agents.values()) {
                if (agent.type.equals("traffic-light") && agent.status != null && 
                    agent.status.contains("PRIORITY")) {
                    priorityRoutes++;
                }
            }
            
            if (priorityRoutes > 0) {
                g2.setColor(new Color(255, 255, 255, 230));
                g2.fillRoundRect(getWidth() - 280, y, 260, 70, 10, 10);
                g2.setColor(new Color(244, 67, 54));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(getWidth() - 280, y, 260, 70, 10, 10);
                
                g2.setColor(new Color(244, 67, 54));
                g2.drawString("TRAFFIC CONTROL ACTIVE", getWidth() - 270, y + 20);
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                g2.setColor(Color.BLACK);
                g2.drawString(priorityRoutes + " intersections in priority mode", getWidth() - 270, y + 40);
                g2.drawString("Emergency route secured", getWidth() - 270, y + 55);
            }
        }
        
        private void drawEmergencies(Graphics2D g2) {
            for (EmergencyStatus emergency : emergencies.values()) {
                Point2D pos = locations.getOrDefault(emergency.location, 
                    new Point2D.Double(emergency.x, emergency.y));
                
                long time = System.currentTimeMillis();
                float pulse = (float)(Math.sin(time / 200.0) * 0.3 + 0.7);
                int size = (int)(90 * pulse);
                
                g2.setColor(new Color(255, 0, 0, 80));
                g2.fillOval((int)pos.getX() - size/2, (int)pos.getY() - size/2, size, size);
                
                g2.setStroke(new BasicStroke(4));
                g2.setColor(new Color(255, 0, 0));
                g2.drawOval((int)pos.getX() - 40, (int)pos.getY() - 40, 80, 80);
                
                g2.setColor(Color.RED);
                g2.setFont(new Font("Arial", Font.BOLD, 45));
                g2.drawString("!", (int)pos.getX() - 12, (int)pos.getY() + 18);
                
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                g2.setColor(Color.BLACK);
                String type = emergency.type.toUpperCase();
                int width = g2.getFontMetrics().stringWidth(type);
                
                g2.setColor(Color.WHITE);
                g2.fillRoundRect((int)pos.getX() - width/2 - 10, (int)pos.getY() + 35, 
                                width + 20, 28, 8, 8);
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect((int)pos.getX() - width/2 - 10, (int)pos.getY() + 35, 
                                width + 20, 28, 8, 8);
                g2.setColor(Color.BLACK);
                g2.drawString(type, (int)pos.getX() - width/2, (int)pos.getY() + 54);
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
            
            g2.setColor(new Color(60, 60, 60));
            g2.fillRoundRect((int)pos.getX() - 20, (int)pos.getY() - 50, 40, 100, 12, 12);
            
            g2.setColor(new Color(40, 40, 40));
            g2.fillOval((int)pos.getX() - 15, (int)pos.getY() - 40, 30, 30);
            g2.fillOval((int)pos.getX() - 15, (int)pos.getY() - 5, 30, 30);
            g2.fillOval((int)pos.getX() - 15, (int)pos.getY() + 20, 30, 30);
            
            if (agent.status != null) {
                Color lightColor = Color.GRAY;
                int yOffset = 0;
                
                if (agent.status.contains("RED")) {
                    lightColor = new Color(244, 67, 54);
                    yOffset = -40;
                } else if (agent.status.contains("YELLOW")) {
                    lightColor = new Color(255, 235, 59);
                    yOffset = -5;
                } else if (agent.status.contains("GREEN")) {
                    lightColor = new Color(76, 175, 80);
                    yOffset = 20;
                }
                
                g2.setColor(lightColor);
                g2.fillOval((int)pos.getX() - 12, (int)pos.getY() + yOffset, 24, 24);
                
                g2.setColor(new Color(lightColor.getRed(), lightColor.getGreen(), 
                                     lightColor.getBlue(), 50));
                g2.fillOval((int)pos.getX() - 18, (int)pos.getY() + yOffset - 6, 36, 36);
            }
            
            if (agent.status != null && agent.status.contains("PRIORITY")) {
                g2.setColor(new Color(255, 0, 0, 60));
                g2.fillOval((int)pos.getX() - 65, (int)pos.getY() - 65, 130, 130);
                
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                g2.setColor(Color.RED);
                String priorityText = "PRIORITY";
                int width = g2.getFontMetrics().stringWidth(priorityText);
                g2.drawString(priorityText, (int)pos.getX() - width/2, (int)pos.getY() + 75);
            }
        }
        
        private void drawAgent(Graphics2D g2, AgentInfo agent) {
            Point2D pos = getPosition(agent);
            Color color = getAgentColor(agent);
            String label = getAgentLabel(agent);
            
            g2.setColor(color);
            g2.fillOval((int)pos.getX() - 25, (int)pos.getY() - 25, 50, 50);
            
            g2.setStroke(new BasicStroke(4));
            g2.setColor(agent.available ? new Color(76, 175, 80) : new Color(244, 67, 54));
            g2.drawOval((int)pos.getX() - 25, (int)pos.getY() - 25, 50, 50);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2.drawString(label, (int)pos.getX() - labelWidth/2, (int)pos.getY() + 5);
            
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(Color.BLACK);
            String name = agent.name;
            int nameWidth = g2.getFontMetrics().stringWidth(name);
            
            g2.setColor(Color.WHITE);
            g2.fillRoundRect((int)pos.getX() - nameWidth/2 - 5, (int)pos.getY() + 30, 
                            nameWidth + 10, 20, 6, 6);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect((int)pos.getX() - nameWidth/2 - 5, (int)pos.getY() + 30, 
                            nameWidth + 10, 20, 6, 6);
            g2.setColor(Color.BLACK);
            g2.drawString(name, (int)pos.getX() - nameWidth/2, (int)pos.getY() + 44);
            
            if ("hospital".equals(agent.type) && agent.capacity > 0) {
                String capacity = agent.currentLoad + "/" + agent.capacity;
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                int capWidth = g2.getFontMetrics().stringWidth(capacity);
                g2.setColor(new Color(100, 100, 100));
                g2.drawString(capacity, (int)pos.getX() - capWidth/2, (int)pos.getY() + 58);
            }
        }
        
        private void drawLegend(Graphics2D g2) {
            int x = 30;
            int y = getHeight() - 180;
            
            g2.setColor(new Color(255, 255, 255, 250));
            g2.fillRoundRect(x, y, 300, 160, 15, 15);
            g2.setColor(new Color(100, 100, 100));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, 300, 160, 15, 15);
            
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(new Color(50, 50, 50));
            g2.drawString("LEGEND", x + 15, y + 25);
            
            String[] labels = {"AMB - Ambulance", "HOS - Hospital", "VEH - Vehicle", 
                              "POL - Police", "TCC - Control Center"};
            Color[] colors = {new Color(220, 20, 60), new Color(0, 150, 0), 
                            new Color(100, 100, 100), new Color(0, 100, 200), 
                            new Color(50, 50, 150)};
            
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            for (int i = 0; i < labels.length; i++) {
                g2.setColor(colors[i]);
                g2.fillOval(x + 20, y + 45 + i * 25, 20, 20);
                
                g2.setColor(new Color(76, 175, 80));
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(x + 20, y + 45 + i * 25, 20, 20);
                
                g2.setColor(Color.BLACK);
                g2.drawString(labels[i], x + 50, y + 60 + i * 25);
            }
            
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(76, 175, 80));
            g2.fillOval(x + 190, y + 50, 15, 15);
            g2.setColor(Color.BLACK);
            g2.drawString("Available", x + 210, y + 62);
            
            g2.setColor(new Color(244, 67, 54));
            g2.fillOval(x + 190, y + 75, 15, 15);
            g2.setColor(Color.BLACK);
            g2.drawString("Busy", x + 210, y + 87);
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
                150 + Math.abs(hash % 900),
                150 + Math.abs((hash / 100) % 600)
            );
        }
        
        private Color getAgentColor(AgentInfo agent) {
            switch (agent.type) {
                case "ambulance": return new Color(220, 20, 60);
                case "hospital": return new Color(0, 150, 0);
                case "vehicle": return new Color(100, 100, 100);
                case "police": return new Color(0, 100, 200);
                case "tcc": return new Color(50, 50, 150);
                default: return Color.GRAY;
            }
        }
        
        private String getAgentLabel(AgentInfo agent) {
            switch (agent.type) {
                case "ambulance": return "AMB";
                case "hospital": return "HOS";
                case "vehicle": return "VEH";
                case "police": return "POL";
                case "tcc": return "TCC";
                default: return "?";
            }
        }
    }
    
    static class AgentInfo {
        String name, type, status;
        double x, y;
        boolean available = true;
        int capacity = 0, currentLoad = 0;
        long lastUpdate;
        
        AgentInfo(String name, String type) {
            this.name = name;
            this.type = type;
            this.lastUpdate = System.currentTimeMillis();
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
    
    static class CoordinationInfo {
        String emergencyId;
        String ambulance = "Pending";
        String hospital = "Pending";
        String policeUnits = "0 units";
        int trafficLights = 0;
        String ambulanceStatus = "INITIATING";
        String hospitalStatus = "STANDBY";
        String policeStatus = "STANDBY";
        String trafficStatus = "NORMAL";
    }
}