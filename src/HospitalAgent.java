import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import java.util.*;

public class HospitalAgent extends Agent {
    private String hospitalName;
    private String capabilityLevel; // low, medium, high
    private int capacity;
    private int currentPatients = 0;
    private Map<String, Doctor> doctors;
    private List<String> departments;
    private List<String> equipment;
    private Map<String, Integer> specialtyScores;
    
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            hospitalName = (String) args[0];
            capabilityLevel = (String) args[1];
            capacity = Integer.parseInt((String) args[2]);
        } else {
            hospitalName = "General Hospital";
            capabilityLevel = "medium";
            capacity = 5;
        }
        
        initializeHospital();
        
        System.out.println("\n=== Hospital " + getAID().getLocalName() + " Initialized ===");
        System.out.println("Name: " + hospitalName);
        System.out.println("Capability: " + capabilityLevel);
        System.out.println("Capacity: " + capacity);
        System.out.println("Departments: " + departments);
        System.out.println("Doctors: " + doctors.size());
        
        // Register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("hospital");
        sd.setName(capabilityLevel + "-hospital");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        addBehaviour(new RespondToPatientRequests());
        addBehaviour(new HandlePatientAdmission());
    }
    
    private void initializeHospital() {
        doctors = new HashMap<>();
        departments = new ArrayList<>();
        equipment = new ArrayList<>();
        specialtyScores = new HashMap<>();
        
        switch (capabilityLevel) {
            case "low":
                departments.add("Emergency Room");
                departments.add("General Medicine");
                
                equipment.add("Basic Diagnostic Equipment");
                equipment.add("X-Ray");
                equipment.add("Basic Lab");
                
                doctors.put("Dr. Smith", new Doctor("Dr. Smith", "General Medicine", 70));
                doctors.put("Dr. Johnson", new Doctor("Dr. Johnson", "Emergency", 65));
                
                specialtyScores.put("trauma", 40);
                specialtyScores.put("cardiac", 30);
                specialtyScores.put("respiratory", 35);
                specialtyScores.put("neurological", 25);
                specialtyScores.put("general", 60);
                break;
                
            case "medium":
                departments.add("Emergency Room");
                departments.add("General Medicine");
                departments.add("Cardiology");
                departments.add("Surgery");
                
                equipment.add("CT Scanner");
                equipment.add("Advanced Lab");
                equipment.add("X-Ray");
                equipment.add("Ultrasound");
                equipment.add("Operating Rooms (2)");
                
                doctors.put("Dr. Williams", new Doctor("Dr. Williams", "Cardiology", 85));
                doctors.put("Dr. Brown", new Doctor("Dr. Brown", "Surgery", 80));
                doctors.put("Dr. Davis", new Doctor("Dr. Davis", "Emergency", 75));
                doctors.put("Dr. Miller", new Doctor("Dr. Miller", "General Medicine", 78));
                
                specialtyScores.put("trauma", 70);
                specialtyScores.put("cardiac", 80);
                specialtyScores.put("respiratory", 65);
                specialtyScores.put("neurological", 60);
                specialtyScores.put("general", 75);
                break;
                
            case "high": // ELITE/PERFECT HOSPITAL
                departments.add("Advanced Trauma Center");
                departments.add("Cardiac Center of Excellence");
                departments.add("Neurosurgery Department");
                departments.add("Advanced ICU");
                departments.add("Emergency Medicine");
                departments.add("Specialized Surgery");
                departments.add("Burn Unit");
                
                equipment.add("MRI (Multiple)");
                equipment.add("Advanced CT Scanner");
                equipment.add("Cardiac Catheterization Lab");
                equipment.add("State-of-the-art Operating Rooms (6)");
                equipment.add("Helicopter Pad");
                equipment.add("Advanced Life Support Systems");
                equipment.add("Complete Laboratory");
                equipment.add("Blood Bank");
                
                // BEST DOCTORS
                doctors.put("Dr. Anderson", new Doctor("Dr. Anderson", "Trauma Surgery", 98));
                doctors.put("Dr. Martinez", new Doctor("Dr. Martinez", "Cardiology", 99));
                doctors.put("Dr. Taylor", new Doctor("Dr. Taylor", "Neurosurgery", 97));
                doctors.put("Dr. Thomas", new Doctor("Dr. Thomas", "Emergency Medicine", 95));
                doctors.put("Dr. Garcia", new Doctor("Dr. Garcia", "Cardiac Surgery", 98));
                doctors.put("Dr. Rodriguez", new Doctor("Dr. Rodriguez", "Critical Care", 96));
                
                specialtyScores.put("trauma", 98);
                specialtyScores.put("cardiac", 100);
                specialtyScores.put("respiratory", 95);
                specialtyScores.put("neurological", 97);
                specialtyScores.put("general", 90);
                break;
        }
    }
    
    // Respond to patient requests
    class RespondToPatientRequests extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("hospital-selection"));
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String[] parts = msg.getContent().split(":");
                String emergencyId = parts[1];
                String type = parts[2];
                String severity = parts[3];
                
                if (currentPatients < capacity) {
                    int capabilityScore = calculateCapability(type, severity);
                    
                    ACLMessage proposal = msg.createReply();
                    proposal.setPerformative(ACLMessage.PROPOSE);
                    proposal.setContent(emergencyId + ":" + capabilityScore);
                    send(proposal);
                    
                    System.out.println(hospitalName + " proposing with capability: " + 
                                     capabilityScore + " for " + type + " emergency");
                } else {
                    ACLMessage refuse = msg.createReply();
                    refuse.setPerformative(ACLMessage.REFUSE);
                    refuse.setContent("CAPACITY_FULL");
                    send(refuse);
                    
                    System.out.println(hospitalName + " at full capacity");
                }
            } else {
                block();
            }
        }
    }
    
    private int calculateCapability(String type, String severity) {
        int baseScore = specialtyScores.getOrDefault(type, 50);
        
        // Severity adjustment
        if (severity.equals("critical")) {
            baseScore += 10;
        } else if (severity.equals("high")) {
            baseScore += 5;
        }
        
        // Capacity bonus
        if (currentPatients < capacity / 2) {
            baseScore += 10;
        }
        
        // Find best doctor for this case
        Doctor bestDoctor = findBestDoctor(type);
        if (bestDoctor != null) {
            baseScore += (bestDoctor.skillLevel / 10);
        }
        
        return Math.min(100, baseScore);
    }
    
    private Doctor findBestDoctor(String emergencyType) {
        Doctor best = null;
        for (Doctor doc : doctors.values()) {
            if (doc.isAvailable && matchesSpecialty(doc.specialty, emergencyType)) {
                if (best == null || doc.skillLevel > best.skillLevel) {
                    best = doc;
                }
            }
        }
        return best;
    }
    
    private boolean matchesSpecialty(String specialty, String emergencyType) {
        specialty = specialty.toLowerCase();
        emergencyType = emergencyType.toLowerCase();
        
        if (specialty.contains(emergencyType)) return true;
        if (emergencyType.equals("cardiac") && specialty.contains("cardio")) return true;
        if (emergencyType.equals("trauma") && specialty.contains("surgery")) return true;
        if (emergencyType.equals("neurological") && specialty.contains("neuro")) return true;
        if (specialty.contains("emergency")) return true;
        
        return false;
    }
    
    // Handle patient admission
    class HandlePatientAdmission extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchConversationId("hospital-selection"),
                MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                )
            );
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    String[] parts = msg.getContent().split(":");
                    String emergencyId = parts[0];
                    String type = parts[1];
                    String severity = parts[2];
                    
                    currentPatients++;
                    
                    System.out.println("\n*** " + hospitalName + " ACCEPTING PATIENT ***");
                    System.out.println("Emergency ID: " + emergencyId);
                    System.out.println("Type: " + type);
                    System.out.println("Severity: " + severity);
                    
                    // Assign best doctor
                    Doctor assignedDoctor = findBestDoctor(type);
                    if (assignedDoctor != null) {
                        assignedDoctor.isAvailable = false;
                        System.out.println("Assigned Doctor: " + assignedDoctor.name + 
                                         " (" + assignedDoctor.specialty + 
                                         ", Skill Level: " + assignedDoctor.skillLevel + ")");
                    }
                    
                    System.out.println("Current Patients: " + currentPatients + "/" + capacity);
                    
                    // Simulate treatment
                    addBehaviour(new OneShotBehaviour() {
                        public void action() {
                            try {
                                Thread.sleep(5000);
                                System.out.println(hospitalName + " completed treatment for " + emergencyId);
                                currentPatients--;
                                if (assignedDoctor != null) {
                                    assignedDoctor.isAvailable = true;
                                }
                                System.out.println("Patient discharged. Current patients: " + 
                                                 currentPatients + "/" + capacity + "\n");
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
        System.out.println("Hospital " + hospitalName + " terminating.");
    }
}

// Doctor class
class Doctor {
    String name;
    String specialty;
    int skillLevel; // 0-100
    boolean isAvailable = true;
    
    Doctor(String name, String specialty, int skillLevel) {
        this.name = name;
        this.specialty = specialty;
        this.skillLevel = skillLevel;
    }
    
    @Override
    public String toString() {
        return name + " (" + specialty + ", Level: " + skillLevel + ")";
    }
}