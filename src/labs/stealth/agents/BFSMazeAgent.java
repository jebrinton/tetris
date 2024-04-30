// @ symbol tells java that stealth.srcs is a lot of files
// -cp == --classpath tells java where to find the classes that this is dependend on
// the : separates items in a list
// javac -cp "lib/Sepia.jar:lib/stealth.jar:." @stealth.srcs
// java -cp "lib/Sepia.jar:lib/stealth.jar:." edu.cwru.sepia.Main2 data/labs/stealth/EmptyMaze.xml

package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;


import java.util.HashSet;       // will need for bfs
import java.util.Queue;         // will need for bfs
import java.util.LinkedList;    // will need for bfs
import java.util.Set;           // will need for bfs
import java.util.Stack;


// JAVA PROJECT IMPORTS


public class BFSMazeAgent
    extends MazeAgent
{

    public BFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        // Sample hardcoded path
        // Path p = new Path(new Vertex(0, 0));
        // for (int i = 1; i <= 4; i++) {
        //     p = new Path(new Vertex(i-1, i), 1.0f, p);
        //     p = new Path(new Vertex(i, i), 1.0f, p);
        // }
        // return p;
        
        // hash to make lookup fast
        Set<Vertex> alreadyVisited = new HashSet<>();
        // queue for next things to check
        Queue<Path> toCheck = new LinkedList<>();
        
        toCheck.add(new Path(src));
        alreadyVisited.add(src);
        
        while (!toCheck.isEmpty()) {
            Path current = toCheck.poll();
            // hashset for quick lookup
            HashSet<Vertex> myNeighbors = new HashSet<>();
            // get possible neighbors (possible duplicates)
            myNeighbors = getNeighbors(state, current.getDestination());
            for (Vertex neighbor : myNeighbors) {
                if (neighbor.equals(goal)) {
                    System.out.println("win");
                    return current;
                }
                // if we haven't encountered this path yet, add to the checking queue
                if (!alreadyVisited.contains(neighbor)) {
                    toCheck.add(new Path(neighbor, 1.0f, current));
                    alreadyVisited.add(neighbor);
                }
            }
        }

        System.out.println("Didn't find anything");
        return new Path(src);
    }

    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        // use the shallow copy from state
        Stack<Vertex> currentPlan = getCurrentPlan();

        // check for if the next move is invalid

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
