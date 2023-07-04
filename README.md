## Project Details
This project was created for the "Sistemas Multi-Agente" class, from FCUL, using Java and the JADE tool.

Made by: Diogo Araújo - 60997

## Project Summary
For this project, we implemented the board game RISK based on an agent architecture, through the JADE platform, which uses agents (that follow a set of rules) to play the game. These agents all follow a different method of "thought", some playing more defensively while others play more aggressively. They are then pit against each other, with the last one remaining winning.

To represent the agents, as well as the board, we created several different classes which serve as objects that function as those pieces of the game. We have the:

- GameMasterAgent: Runs the game and informs agents of its state.

- RISKPlayerAgent: Base class which defines each Agent and the actions they can conduct. Different behaviours are born from this class.

- RISKBoard: Represents the board and its current state.

- RISKCountry: Represents each territory of the board, which has an occupying player and an amount of troops.

- RISKMapGraph: Graph structure which connects all the countries and allows for their organization. Also defines which countries can be moved between.

The game goes through four separate phases sequentially, where each players plays each in their order, with two additional phases in the beginning of the game:

- Waiting Phase: GameMaster waits for all agents to enter the game.

![image](https://github.com/DiogoAraujoHUB/AgentsPlayRISK/assets/61624282/908eab74-f079-48d6-9c38-476788237c5e)

- Placement Phase: Place units in territories that they do not own or that they have occupied. This is the first phase of the game and it only happens once, at the beginning of the game. Each agent is given a certain amount of troops, which are divided between all of them.

![image](https://github.com/DiogoAraujoHUB/AgentsPlayRISK/assets/61624282/fc982bf5-3c59-4b2f-a337-8ecbc3dac547)

- Growth Phase: Agents receive units and place them in their territories. (Agents are prone to place them in border countries)

![image](https://github.com/DiogoAraujoHUB/AgentsPlayRISK/assets/61624282/279982d3-77e1-405f-974f-0d0803a8f71f)

- Attack Phase: Agents attack enemy territories with units from their countries. (Agents are prone to attack when they have a majority of troops)

![image](https://github.com/DiogoAraujoHUB/AgentsPlayRISK/assets/61624282/a4aed901-9200-433d-b265-51891bb19a61)

- Movement Phase: Agents can move their units between their own territories, as long as they do not leave a country empty of troops. (Agents are enticed to move troops to border countries)

![image](https://github.com/DiogoAraujoHUB/AgentsPlayRISK/assets/61624282/719bb0bd-ed25-4773-98ab-82a3c2ca6fef)

- Negotiation Phase: Agents can attempt to create alliances with other agents. When these are made, they are temporary and only last a few turns.

![image](https://github.com/DiogoAraujoHUB/AgentsPlayRISK/assets/61624282/042dfe45-19ad-459a-8cad-7e2afa4325b9)

