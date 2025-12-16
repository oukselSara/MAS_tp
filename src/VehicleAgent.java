import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class VehicleAgent extends Agent {
    
    protected int positionX;
    protected int positionY;
    protected int destinationX;
    protected int destinationY;
    protected int currentSpeed;
    protected boolean shouldStop;
    
    protected void setup() {
        System.out.println("Vehicle " + getLocalName() + " is ready.");
        
        // Get starting position from arguments
        Object[] args = getArguments();
        if (args != null && args.length >= 4) {
            positionX = Integer.parseInt(args[0].toString());
            positionY = Integer.parseInt(args[1].toString());
            destinationX = Integer.parseInt(args[2].toString());
            destinationY = Integer.parseInt(args[3].toString());
        } else {
            // Default values
            positionX = 0;
            positionY = 0;
            destinationX = 10;
            destinationY = 10;
        }
        
        currentSpeed = 1;
        shouldStop = false;
        
        // Add behavior to move
        addBehaviour(new MoveBehaviour(this, 1000));
        
        // Add behavior to receive messages
        addBehaviour(new ReceiveMessageBehaviour());
    }
    
    // Behavior to move the vehicle
    private class MoveBehaviour extends TickerBehaviour {
        
        public MoveBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        protected void onTick() {
            if (!shouldStop) {
                // Simple movement toward destination (fix: don't overshoot)
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
                
                System.out.println(getLocalName() + " at position (" + positionX + "," + positionY + ")");
                
                // Check if reached destination
                if (positionX == destinationX && positionY == destinationY) {
                    System.out.println(getLocalName() + " reached destination!");
                }
            } else {
                System.out.println(getLocalName() + " is stopped (waiting)");
            }
        }
    }
    
    // Behavior to receive messages
    private class ReceiveMessageBehaviour extends jade.core.behaviours.CyclicBehaviour {
        
        public void action() {
            ACLMessage message = receive();
            if (message != null) {
                String content = message.getContent();
                
                if (content.equals("PULL_OVER")) {
                    shouldStop = true;
                    System.out.println(getLocalName() + " pulling over for emergency vehicle");
                } else if (content.equals("CLEAR")) {
                    shouldStop = false;
                    System.out.println(getLocalName() + " resuming movement");
                } else if (content.startsWith("RED_LIGHT")) {
                    shouldStop = true;
                    System.out.println(getLocalName() + " stopping at red light");
                } else if (content.startsWith("GREEN_LIGHT")) {
                    shouldStop = false;
                    System.out.println(getLocalName() + " can go (green light)");
                }
            } else {
                block();
            }
        }
    }
    
    protected void takeDown() {
        System.out.println("Vehicle " + getLocalName() + " terminating.");
    }
}