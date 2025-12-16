import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;

public class AmbulanceAgent extends VehicleAgent {
    
    private boolean isEmergencyActive;
    private int patientCriticalLevel;
    private long emergencyStartTime;
    
    protected void setup() {
        System.out.println("Ambulance " + getLocalName() + " is ready.");
        
        // Get starting position from arguments
        Object[] args = getArguments();
        if (args != null && args.length >= 4) {
            positionX = Integer.parseInt(args[0].toString());
            positionY = Integer.parseInt(args[1].toString());
            destinationX = Integer.parseInt(args[2].toString());
            destinationY = Integer.parseInt(args[3].toString());
        } else {
            positionX = 0;
            positionY = 0;
            destinationX = 10;
            destinationY = 10;
        }
        
        currentSpeed = 2; // Ambulance is faster
        shouldStop = false;
        isEmergencyActive = false;
        patientCriticalLevel = 5; // Scale 1-10
        
        // Add behavior to move
        addBehaviour(new AmbulanceMoveBehaviour(this, 800));
        
        // Add behavior to receive messages
        addBehaviour(new AmbulanceReceiveBehaviour());
        
        // Trigger emergency after 3 seconds
        addBehaviour(new EmergencyTriggerBehaviour(this, 3000));
    }
    
    // Behavior to trigger emergency
    private class EmergencyTriggerBehaviour extends jade.core.behaviours.OneShotBehaviour {
        
        public EmergencyTriggerBehaviour(Agent a, long delay) {
            super(a);
        }
        
        public void action() {
            activateEmergency();
        }
    }
    
    // Activate emergency mode
    private void activateEmergency() {
        isEmergencyActive = true;
        emergencyStartTime = System.currentTimeMillis();
        currentSpeed = 3; // Go even faster
        
        System.out.println("\n*** EMERGENCY ACTIVATED by " + getLocalName() + " ***");
        System.out.println("Patient critical level: " + patientCriticalLevel + "/10");
        
        // Send message to Control Center
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(new AID("ControlCenter", AID.ISLOCALNAME));
        message.setContent("EMERGENCY_START:" + positionX + "," + positionY + ":" + destinationX + "," + destinationY);
        send(message);
    }
    
    // Behavior to move ambulance
    private class AmbulanceMoveBehaviour extends TickerBehaviour {
        
        public AmbulanceMoveBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        protected void onTick() {
            // Ambulance doesn't stop for normal red lights during emergency
            if (!shouldStop || isEmergencyActive) {
                // Move toward destination (fix: don't overshoot)
                if (positionX < destinationX) {
                    positionX = Math.min(positionX + currentSpeed, destinationX);
                } else if (positionX > destinationX) {
                    positionX = Math.max(positionX - currentSpeed, destinationX);
                }
                
                if (positionY < destinationY) {
                    positionY = Math.min(positionY + currentSpeed, destinationY);
                } else if (positionY > destinationY) {
                    positionY = Math.max(positionY - currentSpeed, destinationY);
                }
                
                if (isEmergencyActive) {
                    System.out.println(getLocalName() + " EMERGENCY at (" + positionX + "," + positionY + ")");
                } else {
                    System.out.println(getLocalName() + " at position (" + positionX + "," + positionY + ")");
                }
                
                // Check if reached hospital
                if (positionX == destinationX && positionY == destinationY && isEmergencyActive) {
                    completeEmergency();
                }
            }
        }
    }
    
    // Complete the emergency
    private void completeEmergency() {
        long emergencyDuration = (System.currentTimeMillis() - emergencyStartTime) / 1000;
        isEmergencyActive = false;
        
        System.out.println("\n*** EMERGENCY COMPLETED by " + getLocalName() + " ***");
        System.out.println("Response time: " + emergencyDuration + " seconds");
        
        // Notify Control Center
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(new AID("ControlCenter", AID.ISLOCALNAME));
        message.setContent("EMERGENCY_END:" + emergencyDuration);
        send(message);
        
        // Schedule next emergency after 5 seconds
        addBehaviour(new jade.core.behaviours.WakerBehaviour(this, 5000) {
            protected void onWake() {
                // Set new random destination
                destinationX = (int)(Math.random() * 10);
                destinationY = (int)(Math.random() * 10);
                patientCriticalLevel = (int)(Math.random() * 5) + 5; // 5-10
                activateEmergency();
            }
        });
    }
    
    // Behavior to receive messages
    private class AmbulanceReceiveBehaviour extends jade.core.behaviours.CyclicBehaviour {
        
        public void action() {
            ACLMessage message = receive();
            if (message != null) {
                String content = message.getContent();
                
                if (content.equals("PRIORITY_GRANTED")) {
                    System.out.println(getLocalName() + " received priority access");
                }
            } else {
                block();
            }
        }
    }
}