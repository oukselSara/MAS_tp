import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;

public class TrafficLightAgent extends Agent {
    private String intersection;
    private String currentState = "GREEN"; // GREEN, YELLOW, RED
    private boolean priorityMode = false;
    private int cycleCount = 0;
    private long priorityStartTime = 0;
    
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            intersection = (String) args[0];
        } else {
            intersection = "Intersection1";
        }
        
        System.out.println("\n🚦 Traffic Light System Initialized");
        System.out.println("  Location: " + intersection);
        System.out.println("  Initial State: " + getStateEmoji() + " " + currentState);
        System.out.println("  Cycle Duration: 8 seconds per state");
        
        // Register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("traffic-light");
        sd.setName(intersection);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        addBehaviour(new TrafficLightCycle(this));
        addBehaviour(new HandlePriorityRequests());
        addBehaviour(new MonitorPriorityMode(this));
    }
    
    // Normal traffic light cycle
    class TrafficLightCycle extends TickerBehaviour {
        public TrafficLightCycle(Agent a) {
            super(a, 8000); // Change every 8 seconds
        }
        
        public void onTick() {
            if (!priorityMode) {
                cycleCount++;
                
                // Cycle through states
                switch (currentState) {
                    case "GREEN":
                        currentState = "YELLOW";
                        System.out.println("[" + intersection + "] " + getStateEmoji() + 
                                         " Light: YELLOW (Prepare to stop)");
                        break;
                        
                    case "YELLOW":
                        currentState = "RED";
                        System.out.println("[" + intersection + "] " + getStateEmoji() + 
                                         " Light: RED (Stop)");
                        // Notify vehicles to stop
                        notifyVehicles("STOP");
                        break;
                        
                    case "RED":
                        currentState = "GREEN";
                        System.out.println("[" + intersection + "] " + getStateEmoji() + 
                                         " Light: GREEN (Go)");
                        // Notify vehicles to go
                        notifyVehicles("GO");
                        break;
                }
                
                // Periodic status
                if (cycleCount % 5 == 0) {
                    System.out.println("[" + intersection + "] Status: Normal operation (" + 
                                     cycleCount + " cycles completed)");
                }
            }
        }
    }
    
    // Handle priority vehicle requests from TCC
    class HandlePriorityRequests extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                
                if (content.startsWith("PRIORITY_ROUTE:")) {
                    String location = content.split(":")[1];
                    activatePriorityMode(location);
                    
                    // Send confirmation
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("PRIORITY_ACTIVATED:" + intersection);
                    send(reply);
                }
            } else {
                block();
            }
        }
    }
    
    private void activatePriorityMode(String emergencyLocation) {
        if (!priorityMode) {
            priorityMode = true;
            priorityStartTime = System.currentTimeMillis();
            currentState = "GREEN";
            
            System.out.println("\n╔═══════════════════════════════════════════════════╗");
            System.out.println("║   🚨 PRIORITY MODE ACTIVATED 🚨                  ║");
            System.out.println("╠═══════════════════════════════════════════════════╣");
            System.out.println("║ Location:        " + String.format("%-32s", intersection) + "║");
            System.out.println("║ State:           🟢 FORCED GREEN                  ║");
            System.out.println("║ Reason:          Emergency Vehicle Route          ║");
            System.out.println("║ Duration:        15 seconds                       ║");
            System.out.println("╚═══════════════════════════════════════════════════╝\n");
            
            // Notify all vehicles in area
            notifyVehicles("YIELD_EMERGENCY");
        }
    }
    
    // Monitor and deactivate priority mode
    class MonitorPriorityMode extends TickerBehaviour {
        public MonitorPriorityMode(Agent a) {
            super(a, 2000); // Check every 2 seconds
        }
        
        public void onTick() {
            if (priorityMode) {
                long elapsed = System.currentTimeMillis() - priorityStartTime;
                
                if (elapsed >= 15000) { // 15 seconds
                    deactivatePriorityMode();
                } else {
                    int remaining = (int)((15000 - elapsed) / 1000);
                    if (remaining % 5 == 0) {
                        System.out.println("[" + intersection + "] Priority mode: " + 
                                         remaining + " seconds remaining");
                    }
                }
            }
        }
    }
    
    private void deactivatePriorityMode() {
        priorityMode = false;
        currentState = "GREEN"; // Start with green
        
        System.out.println("\n[" + intersection + "] ✓ Priority mode deactivated");
        System.out.println("[" + intersection + "] Resuming normal traffic light operation\n");
        
        // Notify vehicles
        notifyVehicles("NORMAL_OPERATION");
    }
    
    private void notifyVehicles(String message) {
        // Find all vehicles and send message
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("vehicle");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            
            if (result.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                for (DFAgentDescription dfd : result) {
                    msg.addReceiver(dfd.getName());
                }
                msg.setContent(message + ":" + intersection);
                send(msg);
            }
        } catch (Exception e) {
            // Vehicles might not be registered yet
        }
    }
    
    private String getStateEmoji() {
        switch (currentState) {
            case "GREEN":
                return "🟢";
            case "YELLOW":
                return "🟡";
            case "RED":
                return "🔴";
            default:
                return "⚪";
        }
    }
    
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {}
        
        System.out.println("\n🚦 Traffic Light at " + intersection + " shutting down");
        System.out.println("  Total cycles completed: " + cycleCount);
        System.out.println("  Final state: " + currentState);
    }
}