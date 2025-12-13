import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;

public class PoliceAgent extends Agent {
    private String zone;
    private boolean onDuty = true;
    private String currentIncident = null;
    private String currentLocation;
    private int incidentsResolved = 0;
    
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            zone = (String) args[0];
        } else {
            zone = "Zone1";
        }
        
        currentLocation = zone + "_Station";
        
        System.out.println("\n👮 Police Unit " + getAID().getLocalName() + " reporting for duty");
        System.out.println("  Assigned Zone: " + zone);
        System.out.println("  Status: ON DUTY");
        
        // Register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("police");
        sd.setName(zone + "-police");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        addBehaviour(new PatrolBehaviour(this, 20000));
        addBehaviour(new RespondToIncidents());
        addBehaviour(new MonitorTraffic(this, 30000));
    }
    
    // Regular patrol behavior
    class PatrolBehaviour extends TickerBehaviour {
        private String[] patrolLocations;
        private int currentPatrolIndex = 0;
        
        public PatrolBehaviour(Agent a, long period) {
            super(a, period);
            patrolLocations = new String[]{
                zone + "_North",
                zone + "_South",
                zone + "_East",
                zone + "_West",
                zone + "_Center"
            };
        }
        
        public void onTick() {
            if (onDuty && currentIncident == null) {
                currentLocation = patrolLocations[currentPatrolIndex];
                System.out.println("[" + getLocalName() + "] 🚓 Patrolling " + 
                                 currentLocation + " - All clear");
                
                currentPatrolIndex = (currentPatrolIndex + 1) % patrolLocations.length;
            }
        }
    }
    
    // Respond to incidents from TCC
    class RespondToIncidents extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                
                if (content.startsWith("INCIDENT:")) {
                    String[] parts = content.split(":");
                    String incidentType = parts[1];
                    String location = parts.length > 2 ? parts[2] : zone;
                    
                    handleIncident(incidentType, location, msg.getSender());
                    
                } else if (content.startsWith("ASSIST_EMERGENCY:")) {
                    String emergencyId = content.split(":")[1];
                    assistEmergencyVehicle(emergencyId);
                    
                } else if (content.startsWith("TRAFFIC_CONTROL:")) {
                    String location = content.split(":")[1];
                    controlTraffic(location);
                }
                
            } else {
                block();
            }
        }
    }
    
    private void handleIncident(String type, String location, AID sender) {
        if (currentIncident != null) {
            // Busy with another incident
            ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
            reply.addReceiver(sender);
            reply.setContent("BUSY:" + currentIncident);
            send(reply);
            return;
        }
        
        currentIncident = type + "@" + location;
        onDuty = true;
        
        System.out.println("\n🚨 [" + getLocalName() + "] RESPONDING TO INCIDENT 🚨");
        System.out.println("  Type: " + type);
        System.out.println("  Location: " + location);
        System.out.println("  Current Location: " + currentLocation);
        
        // Confirm response
        ACLMessage confirm = new ACLMessage(ACLMessage.AGREE);
        confirm.addReceiver(sender);
        confirm.setContent("RESPONDING:" + type);
        send(confirm);
        
        // Simulate travel to incident
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                try {
                    Thread.sleep(3000);
                    System.out.println("[" + getLocalName() + "] Arrived at incident scene: " + location);
                    currentLocation = location;
                    
                    // Simulate handling incident
                    Thread.sleep(5000);
                    
                    incidentsResolved++;
                    System.out.println("[" + getLocalName() + "] ✓ Incident resolved: " + type);
                    System.out.println("  Total incidents resolved: " + incidentsResolved);
                    
                    // Inform TCC
                    ACLMessage report = new ACLMessage(ACLMessage.INFORM);
                    report.addReceiver(sender);
                    report.setContent("INCIDENT_RESOLVED:" + type + ":" + location);
                    send(report);
                    
                    currentIncident = null;
                    System.out.println("[" + getLocalName() + "] Returning to patrol\n");
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void assistEmergencyVehicle(String emergencyId) {
        System.out.println("[" + getLocalName() + "] 🚑 Assisting emergency vehicle " + emergencyId);
        System.out.println("  Clearing traffic and securing route");
        
        // Simulate assistance
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                try {
                    Thread.sleep(4000);
                    System.out.println("[" + getLocalName() + "] Emergency route secured for " + emergencyId);
                } catch (Exception e) {}
            }
        });
    }
    
    private void controlTraffic(String location) {
        System.out.println("[" + getLocalName() + "] 🚦 Managing traffic at " + location);
        
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                try {
                    Thread.sleep(3000);
                    System.out.println("[" + getLocalName() + "] Traffic flow restored at " + location);
                } catch (Exception e) {}
            }
        });
    }
    
    // Monitor traffic conditions
    class MonitorTraffic extends TickerBehaviour {
        public MonitorTraffic(Agent a, long period) {
            super(a, period);
        }
        
        public void onTick() {
            if (currentIncident == null) {
                // Randomly detect traffic issues
                if (Math.random() > 0.85) {
                    String[] issues = {"congestion", "accident", "breakdown"};
                    String issue = issues[(int)(Math.random() * issues.length)];
                    
                    System.out.println("[" + getLocalName() + "] ⚠️ Traffic " + issue + 
                                     " detected in " + zone);
                    
                    // Report to TCC
                    reportToTCC(issue);
                }
            }
        }
    }
    
    private void reportToTCC(String issue) {
        // Find TCC and report
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("traffic-control");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage report = new ACLMessage(ACLMessage.INFORM);
                report.addReceiver(result[0].getName());
                report.setContent("TRAFFIC_REPORT:" + issue + ":" + zone + ":" + currentLocation);
                send(report);
                
                System.out.println("[" + getLocalName() + "] Reported " + issue + " to TCC");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {}
        System.out.println("\n👮 Police Unit " + getAID().getLocalName() + " off duty.");
        System.out.println("  Total incidents resolved: " + incidentsResolved);
    }
}