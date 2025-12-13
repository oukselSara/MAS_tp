import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;

public class VehicleAgent extends Agent {
    private String vehicleType;
    private String currentLocation;
    private String destination;
    private int speed = 50;
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
        
        // Update visualization
        VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
            "En route to " + destination, true);
        VisualizationHelper.log("🚗 " + vehicleId + " on road");
        
        if (!vehicleType.equals("regular")) {
            registerWithDF();
        }
        
        addBehaviour(new DriveToDestination(this));
        addBehaviour(new HandleTrafficSignals());
        addBehaviour(new UpdateVisualization(this, 2000));
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
    
    // Periodic visualization update
    class UpdateVisualization extends TickerBehaviour {
        public UpdateVisualization(Agent a, long period) {
            super(a, period);
        }
        
        public void onTick() {
            String status = moving ? "Moving (" + speed + " km/h)" : "Stopped";
            VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
                status, moving);
        }
    }
    
    class DriveToDestination extends TickerBehaviour {
        public DriveToDestination(Agent a) {
            super(a, 10000);
        }
        
        public void onTick() {
            if (!currentLocation.equals(destination) && moving) {
                System.out.println("[" + vehicleId + "] Driving: " + currentLocation + 
                                 " → " + destination + " (Speed: " + speed + " km/h)");
                
                VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
                    "Driving to " + destination, true);
                
                if (Math.random() > 0.6) {
                    currentLocation = destination;
                    System.out.println("[" + vehicleId + "] ✓ Arrived at " + destination);
                    
                    VisualizationHelper.log("📍 " + vehicleId + " arrived at " + destination);
                    VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
                        "Arrived", false);
                    
                    addBehaviour(new WakerBehaviour(myAgent, 5000) {
                        protected void onWake() {
                            destination = "Location" + (int)(Math.random() * 10);
                            System.out.println("[" + vehicleId + "] New destination: " + destination);
                            
                            VisualizationHelper.log("🗺️ " + vehicleId + " new destination: " + destination);
                            VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
                                "Planning route to " + destination, true);
                        }
                    });
                }
            }
        }
    }
    
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
                    
                    VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
                        "Stopped - Red Light", false);
                    
                } else if (content.startsWith("GO")) {
                    moving = true;
                    speed = 50;
                    System.out.println("[" + vehicleId + "] 🟢 MOVING");
                    
                    VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
                        "Moving", true);
                    
                } else if (content.startsWith("YIELD_EMERGENCY")) {
                    moving = false;
                    speed = 0;
                    System.out.println("[" + vehicleId + "] 🚑 YIELDING to emergency vehicle");
                    
                    VisualizationHelper.log("🚑 " + vehicleId + " yielding to emergency vehicle");
                    VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
                        "YIELDING to Emergency", false);
                    
                    addBehaviour(new WakerBehaviour(myAgent, 8000) {
                        protected void onWake() {
                            moving = true;
                            speed = 50;
                            System.out.println("[" + vehicleId + "] Resuming normal speed");
                            
                            VisualizationHelper.updateAgent(getLocalName(), "vehicle", currentLocation,
                                "Resuming", true);
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
        VisualizationHelper.log("🚗 " + vehicleId + " offline");
    }
}