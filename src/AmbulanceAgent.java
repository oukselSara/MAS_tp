import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import java.util.*;

public class AmbulanceAgent extends Agent {
    private String equipmentLevel;
    private String currentLocation;
    private boolean available = true;
    private String currentEmergency = null;
    private Map<String, Integer> equipmentScores;
    private List<String> equipment;
    
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            equipmentLevel = (String) args[0];
            currentLocation = (String) args[1];
        } else {
            equipmentLevel = "basic";
            currentLocation = "Station1";
        }
        
        initializeEquipment();
        
        System.out.println("Ambulance " + getAID().getLocalName() + " ready.");
        System.out.println("  Equipment Level: " + equipmentLevel);
        System.out.println("  Equipment: " + equipment);
        System.out.println("  Location: " + currentLocation);
        
        // Update visualization
        VisualizationHelper.updateAgent(getLocalName(), "ambulance", currentLocation, 
            "Ready - " + equipmentLevel, available);
        VisualizationHelper.log("🚑 " + getLocalName() + " ready at " + currentLocation + " (" + equipmentLevel + ")");
        
        // Register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ambulance");
        sd.setName(equipmentLevel + "-ambulance");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        addBehaviour(new RespondToEmergencies());
        addBehaviour(new HandleAssignment());
        addBehaviour(new ReceiveDestination());
        addBehaviour(new UpdateVisualization(this, 2000));
    }
    
    private void initializeEquipment() {
        equipment = new ArrayList<>();
        equipmentScores = new HashMap<>();
        
        switch (equipmentLevel) {
            case "basic":
                equipment.add("First Aid Kit");
                equipment.add("Oxygen");
                equipment.add("Stretcher");
                equipment.add("Basic Monitoring");
                equipmentScores.put("trauma", 40);
                equipmentScores.put("cardiac", 30);
                equipmentScores.put("respiratory", 35);
                equipmentScores.put("neurological", 25);
                equipmentScores.put("general", 50);
                break;
                
            case "advanced":
                equipment.add("Advanced Life Support Equipment");
                equipment.add("Defibrillator");
                equipment.add("IV Medications");
                equipment.add("Intubation Kit");
                equipment.add("Advanced Monitoring");
                equipment.add("Oxygen");
                equipment.add("Stretcher");
                equipmentScores.put("trauma", 70);
                equipmentScores.put("cardiac", 85);
                equipmentScores.put("respiratory", 80);
                equipmentScores.put("neurological", 65);
                equipmentScores.put("general", 75);
                break;
                
            case "micu":
                equipment.add("Full ICU Equipment");
                equipment.add("Advanced Defibrillator");
                equipment.add("Ventilator");
                equipment.add("Complete Medication Suite");
                equipment.add("Surgical Kit");
                equipment.add("Blood Products");
                equipment.add("Ultrasound");
                equipment.add("Advanced Life Support");
                equipment.add("Telemetry to Hospital");
                equipmentScores.put("trauma", 95);
                equipmentScores.put("cardiac", 100);
                equipmentScores.put("respiratory", 95);
                equipmentScores.put("neurological", 90);
                equipmentScores.put("general", 90);
                break;
        }
    }
    
    // Periodic visualization update
    class UpdateVisualization extends TickerBehaviour {
        public UpdateVisualization(Agent a, long period) {
            super(a, period);
        }
        
        public void onTick() {
            String status = available ? "Available - " + equipmentLevel : "On Mission: " + currentEmergency;
            VisualizationHelper.updateAgent(getLocalName(), "ambulance", 
                currentLocation, status, available);
        }
    }
    
    class RespondToEmergencies extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("ambulance-dispatch"));
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                if (available) {
                    String[] parts = msg.getContent().split(":");
                    String emergencyId = parts[1];
                    String type = parts[2];
                    String severity = parts[3];
                    String location = parts[4];
                    
                    int score = calculateScore(type, severity, location);
                    
                    ACLMessage proposal = msg.createReply();
                    proposal.setPerformative(ACLMessage.PROPOSE);
                    proposal.setContent(emergencyId + ":" + score + ":" + equipmentLevel);
                    send(proposal);
                    
                    System.out.println(getLocalName() + " proposing for " + emergencyId + 
                                     " with score: " + score);
                    VisualizationHelper.log("💭 " + getLocalName() + " proposing for " + emergencyId + " (score: " + score + ")");
                } else {
                    ACLMessage refuse = msg.createReply();
                    refuse.setPerformative(ACLMessage.REFUSE);
                    refuse.setContent("BUSY");
                    send(refuse);
                }
            } else {
                block();
            }
        }
    }
    
    private int calculateScore(String type, String severity, String location) {
        int baseScore = equipmentScores.getOrDefault(type, 50);
        
        if (severity.equals("critical")) {
            baseScore += 20;
        } else if (severity.equals("high")) {
            baseScore += 10;
        }
        
        int distance = Math.abs(currentLocation.hashCode() - location.hashCode()) % 10;
        int distanceScore = Math.max(0, 20 - distance * 2);
        
        if (available) {
            baseScore += 15;
        }
        
        return Math.min(100, baseScore + distanceScore);
    }
    
    class HandleAssignment extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchConversationId("ambulance-dispatch"),
                MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                )
            );
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    String[] parts = msg.getContent().split(":");
                    currentEmergency = parts[0];
                    String targetLocation = parts[1];
                    
                    available = false;
                    System.out.println("\n*** " + getLocalName() + " DISPATCHED ***");
                    System.out.println("Emergency: " + currentEmergency);
                    System.out.println("Equipment: " + equipmentLevel);
                    System.out.println("Heading to: " + targetLocation);
                    
                    VisualizationHelper.log("🚨 " + getLocalName() + " DISPATCHED to " + targetLocation);
                    VisualizationHelper.updateAgent(getLocalName(), "ambulance", 
                        currentLocation, "En route to " + targetLocation, false);
                    
                    // Simulate travel
                    addBehaviour(new OneShotBehaviour() {
                        public void action() {
                            try {
                                Thread.sleep(3000);
                                System.out.println(getLocalName() + " arrived at scene: " + targetLocation);
                                currentLocation = targetLocation;
                                
                                VisualizationHelper.log("✓ " + getLocalName() + " arrived at scene");
                                VisualizationHelper.updateAgent(getLocalName(), "ambulance", 
                                    currentLocation, "At scene: " + currentEmergency, false);
                            } catch (Exception e) {}
                        }
                    });
                }
            } else {
                block();
            }
        }
    }
    
    class ReceiveDestination extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                if (content.startsWith("DESTINATION:")) {
                    String hospital = content.split(":")[1];
                    
                    System.out.println(getLocalName() + " transporting patient to " + hospital);
                    VisualizationHelper.log("🏥 " + getLocalName() + " transporting patient to " + hospital);
                    VisualizationHelper.updateAgent(getLocalName(), "ambulance", 
                        currentLocation, "Transporting to " + hospital, false);
                    
                    // Simulate transport
                    addBehaviour(new OneShotBehaviour() {
                        public void action() {
                            try {
                                Thread.sleep(4000);
                                System.out.println(getLocalName() + " arrived at " + hospital);
                                System.out.println("Patient delivered successfully!");
                                
                                available = true;
                                currentEmergency = null;
                                currentLocation = "Station1"; // Return to station
                                
                                System.out.println(getLocalName() + " returning to station and available again\n");
                                
                                VisualizationHelper.log("✓ " + getLocalName() + " patient delivered, returning to base");
                                VisualizationHelper.updateAgent(getLocalName(), "ambulance", 
                                    currentLocation, "Available at station", true);
                            } catch (Exception e) {}
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
            DFService.deregister(this);
        } catch (Exception e) {}
        System.out.println("Ambulance " + getAID().getLocalName() + " terminating.");
        VisualizationHelper.log("🚑 " + getLocalName() + " offline");
    }
}