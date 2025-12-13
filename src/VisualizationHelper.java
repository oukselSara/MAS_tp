import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for agents to update the visualization GUI
 */
public class VisualizationHelper {
    private static final Map<String, Double> LOCATION_X = new HashMap<>();
    private static final Map<String, Double> LOCATION_Y = new HashMap<>();
    
    static {
        // Initialize location coordinates
        initializeLocations();
    }
    
    private static void initializeLocations() {
        // Intersections
        LOCATION_X.put("Intersection1", 300.0);
        LOCATION_Y.put("Intersection1", 300.0);
        
        LOCATION_X.put("Intersection2", 450.0);
        LOCATION_Y.put("Intersection2", 300.0);
        
        LOCATION_X.put("Intersection3", 600.0);
        LOCATION_Y.put("Intersection3", 300.0);
        
        // Emergency locations
        LOCATION_X.put("Downtown", 250.0);
        LOCATION_Y.put("Downtown", 200.0);
        
        LOCATION_X.put("Suburb_A", 450.0);
        LOCATION_Y.put("Suburb_A", 450.0);
        
        LOCATION_X.put("Industrial_Zone", 650.0);
        LOCATION_Y.put("Industrial_Zone", 200.0);
        
        LOCATION_X.put("ResidentialArea", 300.0);
        LOCATION_Y.put("ResidentialArea", 550.0);
        
        LOCATION_X.put("Highway_Exit", 700.0);
        LOCATION_Y.put("Highway_Exit", 400.0);
        
        // Stations and zones
        LOCATION_X.put("Station1", 150.0);
        LOCATION_Y.put("Station1", 150.0);
        
        LOCATION_X.put("Station2", 750.0);
        LOCATION_Y.put("Station2", 150.0);
        
        LOCATION_X.put("Zone1", 200.0);
        LOCATION_Y.put("Zone1", 400.0);
        
        LOCATION_X.put("Zone2", 700.0);
        LOCATION_Y.put("Zone2", 500.0);
        
        // Numbered locations
        for (int i = 0; i <= 9; i++) {
            int col = i % 5;
            int row = i / 5;
            LOCATION_X.put("Location" + i, 100.0 + col * 150);
            LOCATION_Y.put("Location" + i, 100.0 + row * 150);
        }
    }
    
    /**
     * Update agent position on visualization
     */
    public static void updateAgent(String agentName, String agentType, String location, 
                                   String status, boolean available) {
        updateAgent(agentName, agentType, location, status, available, 0, 0);
    }
    
    /**
     * Update agent with capacity info (for hospitals)
     */
    public static void updateAgent(String agentName, String agentType, String location, 
                                   String status, boolean available, int capacity, int currentLoad) {
        try {
            TrafficSystemVisualization gui = TrafficSystemVisualization.getInstance();
            
            Double x = LOCATION_X.getOrDefault(location, 0.0);
            Double y = LOCATION_Y.getOrDefault(location, 0.0);
            
            // Add some randomness to avoid exact overlaps
            if (x == 0.0 && y == 0.0) {
                x = 100.0 + (agentName.hashCode() % 700);
                y = 100.0 + ((agentName.hashCode() / 100) % 600);
            }
            
            gui.updateAgent(agentName, agentType, x, y, status, available, capacity, currentLoad);
        } catch (Exception e) {
            // GUI might not be initialized yet
        }
    }
    
    /**
     * Add log message to visualization
     */
    public static void log(String message) {
        try {
            TrafficSystemVisualization.getInstance().addLog(message);
        } catch (Exception e) {
            // GUI might not be initialized yet
        }
    }
    
    /**
     * Add emergency to visualization
     */
    public static void addEmergency(String id, String type, String location) {
        try {
            TrafficSystemVisualization gui = TrafficSystemVisualization.getInstance();
            Double x = LOCATION_X.getOrDefault(location, 400.0);
            Double y = LOCATION_Y.getOrDefault(location, 400.0);
            gui.addEmergency(id, type, location, x, y);
        } catch (Exception e) {
            // GUI might not be initialized yet
        }
    }
    
    /**
     * Update emergency status
     */
    public static void updateEmergency(String id, String status) {
        try {
            TrafficSystemVisualization.getInstance().updateEmergency(id, status);
        } catch (Exception e) {
            // GUI might not be initialized yet
        }
    }
    
    /**
     * Remove completed emergency
     */
    public static void removeEmergency(String id) {
        try {
            TrafficSystemVisualization.getInstance().removeEmergency(id);
        } catch (Exception e) {
            // GUI might not be initialized yet
        }
    }
    
    /**
     * Get X coordinate for location
     */
    public static double getLocationX(String location) {
        return LOCATION_X.getOrDefault(location, 0.0);
    }
    
    /**
     * Get Y coordinate for location
     */
    public static double getLocationY(String location) {
        return LOCATION_Y.getOrDefault(location, 0.0);
    }
}