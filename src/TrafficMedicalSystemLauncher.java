import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class TrafficMedicalSystemLauncher {
    public static void main(String[] args) {
        try {
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost");
            p.setParameter(Profile.GUI, "true");
            
            AgentContainer container = rt.createMainContainer(p);
            
            // Create Traffic Control Center
            AgentController tcc = container.createNewAgent("TCC", 
                "TrafficControlCenter", null);
            tcc.start();
            
            // Create Hospitals
            String[] hospital1Args = {"General Hospital", "medium", "5"};
            String[] hospital2Args = {"Elite Medical Center", "high", "3"};
            String[] hospital3Args = {"Community Clinic", "low", "8"};
            
            container.createNewAgent("Hospital1", "HospitalAgent", hospital1Args).start();
            container.createNewAgent("Hospital2", "HospitalAgent", hospital2Args).start();
            container.createNewAgent("Hospital3", "HospitalAgent", hospital3Args).start();
            
            // Create Ambulances
            String[] amb1Args = {"basic", "Station1"};
            String[] amb2Args = {"advanced", "Station2"};
            String[] amb3Args = {"micu", "Station1"};
            
            container.createNewAgent("Ambulance1", "AmbulanceAgent", amb1Args).start();
            container.createNewAgent("Ambulance2", "AmbulanceAgent", amb2Args).start();
            container.createNewAgent("Ambulance3", "AmbulanceAgent", amb3Args).start();
            
            // Create Traffic Lights
            container.createNewAgent("TrafficLight1", "TrafficLightAgent", 
                new String[]{"Intersection1"}).start();
            container.createNewAgent("TrafficLight2", "TrafficLightAgent", 
                new String[]{"Intersection2"}).start();
            container.createNewAgent("TrafficLight3", "TrafficLightAgent", 
                new String[]{"Intersection3"}).start();
            
            // Create Regular Vehicles
            for (int i = 1; i <= 5; i++) {
                container.createNewAgent("Vehicle" + i, "VehicleAgent", 
                    new String[]{"regular"}).start();
            }
            
            // Create Police Units
            container.createNewAgent("Police1", "PoliceAgent", 
                new String[]{"Zone1"}).start();
            container.createNewAgent("Police2", "PoliceAgent", 
                new String[]{"Zone2"}).start();
            
            // Create Emergency Generator
            container.createNewAgent("EmergencyGen", "EmergencyGenerator", null).start();
            
            System.out.println("=== Traffic & Medical Emergency System Started ===");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
