// javac -cp "lib/Sepia.jar:lib/stealth.jar:." @stealth.srcs
// java -cp "lib/Sepia.jar:lib/stealth.jar:." edu.cwru.sepia.Main2 data/labs/stealth/EmptyMaze.xml

package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;


import java.util.HashSet;   // will need for dfs
import java.util.Stack;     // will need for dfs
import java.util.Set;       // will need for dfs


// JAVA PROJECT IMPORTS


public class DFSMazeAgent
    extends MazeAgent
{

    public DFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        Stack<Path> nubs = new Stack<>();
        nubs.add(new Path(src));

        HashSet<Vertex> alreadyVisited = new HashSet<>();
        alreadyVisited.add(src);

        while (!nubs.isEmpty()) {
            Path current = nubs.pop();
            // hashset for quick lookup
            HashSet<Vertex> myNeighbors = new HashSet<>();
            // get possible neighbors (possible duplicates)
            myNeighbors = getNeighbors(state, current.getDestination());
            for (Vertex neighbor : myNeighbors) {
                if (neighbor.equals(goal)) {
                    System.out.println("Win");
                    return current;
                }
                // if we haven't encountered this path yet, add to the checking stack
                if (!alreadyVisited.contains(neighbor)) {
                    nubs.add(new Path(neighbor, 1.0f, current));
                    alreadyVisited.add(neighbor);
                }
            }
        }

        return null;
    }

    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        return false;
    }

    public HashSet<Vertex> getNeighbors(StateView state, Vertex vertex) {
        int xVal = vertex.getXCoordinate();
        int yVal = vertex.getYCoordinate();
        HashSet<Vertex> neighbors = new HashSet<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if ((!(i == 0 && j == 0)) && isSafeSquare(state, xVal + i, yVal + j)) {
                    neighbors.add(new Vertex(xVal + i, yVal + j));
                }
            }
        }
        
        return neighbors;
    }

    public boolean isSafeSquare(StateView state, int x, int y) {
        // found enemy
        if (state.isUnitAt(x, y) && (state.unitAt(x, y) == this.getEnemyTargetUnitID())) {
            return true;
        }
        // found open square
        else {
            return state.inBounds(x, y) && (!state.isUnitAt(x, y)) && (!state.isResourceAt(x, y));
        }
    }

}
