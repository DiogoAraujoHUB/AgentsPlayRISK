package risk;

import jade.core.Agent;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jade.core.AID;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.DFService;
import jade.lang.acl.UnreadableException;

public class RISKPlayerAgent extends Agent {
    //Set up the player agent
    protected void setup() {
        try {
            //Set up the Game Master description
            ServiceDescription sd = new ServiceDescription();
            sd.setType("Player");
            sd.setName(getLocalName());

            //Create agent description of itself
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            //Register description with DF
            dfd.addServices(sd);
            try {
                DFService.register(this, dfd);
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            //Set its message queue

            //Notify game master that player has been created
            ACLMessage hello = new ACLMessage(ACLMessage.INFORM);
            hello.setContent(RISKGameMasterAgent.HELLO);
            hello.addReceiver(new AID("GameMaster", AID.ISLOCALNAME));
            send(hello);

            //Add behaviour to process messages
            CyclicBehaviour handle_queries = new CyclicBehaviour(this) {
                public void action() {
                    //Get message with action we need to decide on
                    ACLMessage decision = blockingReceive();

                    //Process all messages that are not null
                    if (decision != null) {
                        //Get the message content
                        String[] content = decision.getContent().split(":");

                        //Check if player was accepted for game
                        if (content[0].equals(RISKGameMasterAgent.DECISION)) {
                            //Get action player must decide on
                            String action = content[1];

                            //Check what action they are referring to
                            if (action.equals(RISKGameMasterAgent.SETUP)) { //Setup Phase
                                setup_phase(decision);
                            } else if (action.equals(RISKGameMasterAgent.ATTACK)) { //Attack Phase
                                attack_phase(decision);
                            } else if (action.equals(RISKGameMasterAgent.DEFEND)) { //Defense Phase
                                defense_phase(decision, content);
                            } else if (action.equals(RISKGameMasterAgent.PLACE)) { //Growth Phase
                                growth_phase(decision);
                            } else if (action.equals(RISKGameMasterAgent.MOVE)) { //Movement Phase
                                movement_phase(decision);
                            } else if (action.equals(RISKGameMasterAgent.ALLIANCE)) { //Negotiation Phase (Alliance)
                                negotiation_phase(decision);
                            } else if (action.equals(RISKGameMasterAgent.FORM)) { //Negotiation Phase (Form)
                                form_phase(decision, content);
                            } else {
                                //Message has not been defined or is badly written
                                System.out.println("Message was not understood.");
                            }
                        }
                    }
                }
            };

            //Add the behaviour to the player agent
            addBehaviour(handle_queries);
        } catch (Exception e) {
            System.out.println("Saw exception in GuestAgent: " + e);
            e.printStackTrace();
        }
    }

    //Define the setup phase for the agent
    private void setup_phase(ACLMessage decision) {
        //Define random for random agent
        Random r = new Random();

        //Get game state
        RISKBoard board = get_game_state(decision);

        //Go through loop to find valid placement
        while (true) {
            //Get list of countries that still arent owned
            List<Integer> unowned = board.get_unowned_countries();

            //Get parameters for random
            int min = 0;
            int max = unowned.size();

            //Get placement area
            int pos = r.nextInt(max);
            int result = unowned.get(pos);

            //Check if set up area is valid
            String owner = board.get_country_owner(result);
            if (owner == null) {
                //Area is valid, therefore set up troops
                send_setup(decision, result);
                break;
            }
        }
    }

    //Send the setup message to the game master (SETUP:1)
    private void send_setup(ACLMessage decision, int country) {
        ACLMessage reply = decision.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(RISKGameMasterAgent.SETUP + ":" + country);
        send(reply);
    }

    //Define the growth phase behaviour for the agent (PLACE:3:2)
    private void growth_phase(ACLMessage decision) {
        //Define random for random agent
        Random r = new Random();

        //Get game state
        RISKBoard board = get_game_state(decision);

        //Get player owned countries
        List<Integer> countries = board.get_player_countries(getLocalName());

        //Define random area (list size)
        int min = 0;
        int max = countries.size();

        //Get one of the countries from that list
        int pos = r.nextInt(max);
        int country = countries.get(pos);

        //Get a number between one and three (doesnt take into account number of troops available)
        int num_troops = r.nextInt(3) + 1;

        //Place troops in that country
        send_placement(decision, country, num_troops);
    }

    //Send the placement message to the game master
    private void send_placement(ACLMessage decision, int country, int num_troops) {
        ACLMessage reply = decision.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(RISKGameMasterAgent.PLACE + ":" + country + ":" + num_troops);
        send(reply);
    }

    //Define the attack phase behaviour for the agent (ATTACK:2:3:1)
    private void attack_phase(ACLMessage decision) {
        //Get Game State
        RISKBoard board = get_game_state(decision);

        //Get Countries that agent owns
        List<Integer> countries = board.get_player_countries(getLocalName());

        //Define random for random agent
        Random r = new Random();
        int min = 0;
        int max = countries.size();

        //Go through loop until attack is valid
        int attempts = 0; //Save a max amount of attempts (6)
        while (true) {
            //Max amount of attempts have been reached so just pass
            if (attempts >= 6) {
                send_pass(decision);
                break;
            }

            //Get a country from list of possible countries
            int pos = r.nextInt(max);
            int attacking_country = countries.get(pos);

            //Select the number of troops to attack with (from attacking country) (using random)
            int troopsAvailable = board.get_country_troops(attacking_country);

            //NEW: Check if country only has one troop (cant attack)
            if (troopsAvailable == 1) {
                attempts++;
                continue;
            }

            //Get connections to the attacking country
            int[] connections = board.get_country_connections(attacking_country);

            //Get the enemy countries from connections
            List<Integer> enemyCountries = new ArrayList<>();
            for (int country : connections) {
                //Check if country belongs to another player (owned by player, alliance)
                if (!board.get_country_owner(country).equals(getLocalName())) {
                    if (!board.alliance_exists(getLocalName(), board.get_country_owner(country))) { //Check alliance
                        //Add to enemy countries list
                        enemyCountries.add(country);
                    }
                }
            }

            //No enemy countries in connections, so try another country
            if (enemyCountries.size() == 0) {
                attempts++;
                continue;
            }

            //Get random enemy country (using random)
            min = 0;
            max = enemyCountries.size();
            pos = r.nextInt(max);
            int defending_country = enemyCountries.get(pos);

            //NEW: Choose amount of troops to attack with (random)
            min = 1;
            max = troopsAvailable - 1;
            int num_troops = r.nextInt(max) + min;

            //Send message with attack parameters
            send_attack(decision, attacking_country, defending_country, num_troops);
            break;
        }
    }

    //Send the attack message to the game master (ATTACK:3:2:1)
    private void send_attack(ACLMessage decision, int attacking_country, int defending_country, int num_troops) {
        ACLMessage reply = decision.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(RISKGameMasterAgent.ATTACK + ":" + attacking_country + ":" + defending_country + ":" + num_troops);
        send(reply);
    }

    //Define the defense phase behaviour for the agent (DEFEND:1:2)
    private void defense_phase(ACLMessage decision, String[] content) {
        //Define random for random agent
        Random r = new Random();

        //Get country being attacked and troops attacking (attackers are used in comparison -> not in random)
        int defending_country = Integer.parseInt(content[2]);
        int num_attackers = Integer.parseInt(content[3]);

        //Get game state
        RISKBoard board = get_game_state(decision);

        //Check how many troops country has to defend with (if it only has 1 troop then we defend with 1 troop)
        int num_defenders = 1;
        if (board.get_country_troops(defending_country) >= 2) {
            int min = 1;
            int max = 2;

            //Choose amount of troops defending
            num_defenders = r.nextInt(max) + min;
        }

        //Send message with defense parameters
        send_defense(decision, defending_country, num_defenders);
    }

    //Send the defense message to the game master (DEFEND:2:1)
    private void send_defense(ACLMessage decision, int defending_country, int num_defenders) {
        ACLMessage reply = decision.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(RISKGameMasterAgent.DEFEND + ":" + defending_country + ":" + num_defenders);
        send(reply);
    }

    //Define the movement phase behaviour for the agent (MOVE:2:3:1)
    private void movement_phase(ACLMessage decision) {
        //Get game state
        RISKBoard board = get_game_state(decision);

        //Get Countries that agent owns
        List<Integer> countries = board.get_player_countries(getLocalName());

        //Define random for random agent
        Random r = new Random();
        int min = 0;
        int max = countries.size();

        //Go through loop until movement is valid
        int attempts = 0; //Set a max amount of attempts (6)
        while (true) {
            //Max amount of attempts have been reached so just pass
            if (attempts >= 6) {
                send_pass(decision);
                break;
            }

            //Get a country from list of possible countries
            int pos = r.nextInt(max);
            int initial_country = countries.get(pos);

            //Select the number of troops to attack with (from initial country)
            int troopsAvailable = board.get_country_troops(initial_country);

            //NEW: Check if country only has one troop (cant move)
            if (troopsAvailable == 1) {
                attempts++;
                continue;
            }

            //Get connections to the initial country
            int[] connections = board.get_country_connections(initial_country);

            //Get the owned countries and enemy countries from connections
            List<Integer> ownedCountries = new ArrayList<>();
            List<Integer> enemyCountries = new ArrayList<>();
            for (int country : connections) {
                //Check if country belongs to player
                if (board.get_country_owner(country).equals(getLocalName())) {
                    ownedCountries.add(country);
                } else {
                    enemyCountries.add(country);
                }
            }

            //Check if there are enemy countries connected (dont move as it serves as wall)
            if (enemyCountries.size() != 0) {
                send_pass(decision);
                break;
            }

            //No owned countries in connections and no enemies, so get another country
            if (ownedCountries.size() == 0) {
                attempts++;
                continue;
            }

            //Get new random parameters
            min = 0;
            max = ownedCountries.size();

            //Get a country from list of owned countries
            pos = r.nextInt(max);
            int final_country = ownedCountries.get(pos);

            //NEW: Choose amount of troops to move with (random)
            min = 1;
            max = troopsAvailable;
            int num_troops = r.nextInt(max - min) + min;

            //Send message with movement parameters
            send_movement(decision, initial_country, final_country, num_troops);
            break;
        }
    }

    //Send the movement message to the game master (MOVE:2:3:1)
    private void send_movement(ACLMessage decision, int initial_country, int final_country, int num_troops) {
        ACLMessage reply = decision.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(RISKGameMasterAgent.MOVE + ":" + initial_country + ":" + final_country + ":" + num_troops);
        send(reply);
    }

    //Define the negotiation phase behaviour for the agent (MOVE:2:3:1)
    private void negotiation_phase(ACLMessage decision) {
        //Define random for random agent
        Random r = new Random();
        int min = 1;
        int max = 100;

        //Get chance of negotiation starting (5%)
        int chance = r.nextInt(max) + min;

        //Get game state
        RISKBoard board = get_game_state(decision);

        //Check if negotiation is starting
        if (chance <= 5) { //Start negotiation (5%)
            //Get list of still playing players
            List<String> players = RISKGameMasterAgent.get_current_players();

            //Check if list has more than two players (alliances cant be made between final two players)
            if (players.size() <= 2) {
                send_negotiation(decision, false, "");
                return;
            }

            //Get a player to make an alliance with (loop)
            min = 0;
            max = players.size();
            while (true) {
                //Choose one of the players (random)
                int pos = r.nextInt(max);
                String player = players.get(pos);

                //Check if it isnt current player
                if (player.equals(getLocalName())) {
                    continue;
                }

                //Send negotiation (alliance) to player that was chosen
                send_negotiation(decision, true, player);
                break;
            }
        } else {
            //Dont start negotiation (95%)
            send_negotiation(decision, false, "");
        }
    }

    //Send the negotiation message to the game master (ALLIANCE:ACCEPT:Player2)
    private void send_negotiation(ACLMessage decision, boolean accept, String player_id) {
        ACLMessage reply = decision.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        if (accept) {
            reply.setContent(RISKGameMasterAgent.ALLIANCE + ":" + RISKGameMasterAgent.ACCEPT + ":" + player_id);
        } else {
            reply.setContent(RISKGameMasterAgent.ALLIANCE + ":" + RISKGameMasterAgent.DECLINE);
        }
        send(reply);
    }

    //Receive the negotiation message sent by a player via game master and accept or decline it (FORM:ACCEPT)
    private void form_phase(ACLMessage decision, String[] content) {
        //Get player that is requesting alliance
        String possible_ally = content[2];

        //Define random for random agent
        Random r = new Random();
        int min = 1;
        int max = 100;

        //Get chance that it will accept alliance forming (50%)
        int chance = r.nextInt(max) + min;

        //Check if alliance has been formed (50%)
        if (chance <= 50) {
            //Form alliance
            send_form(decision, true);
        } else {
            //Dont form alliance
            send_form(decision, false);
        }
    }

    //Send the forming message to the game master (FORM:ACCEPT)
    private void send_form(ACLMessage decision, boolean accept) {
        ACLMessage reply = decision.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        if (accept) {
            reply.setContent(RISKGameMasterAgent.FORM + ":" + RISKGameMasterAgent.ACCEPT);
        } else {
            reply.setContent(RISKGameMasterAgent.FORM + ":" + RISKGameMasterAgent.DECLINE);
        }
        send(reply);
    }

    //Send the pass message to the game master (PASS)
    private void send_pass(ACLMessage decision) {
        ACLMessage reply = decision.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(RISKGameMasterAgent.PASS);
        send(reply);
    }

    //Ask for state of game from game master
    public RISKBoard get_game_state(ACLMessage decision) {
        try {
            //Reply to message
            ACLMessage reply = decision.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(RISKGameMasterAgent.STATE);
            send(reply);

            //Get game master response
            ACLMessage response = blockingReceive();

            //Get board as content
            RISKBoard content = (RISKBoard) response.getContentObject();

            //Return current state
            return content;
        } catch (Exception e) {
            System.out.println("Error in Agent getting the Game State: " + e);
        }

        //In case there was an error in getting game state, just use memory of it
        return null;
    }
}

