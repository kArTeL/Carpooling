import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class PassengerAgent extends Agent {
	
	private String _origin, _destiny, _departTime, _arrivalTime;
	// The list of known cart/seller agents
	private AID[] sellerAgents;
        //GUI of passenger agent
        private PassengerGui myGui;

	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
            
            myGui = new PassengerGui(this);
            myGui.showGui();
            
            System.out.println("Hola! 'Pasajero' "+getAID().getName()+" is ready.");
            

            // Get the title of the book to buy as a start-up argument            
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Passenger-agent "+getAID().getName()+" terminating.");
	}
        
        public void askForRide(final String origin, final String destiny,
                                    final String arrivalTime) {
            addBehaviour(new TickerBehaviour(this, 6000) {
                    protected void onTick() {
                        _origin = origin;
                        _arrivalTime = arrivalTime;
                        _destiny = destiny;
                        System.out.println("[" +getAID().getName()+ "]: Intentando apartar campo para " + _destiny+"("+_arrivalTime+")");
                        // Update the list of carts agents
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("car-pooling");
                        template.addServices(sd);
                        try {
                                DFAgentDescription[] result = DFService.search(myAgent, template); 
                                System.out.println("[" +getAID().getName()+ "]: Se encontraron los siguientes carros disponibles:");
                                sellerAgents = new AID[result.length];
                                for (int i = 0; i < result.length; ++i) {
                                        sellerAgents[i] = result[i].getName();
                                        System.out.println(sellerAgents[i].getName());
                                }
                        }
                        catch (FIPAException fe) {
                                fe.printStackTrace();
                        }

                        // Perform the request
                        myAgent.addBehaviour(new RequestPerformer());
                    }
                } );
	}

	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by Book-buyer agents to request seller 
	   agents the target book.
	 */
	private class RequestPerformer extends Behaviour {
            private AID bestSeller; // The agent who provides the best offer 
            private int bestPrice;  // The best offered price
            private int repliesCnt = 0; // The counter of replies from seller agents
            private MessageTemplate mt; // The template to receive replies
            private int step = 0;

            public void action() {
                switch (step) {
                case 0:
                        // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i) {
                            cfp.addReceiver(sellerAgents[i]);
                    } 
                    cfp.setContent(_destiny);
                    cfp.setConversationId("car-pool");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("car-pool"),
                                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer 
                            String[] propose = reply.getContent().split(";");
                            String price_format = propose[3];                                    
                            int price = Integer.parseInt(price_format.substring(price_format.indexOf("=")+1));
                            if ((bestSeller == null || price < bestPrice) && checkFunction(propose)) {
                                // This is the best offer at present

                                bestPrice = price;
                                _departTime = propose[1].substring(propose[1].indexOf("=")+1);
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length) {
                                // We received all replies
                                step = 2; 
                        }
                    }
                    else {
                            block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(_destiny);
                    order.setConversationId("car-pool");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("car-pool"),
                                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:      
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                            // Purchase order reply received
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                    // Purchase successful. We can terminate
                                    System.out.println("[" +getAID().getName()+ "]: Campo reservado en carro: "+reply.getSender().getName());
                                    System.out.println("[" +getAID().getName()+ "]: Ruta "+ _origin+"("+_departTime+") -> "+_destiny+", Precio = $"+bestPrice);
                                    myAgent.doDelete();
                            }
                            else {
                                    System.out.println("[" +getAID().getName()+ "]: Fallo: El viaje encontrado no contiene campos.");
                            }

                            step = 4;
                    }
                    else {
                            block();
                    }
                    break;
                }        
        }

            public boolean done() {
                if (step == 2 && bestSeller == null) {
                        System.out.println("[" +getAID().getName()+ "]: Fallo: No hay viajes para el destino dado.");
                }
                return ((step == 2 && bestSeller == null) || step == 4);
            }
            
            public boolean checkFunction(String[] proposition) {
                if(!_origin.equals(proposition[0].substring(proposition[0].indexOf("=") + 1))) 
                    return false;
                String[] proposedArrivalTime = proposition[2].substring(proposition[2].indexOf("=") +1).split(":");
                String[] myArrivalTime = _arrivalTime.split(":");
                if(Integer.parseInt(proposedArrivalTime[0]) < Integer.parseInt(myArrivalTime[0])) {
                    return true;
                } else if(Integer.parseInt(proposedArrivalTime[0]) == Integer.parseInt(myArrivalTime[0]) && Integer.parseInt(proposedArrivalTime[1]) <= Integer.parseInt(myArrivalTime[1])) {
                    return true;
                } else {
                    return false;
                }
            }
	} // End of inner class RequestPerformer 
        
}
