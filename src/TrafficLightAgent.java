import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;

public class TrafficLightAgent extends Agent {
    
    private int intersectionX;
    private int intersectionY;
    private String currentColor;
    private int colorTimer;
    private boolean priorityMode;
    
    protected void setup() {
        System.out.println("Traffic Light " + getLocalName() + " is ready.");
        
        // Get position from arguments
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            intersectionX = Integer.parseInt(args[0].toString());
            intersectionY = Integer.parseInt(args[1].toString());
        } else {
            intersectionX = 5;
            intersectionY = 5;
        }
        
        currentColor = "RED";
        colorTimer = 0;
        priorityMode = false;
        
        System.out.println("Traffic Light at intersection (" + intersectionX + "," + intersectionY + ")");
        
        // Add behavior to cycle colors
        addBehaviour(new ColorCycleBehaviour(this, 1000));
        
        // Add behavior to receive messages
        addBehaviour(new ReceiveCommandBehaviour());
    }
    
    // Behavior to cycle through colors
    private class ColorCycleBehaviour extends TickerBehaviour {
        
        public ColorCycleBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        protected void onTick() {
            if (!priorityMode) {
                colorTimer++;
                
                if (currentColor.equals("GREEN") && colorTimer >= 5) {
                    currentColor = "YELLOW";
                    colorTimer = 0;
                    System.out.println(getLocalName() + " is now YELLOW");
                } else if (currentColor.equals("YELLOW") && colorTimer >= 2) {
                    currentColor = "RED";
                    colorTimer = 0;
                    System.out.println(getLocalName() + " is now RED");
                } else if (currentColor.equals("RED") && colorTimer >= 5) {
                    currentColor = "GREEN";
                    colorTimer = 0;
                    System.out.println(getLocalName() + " is now GREEN");
                }
            } else {
                // Stay green during priority mode
                if (!currentColor.equals("GREEN")) {
                    currentColor = "GREEN";
                    System.out.println(getLocalName() + " PRIORITY MODE - GREEN");
                }
            }
        }
    }
    
    // Behavior to receive commands
    private class ReceiveCommandBehaviour extends jade.core.behaviours.CyclicBehaviour {
        
        public void action() {
            ACLMessage message = receive();
            if (message != null) {
                String content = message.getContent();
                
                if (content.equals("GIVE_PRIORITY")) {
                    activatePriorityMode();
                } else if (content.equals("CLEAR_PRIORITY")) {
                    deactivatePriorityMode();
                }
            } else {
                block();
            }
        }
    }
    
    // Activate priority mode for emergency vehicles
    private void activatePriorityMode() {
        priorityMode = true;
        currentColor = "GREEN";
        System.out.println("*** " + getLocalName() + " PRIORITY MODE ACTIVATED ***");
        
        // Send confirmation to Control Center
        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
        response.addReceiver(new AID("ControlCenter", AID.ISLOCALNAME));
        response.setContent("PRIORITY_ACTIVE:" + intersectionX + "," + intersectionY);
        send(response);
    }
    
    // Deactivate priority mode
    private void deactivatePriorityMode() {
        priorityMode = false;
        colorTimer = 0;
        System.out.println(getLocalName() + " returning to normal cycle");
    }
    
    protected void takeDown() {
        System.out.println("Traffic Light " + getLocalName() + " terminating.");
    }
}