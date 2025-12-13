import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;

public class VehicleAgent extends Agent {
    private String vehicleType; // regular, emergency
    private String currentLocation;
    private String destination;
    private int speed = 50; // km/h
    private boolean moving = true;
    private String vehicleId;
    
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            vehicleType = (String) args[0];
        } else {
            vehicleType = "regular";
        }
        
        vehicleId = getAID().getLocalName();
        currentLocation = "Location" + (int)(Math.random() * 10);
        destination = "Location" + (int)(Math.random() * 10);
        
        System.out.println("Vehicle " + vehicleId + " created (" + vehicleType + ")");
        System.out.println("  Starting at: " + currentLocation);
        System.out.println("  Heading to: " + destination);
        System.out.println("  Speed: " + speed + " km/h");
        
        // Register with DF if needed
        if (!vehicleType.equals("regular")) {
            registerWithDF();
        }
        
        addBehaviour(new DriveToDestination(this));
        addBehaviour(new HandleTrafficSignals());
    }
    
    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("vehicle");
        sd.setName(vehicleType + "-vehicle");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Main driving behavior
    class DriveToDestination extends TickerBehaviour {
        public DriveToDestination(Agent a) {
            super(a, 10000); // Update every 10 seconds
        }
        
        public void onTick() {
            if (!currentLocation.equals(destination) && moving) {
                // Simulate movement
                System.out.println("[" + vehicleId + "] Driving: " + currentLocation + 
                                 " → " + destination + " (Speed: " + speed + " km/h)");
                
                // Randomly reach destination
                if (Math.random() > 0.6) {
                    currentLocation = destination;
                    System.out.println("[" + vehicleId + "] ✓ Arrived at " + destination);
                    
                    // Wait a bit, then set new destination
                    addBehaviour(new WakerBehaviour(myAgent, 5000) {
                        protected void onWake() {
                            destination = "Location" + (int)(Math.random() * 10);
                            System.out.println("[" + vehicleId + "] New destination: " + destination);
                        }
                    });
                }
            }
        }
    }
    
    // Handle traffic signals and priority vehicles
    class HandleTrafficSignals extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                
                if (content.startsWith("STOP")) {
                    moving = false;
                    speed = 0;
                    System.out.println("[" + vehicleId + "] 🔴 STOPPED at traffic light");
                    
                } else if (content.startsWith("GO")) {
                    moving = true;
                    speed = 50;
                    System.out.println("[" + vehicleId + "] 🟢 MOVING");
                    
                } else if (content.startsWith("YIELD_EMERGENCY")) {
                    moving = false;
                    speed = 0;
                    System.out.println("[" + vehicleId + "] 🚑 YIELDING to emergency vehicle");
                    
                    // Resume after emergency passes
                    addBehaviour(new WakerBehaviour(myAgent, 8000) {
                        protected void onWake() {
                            moving = true;
                            speed = 50;
                            System.out.println("[" + vehicleId + "] Resuming normal speed");
                        }
                    });
                }
            } else {
                block();
            }
        }
    }
    
    protected void takeDown() {
        try {
            if (!vehicleType.equals("regular")) {
                DFService.deregister(this);
            }
        } catch (Exception e) {}
        System.out.println("Vehicle " + vehicleId + " terminating.");
    }
}