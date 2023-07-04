package risk;

import java.io.Serializable;
import java.util.HashMap;

//Create a class for the countries in RISK
public class RISKCountry implements Serializable {
	//Instance variables
	private String continent; //continent that the country is set in
	private String name; //name of the country
	private String owner; //owner of the country
	private int[] connections; //connections of the country
	private int nr_troops; //number of troops in the country

	//Initialize the Country (name, continent and connections. Owner starts null)
	public RISKCountry(String name, String continent, int[] connections) {
		this.name = name;
		this.continent = continent;
		this.connections = connections;
		this.nr_troops = 0;
		this.owner = null;
	}

	//Getters of country

	//Get the name of the country
	public String get_name() {
		return this.name;
	}

	//Get the continent of the country
	public String get_continent() {
		return this.continent;
	}

	//Get the connections of the country
	public int[] get_connections() {
		return this.connections;
	}

	//Get the owner of the country
	public String get_owner() {
		return this.owner;
	}

	//Setters of country

	//Set the owner of the country
	public void set_owner(String owner) {
		this.owner = owner;
	}

	//Add troops to the country
	public void add_troops(int number) {
		this.nr_troops += number;
	}

	//Remove troops from the country
	public boolean remove_troops(int number) {
		//Check if the country stays with atleast one troop
		if (this.nr_troops - number >= 0) {
			this.nr_troops -= number;
			return true;
		}

		//Not enough troops, inform game master of that happening
		return false;
	}

	//Get number of troops in country
	public int get_troops() {
		return this.nr_troops;
	}
}
