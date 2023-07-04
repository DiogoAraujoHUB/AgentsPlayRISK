package risk;

import java.io.Serializable;
import java.util.*;

public class RISKBoard implements Serializable {
	//Instance variables
	private Map<Integer, RISKCountry> id_to_country; //Compare country ID to country (name, continent, connections)
	private Map<RISKCountry, Integer> country_to_id; //Opposite of previous, compares country to country id
	private Map<String, Integer> continent_bonuses; //Create the continent bonuses map
	private ArrayList<String[]> alliances; //Define the alliances between players
	private HashMap<String[], Integer> alliances_counter; //Define the countdown for the alliances

	//Initialize the RISKBoard class
	public RISKBoard() {
		//Initialize instance variables
		this.id_to_country = new HashMap<>();
		this.country_to_id = new HashMap<>();
		this.continent_bonuses = new HashMap<>();
		this.alliances = new ArrayList<>();
		this.alliances_counter = new HashMap<>();

		//Add to the continent bonuses map
		this.continent_bonuses.put("NA", 5);
		this.continent_bonuses.put("EU", 5);
		this.continent_bonuses.put("SA", 2);
		this.continent_bonuses.put("AF", 3);
		this.continent_bonuses.put("AS", 7);
		this.continent_bonuses.put("AU", 2);
		
		//Create countries in the North America (NA) continent
		this.id_to_country.put(1, new RISKCountry("Alaska", "NA", new int[] {2, 6, 31}));
		this.id_to_country.put(2, new RISKCountry("Alberta", "NA", new int[] {1, 6, 7, 9}));
		this.id_to_country.put(3, new RISKCountry("Central America", "NA", new int[] {4, 9, 19}));
		this.id_to_country.put(4, new RISKCountry("Eastern US", "NA", new int[] {3, 9, 7, 8}));
		this.id_to_country.put(5, new RISKCountry("Greenland", "NA", new int[] {6, 7, 8, 11}));
		this.id_to_country.put(6, new RISKCountry("Northwest Territory", "NA", new int[] {1, 2, 5, 7}));
		this.id_to_country.put(7, new RISKCountry("Ontario", "NA", new int[] {2, 4, 5, 6, 8, 9}));
		this.id_to_country.put(8, new RISKCountry("Quebec", "NA", new int[] {4, 5, 7}));
		this.id_to_country.put(9, new RISKCountry("Western US", "NA", new int[] {2, 3, 4, 7}));

		//Create countries in the Europe (EU) continent
		this.id_to_country.put(10, new RISKCountry("Great Britain", "EU", new int[] {11, 12, 13, 0}));
		this.id_to_country.put(11, new RISKCountry("Iceland", "EU", new int[] {5, 10, 13}));
		this.id_to_country.put(12, new RISKCountry("Northern Europe", "EU", new int[] {10, 13, 14, 15, 0}));
		this.id_to_country.put(13, new RISKCountry("Scandinavia", "EU", new int[] {10, 11, 12, 15}));
		this.id_to_country.put(14, new RISKCountry("Southern Europe", "EU", new int[] {12, 15, 0, 22,24,32}));
		this.id_to_country.put(15, new RISKCountry("Ukraine", "EU", new int[] {12, 13, 14, 26, 32, 36}));
		this.id_to_country.put(0, new RISKCountry("Western Europe", "EU", new int[] {10, 12, 14,24}));
		
		//Create countries in the South America (SA) continent
		this.id_to_country.put(16, new RISKCountry("Argentina", "SA", new int[] {17, 18}));
		this.id_to_country.put(17,new RISKCountry("Brazil", "SA", new int[] {16, 18, 19,24}));
		this.id_to_country.put(18,new RISKCountry("Peru", "SA", new int[] {16, 17, 19}));
		this.id_to_country.put(19,new RISKCountry("Venezuela", "SA", new int[] {3, 17, 18}));
		
		//Create countries in the African (AF) continent
		this.id_to_country.put(20, new RISKCountry("Congo", "AF", new int[] {21, 24, 25}));
		this.id_to_country.put(21, new RISKCountry("East Africa", "AF", new int[] {20, 22, 23, 24, 25}));
		this.id_to_country.put(22, new RISKCountry("Egypt", "AF", new int[] {14,21, 24,32}));
		this.id_to_country.put(23, new RISKCountry("Madagascar", "AF", new int[] {21, 25}));
		this.id_to_country.put(24, new RISKCountry("North Africa", "AF", new int[] {14,0,17,20,21,22}));
		this.id_to_country.put(25, new RISKCountry("South Africa", "AF", new int[] {20, 21, 23}));
		
		//Create countries in the Asian (AS) continent
		this.id_to_country.put(26, new RISKCountry("Afghanistan", "AS", new int[] {15, 27, 28, 32, 36}));
		this.id_to_country.put(27, new RISKCountry("China", "AS", new int[] {26, 28, 33, 34, 35, 36}));
		this.id_to_country.put(28, new RISKCountry("India", "AS", new int[] {26, 27, 32, 34}));
		this.id_to_country.put(29, new RISKCountry("Irkutsk", "AS", new int[] {31, 33, 35, 37}));
		this.id_to_country.put(30, new RISKCountry("Japan", "AS", new int[] {31, 33}));
		this.id_to_country.put(31, new RISKCountry("Kamchatka", "AS", new int[] {1, 29, 30, 33, 37}));
		this.id_to_country.put(32, new RISKCountry("Middle East", "AS", new int[] {14,15,22,26, 28}));
		this.id_to_country.put(33, new RISKCountry("Mongolia", "AS", new int[] {27, 29, 30, 31, 35}));
		this.id_to_country.put(34, new RISKCountry("Siam", "AS", new int[] {27, 28, 39}));
		this.id_to_country.put(35, new RISKCountry("Siberia", "AS", new int[] {27, 29, 33, 36, 37}));
		this.id_to_country.put(36, new RISKCountry("Ural", "AS", new int[] {15, 26, 27, 35}));
		this.id_to_country.put(37, new RISKCountry("Yakutsk", "AS", new int[] {29, 31, 35}));
		
		//Create countries in the Australia (AU) continent
		this.id_to_country.put(38, new RISKCountry("Eastern Australia", "AU", new int[] {40, 41}));
		this.id_to_country.put(39, new RISKCountry("Indonesia", "AU", new int[] {34, 40, 41}));
		this.id_to_country.put(40, new RISKCountry("New Guinea", "AU", new int[] {38, 39, 41}));
		this.id_to_country.put(41, new RISKCountry("Western Australia", "AU", new int[] {38,39,40}));
		
		//Create a reverse list
		for (int i : this.id_to_country.keySet()) {
			this.country_to_id.put(this.id_to_country.get(i), i);
		}
	}

	//Country getters

	//Get the owner of a country using the countries id
	public String get_country_owner(int country_id) {
		return this.id_to_country.get(country_id).get_owner();
	}

	//Return all the countries in the board in a list
	public List<Integer> get_countries() {
		//Create list to save countries
		ArrayList<Integer> countries = new ArrayList<>(country_to_id.values());

		//Return countries
		return countries;
	}

	//Return all the countries in the board that still arent owned in a list
	public List<Integer> get_unowned_countries() {
		//Create list to save countries
		ArrayList<Integer> countries = new ArrayList<>(country_to_id.values());
		ArrayList<Integer> unowned = new ArrayList<>();

		//Go through list and check which ones dont have an owner
		for (int country : countries) {
			if (get_country_owner(country) == null) {
				unowned.add(country);
			}
		}

		//Return countries that dont have owners
		return unowned;
	}

	//Get the name of a country using the countries id
	public String get_country_name(int country_id) {
		return this.id_to_country.get(country_id).get_name();
	}

	//Get the amount of troops of a country using the countries id
	public int get_country_troops(int country_id) {
		return this.id_to_country.get(country_id).get_troops();
	}

	//Get the connections of a country using the countries id
	public int[] get_country_connections(int country_id) {
		return this.id_to_country.get(country_id).get_connections();
	}

	//Print all the connections a country has using the countries id
	public void print_all_connections(int country_id) {
		//Get the country, using the id
		RISKCountry country = this.id_to_country.get(country_id);

		//Print the connections of the country
		System.out.println(" | "+country_id+" - "+country.get_name()+" | ");
		for (int id : country.get_connections()) {
			System.out.println(this.id_to_country.get(id).get_name());
		}
	}
	
	//Country modifiers

	//Get all countries that the player owns
	public List<Integer> get_player_countries(String player_id) {
		//List to save all player owned countries
		List<Integer> countries = new ArrayList<>();

		//Go through list of countries
		for (RISKCountry country : country_to_id.keySet()) {
			if (country.get_owner() != null && country.get_owner().equals(player_id)) {
				countries.add(country_to_id.get(country));
			}
		}

		return countries;
	}

	//Add an amount of troops to a country using the countries id
	public void add_troops_to_country(int nr_troops, int country_id) {
		this.id_to_country.get(country_id).add_troops(nr_troops);
	}

	//Remove an amount of troops from a country using its id
	public boolean remove_troops_from_country(int nr_troops, int country_id) {
		return this.id_to_country.get(country_id).remove_troops(nr_troops);
	}

	//Set the owner of a country using the countries id
	public void set_country_owner(String owner_id, int country_id) {
		this.id_to_country.get(country_id).set_owner(owner_id);
	}


	//Move a certain amount of troops between countries using their id
	public void move_troops(int origin_country_id, int destination_country_id, int troops) {
		//Get both the countries using their id
		RISKCountry origin = this.id_to_country.get(origin_country_id);
		RISKCountry destination = this.id_to_country.get(destination_country_id);

		//Remove troops from origin country and add troops to destination country
		origin.add_troops(-troops);
		destination.add_troops(troops);
	}
	
	// ATTACK RULES
	// Attacker must have between 1 and 5 units
	// Defender can defend himself with 1 or 2 units
	// Attacker rolls dice based on the amount of attackers - 1 (amount of dice)
	// Defender rolls dice based on the amount_defenders (amount of dice)
	// The values of the Dice can only go between 1 to 6
	public int[] attack(int amount_attackers, int amount_defenders) {
		//Create the dice as well as their results
		Random rand = new Random();
		ArrayList<Integer> attacker_dice_results = new ArrayList<Integer>();
		ArrayList<Integer> defender_dice_results = new ArrayList<Integer>();
		
		//Roll the dice of the attackers
		for (int i = 0; i < amount_attackers; i++) {
			attacker_dice_results.add(rand.nextInt(6) + 1);
		}
		
		//Roll the dice of the defender
		for (int i = 0; i < amount_defenders; i++) {
			defender_dice_results.add(rand.nextInt(6) + 1);
		}

		//Sort the results (we do this as we only compare the highest values)
		int[] results = new int[2]; 
		Collections.sort(attacker_dice_results, Collections.reverseOrder());  
		Collections.sort(defender_dice_results, Collections.reverseOrder());

		//Verify the results
		//System.out.println("Attacker Results (Dice): " + attacker_dice_results);
		//System.out.println("Defender Results (Dice): " + defender_dice_results);

		//Go through loop while there are certain attackers that havent fought
		while (!attacker_dice_results.isEmpty()) {
			//Get and remove the result of the attacker that is fighting
			int attacker_dice = attacker_dice_results.get(0);
			attacker_dice_results.remove(0);

			//If there are defenders left, they fight (check if defenders list is empty)
			if (!defender_dice_results.isEmpty()) {
				//Get and remove the result of the defender that is fighting
				int defender_dice = defender_dice_results.get(0);
				defender_dice_results.remove(0);

				//Check who wins the fight
				if (attacker_dice > defender_dice) { //Attacker (above)
					results[1]--;
				} else { //Defender (equal or below)
					results[0]--;
				}
			}
		}

		//Return the results (who won and who lost)
		return results;
	}
	
	//Continent Functions

	//Get the owners of the continents (if they have all the countries in the continent)
	public HashMap<String, String> get_continent_owners() {
		//Create an array for the continents and a map for their owners
		String[] continents = new String[] {"NA", "EU", "SA", "AF", "AS", "AU"};
		HashMap<String, String> continent_owners = new HashMap<String, String>();

		//Go through array of continents
		for (String continent : continents) {
			//Get the of the first country of that continent
			String owner = this.get_countries_in_continent(continent).get(0).get_owner();

			//Check if owner of that country is null (if it is then pass onto next continent)
			if (owner == null) {
				continent_owners.put(continent, owner);
				continue;
			}

			//Go through countries of continent
			for (RISKCountry country : this.get_countries_in_continent(continent)) {
				//If the owner of that country is null or if the country has a different owner
				if (country.get_owner() == null || !country.get_owner().equals(owner)) {
					owner = null;
					break;
				}
			}

			//Insert owner of continent into map associated to continent
			continent_owners.put(continent, owner);
		}

		//Return map with continents and their owners
		return continent_owners;
	}

	//Get all the countries in a continent
	public List<RISKCountry> get_countries_in_continent(String continent) {
		//Create a list where we will keep the countries of that continent
		List<RISKCountry> found_countries = new ArrayList<>();

		//Go through all countries
		for (RISKCountry country : this.country_to_id.keySet()) {
			//If country is associated to continent then add it to list
			if (continent.equals(country.get_continent())) {
				found_countries.add(country);
			}
		}

		//Return list with all countries of continent
		return found_countries;
	}


	//NEW: Changed the connection function and associated validation functions
	//Check if two countries are connected
	public boolean countries_connected(int first_id, int second_id) {
		//Get first country and ID of second country
		RISKCountry first_country  = this.id_to_country.get(first_id);

		//Go through country connections
		for (int connection_id : first_country.get_connections()) {
			//Check if one of the connections is country we are comparing to
			if (connection_id == second_id) {
				return true;
			}
		}

		//Country was not one of the connection
		return false;
	}

	//Get the bonus of that continent from the array
	public int get_continent_bonus(String continent_name) {
		return this.continent_bonuses.get(continent_name);
	}

	//Get the total troop growth of a player (based on number of countries controlled divided by 3 and rounded down)
	public int get_troop_growth(String player_id) {
		//Get all owners of continents (give bonuses)
		HashMap<String, String> continent_owners = get_continent_owners();

		//Go through list of continent owners
		int bonus = 0;
		for (String continent : continent_owners.keySet()) {
			//Get owner of continent
			String owner = continent_owners.get(continent);

			//Check if owner doesnt exist (no bonus)
			if (owner == null) {
				continue;
			}

			//Compare owner with current player id (if so get bonus of continet)
			if (owner.equals(player_id)) {
				bonus = bonus + get_continent_bonus(continent);
			}
		}

		//Get how many countries the player has
		int num_countries = get_player_countries(player_id).size();

		//Calculate growth using number of countries and continent bonus
		int growth = (num_countries / 3) + bonus;

		//Return growth
		return growth;
	}

	//Get alliance counters (countdown for each alliance -> starts at 3)
	public HashMap<String[], Integer> get_alliances_counter() {
		return alliances_counter;
	}

	//Replace the counter value with the new value
	public void replace_alliances_counter(String[] pair, int counter) {
		//Remove the initial value from the map
		alliances_counter.remove(pair);

		//Add new value into map
		alliances_counter.put(pair, counter);
	}

	//Check if an alliance exists between a pair of players
	public boolean alliance_exists(String p1, String p2) {
		//Create a pair for the players
		String[] pair = new String[] {p1, p2};
		Arrays.sort(pair);

		//Check if they are in an alliance
		return this.alliances.contains(pair);
	}

	//Add an alliance (pair of players) to the alliance list
	public void add_alliance(String p1, String p2) {
		//Create a pair for the players
		String[] pair = new String[] {p1, p2};
		Arrays.sort(pair);

		//Add the pair to the alliance list
		this.alliances.add(pair);
		this.alliances_counter.put(pair, 3);
	}

	//Break an alliance, therefore removing the pair from the list
	public void break_alliance(String p1, String p2) {
		//Create a pair for the players
		String[] pair = new String[] {p1, p2};
		Arrays.sort(pair);

		//Remove the pair from the alliance list
		this.alliances.remove(pair);
		this.alliances_counter.remove(pair);

		//Verify alliance removal
		System.out.println("Alliamce between " + p1 + " and " + p2 + " has ended!");
	}
}
