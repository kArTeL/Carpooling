import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class CarAgent extends Agent {
	// The rides a car agent is offering
	private Map rides;
	// The GUI by means of which the user can add rides
	private CarGui myGui;

	// Put agent initializations here
        @Override
	protected void setup() {
            // Create the catalogue
            rides = new HashMap<String, Ride>(); //Destiny, ride info

            // Create and show the GUI 
            myGui = new CarGui(this);
            myGui.showGui();

            // Register the car-agent service in the yellow pages
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("car-pooling");
            sd.setName("JADE-car-pooling");
            dfd.addServices(sd);
            try {
                    DFService.register(this, dfd);
            }
            catch (FIPAException fe) {
                    fe.printStackTrace();
            }

            // Add the behaviour serving queries from passenger agents
            addBehaviour(new OfferRequestsServer());

            // Add the behaviour serving seat resevations from passenger agents
            addBehaviour(new PurchaseOrdersServer());
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Close the GUI
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("Car-agent "+getAID().getName()+" terminating.");
	}

	/**
     This is invoked by the GUI when the user adds a new offered ride
     * @param arrivalTime
	 */
	public void updateCatalogue(final String origin, final String destiny, final String departureTime ,
                                    final String arrivalTime, final int freeSeats, final int price) {
            addBehaviour(new OneShotBehaviour() {
                public void action() {
                        rides.put(destiny, new Ride(origin, destiny, departureTime, arrivalTime, freeSeats, price));
                        System.out.println("[" +getAID().getName()+ "]: Nuevo viaje agreado: " + origin + "("+departureTime+") -> " + destiny + "("+arrivalTime+"),"+" precio: $" + price + ". Campos libres:");
                        System.out.println("Campos libres:" + freeSeats);
                }      
            } );
	}

	/**
	   Inner class OfferRequestsServer.
	   This is the behaviour used by Car agents to serve incoming requests 
	   for offer from passegner agents.
	   If the requested ride is in the local catalogue the car agent replies 
	   with a PROPOSE message specifying the price, origin place and departure time. Otherwise a REFUSE message is
	   sent back.
	 */
	private class OfferRequestsServer extends CyclicBehaviour {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    // CFP Message received. Process it
                    String destiny = msg.getContent();
                    ACLMessage reply = msg.createReply();
                    
                    Ride proposeRide = (Ride) rides.get(destiny);
                    if (proposeRide != null && proposeRide.freeSeats > 0) {
                        // There are rides to the destiny place. Reply with ride information.
                        reply.setPerformative(ACLMessage.PROPOSE);
                        String proposal = "origin="+proposeRide.origin +";departTime="+proposeRide.getDepartTime()+";arrivalTime="+proposeRide.getArrivalTime()+";price="+proposeRide.price;
                        reply.setContent(proposal);
                    }
                    else {
                        // There are no rides to the destiny.
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("not-available");
                    }
                    myAgent.send(reply);
                }
                else {
                    block();
                }
            }
	}  // End of inner class OfferRequestsServer

	/**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by Car agents to serve incoming 
	   offer acceptances (i.e. reserve seats on ride) from passenger agents.
	   The seller agent reduces the amount of free seats on the given ride 
	   and replies with an INFORM message to notify the passegner that the
	   recervation has been sucesfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Ride ride = (Ride) rides.get(title);
				if (ride != null && ride.freeSeats > 0) {
                                    ride.freeSeats--;
                                    rides.put(ride.destiny, ride);
                                    reply.setPerformative(ACLMessage.INFORM);
                                    System.out.println("[" +getAID().getName()+ "]: Campo en el viaje " + ride.origin + "("+ride.getDepartTime()+") -> " + ride.destiny + "("+ride.getArrivalTime()+") reservado para " + msg.getSender().getName() + " quedan "+ride.freeSeats + " campos disponibles");
				}
				else {
					// The seat has been taken by other passenger in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer
}
