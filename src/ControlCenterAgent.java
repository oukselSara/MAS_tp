import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import java.util.ArrayList;

public class ControlCenterAgent extends Agent {
    
    private int totalEmergencies;
    private ArrayList<Long> responseTimes;
    private boolean emergencyInProgress;
    private int totalVehicles;
    private int vehiclesArrived;
    
    protected void setup() {
        System.out.println("\n===========================================");
        System.out.println("Traffic Control Center is ONLINE");
        System.out.println("===========================================\n");
        
        totalEmergencies = 0;
        responseTimes = new ArrayList<Long>();
        emergencyInProgress = false;
        totalVehicles = 4; 
        vehiclesArrived = 0;
        
        addBehaviour(new ReceiveEmergencyBehaviour());
    }
    
    private class ReceiveEmergencyBehaviour extends jade.core.behaviours.CyclicBehaviour {
        
        public void action() {
            ACLMessage message = receive();
            if (message != null) {
                String content = message.getContent();
                
                if (content.startsWith("EMERGENCY_START")) {
                    handleEmergencyStart(content);
                } else if (content.startsWith("EMERGENCY_END")) {
                    handleEmergencyEnd(content);
                } else if (content.startsWith("PRIORITY_ACTIVE")) {
                    System.out.println("Control Center: Priority confirmed at intersection");
                } else if (content.equals("VEHICLE_ARRIVED")) {
                    handleVehicleArrival();
                }
            } else {
                block();
            }
        }
    }
    
    private void handleEmergencyStart(String messageContent) {
        if (emergencyInProgress) {
            System.out.println("Control Center: Another emergency already in progress");
            return;
        }
        
        emergencyInProgress = true;
        totalEmergencies++;
        
        System.out.println("\n===========================================");
        System.out.println("  EMERGENCY ALERT RECEIVED              ");
        System.out.println("===========================================");
        
        String[] parts = messageContent.split(":");
        if (parts.length >= 3) {
            String startPos = parts[1];
            String endPos = parts[2];
            System.out.println("Route: " + startPos + " -> " + endPos);
        }
        
        giveTrafficLightsPriority();
        notifyVehiclesToPullOver();
    }
    
    private void handleEmergencyEnd(String messageContent) {
        emergencyInProgress = false;
        
        String[] parts = messageContent.split(":");
        if (parts.length >= 2) {
            long responseTime = Long.parseLong(parts[1]);
            responseTimes.add(responseTime);
            
            System.out.println("\n===========================================");
            System.out.println("  EMERGENCY COMPLETED                   ");
            System.out.println("===========================================");
            System.out.println("Response time: " + responseTime + " seconds");
        }
        
        clearTrafficLightsPriority();
        notifyVehiclesToResume();
        displayStatistics();
    }
    
    private void giveTrafficLightsPriority() {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("TrafficLight1", AID.ISLOCALNAME));
        message.addReceiver(new AID("TrafficLight2", AID.ISLOCALNAME));
        message.setContent("GIVE_PRIORITY");
        send(message);
        
        System.out.println("Control Center: Priority granted to traffic lights");
    }
    
    private void clearTrafficLightsPriority() {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("TrafficLight1", AID.ISLOCALNAME));
        message.addReceiver(new AID("TrafficLight2", AID.ISLOCALNAME));
        message.setContent("CLEAR_PRIORITY");
        send(message);
        
        System.out.println("Control Center: Priority cleared from traffic lights");
    }
    
    private void notifyVehiclesToPullOver() {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("Vehicle1", AID.ISLOCALNAME));
        message.addReceiver(new AID("Vehicle2", AID.ISLOCALNAME));
        message.addReceiver(new AID("Vehicle3", AID.ISLOCALNAME));
        message.setContent("PULL_OVER");
        send(message);
        
        System.out.println("Control Center: Vehicles instructed to pull over");
    }
    
    private void notifyVehiclesToResume() {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("Vehicle1", AID.ISLOCALNAME));
        message.addReceiver(new AID("Vehicle2", AID.ISLOCALNAME));
        message.addReceiver(new AID("Vehicle3", AID.ISLOCALNAME));
        message.setContent("CLEAR");
        send(message);
        
        System.out.println("Control Center: Vehicles instructed to resume");
    }
    
    private void displayStatistics() {
        System.out.println("\n--- TRAFFIC CONTROL CENTER STATISTICS ---");
        System.out.println("Total emergencies handled: " + totalEmergencies);
        
        if (responseTimes.size() > 0) {
            long total = 0;
            for (Long time : responseTimes) {
                total += time;
            }
            long average = total / responseTimes.size();
            System.out.println("Average response time: " + average + " seconds");
        }
        System.out.println("Vehicles arrived: " + vehiclesArrived + "/" + totalVehicles);
        System.out.println("------------------------------------------\n");
        
        checkSimulationComplete();
    }
    
    private void handleVehicleArrival() {
        vehiclesArrived++;
        System.out.println("Control Center: Vehicle arrived (" + vehiclesArrived + "/" + totalVehicles + ")");
        checkSimulationComplete();
    }
    
    private void checkSimulationComplete() {
        if (vehiclesArrived >= totalVehicles && totalEmergencies >= 2 && !emergencyInProgress) {
            System.out.println("\n===========================================");
            System.out.println("  SIMULATION COMPLETE                   ");
            System.out.println("===========================================");
            System.out.println("All vehicles reached destinations.");
            System.out.println("All emergencies handled.");
            displayFinalStatistics();
            shutdownSystem();
        }
    }
    
    private void displayFinalStatistics() {
        System.out.println("\n========== FINAL STATISTICS ==========");
        System.out.println("Total emergencies: " + totalEmergencies);
        
        if (responseTimes.size() > 0) {
            long total = 0;
            for (Long time : responseTimes) {
                total += time;
            }
            long average = total / responseTimes.size();
            System.out.println("Average response time: " + average + " seconds");
            System.out.println("All response times: " + responseTimes);
        }
        
        System.out.println("Total vehicles: " + totalVehicles);
        System.out.println("Vehicles arrived: " + vehiclesArrived);
        System.out.println("======================================\n");
    }
    
    private void shutdownSystem() {
        addBehaviour(new jade.core.behaviours.WakerBehaviour(this, 2000) {
            protected void onWake() {
                System.out.println("\nShutting down all agents...");
                
                try {
                    AgentContainer container = getContainerController();
                    container.kill();
                } catch (Exception e) {
                    System.out.println("Error during shutdown: " + e.getMessage());
                }
            }
        });
    }
    
    protected void takeDown() {
        System.out.println("\nTraffic Control Center shutting down.");
    }
}