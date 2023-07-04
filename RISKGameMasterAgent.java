package risk;

import jade.core.AID;
import jade.core.Agent;
import jade.core.MessageQueue;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import javax.swing.JOptionPane;

// Jogo RISK
// Inicializar o Game Master (recebe o número mínimo e máximo de jogadores para um jogo)
// Game Master espera por jogadores
// Inicializar os jogadores (cada um recebe o ID do Game Master)
// Jogadores perguntam ao Game Master se o jogo tem um slot disponível
// Game Master inicia o jogo
// Game Master dá o turno a cada um dos jogadores
//    || Turno do jogador ||
//    0 - Game Master indica número de exércitos recebidos
//    1 - O jogador recebe um número de exércitos e dispõe-os no tabuleiro
//    2 - Game Master recebe essa info
// 	  3 - O jogador escolhe territórios adjacentes aos seus para ataca
//    4 - Game Master recebe essa info e notifica os dois envolvidos do resultado
//    5 - O jogador movimenta exércitos para territórios adjacentes
// 	  6 - Game Master recebe essa info   
// 	  7 - O jogador negocia alianças e pactos de não agressão
// Sempre que pedido, o Game Master informa o estado do jogo

// Inicializar o Game Master (recebe o número mínimo e máximo de jogadores para um jogo)
// Game Master espera por jogadores
// Inicializar os jogadores (cada um recebe o ID do Game Master)
// Jogadores tentam se inscrever no jogo
// Game Master responde com OK/FAIL
// Game Master inicia o jogo quando estiver cheio

// A QUALQUER MOMENTO (parallel)
// 		+ Jogadores podem pedir estado do tabuleiro
// 		+ Jogadores negociam alianças e pactos de não agressão

// INÍCIO
// Game Master dá shuffle na lista de jogadores e segue essa ordem de turnos
// // PLACEMENT PHASE (tem que ser síncrona)
//	    	- Game Master indica a cada jogador número de exércitos para colocar
//			- ** Enquanto houverem países livres AND exercitos por colocar **
// 				- ** Para cada jogador **
//					- Se houverem países livres AND o jogador tem exercitos por colocar
// 						- Game Master espera pela resposta do jogador com PLACE(country_id, 1)
//						- Ao receber uma resposta, comunica a todos o efeito

// RESTO DO JOGO
// - DINÂMICA DE TURNO
//	    // GROWTH PHASE (tem que ser síncrona)
// 			- Game Master comunica o início da GROWTH PHASE
//			- ** Para cada jogador **
//				- ** Enquanto jogador tiver exércitos por colocar**
//					- Game Master pede ao jogador que coloque exércitos
//					- Jogador responde com PLACE(country_id, amount)
//					- Game Master comunica a todos a alteração
//
//	    // ATTACK PHASE (tem que ser síncrona)
//			- Game Master comunica o início da ATTACK PHASE
//			- ** Para cada jogador **
//	    		- ** Enquanto jogador não responder com pass **
// 					- Game Master pergunta ao jogador o que quer fazer (ATTACK(from, to, número de tropas), PASS)
//					- Se for ATTACK:
//						- Se é possível:
//							- Game Master pergunta ao defensor quantas tropas quer usar para defender
//							- Quando defensor responder, simula o ataque
//							- Se houver conquista, o Game Master altera o owner do território
//							  e move as unidades sobreviventes para o território conquistado
//							- Game Master comunica os resultados do ataque a todos os jogadores
//

//Message formats for Player are:
//HELLO -> Just hello, only an introduction so that the game master can add to list
//STATE -> Asks for current game state (board)
//PLACE:3:2 -> Place in the country with an id of 3 2 troops
//ATTACK:2:3:1 -> Attack from country 2 to country 3 with 1 troop
//DEFEND:1:2 -> Defend country 1 with 2 troops
//SETUP:3 -> Adds a single troop to country 3 (Setup phase)
//MOVE:2:3:1 -> Move from country 2 to 3 1 troop
//ALLIANCE:ACCEPT:Player1 -> Sends an alliance request to Player 1
//FORM:ACCEPT -> Accepts the alliance request that was received

//Message formats for Game Master are:
//DECISION:PLACE -> Gives a decision to the agent of attacking (agent will receive this message and act upon it)
//DECISION:ATTACK -> Gives a decision to the agent of attacking
//DECISION:DEFEND:2:1 -> Gives a decision to the agent of defending country 2 which is being attacked by 1 troop
//DECISION:SETUP -> Gives a decision to the agent of setting up
//DECISION:MOVE -> Gives a decision to the agent of moving his troops
//DECISION:ALLIANCE -> Gives a decision to the agent of creating an alliance
//DECISION:FORM:Player2 -> Send a decision to an agent which Player2 has requested an alliance with

@SuppressWarnings("serial")
public class RISKGameMasterAgent extends Agent {
	//Setup basic message formats
	public final static String HELLO = "HELLO";
	public final static String STATE = "STATE";
	public final static String DECISION = "DECISION";

	//Setup game message formats
	public final static String PASS = "PASS";
	public final static String SETUP = "SETUP";
	public final static String PLACE = "PLACE";
	public static String ATTACK = "ATTACK";
	public final static String DEFEND = "DEFEND";
	public final static String MOVE = "MOVE";
	public final static String ALLIANCE = "ALLIANCE";
	public final static String FORM = "FORM";
	public final static String ACCEPT = "ACCEPT";
	public final static String DECLINE = "DECLINE";

	//Instance variables
	private int turnCounter = 1;
	private final int maxAmountOfPlayers = 4;
	private final static ArrayList<String> current_players = new ArrayList<>();
	private final RISKBoard board = new RISKBoard();

	//Sets up agent by registering it with
	@Override
	protected void setup() {
		System.out.println("Initiating Game Master");

		//Set up the Game Master description
		ServiceDescription sd = new ServiceDescription();
		sd.setType("GameMaster");
		sd.setName(getLocalName());

		//Create agent description of itself
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());

		//Add agent to DF
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}


		/*
		//Create cyclic behaviour (handles player queries)
		CyclicBehaviour handle_queries = new CyclicBehaviour(this) {
			@Override
			public void action() {
				//Receive message from agent
				ACLMessage msg = receive();

				//Check message contents
				if (msg != null) {
					//Player asks for State, respond with state
					String[] content = msg.getContent().split(":");
					if (STATE.equals(content[0])) {
						returnGameState();
					}
				} else {
					//Block behaviour if no message has arrived
					block();
				}
			}
		};
		 */

		//Create FSM (Match Behaviour)
		FSMBehaviour matchHandling = new FSMBehaviour(this);

		//First state of FSM
		//This is the waiting phase, where we get each player that is going to play
		matchHandling.registerFirstState(new WaitingPhase() {}, "waiting_phase");

		//This is the setup phase, where we set up the game according to each player that will be playing
		matchHandling.registerState(new SetupPhase() {}, "setup_phase");

		//Create main game (loop which will contain all phases)
		matchHandling.registerState(new MainGame() {}, "main_game");

		//Register transitions between states
		matchHandling.registerTransition("waiting_phase", "setup_phase", 0);
		matchHandling.registerTransition("waiting_phase", "waiting_phase", 1);
		matchHandling.registerTransition("setup_phase", "main_game", 0);
		matchHandling.registerTransition("main_game", "main_game", 0);
		matchHandling.registerTransition("main_game", "waiting_phase", 1);

		//Create parallel behaviour and add GameMaster behaviours to it
		ParallelBehaviour pb = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);
		//pb.addSubBehaviour(handle_queries);
		pb.addSubBehaviour(matchHandling);

		//Add parallel behaviour
		addBehaviour(pb);
		System.out.println("Finishing Game Master.");
	}

	//Get players of RISK that are still playing
	public static List<String> get_current_players() {
		return current_players;
	}

	//Send current game state to player that asked for it
	private void returnGameState(ACLMessage response) {
		try {
			//Send player message with game state
			ACLMessage reply = response.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			reply.setContentObject(board);
			send(reply);
		} catch (Exception e) {
			System.out.println("Error while sending Game State " + e);
		}
	}

	//Send player a message and get response and return it
	//Response is split in order to get additional parameters
	public String[] get_decision(String player_id, String action, int extra1, int extra2, String extra3) {
		//Check if action is DEFEND (needs other rules as defender must know what is happening)
		if (action.equals(DEFEND)) {
			//Send player message
			ACLMessage decision = new ACLMessage(ACLMessage.INFORM);
			decision.setContent(DECISION + ":" + action + ":" + extra1 + ":" + extra2); //Extras are country being attack and number of troops attacking
			decision.addReceiver(new AID(player_id, AID.ISLOCALNAME));
			send(decision);
		} else if (action.equals(FORM)) {
			//Send player message
			ACLMessage decision = new ACLMessage(ACLMessage.INFORM);
			decision.setContent(DECISION + ":" + action + ":" + extra3); //Extra is player requesting alliance
			decision.addReceiver(new AID(player_id, AID.ISLOCALNAME));
			send(decision);
		} else {
			//Send player initial message
			ACLMessage decision = new ACLMessage(ACLMessage.INFORM);
			decision.setContent(DECISION + ":" + action);
			decision.addReceiver(new AID(player_id, AID.ISLOCALNAME));
			send(decision);
		}

		//Wait for player response to complete action
		String[] content = null;
		while (true) {
			//Wait for player response
			ACLMessage response = blockingReceive();
			content = response.getContent().split(":");

			//Check if player asked for state
			if (content[0].equals(STATE)) {
				//Return game state to player
				returnGameState(response);
			} else if (content[0].equals(action) || content[0].equals(PASS)) {
				//Got information we needed, so exit function
				break;
			}
		}

		//return content of message
		return content;
	}

	//Check if all troops have been placed for all players
	public boolean all_troops_placed(HashMap<String, Integer> remaining_troops) {
		//Get total amount of placeable troops
		int sum = 0;
		for (int val : remaining_troops.values()) {
			sum += val;
		}

		//Return total amount of placeable troops
		return sum <= 0;
	}

	//Insert players into game and assign them IDs
	//Then shuffle those players on the list and use that as order of game
	private class WaitingPhase extends OneShotBehaviour {
		//Instance variables

		//Define the setup phase
		public void action() {
			//Enter waiting phase
			if (current_players.size() == 0) {
				System.out.println("Entering Waiting Phase.");
			}

			//Receive message from agent
			ACLMessage msg = blockingReceive();

			//Check message contents
			if (msg != null) {
				String content = msg.getContent();
				if (HELLO.equals(content)) { //Player asks for State, respond with state
					//Add player to list and return id to player
					add_player(msg.getSender().getLocalName());
					System.out.println("Current Players == " + current_players.size());
				}
			}
		}

		//Add player onto list
		public void add_player(String player_id) {
			current_players.add(player_id);
		}

		//End the phase and move onto next phase
		public int onEnd() {
			//Check if amount of players has been reached
			if (current_players.size() != maxAmountOfPlayers) {
				return 1;
			}

			//Limit of players has not been reached
			return 0;
		}
	}

	// SETUP PHASE (tem que ser síncrona)
	//	    	- Game Master indica a cada jogador número de exércitos para colocar
	//			- ** Enquanto houverem países livres AND exercitos por colocar **
	// 				- ** Para cada jogador **
	//					- Se houverem países livres AND o jogador tem exercitos por colocar
	// 						- Game Master espera pela resposta do jogador com PLACE(country_id, 1)
	//						- Ao receber uma resposta, comunica a todos o efeito
	private class SetupPhase extends OneShotBehaviour {
		//Instance variables

		@Override
		public void action() {
			//Declare the scanner used to wait for user response
			Scanner scanner = new Scanner(System.in);

			//Verify the start of the setup phase and wait for user input to begin
			System.out.println("Entering Setup Phase.");
			scanner.nextLine();

			//Shuffle player list (simulated initial dice roll)
			Collections.shuffle(current_players);

			//Insert into list all countries that dont have an owner
			List<Integer> remaining_countries = board.get_countries();

			//Check if all countries have been occupied
			while (remaining_countries.size() != 0) {
				//Go through players and place troops
				for (String player_id : current_players) {
					//Check if there are any more remaining countries
					if (remaining_countries.size() == 0) {
						break;
					}

					//Check if placement is valid (continue loop until it is)
					while (true) {
						//Get player decision
						String[] decision = get_decision(player_id, SETUP, 0, 0, "");

						//Get country parameters based on decision
						int country_id = Integer.parseInt(decision[1]);

						//Get position of country on list
						int pos = 0;
						for (pos = 0; pos < remaining_countries.size(); pos++) {
							if (country_id == remaining_countries.get(pos)) {
								break;
							}
						}

						//Check if placement is valid
						if (validate_setup(country_id)) {
							//Place the troops on the board
							remaining_countries.remove(pos); //Remove country from list
							board.set_country_owner(player_id, country_id); //Set owner
							board.add_troops_to_country(1, country_id); //Add troop to country

							//Verify placement of troops
							String country_name = board.get_country_name(country_id);
							System.out.println(player_id + " placed one troop in " + country_name);
							break;
						} else {
							//Placement was invalid
							//System.out.println("Invalid set up!");
						}
					}
				}
			}
		}

		//Check if setup of troops is valid (based on player decision)
		public boolean validate_setup(int country_id) {
			//Check if setup is ok (ownership)
			if (board.get_country_owner(country_id) == null) { //owner (cant place in already owned position)
				return true;
			}

			//Another player already owned the country
			return false;
		}

		//End the phase and move onto next phase
		public int onEnd() {
			return 0;
		}
	}

	private class MainGame extends OneShotBehaviour {
		//Instance variables

		// RESTO DO JOGO
		// - DINÂMICA DE TURNO
		//	    // GROWTH PHASE (tem que ser síncrona)
		// 			- Game Master comunica o início da GROWTH PHASE
		//			- ** Para cada jogador **
		//				- ** Enquanto jogador tiver exércitos por colocar**
		//					- Game Master pede ao jogador que coloque exércitos
		//					- Jogador responde com PLACE(country_id, amount)
		//					- Game Master comunica a todos a alteração
		//
		//	    // ATTACK PHASE (tem que ser síncrona)
		//			- Game Master comunica o início da ATTACK PHASE
		//			- ** Para cada jogador **
		//	    		- ** Enquanto jogador não responder com pass **
		// 					- Game Master pergunta ao jogador o que quer fazer (ATTACK(from, to, número de tropas), PASS)
		//					- Se for ATTACK:
		//						- Se é possível:
		//							- Game Master pergunta ao defensor quantas tropas quer usar para defender
		//							- Quando defensor responder, simula o ataque
		//							- Se houver conquista, o Game Master altera o owner do território
		//							  e move as unidades sobreviventes para o território conquistado
		//							- Game Master comunica os resultados do ataque a todos os jogadores
		//

		//NEW: Added the movement phase and shifted the growth phase to be before the attack phase
		//Run the game with loop (GROWTH -> ATTACK -> MOVEMENT -> NEGOTIATION)
		@Override
		public void action() {
			//Declare the scanner used to wait for user response
			Scanner scanner = new Scanner(System.in);

			//Verify the countries owned by the players
			for (String player_id : current_players) {
				System.out.println(player_id + " currently owns " + board.get_player_countries(player_id).size() + " countries.");
			}

			//Verify that we have entered the Main Game Loop
			System.out.println("Starting Turn " + turnCounter + ".");
			System.out.println("Entering Main Game Loop ---------------------------------------------");

			//wait for user response to start turn
			scanner.nextLine();

			//Go through all phases of the Main Game
			growth_phase();
			attack_phase();
			movement_phase();
			negotiation_phase();

			//Verify that we have left main game loop
			System.out.println("---------------------------------------------------------------------");

			//Increment the number of turns
			turnCounter++;
		}

		//On ending the Phase, either restart the loop as there are still multiple players or end the game (if 1 player is left)
		public int onEnd() {
			//Check if game has ended, if not then restart loop
			if (is_game_over()) {
				//End the game and return to waiting phase
				System.out.println("GAME OVER! Winner is: " + current_players.get(0));

				//Remove final player from list
				current_players.remove(0);

				//Reset game back into Waiting Phase
				return 1;
			}

			return 0;
		}

		//Start with the growth phase
		//In the growth phase, players add troops to countries (based on countries owned)
		public void growth_phase() {
			//Start the growth phase
			System.out.println("Growth Phase started!");

			//Get current amount of placeable troops for each player (based on countries owned)
			HashMap<String, Integer> remaining_troops = new HashMap<>();
			for (String player_id : current_players) {
				//Get amount of countries owned by player
				//System.out.println(player_id + " owns " + board.get_player_countries(player_id).size() + " countries.");

				//Insert troops player gets to place
				remaining_troops.put(player_id, board.get_troop_growth(player_id));
			}

			//Check if all the troops have been placed
			while (!all_troops_placed(remaining_troops)) {
				//Go through list of players and place troops
				for (String player_id : current_players) {
					//Check if player still has troops to place (pass if they don't)
					if (remaining_troops.get(player_id) == 0) {
						continue;
					}

					//Check if placement is valid (continue loop until it is)
					while (true) {
						//Get player decision
						String[] decision = get_decision(player_id, PLACE, 0, 0, "");

						//Get country parameters based on decision
						int country_id = Integer.parseInt(decision[1]);
						int num_troops = Integer.parseInt(decision[2]);

						//Check if placement is valid
						if (validate_placement(player_id, country_id, num_troops, remaining_troops)) {
							//Place the troops on the board
							remaining_troops.put(player_id, remaining_troops.get(player_id) - num_troops); //Remove troops from list
							board.add_troops_to_country(num_troops, country_id); //Add troops to the country

							//Verify placement
							String country_name = board.get_country_name(country_id); //Get name of country for placement
							System.out.println(player_id + " placed " + num_troops + " troops in " + country_name + ". (" + remaining_troops.get(player_id) + " remaining)");
							break;
						} else {
							//Placement was invalid
							//System.out.println("Invalid placement for troops.");
						}
					}
				}
			}
		}

		//Check if placement of troops is valid (based on player decision)
		public boolean validate_placement(String player_id, int country_id, int num_troops, HashMap<String, Integer> remaining_troops) {
			//Check if placement is ok (ownership)
			if (board.get_country_owner(country_id).equals(player_id)) { //owner
				if (num_troops <= remaining_troops.get(player_id)) { //quantity of troops
					return true;
				}
			}

			//Another player already owned the country
			return false;
		}

		//After the growth phase, passes to the attack phase (players attack each other or pass)
		public void attack_phase() {
			//Start the attack phase
			System.out.println("Attack Phase Started!");

			//Go through list of players, in order, for attack
			ArrayList<String> dead_players = new ArrayList<>();
			for (String player_id : current_players) {
				//Check if player has died (pass his turn since it removes him after)
				if (board.get_player_countries(player_id).size() == 0) {
					continue;
				}

				//Go through player's turn
				int attacks = 0; //Max amount of attacks (can only do 5 at max)
				while (true) {
					//Get player decision
					String[] decision = get_decision(player_id, ATTACK, 0, 0, "");

					//Check if player passed
					//Check if max amount of attacks has been reached
					if (decision[0].equals(PASS) || attacks >= 5) {
						System.out.println(player_id + " has finished their attack.");
						break;
					}

					//Check if player decides to attack
					if (decision[0].equals(ATTACK)) {
						//Get the attacking and defending country as well as number of attackers
						int attacker_country_id = Integer.parseInt(decision[1]);
						int defender_country_id = Integer.parseInt(decision[2]);
						int number_of_attackers = Integer.parseInt(decision[3]);

						//Validate the attack (if it is allowed)
						if (validate_attack(player_id, attacker_country_id, defender_country_id, number_of_attackers)) {
							//Get additional info about the attack
							String attacking_country_name = board.get_country_name(attacker_country_id);
							String defender_country_name = board.get_country_name(defender_country_id);
							String defending_country_owner = board.get_country_owner(defender_country_id);
							System.out.println(attacking_country_name + " (" + player_id + ") is attacking " + defender_country_name + "!");

							//Get number of defenders
							int number_of_defenders = get_defenders(attacker_country_id, defender_country_id);

							//Make the attack happen (attackers vs defenders)
							int[] results = board.attack(number_of_attackers, number_of_defenders);
							System.out.println((results[0] * -1) + " " + attacking_country_name + " (" + player_id + ") troops died attacking the enemy. (Initial Attackers == " + number_of_attackers + ")");
							System.out.println((results[1] * -1) + " " + defender_country_name + " (" + defending_country_owner + ") troops died defending their country. (Initial defenders == " + number_of_defenders + ")");

							//Remove lost troops from attacking and defending country
							board.remove_troops_from_country(results[0] * -1, attacker_country_id);
							board.remove_troops_from_country(results[1] * -1, defender_country_id);

							//If the country was conquered then there is a new owner (attack won)
							if (board.get_country_troops(defender_country_id) == 0) {
								board.set_country_owner(player_id, defender_country_id); //Set owner to attacker
								board.remove_troops_from_country(number_of_attackers + results[0], attacker_country_id); //Remove attackers from attacking country
								board.add_troops_to_country(number_of_attackers + results[0], defender_country_id); //Add attackers to defending country

								//Verify conquer of country
								int num_troops = number_of_attackers + results[0];
								System.out.println(player_id + " has conquered " + defender_country_name + "! (Ocuppied by " + num_troops + ")");

								//Check if defending player has any more countries
								if (board.get_player_countries(defending_country_owner).size() == 0) {
									//Add player to dead players list
									dead_players.add(defending_country_owner);

									//Verify removal from game
									System.out.println(defending_country_owner + " has been eliminated from the game!");
								}
							}

							//Increase amount of attacks done by player
							attacks++;
						} else {
							System.out.println("Attack is not valid, please try again.");
						}
					}
				}
			}

			//Go through list of dead players and remove them from game
			for (String dead_player : dead_players) {
				//Get position of player in game
				int pos = 0;
				for (String player : current_players) {
					//Check if player found is defending country owner
					if (player.equals(dead_player)) {
						break;
					}

					//Increment position in loop
					pos++;
				}

				//Remove player from current players list
				current_players.remove(pos);

				//Check if there is only one person left (game is over)
				if (current_players.size() == 1) {
					System.out.println("Reached end of game");
					onEnd();
				}
			}
		}

		//Validate the attack and make sure it can be done
		public boolean validate_attack(String player_id, int attacking_country_id, int defender_country_id, int number_of_troops) {
			//Create validations for countries (ownership, connection, quantity)
			boolean validate_countries = false;
			if (player_id.equals(board.get_country_owner(attacking_country_id))) { //initial country (ownership)
				if (!player_id.equals(board.get_country_owner(defender_country_id))) { //final country (ownership)
					if (board.countries_connected(attacking_country_id, defender_country_id)) { //Connection
						if (!alliance_exists(player_id, board.get_country_owner(defender_country_id))) { //Alliance
							validate_countries = true;
						}
					}
				}
			}

			//Validation of countries is true
			if (validate_countries) {
				//Validate troops
				if (number_of_troops >= 1) { //Troops being used
					if (board.get_country_troops(attacking_country_id) - number_of_troops >= 1) { //number of troops in country
						return true;
					}
				}
			}

			//Movement couldn't be done as it broke one of the rules
			return false;
		}

		//Get total amount of defenders in country
		public int get_defenders(int attacker_country_id, int defender_country_id) {
			//Get additional parameters for country
			String defender_country_owner_id = board.get_country_owner(defender_country_id);
			int attacker_country_troops = board.get_country_troops(attacker_country_id);

			//Check for defender choice (continue loop until he chooses)
			while (true) {
				//Get defender choice
				String[] decision = get_decision(defender_country_owner_id, DEFEND, defender_country_id, attacker_country_troops, "");

				//Get country and number of defenders
				int decision_country_id = Integer.parseInt(decision[1]);
				int decision_number = Integer.parseInt(decision[2]);

				//Check if defense is valid
				boolean defense = validate_defense(defender_country_owner_id, defender_country_id, decision_country_id, decision_number);
				if (defense) {
					//Return the amount of defenders it is validated
					return decision_number;
				}
			}
		}

		//Check if defense is valid
		public boolean validate_defense(String defender_id, int initial_country_id, int decision_country_id, int number_of_defenders) {
			//Create validations for countries (ownership, connection, quantity)
			boolean validate_countries = false;
			if (defender_id.equals(board.get_country_owner(decision_country_id))) { //owner of defense country
				if (decision_country_id == initial_country_id) { //same country
					validate_countries = true;
				}
			}

			//Validation of countries is true so we validate troops
			if (validate_countries) {
				//Validate troops
				if (number_of_defenders >= 1) { //Troops being used
					if (board.get_country_troops(decision_country_id) - number_of_defenders >= 0) { //number of troops in country
						return true;
					}
				}
			}

			//Defense wasn't valid so it is not allowed
			return false;
		}

		//After the attack phase there is the movement phase (players can move their units between their countries)
		public void movement_phase() {
			//Start the movement phase
			System.out.println("Movement Phase Started!");

			//Go through list of players, in order, for movement
			for (String player_id : current_players) {
				//Go through player turn
				int movements = 0; //Max amount of movements that can be done by player (max is 5)
				while (true) {
					//Get player decision
					String[] decision = get_decision(player_id, MOVE, 0, 0, "");

					//Check if player passed
					if (decision[0].equals(PASS) || movements >= 5) {
						System.out.println(player_id + " has finished their movement!");
						break;
					}

					//Check if player decides to move
					if (decision[0].equals(MOVE)) {
						System.out.println(player_id + " is moving their troops.");

						//Get the attacking and defending country as well as number of attackers
						int initial_country_id = Integer.parseInt(decision[1]);
						int final_country_id = Integer.parseInt(decision[2]);
						int num_troops = Integer.parseInt(decision[3]);

						//Validate the movement (check if it is allowed)
						if (validate_movement(player_id, initial_country_id, final_country_id, num_troops)) {
							//Get additional info about the movement
							String initial_country_name = board.get_country_name(initial_country_id);
							String final_country_name = board.get_country_name(final_country_id);

							//Verify the movement
							System.out.println(initial_country_name + " (" + player_id + ") is moving " + num_troops + " troops to " + final_country_name + ".");

							//Place troops in the final country
							board.remove_troops_from_country(num_troops, initial_country_id); //Remove
							board.add_troops_to_country(num_troops, final_country_id); //Place

							//Increase amount of movements done by player
							movements++;
						} else {
							System.out.println("Movement is not valid, please try again.");
						}
					}
				}
			}
		}

		//Validate the movement to make sure that it follows rules and can be done
		public boolean validate_movement (String player_id,int initial_country_id, int final_country_id, int number_of_troops){
			//Create validations for countries (ownership, connection, quantity)
			boolean validate_countries = false;
			if (player_id.equals(board.get_country_owner(initial_country_id))) { //initial country (owner)
				if (player_id.equals(board.get_country_owner(final_country_id))) { //final country (owner)
					if (board.countries_connected(initial_country_id, final_country_id)) { //connection
						validate_countries = true;
					}
				}
			}

			//Validation of countries is true
			if (validate_countries) {
				//Validate troops
				if (number_of_troops >= 1) { //Troops being used
					if (board.get_country_troops(initial_country_id) - number_of_troops >= 1) { //number of troops in country
						return true;
					}
				}
			}

			//Movement couldn't be done as it broke one of the rules
			return false;
		}

		//Negotiation phase where players can negotiate alliances between each other
		public void negotiation_phase() {
			//Start the negotiation phase
			System.out.println("Negotiation Phase has Started!");

			//Get current alliances duration
			HashMap<String[], Integer> alliances_counter = board.get_alliances_counter();

			//Go through counter and decrease all alliances countdown
			for (String[] pair : alliances_counter.keySet()) {
				//Get associated counter and decrease it
				int countdown = alliances_counter.get(pair) - 1;

				//Check if countdown has reached 0
				if (countdown == 0) {
					//Get players in alliance
					String p1 = pair[0];
					String p2 = pair[1];

					//Break alliance between players
					board.break_alliance(p1, p2);
				} else {
					//Replace countdown value in pair
					board.replace_alliances_counter(pair, countdown);
				}
			}

			//Go through list of players, in order, ask if they want to form an alliance with another player
			for (String current_player : current_players) {
				//Go through the player's turn
				while (true) {
					//Get player decision
					String[] decision = get_decision(current_player, ALLIANCE, 0, 0, "");

					//Check if player passed
					if (decision[0].equals(PASS)) {
						break;
					}

					//Check if player decides to make alliance
					if (decision[1].equals(ACCEPT)) {
						//Player has chosen a target to ally with
						String target_player = decision[2];

						//Validate the alliance (if it doesn't already exist)
						if (validate_alliance(current_player, target_player)) {
							//Verify the alliance creation
							System.out.println(current_player + " is trying to ally with " + target_player);

							//Get decision from target (ACCEPT or DECLINE)
							String[] response = get_decision(target_player, FORM, 0, 0, current_player);

							//Check if the alliance was accepted or not
							if (response[1].equals(ACCEPT)) {
								System.out.println("Players " + current_player + " and " + target_player + " have formed an Alliance.");
								add_alliance(current_player, target_player); //Create the alliance
							} else if (response[1].equals(DECLINE)) {
								System.out.println("Players " + current_player + " and " + target_player + " have NOT formed an Alliance.");
							}

							//Decision has been made, so break loop
							break;
						} else {
							//System.out.println("Alliance decision is not valid, please try again.");
						}
					} else if (decision[1].equals(DECLINE)) {
						System.out.println(current_player + " has refused to make an Alliance with anyone.");

						//Decision has been made, so break loop
						break;
					}
				}
			}
		}

		//Check if the alliance is valid
		public boolean validate_alliance(String p1, String p2) {
			//Check if two players are already in an alliance
			boolean check = alliance_exists(p1, p2);

			//Return the validation
			return check;
		}

		//Check if an alliance already exists
		public boolean alliance_exists(String p1, String p2) {
			//Check if the alliance exists for that pair
			return board.alliance_exists(p1, p2);
		}

		//Add an alliance to the list of alliances
		public void add_alliance(String p1, String p2) {
			//Add the pair to the alliance list
			board.add_alliance(p1, p2);
		}

		//Break the alliance between two players
		public void break_alliance(String p1, String p2) {
			//Break the alliance between the pair (remove from list)
			board.break_alliance(p1, p2);
		}

		//Check if game is over (only 1 person is remaining)
		//This means that everyone else lost, therefore this person won
		public boolean is_game_over () {
			return (current_players.size() == 1);
		}
	}
}
