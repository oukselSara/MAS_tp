import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import java.util.*;

public class TrafficControlCenter extends Agent {
    private Map<String, String> trafficStatus = new HashMap<>();
    private Map<String, EmergencyInfo> activeEmergencies = new HashMap<>();
    private int emergencyCounter = 0;
    
    protected void setup() {
        System.out.println("Traffic Control Center " + getAID().getName() + " is ready.");
        
        // Register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("traffic-control");
        sd.setName("TCC");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Add behaviors
        addBehaviour(new EmergencyHandler());
        addBehaviour(new TrafficMonitor(this, 15000));
        addBehaviour(new AmbulanceResponseHandler());
        addBehaviour(new HospitalResponseHandler());
    }
    
    // Handle emergency requests
    class EmergencyHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                if (content.startsWith("EMERGENCY:")) {
                    handleEmergency(content, msg.getSender());
                }
            } else {
                block();
            }
        }
        
        private void handleEmergency(String content, AID sender) {
            String[] parts = content.split(":");
            String emergencyId = "EMG" + (++emergencyCounter);
            String type = parts[1];
            String severity = parts[2];
            String location = parts[3];
            
            System.out.println("\n=== NEW EMERGENCY ===");
            System.out.println("ID: " + emergencyId);
            System.out.println("Type: " + type);
            System.out.println("Severity: " + severity);
            System.out.println("Location: " + location);
            
            EmergencyInfo emergency = new EmergencyInfo(emergencyId, type, severity, location);
            activeEmergencies.put(emergencyId, emergency);
            
            // Request ambulance proposals
            findBestAmbulance(emergency);
        }
    }
    
    private void findBestAmbulance(EmergencyInfo emergency) {
        // Search for ambulances
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ambulance");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            System.out.println("Found " + result.length + " ambulances");
            
            // Send CFP to all ambulances
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (DFAgentDescription dfd : result) {
                cfp.addReceiver(dfd.getName());
            }
            cfp.setContent("AMBULANCE_NEEDED:" + emergency.id + ":" + 
                          emergency.type + ":" + emergency.severity + ":" + 
                          emergency.location);
            cfp.setConversationId("ambulance-dispatch");
            cfp.setReplyWith("cfp" + System.currentTimeMillis());
            send(cfp);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Handle ambulance proposals
    class AmbulanceResponseHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                MessageTemplate.MatchConversationId("ambulance-dispatch"));
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String[] parts = msg.getContent().split(":");
                String emergencyId = parts[0];
                int score = Integer.parseInt(parts[1]);
                String equipment = parts[2];
                
                EmergencyInfo emergency = activeEmergencies.get(emergencyId);
                if (emergency != null && !emergency.ambulanceAssigned) {
                    emergency.ambulanceProposals.add(new AmbulanceProposal(
                        msg.getSender(), score, equipment));
                    
                    // Wait a bit for all proposals, then select best
                    if (emergency.ambulanceProposals.size() >= 2) {
                        selectBestAmbulance(emergency);
                    }
                }
            } else {
                block();
            }
        }
    }
    
    private void selectBestAmbulance(EmergencyInfo emergency) {
        if (emergency.ambulanceAssigned) return;
        
        AmbulanceProposal best = null;
        for (AmbulanceProposal prop : emergency.ambulanceProposals) {
            if (best == null || prop.score > best.score) {
                best = prop;
            }
        }
        
        if (best != null) {
            emergency.ambulanceAssigned = true;
            emergency.selectedAmbulance = best.ambulance;
            
            // Accept best ambulance
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(best.ambulance);
            accept.setContent(emergency.id + ":" + emergency.location);
            accept.setConversationId("ambulance-dispatch");
            send(accept);
            
            System.out.println("Selected ambulance: " + best.ambulance.getLocalName() + 
                             " (Score: " + best.score + ", Equipment: " + best.equipment + ")");
            
            // Reject others
            for (AmbulanceProposal prop : emergency.ambulanceProposals) {
                if (!prop.ambulance.equals(best.ambulance)) {
                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reject.addReceiver(prop.ambulance);
                    reject.setContent(emergency.id);
                    reject.setConversationId("ambulance-dispatch");
                    send(reject);
                }
            }
            
            // Clear traffic path
            clearTrafficPath(emergency.location);
            
            // Find best hospital
            findBestHospital(emergency);
        }
    }
    
    private void findBestHospital(EmergencyInfo emergency) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("hospital");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            System.out.println("Found " + result.length + " hospitals");
            
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (DFAgentDescription dfd : result) {
                cfp.addReceiver(dfd.getName());
            }
            cfp.setContent("PATIENT_INCOMING:" + emergency.id + ":" + 
                          emergency.type + ":" + emergency.severity);
            cfp.setConversationId("hospital-selection");
            cfp.setReplyWith("hospital-cfp" + System.currentTimeMillis());
            send(cfp);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Handle hospital proposals
    class HospitalResponseHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                MessageTemplate.MatchConversationId("hospital-selection"));
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String[] parts = msg.getContent().split(":");
                String emergencyId = parts[0];
                int capability = Integer.parseInt(parts[1]);
                
                EmergencyInfo emergency = activeEmergencies.get(emergencyId);
                if (emergency != null && !emergency.hospitalAssigned) {
                    emergency.hospitalProposals.add(new HospitalProposal(
                        msg.getSender(), capability));
                    
                    if (emergency.hospitalProposals.size() >= 2) {
                        selectBestHospital(emergency);
                    }
                }
            } else {
                block();
            }
        }
    }
    
    private void selectBestHospital(EmergencyInfo emergency) {
        if (emergency.hospitalAssigned) return;
        
        HospitalProposal best = null;
        for (HospitalProposal prop : emergency.hospitalProposals) {
            if (best == null || prop.capability > best.capability) {
                best = prop;
            }
        }
        
        if (best != null) {
            emergency.hospitalAssigned = true;
            emergency.selectedHospital = best.hospital;
            
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(best.hospital);
            accept.setContent(emergency.id + ":" + emergency.type + ":" + emergency.severity);
            accept.setConversationId("hospital-selection");
            send(accept);
            
            System.out.println("Selected hospital: " + best.hospital.getLocalName() + 
                             " (Capability: " + best.capability + ")");
            
            // Notify ambulance of hospital destination
            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
            inform.addReceiver(emergency.selectedAmbulance);
            inform.setContent("DESTINATION:" + best.hospital.getLocalName());
            send(inform);
            
            // Reject other hospitals
            for (HospitalProposal prop : emergency.hospitalProposals) {
                if (!prop.hospital.equals(best.hospital)) {
                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reject.addReceiver(prop.hospital);
                    reject.setContent(emergency.id);
                    reject.setConversationId("hospital-selection");
                    send(reject);
                }
            }
        }
    }
    
    private void clearTrafficPath(String location) {
        // Send priority messages to traffic lights
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("traffic-light");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            for (DFAgentDescription dfd : result) {
                msg.addReceiver(dfd.getName());
            }
            msg.setContent("PRIORITY_ROUTE:" + location);
            send(msg);
            
            System.out.println("Traffic path cleared for emergency route");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Monitor traffic periodically
    class TrafficMonitor extends TickerBehaviour {
        public TrafficMonitor(Agent a, long period) {
            super(a, period);
        }
        
        public void onTick() {
            System.out.println("\n--- Traffic Status Update ---");
            System.out.println("Active Emergencies: " + activeEmergencies.size());
            for (EmergencyInfo e : activeEmergencies.values()) {
                System.out.println("  " + e.id + " - Status: " + 
                    (e.ambulanceAssigned ? "Ambulance Dispatched" : "Waiting"));
            }
        }
    }
    
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {}
        System.out.println("Traffic Control Center terminating.");
    }
}

// Helper classes
class EmergencyInfo {
    String id;
    String type;
    String severity;
    String location;
    boolean ambulanceAssigned = false;
    boolean hospitalAssigned = false;
    AID selectedAmbulance;
    AID selectedHospital;
    List<AmbulanceProposal> ambulanceProposals = new ArrayList<>();
    List<HospitalProposal> hospitalProposals = new ArrayList<>();
    
    EmergencyInfo(String id, String type, String severity, String location) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.location = location;
    }
}

class AmbulanceProposal {
    AID ambulance;
    int score;
    String equipment;
    
    AmbulanceProposal(AID ambulance, int score, String equipment) {
        this.ambulance = ambulance;
        this.score = score;
        this.equipment = equipment;
    }
}

class HospitalProposal {
    AID hospital;
    int capability;
    
    HospitalProposal(AID hospital, int capability) {
        this.hospital = hospital;
        this.capability = capability;
    }
}