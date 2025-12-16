import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class MainLauncher {
    
    public static void main(String[] args) {
        try {
            
            Runtime runtime = Runtime.instance();
            
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.GUI, "true"); 
            
            
            AgentContainer mainContainer = runtime.createMainContainer(profile);
            
            System.out.println("Starting Traffic Management System...\n");
            
            
            AgentController controlCenter = mainContainer.createNewAgent(
                "ControlCenter", 
                "ControlCenterAgent", 
                new Object[]{}
            );
            controlCenter.start();
            
            Thread.sleep(500);
            
            
            AgentController light1 = mainContainer.createNewAgent(
                "TrafficLight1", 
                "TrafficLightAgent", 
                new Object[]{5, 5}
            );
            light1.start();
            
            AgentController light2 = mainContainer.createNewAgent(
                "TrafficLight2", 
                "TrafficLightAgent", 
                new Object[]{8, 8}
            );
            light2.start();
            
            Thread.sleep(500);
            
            
            
            AgentController vehicle1 = mainContainer.createNewAgent(
                "Vehicle1", 
                "VehicleAgent", 
                new Object[]{0, 0, 10, 10}
            );
            vehicle1.start();
            
            AgentController vehicle2 = mainContainer.createNewAgent(
                "Vehicle2", 
                "VehicleAgent", 
                new Object[]{2, 2, 8, 8}
            );
            vehicle2.start();
            
            AgentController vehicle3 = mainContainer.createNewAgent(
                "Vehicle3", 
                "VehicleAgent", 
                new Object[]{1, 3, 9, 7}
            );
            vehicle3.start();
            
            Thread.sleep(500);
            
            
            
            AgentController ambulance = mainContainer.createNewAgent(
                "Ambulance1", 
                "AmbulanceAgent", 
                new Object[]{0, 0, 10, 10}
            );
            ambulance.start();
            
            System.out.println("\nAll agents started successfully!");
            System.out.println("Emergency will be triggered in 3 seconds...\n");
            System.out.println("==========================================\n");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}