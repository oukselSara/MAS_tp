import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import javax.swing.SwingUtilities;

public class TrafficMedicalSystemLauncher {
    public static void main(String[] args) {
        // Start visualization GUI first
        SwingUtilities.invokeLater(() -> {
            TrafficSystemVisualization gui = TrafficSystemVisualization.getInstance();
            gui.addLog("System Initialization Started...");
        });
        
        try {
            // Small delay to let GUI initialize
            Thread.sleep(500);
            
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost");
            p.setParameter(Profile.GUI, "true");
            
            AgentContainer container = rt.createMainContainer(p);
            
            TrafficSystemVisualization gui = TrafficSystemVisualization.getInstance();
            gui.addLog("JADE Container Created");
            
            // Create Traffic Control Center
            AgentController tcc = container.createNewAgent("TCC", 
                "TrafficControlCenter", null);
            tcc.start();
            gui.addLog("✓ Traffic Control Center Started");
            
            // Create Hospitals
            String[] hospital1Args = {"General Hospital", "medium", "5"};
            String[] hospital2Args = {"Elite Medical Center", "high", "3"};
            String[] hospital3Args = {"Community Clinic", "low", "8"};
            
            container.createNewAgent("Hospital1", "HospitalAgent", hospital1Args).start();
            container.createNewAgent("Hospital2", "HospitalAgent", hospital2Args).start();
            container.createNewAgent("Hospital3", "HospitalAgent", hospital3Args).start();
            gui.addLog("✓ 3 Hospitals Initialized");
            
            // Create Ambulances
            String[] amb1Args = {"basic", "Station1"};
            String[] amb2Args = {"advanced", "Station2"};
            String[] amb3Args = {"micu", "Station1"};
            
            container.createNewAgent("Ambulance1", "AmbulanceAgent", amb1Args).start();
            container.createNewAgent("Ambulance2", "AmbulanceAgent", amb2Args).start();
            container.createNewAgent("Ambulance3", "AmbulanceAgent", amb3Args).start();
            gui.addLog("✓ 3 Ambulances Deployed");
            
            // Create Traffic Lights
            container.createNewAgent("TrafficLight1", "TrafficLightAgent", 
                new String[]{"Intersection1"}).start();
            container.createNewAgent("TrafficLight2", "TrafficLightAgent", 
                new String[]{"Intersection2"}).start();
            container.createNewAgent("TrafficLight3", "TrafficLightAgent", 
                new String[]{"Intersection3"}).start();
            gui.addLog("✓ 3 Traffic Lights Active");
            
            // Create Regular Vehicles
            for (int i = 1; i <= 5; i++) {
                container.createNewAgent("Vehicle" + i, "VehicleAgent", 
                    new String[]{"regular"}).start();
            }
            gui.addLog("✓ 5 Vehicles on Road");
            
            // Create Police Units
            container.createNewAgent("Police1", "PoliceAgent", 
                new String[]{"Zone1"}).start();
            container.createNewAgent("Police2", "PoliceAgent", 
                new String[]{"Zone2"}).start();
            gui.addLog("✓ 2 Police Units on Patrol");
            
            // Create Emergency Generator
            container.createNewAgent("EmergencyGen", "EmergencyGenerator", null).start();
            gui.addLog("✓ Emergency Generator Active");
            
            gui.addLog("═══════════════════════════════════════");
            gui.addLog("✓ Traffic & Medical Emergency System Online");
            gui.addLog("═══════════════════════════════════════");
            
            System.out.println("=== Traffic & Medical Emergency System Started with GUI ===");
            
        } catch (Exception e) {
            e.printStackTrace();
            TrafficSystemVisualization.getInstance().addLog("ERROR: " + e.getMessage());
        }
    }
}