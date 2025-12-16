import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;

public class VehicleAgent extends Agent {
    
    protected int positionX;
    protected int positionY;
    protected int destinationX;
    protected int destinationY;
    protected int currentSpeed;
    protected boolean shouldStop;
    protected boolean hasArrived;
    
    protected void setup() {
        System.out.println("Vehicle " + getLocalName() + " is ready.");
        
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
        
        currentSpeed = 1;
        shouldStop = false;
        hasArrived = false;
        
        addBehaviour(new MoveBehaviour(this, 1000));
        addBehaviour(new ReceiveMessageBehaviour());
    }
    
    private class MoveBehaviour extends TickerBehaviour {
        
        public MoveBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        protected void onTick() {
            if (hasArrived) {
                return;
            }
            
            if (!shouldStop) {
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
                
                if (positionX == destinationX && positionY == destinationY) {
                    hasArrived = true;
                    System.out.println(getLocalName() + " reached destination and STOPPED!");
                    
                    ACLMessage arrivalMsg = new ACLMessage(ACLMessage.INFORM);
                    arrivalMsg.addReceiver(new AID("ControlCenter", AID.ISLOCALNAME));
                    arrivalMsg.setContent("VEHICLE_ARRIVED");
                    send(arrivalMsg);
                }
            } else {
                System.out.println(getLocalName() + " is stopped (waiting)");
            }
        }
    }
    
    private class ReceiveMessageBehaviour extends jade.core.behaviours.CyclicBehaviour {
        
        public void action() {
            ACLMessage message = receive();
            if (message != null) {
                String content = message.getContent();
                
                if (content.equals("PULL_OVER")) {
                    if (!hasArrived) {
                        shouldStop = true;
                        System.out.println(getLocalName() + " pulling over for emergency vehicle");
                    }
                } else if (content.equals("CLEAR")) {
                    if (!hasArrived) {
                        shouldStop = false;
                        System.out.println(getLocalName() + " resuming movement");
                    }
                } else if (content.startsWith("RED_LIGHT")) {
                    if (!hasArrived) {
                        shouldStop = true;
                        System.out.println(getLocalName() + " stopping at red light");
                    }
                } else if (content.startsWith("GREEN_LIGHT")) {
                    if (!hasArrived) {
                        shouldStop = false;
                        System.out.println(getLocalName() + " can go (green light)");
                    }
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