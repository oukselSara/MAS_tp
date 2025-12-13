import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;

public class TrafficLightAgent extends Agent {
    private String intersection;
    private String currentState = "GREEN";
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
        System.out.println("  Initial State: "+ " " + currentState);
        System.out.println("  Cycle Duration: 8 seconds per state");
        
        // Update visualization
        VisualizationHelper.updateAgent(getLocalName(), "traffic-light", intersection,
            currentState, true);
        VisualizationHelper.log("🚦 " + intersection + " traffic light active");
        
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
        addBehaviour(new UpdateVisualization(this, 1000));
    }
    
    // Periodic visualization update
    class UpdateVisualization extends TickerBehaviour {
        public UpdateVisualization(Agent a, long period) {
            super(a, period);
        }
        
        public void onTick() {
            String status = currentState + (priorityMode ? " [PRIORITY]" : "");
            VisualizationHelper.updateAgent(getLocalName(), "traffic-light", intersection,
                status, true);
        }
    }
    
    class TrafficLightCycle extends TickerBehaviour {
        public TrafficLightCycle(Agent a) {
            super(a, 8000);
        }
        
        public void onTick() {
            if (!priorityMode) {
                cycleCount++;
                
                switch (currentState) {
                    case "GREEN":
                        currentState = "YELLOW";
                        System.out.println("[" + intersection + "] " + 
                                         " Light: YELLOW (Prepare to stop)");
                        break;
                        
                    case "YELLOW":
                        currentState = "RED";
                        System.out.println("[" + intersection + "] "+ 
                                         " Light: RED (Stop)");
                        notifyVehicles("STOP");
                        break;
                        
                    case "RED":
                        currentState = "GREEN";
                        System.out.println("[" + intersection + "] " + 
                                         " Light: GREEN (Go)");
                        notifyVehicles("GO");
                        break;
                }
                
                VisualizationHelper.updateAgent(getLocalName(), "traffic-light", intersection,
                    currentState, true);
                
                if (cycleCount % 5 == 0) {
                    System.out.println("[" + intersection + "] Status: Normal operation (" + 
                                     cycleCount + " cycles completed)");
                }
            }
        }
    }
    
    class HandlePriorityRequests extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                
                if (content.startsWith("PRIORITY_ROUTE:")) {
                    String location = content.split(":")[1];
                    activatePriorityMode(location);
                    
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
            System.out.println("║    PRIORITY MODE ACTIVATED                   ║");
            System.out.println("╠═══════════════════════════════════════════════════╣");
            System.out.println("║ Location:        " + String.format("%-32s", intersection) + "║");
            System.out.println("║ State:            FORCED GREEN                  ║");
            System.out.println("║ Reason:          Emergency Vehicle Route          ║");
            System.out.println("║ Duration:        15 seconds                       ║");
            System.out.println("╚═══════════════════════════════════════════════════╝\n");
            
            VisualizationHelper.log(intersection + " PRIORITY MODE - Emergency route");
            VisualizationHelper.updateAgent(getLocalName(), "traffic-light", intersection,
                "PRIORITY - FORCED GREEN", true);
            
            notifyVehicles("YIELD_EMERGENCY");
        }
    }
    
    class MonitorPriorityMode extends TickerBehaviour {
        public MonitorPriorityMode(Agent a) {
            super(a, 2000);
        }
        
        public void onTick() {
            if (priorityMode) {
                long elapsed = System.currentTimeMillis() - priorityStartTime;
                
                if (elapsed >= 15000) {
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
        currentState = "GREEN";
        
        System.out.println("\n[" + intersection + "] ✓ Priority mode deactivated");
        System.out.println("[" + intersection + "] Resuming normal traffic light operation\n");
        
        VisualizationHelper.log("✓ " + intersection + " returning to normal operation");
        VisualizationHelper.updateAgent(getLocalName(), "traffic-light", intersection,
            currentState, true);
        
        notifyVehicles("NORMAL_OPERATION");
    }
    
    private void notifyVehicles(String message) {
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
        }
    }
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {}
        
        System.out.println("\n Traffic Light at " + intersection + " shutting down");
        System.out.println("  Total cycles completed: " + cycleCount);
        System.out.println("  Final state: " + currentState);
        
        VisualizationHelper.log("🚦 " + intersection + " offline");
    }
}