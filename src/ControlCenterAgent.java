import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

public class ControlCenterAgent extends Agent {
    
    private int totalEmergencies;
    private ArrayList<Long> responseTimes;
    private boolean emergencyInProgress;
    
    protected void setup() {
        
        System.out.println("Traffic Control Center is ONLINE");
        
        
        totalEmergencies = 0;
        responseTimes = new ArrayList<Long>();
        emergencyInProgress = false;
        
        
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

        System.out.println("**** EMERGENCY ALERT RECEIVED ****             ");
        
        
        
        String[] parts = messageContent.split(":");
        if (parts.length >= 3) {
            String startPos = parts[1];
            String endPos = parts[2];
            System.out.println("Route: " + startPos + " to " + endPos);
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
            
            System.out.println("**** EMERGENCY COMPLETED ****");         
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
        
        
        message.addReceiver(new AID("Vehicle 1", AID.ISLOCALNAME));
        message.addReceiver(new AID("Vehicle 2", AID.ISLOCALNAME));
        message.addReceiver(new AID("Vehicle 3", AID.ISLOCALNAME));
        message.setContent("PULL_OVER");
        send(message);
        
        System.out.println("Control Center: Vehicles instructed to pull over");
    }
    
    
    private void notifyVehiclesToResume() {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID("Vehicle 1", AID.ISLOCALNAME));
        message.addReceiver(new AID("Vehicle 2", AID.ISLOCALNAME));
        message.addReceiver(new AID("Vehicle 3", AID.ISLOCALNAME));
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
        System.out.println("------------------------------------------\n");
    }
    
    protected void takeDown() {
        System.out.println("\nTraffic Control Center shutting down.");
    }
}