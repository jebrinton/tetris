package src.labs.rl.maze.agents;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.labs.rl.maze.agents.StochasticAgent;
import edu.bu.labs.rl.maze.agents.StochasticAgent.RewardFunction;
import edu.bu.labs.rl.maze.agents.StochasticAgent.TransitionModel;
import edu.bu.labs.rl.maze.utilities.Coordinate;
import edu.bu.labs.rl.maze.utilities.Pair;



public class ValueIterationAgent
    extends StochasticAgent
{

    public static final double GAMMA = 1; // feel free to change this around!
    public static final double EPSILON = 1e-6; // don't change this though

    private Map<Coordinate, Double> utilities;

	public ValueIterationAgent(int playerNum)
	{
		super(playerNum);
        this.utilities = null;
	}

    public Map<Coordinate, Double> getUtilities() { return this.utilities; }
    private void setUtilities(Map<Coordinate, Double> u) { this.utilities = u; }

    public boolean isTerminalState(Coordinate c)
    {
        return c.equals(StochasticAgent.POSITIVE_TERMINAL_STATE)
            || c.equals(StochasticAgent.NEGATIVE_TERMINAL_STATE);
    }

    /**
     * A method to get an initial utility map where every coordinate is mapped to the utility 0.0
     */
    public Map<Coordinate, Double> getZeroMap(StateView state)
    {
        Map<Coordinate, Double> m = new HashMap<Coordinate, Double>();
        for(int x = 0; x < state.getXExtent(); ++x)
        {
            for(int y = 0; y < state.getYExtent(); ++y)
            {
                if(!state.isResourceAt(x, y))
                {
                    // we can go here
                    m.put(new Coordinate(x, y), 0.0);
                }
            }
        }
        return m;
    }

    public void valueIteration(StateView state)
    {
        // TODO: complete me!

        this.utilities = getZeroMap(state);

        // get reward for coordinate

        // maximum change in the utility of any state
        Double delta = null;

        Direction[] directions = new Direction[]{
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST
        };

        Map<Coordinate, Double> utils = getZeroMap(state);
        Map<Coordinate, Double> utilsPrime = getZeroMap(state);

        // loop until epsilon(1-gamma)/gamma is less than delta
        while (delta == null || !(delta <= EPSILON * (1 - GAMMA) / GAMMA)) {
            delta = 0.0;
            // make a deep copy of the utilities
            utils = new HashMap<>(utilsPrime);

            // iterate through each coordinate; i is the row, j is the col
            for(Coordinate s : utils.keySet())
            {
                double newUtility = RewardFunction.getReward(s);

                if(!this.isTerminalState(s))
                {
                    // calculate the max action
                    double maxAction = Double.NEGATIVE_INFINITY;

                    // we want to loop through a and then loop through possible s'
                    for (Direction direction : directions) {
                        // sub sum
                        double actionSum = 0.0;

                        Set<Pair<Coordinate, Double> > transitionsAndProbs = TransitionModel.getTransitionProbs(state, s, direction);
                        // now we have a set of all possible moves
                        for (Pair<Coordinate, Double> pair : transitionsAndProbs) {
                            // add to sum the transition probability multiplied by U[s']
                            actionSum += pair.getSecond() * utils.get(pair.getFirst());
                        }

                        // set maxAction to the max of itself and the current action's sum
                        maxAction = Math.max(actionSum, maxAction);
                    }
                    newUtility += GAMMA * maxAction;
                }

                // Bellman equation
                utilsPrime.put(s, newUtility);

                // update the value of delta to the largest change in utility
                if (Math.abs(utilsPrime.get(s) - utils.get(s)) > delta) {
                    delta = Math.abs(utilsPrime.get(s) - utils.get(s));
                }
            }

            System.out.println(utilsPrime);
            System.out.println(delta);
            System.out.println();
            System.out.println();
        }

        System.out.println(utils);
        this.setUtilities(utils);
    }

    /*
     * Returns deep copy of utilities
     */
    public static Map<Coordinate, Double> deepCopyMap(Map<Coordinate, Double> original) {
        if (original == null) {
            return null;
        }

        Map<Coordinate, Double> copy = new HashMap<>();
        
        for (Map.Entry<Coordinate, Double> entry : original.entrySet()) {
            Coordinate originalKey = entry.getKey();
            Double value = entry.getValue();
    
            Coordinate copiedKey = originalKey.clone();
    
            copy.put(copiedKey, value);
        }
        return copy;
    }

    @Override
    public void computePolicy(StateView state,
                              HistoryView history)
    {
        // compute the utilities
        this.valueIteration(state);

        // compute the policy from the utilities
        Map<Coordinate, Direction> policy = new HashMap<Coordinate, Direction>();

        for(Coordinate c : this.getUtilities().keySet())
        {
            // figure out what to do when in this state
            double maxActionUtility = Double.NEGATIVE_INFINITY;
            Direction bestDirection = null;

            // go over every action
            for(Direction d : TransitionModel.CARDINAL_DIRECTIONS)
            {

                // measure how good this action is as a weighted combination of future state's utilities
                double thisActionUtility = 0.0;
                for(Pair<Coordinate, Double> transition : TransitionModel.getTransitionProbs(state, c, d))
                {
                    thisActionUtility += transition.getSecond() * this.getUtilities().get(transition.getFirst());
                }

                // keep the best one!
                if(thisActionUtility > maxActionUtility)
                {
                    maxActionUtility = thisActionUtility;
                    bestDirection = d;
                }
            }

            // policy recommends the best action for every state
            policy.put(c, bestDirection);
        }

        this.setPolicy(policy);
    }

}
