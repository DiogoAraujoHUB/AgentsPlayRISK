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
