import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;

public class EmergencyGenerator extends Agent {
    private String[] emergencyTypes = {"cardiac", "trauma", "respiratory", "neurological", "general"};
    private String[] severities = {"low", "medium", "high", "critical"};
    private String[] locations = {"Downtown", "Suburb_A", "Industrial_Zone", "ResidentialArea", "Highway_Exit"};
    private int emergencyCount = 0;
    
    protected void setup() {
        System.out.println("Emergency Generator started - Will generate emergencies periodically");
        
        // Update visualization
        VisualizationHelper.log("⚠️ Emergency Generator activated");
        
        // Generate emergencies every 25 seconds
        addBehaviour(new TickerBehaviour(this, 25000) {
            protected void onTick() {
                generateEmergency();
            }
        });
        
        // Generate first emergency immediately (after 5 seconds)
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                try {
                    Thread.sleep(5000);
                    generateEmergency();
                } catch (Exception e) {}
            }
        });
    }
    
    private void generateEmergency() {
        emergencyCount++;
        String type = emergencyTypes[(int)(Math.random() * emergencyTypes.length)];
        String severity = severities[(int)(Math.random() * severities.length)];
        String location = locations[(int)(Math.random() * locations.length)];
        
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║   NEW EMERGENCY CALL #" + emergencyCount);
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║ Type:     " + type);
        System.out.println("║ Severity: " + severity);
        System.out.println("║ Location: " + location);
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        // Update visualization
        String emergencyId = "EMG" + emergencyCount;
        VisualizationHelper.log("⚠️ EMERGENCY #" + emergencyCount + ": " + type + 
                              " (" + severity + ") at " + location);
        
        // Find TCC and send emergency
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("traffic-control");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(result[0].getName());
                msg.setContent("EMERGENCY:" + type + ":" + severity + ":" + location);
                send(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void takeDown() {
        System.out.println("Emergency Generator terminating.");
        System.out.println("Total emergencies generated: " + emergencyCount);
        VisualizationHelper.log("⚠️ Emergency Generator offline - " + emergencyCount + " emergencies generated");
    }
}