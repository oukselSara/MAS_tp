import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficSystemVisualization extends JFrame {
    private VisualizationPanel visualPanel;
    private JTextArea logArea;
    private JPanel statusPanel;
    private Map<String, AgentInfo> agents;
    private List<String> recentLogs;
    private Map<String, EmergencyStatus> emergencies;
    private static TrafficSystemVisualization instance;
    
    public static TrafficSystemVisualization getInstance() {
        if (instance == null) {
            instance = new TrafficSystemVisualization();
        }
        return instance;
    }
    
    private TrafficSystemVisualization() {
        agents = new ConcurrentHashMap<>();
        recentLogs = Collections.synchronizedList(new ArrayList<>());
        emergencies = new ConcurrentHashMap<>();
        
        setTitle("Traffic & Medical Emergency System - Real-Time Visualization");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Create main visualization panel
        visualPanel = new VisualizationPanel();
        visualPanel.setPreferredSize(new Dimension(900, 900));
        
        // Create right side panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(500, 900));
        
        // Status panel at top
        statusPanel = createStatusPanel();
        rightPanel.add(statusPanel, BorderLayout.NORTH);
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 0));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(500, 600));
        rightPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Legend panel at bottom
        JPanel legendPanel = createLegendPanel();
        rightPanel.add(legendPanel, BorderLayout.SOUTH);
        
        add(visualPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        
        // Start update timer
        javax.swing.Timer updateTimer = new javax.swing.Timer(100, e -> {
            visualPanel.repaint();
            updateStatusPanel();
        });
        updateTimer.start();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 5, 5));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createTitledBorder("System Status"));
        panel.setPreferredSize(new Dimension(500, 200));
        return panel;
    }
    
    private void updateStatusPanel() {
        statusPanel.removeAll();
        
        int ambulances = 0, ambulancesAvailable = 0;
        int hospitals = 0, hospitalCapacity = 0, hospitalPatients = 0;
        int vehicles = 0;
        int police = 0;
        int activeEmergencies = emergencies.size();
        
        for (AgentInfo agent : agents.values()) {
            switch (agent.type) {
                case "ambulance":
                    ambulances++;
                    if (agent.available) ambulancesAvailable++;
                    break;
                case "hospital":
                    hospitals++;
                    hospitalCapacity += agent.capacity;
                    hospitalPatients += agent.currentLoad;
                    break;
                case "vehicle":
                    vehicles++;
                    break;
                case "police":
                    police++;
                    break;
            }
        }
        
        statusPanel.add(createStatusLabel("Active Emergencies: " + activeEmergencies, 
            activeEmergencies > 0 ? Color.RED : Color.GREEN));
        statusPanel.add(createStatusLabel(
            String.format("Ambulances: %d total, %d available", ambulances, ambulancesAvailable),
            Color.BLUE));
        statusPanel.add(createStatusLabel(
            String.format("Hospitals: %d total, %d/%d patients", hospitals, hospitalPatients, hospitalCapacity),
            Color.MAGENTA));
        statusPanel.add(createStatusLabel("Vehicles: " + vehicles, Color.DARK_GRAY));
        statusPanel.add(createStatusLabel("Police Units: " + police, new Color(0, 100, 200)));
        
        statusPanel.revalidate();
        statusPanel.repaint();
    }
    
    private JLabel createStatusLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(color);
        return label;
    }
    
    private JPanel createLegendPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Legend"));
        panel.setBackground(Color.WHITE);
        
        panel.add(createLegendItem("🚑 Ambulance", Color.RED));
        panel.add(createLegendItem("🏥 Hospital", new Color(0, 150, 0)));
        panel.add(createLegendItem("🚗 Vehicle", Color.DARK_GRAY));
        panel.add(createLegendItem("👮 Police", Color.BLUE));
        panel.add(createLegendItem("🚦 Traffic Light", new Color(100, 100, 100)));
        panel.add(createLegendItem("⚠️ Emergency", Color.ORANGE));
        
        return panel;
    }
    
    private JPanel createLegendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
        item.setBackground(Color.WHITE);
        
        JPanel colorBox = new JPanel();
        colorBox.setPreferredSize(new Dimension(20, 20));
        colorBox.setBackground(color);
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        
        item.add(colorBox);
        item.add(label);
        return item;
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
            recentLogs.add(0, message);
            if (recentLogs.size() > 100) {
                recentLogs.remove(recentLogs.size() - 1);
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void addEmergency(String id, String type, String location, double x, double y) {
        EmergencyStatus status = new EmergencyStatus(id, type, location, x, y);
        emergencies.put(id, status);
    }
    
    public void updateEmergency(String id, String status) {
        EmergencyStatus emergency = emergencies.get(id);
        if (emergency != null) {
            emergency.status = status;
        }
    }
    
    public void removeEmergency(String id) {
        emergencies.remove(id);
    }
    
    class VisualizationPanel extends JPanel {
        private final int GRID_SIZE = 100;
        private Map<String, Point2D> locationMap;
        
        public VisualizationPanel() {
            setBackground(new Color(250, 250, 250));
            initializeLocations();
        }
        
        private void initializeLocations() {
            locationMap = new HashMap<>();
            // Create a grid of locations
            String[] locations = {
                "Downtown", "Suburb_A", "Industrial_Zone", "ResidentialArea", "Highway_Exit",
                "Station1", "Station2", "Zone1", "Zone2",
                "Intersection1", "Intersection2", "Intersection3"
            };
            
            int cols = 4;
            for (int i = 0; i < locations.length; i++) {
                int row = i / cols;
                int col = i % cols;
                locationMap.put(locations[i], new Point2D.Double(
                    150 + col * 180,
                    150 + row * 180
                ));
            }
            
            // Add numbered locations
            for (int i = 0; i <= 9; i++) {
                String loc = "Location" + i;
                int col = i % 5;
                int row = i / 5;
                locationMap.put(loc, new Point2D.Double(
                    100 + col * 150,
                    100 + row * 150
                ));
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw road network
            drawRoadNetwork(g2d);
            
            // Draw intersections
            drawIntersections(g2d);
            
            // Draw emergencies
            drawEmergencies(g2d);
            
            // Draw agents
            drawAgents(g2d);
            
            // Draw connections
            drawConnections(g2d);
        }
        
        private void drawRoadNetwork(Graphics2D g2d) {
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(8));
            
            // Draw horizontal roads
            for (int i = 1; i < 6; i++) {
                g2d.drawLine(50, i * 150, 850, i * 150);
            }
            
            // Draw vertical roads
            for (int i = 1; i < 6; i++) {
                g2d.drawLine(i * 150, 50, i * 150, 800);
            }
        }
        
        private void drawIntersections(Graphics2D g2d) {
            for (AgentInfo agent : agents.values()) {
                if ("traffic-light".equals(agent.type)) {
                    Point2D pos = getAgentPosition(agent);
                    
                    // Draw intersection
                    g2d.setColor(new Color(100, 100, 100));
                    g2d.fillRect((int)pos.getX() - 20, (int)pos.getY() - 20, 40, 40);
                    
                    // Draw traffic light
                    Color lightColor = Color.GRAY;
                    if (agent.status != null) {
                        if (agent.status.contains("GREEN")) {
                            lightColor = Color.GREEN;
                        } else if (agent.status.contains("YELLOW")) {
                            lightColor = Color.YELLOW;
                        } else if (agent.status.contains("RED")) {
                            lightColor = Color.RED;
                        }
                    }
                    
                    g2d.setColor(Color.BLACK);
                    g2d.fillRoundRect((int)pos.getX() - 15, (int)pos.getY() - 35, 30, 70, 10, 10);
                    g2d.setColor(lightColor);
                    g2d.fillOval((int)pos.getX() - 10, (int)pos.getY() - 10, 20, 20);
                    
                    // Priority mode indicator
                    if (agent.status != null && agent.status.contains("PRIORITY")) {
                        g2d.setColor(new Color(255, 0, 0, 100));
                        g2d.fillOval((int)pos.getX() - 40, (int)pos.getY() - 40, 80, 80);
                    }
                }
            }
        }
        
        private void drawEmergencies(Graphics2D g2d) {
            for (EmergencyStatus emergency : emergencies.values()) {
                Point2D pos = locationMap.getOrDefault(emergency.location, 
                    new Point2D.Double(emergency.x, emergency.y));
                
                // Pulsing effect
                long time = System.currentTimeMillis();
                int pulse = (int)(Math.sin(time / 200.0) * 10 + 10);
                
                // Draw emergency icon
                g2d.setColor(new Color(255, 100, 0, 150));
                g2d.fillOval((int)pos.getX() - 25 - pulse, (int)pos.getY() - 25 - pulse, 
                            50 + pulse * 2, 50 + pulse * 2);
                
                g2d.setColor(Color.ORANGE);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval((int)pos.getX() - 25 - pulse, (int)pos.getY() - 25 - pulse, 
                            50 + pulse * 2, 50 + pulse * 2);
                
                // Draw emergency symbol
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString("⚠", (int)pos.getX() - 10, (int)pos.getY() + 8);
                
                // Draw emergency info
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.setColor(Color.BLACK);
                g2d.drawString(emergency.type, (int)pos.getX() - 20, (int)pos.getY() + 40);
            }
        }
        
        private void drawAgents(Graphics2D g2d) {
            for (AgentInfo agent : agents.values()) {
                Point2D pos = getAgentPosition(agent);
                
                // Skip traffic lights (already drawn)
                if ("traffic-light".equals(agent.type)) continue;
                
                // Determine color and shape based on type
                Color color = getAgentColor(agent);
                String symbol = getAgentSymbol(agent);
                
                // Draw agent
                g2d.setColor(color);
                g2d.fillOval((int)pos.getX() - 15, (int)pos.getY() - 15, 30, 30);
                
                // Draw border
                g2d.setColor(agent.available ? Color.GREEN : Color.RED);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval((int)pos.getX() - 15, (int)pos.getY() - 15, 30, 30);
                
                // Draw symbol
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                FontMetrics fm = g2d.getFontMetrics();
                int symbolWidth = fm.stringWidth(symbol);
                g2d.drawString(symbol, (int)pos.getX() - symbolWidth/2, (int)pos.getY() + 6);
                
                // Draw label
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                String label = agent.name;
                if (agent.type.equals("hospital") || agent.type.equals("ambulance")) {
                    label += agent.available ? " ✓" : " ✗";
                }
                int labelWidth = g2d.getFontMetrics().stringWidth(label);
                g2d.drawString(label, (int)pos.getX() - labelWidth/2, (int)pos.getY() + 30);
                
                // Draw status for hospitals
                if ("hospital".equals(agent.type) && agent.capacity > 0) {
                    String capacityStr = agent.currentLoad + "/" + agent.capacity;
                    int capWidth = g2d.getFontMetrics().stringWidth(capacityStr);
                    g2d.drawString(capacityStr, (int)pos.getX() - capWidth/2, (int)pos.getY() + 42);
                }
            }
        }
        
        private void drawConnections(Graphics2D g2d) {
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                         0, new float[]{5, 5}, 0));
            
            for (EmergencyStatus emergency : emergencies.values()) {
                // Draw line from emergency to responding units
                for (AgentInfo agent : agents.values()) {
                    if ((agent.type.equals("ambulance") && !agent.available) ||
                        (agent.type.equals("hospital") && agent.currentLoad > 0)) {
                        
                        Point2D emergencyPos = locationMap.getOrDefault(emergency.location,
                            new Point2D.Double(emergency.x, emergency.y));
                        Point2D agentPos = getAgentPosition(agent);
                        
                        g2d.setColor(new Color(255, 0, 0, 100));
                        g2d.drawLine((int)emergencyPos.getX(), (int)emergencyPos.getY(),
                                   (int)agentPos.getX(), (int)agentPos.getY());
                    }
                }
            }
        }
        
        private Point2D getAgentPosition(AgentInfo agent) {
            // Use custom position if set
            if (agent.x > 0 && agent.y > 0) {
                return new Point2D.Double(agent.x, agent.y);
            }
            
            // Try to get position from status
            if (agent.status != null) {
                for (String location : locationMap.keySet()) {
                    if (agent.status.contains(location)) {
                        return locationMap.get(location);
                    }
                }
            }
            
            // Default position based on type
            return getDefaultPosition(agent);
        }
        
        private Point2D getDefaultPosition(AgentInfo agent) {
            int hash = agent.name.hashCode();
            int x = 100 + (Math.abs(hash) % 700);
            int y = 100 + (Math.abs(hash / 100) % 600);
            return new Point2D.Double(x, y);
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
        
        private String getAgentSymbol(AgentInfo agent) {
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
        String name;
        String type;
        double x, y;
        String status;
        boolean available = true;
        int capacity = 0;
        int currentLoad = 0;
        long lastUpdate;
        
        AgentInfo(String name, String type) {
            this.name = name;
            this.type = type;
            this.lastUpdate = System.currentTimeMillis();
        }
    }
    
    static class EmergencyStatus {
        String id;
        String type;
        String location;
        double x, y;
        String status;
        
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