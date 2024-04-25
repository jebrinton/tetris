package src.pas.battleship.agents;

import java.util.ArrayList;
// SYSTEM IMPORTS
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

// JAVA PROJECT IMPORTS
import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.game.EnemyBoard.Outcome;
import edu.bu.battleship.utils.Coordinate;

import edu.bu.battleship.game.Constants;
import edu.bu.battleship.game.EnemyBoard;


public class ProbabilisticAgent extends Agent {
    public class Rally {
        Coordinate nwCoord;
        Coordinate seCoord;
        int length;
        boolean isVert;

        // Constructor with parameters for north-west and south-east coordinates
        public Rally(Coordinate nwCoord, Coordinate seCoord) {
            this.nwCoord = nwCoord;
            this.seCoord = seCoord;
            this.length = calculateLength(nwCoord, seCoord);
            this.isVert = calculateVert(nwCoord, seCoord);
        }

        // Constructor with parameters for north-west coordinate, south-east coordinate, and length
        public Rally(Coordinate nwCoord, Coordinate seCoord, int length) {
            this(nwCoord, seCoord, length, false);
            this.isVert = calculateVert(nwCoord, seCoord);
        }

        // Constructor with parameters for north-west coordinate, south-east coordinate, and length
        public Rally(Coordinate nwCoord, Coordinate seCoord, int length, boolean isVert) {
            this.nwCoord = nwCoord;
            this.seCoord = seCoord;
            this.length = length;
            this.isVert = isVert;
        }

        // Helper method to calculate length based on coordinates
        private int calculateLength(Coordinate nwCoord, Coordinate seCoord) {
            if (nwCoord == null || seCoord == null) {
                return 0;
            }
            // Calculation logic goes here
            // For example:
            int length = 1 + Math.abs(nwCoord.getXCoordinate() - seCoord.getXCoordinate()) + Math.abs(nwCoord.getYCoordinate() - seCoord.getYCoordinate());
            return length;
        }

        // Helper method to determine if the rally track is vertical
        private boolean calculateVert(Coordinate nwCoord, Coordinate seCoord) {
            if (nwCoord == null || seCoord == null) {
                return false;
            }
            return nwCoord.getXCoordinate() == seCoord.getXCoordinate();
        }

        public void addCoord(Coordinate newCoord) {
            // check for null (Thank you CPK)
            if (this.nwCoord == null || this.seCoord == null) {
                this.setNwCoord(newCoord);
                this.setSeCoord(newCoord);
            }
            // Check if the new coordinate is further NW or further SE
            if (newCoord.getXCoordinate() <= nwCoord.getXCoordinate() && newCoord.getYCoordinate() <= nwCoord.getYCoordinate()) {
                this.setNwCoord(newCoord);
            } else if (newCoord.getXCoordinate() >= seCoord.getXCoordinate() && newCoord.getYCoordinate() >= seCoord.getYCoordinate()) {
                this.setSeCoord(newCoord);
            }
        }

        // Getter and setter for nwCoord
        public Coordinate getNwCoord() {
            return nwCoord;
        }

        public void setNwCoord(Coordinate nwCoord) {
            this.nwCoord = nwCoord;
            this.isVert = calculateVert(nwCoord, seCoord);
            this.length = calculateLength(nwCoord, seCoord);
        }

        // Getter and setter for seCoord
        public Coordinate getSeCoord() {
            return seCoord;
        }

        public void setSeCoord(Coordinate seCoord) {
            this.seCoord = seCoord;
            this.isVert = calculateVert(nwCoord, seCoord);
            this.length = calculateLength(nwCoord, seCoord);
        }

        // Getter for length
        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        // Getter for isVert
        public boolean isVert() {
            return isVert;
        }

        @Override
        public String toString() {
            return this.nwCoord + "---" + this.length + "---" + this.seCoord;
        }
    }
    // Instance variables
    private int targetLength;
    private LinkedList<Coordinate> moves;
    private int[][] ways;
    private int[][] targets;
    Rally rally;

    public ProbabilisticAgent(String name)
    {
        super(name);
        System.out.println("[INFO] ProbabilisticAgent.ProbabilisticAgent: constructed agent");

        this.targetLength = 0;
        this.moves = new LinkedList<Coordinate>();
        this.rally = new Rally(null, null, 0);
    }

    @Override
    public Coordinate makeMove(final GameView game)
    {
        // constants needed for main logic
        Constants gameConstants = game.getGameConstants();
        // x is number of cols
        int xSize = gameConstants.getNumCols();
        // y is number of rows
        int ySize = gameConstants.getNumRows();

        Coordinate lastMove = moves.peek();
        EnemyBoard.Outcome[][] outcomes = game.getEnemyBoardView();

        Coordinate shot = null;

        // create initial distribution (arbitrary for now)
        if (ways == null) {
            // ways = createInitialDistribution(xSize, ySize);
            ways = new int[xSize][ySize];
            for (int row = 0; row < outcomes.length; row++) {
                for (int col = 0; col < outcomes.length; col++) {
                    ways[col][row] = calculateOpenness(new Coordinate(col, row), 5, game, outcomes);
                    System.out.print(ways[col][row] + " ");
                }
                System.out.println();
            }
        }

        // first move, setup of ways to place a ship
        if (lastMove == null) {
            shot = new Coordinate(xSize / 2, ySize / 2);
        }
        // actual moves
        else {
            // update ways
            ways = updateWays(lastMove, ways, 5, game, outcomes);

            // get info on what the last shot was
            EnemyBoard.Outcome lastOutcome = outcomes[lastMove.getXCoordinate()][lastMove.getYCoordinate()];

            int lastX = lastMove.getXCoordinate();
            int lastY = lastMove.getYCoordinate();
            // if the last shot sunk the ship
            if (lastOutcome.equals(EnemyBoard.Outcome.SUNK)) {
                // reset rally
                rally.setNwCoord(null);
                rally.setSeCoord(null);
                rally.setLength(0);
            }
            else if (lastOutcome.equals(EnemyBoard.Outcome.HIT)) {
                // update rally
                rally.addCoord(lastMove);
            }
            // we need to check for adjacent ships confusing targetLength > 0 at some point
            if (rally.getLength() > 0) {
                System.out.println("Ry: " + this.rally);
                // case of doing the first shot
                if (rally.getLength() == 1) {
                    
                    List<Coordinate> cardinalSteps = new ArrayList<>();

                    // One step north
                    cardinalSteps.add(new Coordinate(0, 1));
                    // One step south
                    cardinalSteps.add(new Coordinate(0, -1));
                    // One step east
                    cardinalSteps.add(new Coordinate(1, 0));
                    // One step west
                    cardinalSteps.add(new Coordinate(-1, 0));

                    // check each direction for longest distance away from obstacles, and try that direction first
                    int recordSpaces = 0;
                    Coordinate recordCoord = rally.getNwCoord();
                    for (Coordinate coordinate : cardinalSteps) {
                        int openSpaces = 0;
                        int x = rally.getNwCoord().getXCoordinate();
                        int y = rally.getSeCoord().getYCoordinate();
                        int dx = coordinate.getXCoordinate();
                        int dy = coordinate.getYCoordinate();
                        while (canShoot(x + dx, y + dy, game, outcomes)) {
                            openSpaces++;
                            x += dx;
                            y += dy;
                            // update furthest direction
                            if (openSpaces > recordSpaces) {
                                recordSpaces = openSpaces;
                                recordCoord = new Coordinate(rally.getNwCoord().getXCoordinate() + dx, rally.getNwCoord().getYCoordinate() + dy);
                            }
                        }
                    }

                    // update shot with longest direction
                    shot = recordCoord;
                    System.out.print("RC was " + shot);

                    if (!canShoot(shot.getXCoordinate(), shot.getYCoordinate(), game, outcomes)) {
                        System.out.print("Illegal shot in first rally: " + shot);
                        shot = randomShot(outcomes);
                    }
                    /*
                    int targetProb = 0;
                    
                    // Check if moving west is within bounds and has a higher probability
                    if (canShoot(lastX - 1, lastY, game, outcomes) && ways[lastX - 1][lastY] > targetProb) {
                        shot = new Coordinate(lastX - 1, lastY);
                        targetProb = ways[lastX - 1][lastY];
                    }

                    // Check if moving east is within bounds and has a higher probability
                    if (canShoot(lastX + 1, lastY, game, outcomes) && ways[lastX + 1][lastY] > targetProb) {
                        shot = new Coordinate(lastX + 1, lastY);
                        targetProb = ways[lastX + 1][lastY];
                    }

                    // Check if moving north is within bounds and has a higher probability
                    if (canShoot(lastX, lastY - 1, game, outcomes) && ways[lastX][lastY - 1] > targetProb) {
                        shot = new Coordinate(lastX, lastY - 1);
                        targetProb = ways[lastX][lastY - 1];
                    }

                    // Check if moving south is within bounds and has a higher probability
                    if (canShoot(lastX, lastY + 1, game, outcomes) && ways[lastX][lastY + 1] > targetProb) {
                        shot = new Coordinate(lastX, lastY + 1);
                        targetProb = ways[lastX][lastY + 1];
                    }
                    */
                }
                // rally is longer than length 1
                else {
                    // if this is a vertical ship, our directions should be north and south
                    if (rally.isVert) {
                        int nDist = calculateFreeSpaces(rally.getNwCoord(), 0, -1, game, outcomes);
                        int sDist = calculateFreeSpaces(rally.getSeCoord(), 0, 1, game, outcomes);

                        System.out.println("North: " + nDist + " South: " + sDist);

                        // go north one spot
                        if (nDist > sDist) {
                            shot = new Coordinate(rally.getNwCoord().getXCoordinate(), rally.getNwCoord().getYCoordinate() - 1);
                        }
                        // go south one spot
                        else {
                            shot = new Coordinate(rally.getSeCoord().getXCoordinate(), rally.getSeCoord().getYCoordinate() + 1);
                        }
                    }
                    // if this is a horizontal ship, our directions should be east and west
                    else {
                        int wDist = calculateFreeSpaces(rally.getNwCoord(), -1, 0, game, outcomes);
                        int eDist = calculateFreeSpaces(rally.getSeCoord(), 1, 0, game, outcomes);

                        System.out.println("West: " + wDist + " East: " + eDist);

                        // go west one spot
                        if (wDist > eDist) {
                            shot = new Coordinate(rally.getNwCoord().getXCoordinate() - 1, rally.getNwCoord().getYCoordinate());
                        }
                        // go east one spot
                        else {
                            shot = new Coordinate(rally.getSeCoord().getXCoordinate() + 1, rally.getSeCoord().getYCoordinate());
                        }
                    }
                    if (!canShoot(shot.getXCoordinate(), shot.getYCoordinate(), game, outcomes)) {
                        System.out.print("Illegal shot in rally 2: " + shot);
                        shot = randomShot(outcomes);
                    }
                }
            }
            // hunt mode, set to random shots for now
            else {
                shot = randomShot(outcomes);

                System.out.print(Arrays.toString(ways));
                // iterate through every square in ways and select the openest square
                int maxOpenness = -99999;
                for (int x = 0; x < ways.length; x++) {
                    for (int y = 0; y < ways[x].length; y++) {
                        // only care about even squares
                        if ((x + y) % 2 == 0 && ways[x][y] > maxOpenness) {
                            maxOpenness = ways[x][y];
                            shot = new Coordinate(x, y);
                            System.out.println("New max openness of " + maxOpenness + " at " + shot);
                        }
                    }
                }

                // I think this should work, but if not, here's so we don't crash and burn
                if (!canShoot(shot.getXCoordinate(), shot.getYCoordinate(), game, outcomes)) {
                    System.out.print("Illegal shot in hunt mode: " + shot);
                    shot = randomShot(outcomes);
                }
            }
        }

        // add shot so we have record of it next turn
        this.moves.addFirst(shot);
        return shot;
    }

    @Override
    public void afterGameEnds(final GameView game) {}

    public void printBoard(EnemyBoard.Outcome[][] outcomes) {
        for (int col = 0; col < outcomes.length; col++) {
            for (int row = 0; row < outcomes[col].length; row++) {
                System.out.print(outcomes[row][col].toString().toLowerCase().charAt(0) + " ");
            }
            System.out.println();
        }
        System.out.println("---------------------");
    }

    /**
     * Creates an initial distribution (higher in the middle, but not actual values for now)
     * @param xSize
     * @param ySize
     * @return
     */
    public int[][] createInitialDistribution(int xSize, int ySize) {
        int[][] ways = new int[xSize][ySize];
        for (int row = 0; row < ySize; row++) {
            for (int col = 0; col < xSize; col++) {
                ways[row][col] = ySize - Math.abs((ySize / 2) - row) + xSize - Math.abs((xSize / 2) - col);
                System.out.print(ways[row][col] + " ");
            }
            System.out.println();
        }
        return ways;
    }

    /**
     * Returns a random coordinate among the remaining ones
     * @param outcomes
     * @return
     */
    public Coordinate randomShot(EnemyBoard.Outcome[][] outcomes) {
        Coordinate shot = null;

        // hash set that will hold all possible coords we can attack
        HashSet<Coordinate> hs = new HashSet<>();

        // create a hash set that contains every unknown square
        for (int i = 0; i < outcomes.length; i++) {
            for (int j = 0; j < outcomes[i].length; j++) {
                // only add even squares
                if (outcomes[i][j].equals(EnemyBoard.Outcome.UNKNOWN) && (i + j) % 2 == 0) {
                    hs.add(new Coordinate(i, j));
                }
            }
        }

        // if there are no even squares left ( :( )
        if (hs.isEmpty()) {
            return oldRandomShot(outcomes);
        }
        
        // Use an iterator to iterate through the HashSet
        Iterator<Coordinate> iterator = hs.iterator();
        
        // Generate a random number to select an element
        int randomNumber = (int) (Math.random() * hs.size());
        
        // Iterate through the HashSet
        int i = 0;
        while (iterator.hasNext()) {
            if (i == randomNumber) {
                shot = iterator.next();
                break;
            }
            else {
                iterator.next();
            }
            i++;
        }

        return shot;
    }

    /**
     * Returns a random coordinate among the remaining ones
     * @param outcomes
     * @return
     */
    public Coordinate oldRandomShot(EnemyBoard.Outcome[][] outcomes) {
        Coordinate shot = null;

        // hash set that will hold all possible coords we can attack
        HashSet<Coordinate> hs = new HashSet<>();

        // create a hash set that contains every unknown square
        for (int i = 0; i < outcomes.length; i++) {
            for (int j = 0; j < outcomes[i].length; j++) {
                if (outcomes[i][j].equals(EnemyBoard.Outcome.UNKNOWN)) {
                    hs.add(new Coordinate(i, j));
                }
            }
        }
        
        // Use an iterator to iterate through the HashSet
        Iterator<Coordinate> iterator = hs.iterator();
        
        // Generate a random number to select an element
        int randomNumber = (int) (Math.random() * hs.size());
        
        // Iterate through the HashSet
        int i = 0;
        while (iterator.hasNext()) {
            if (i == randomNumber) {
                shot = iterator.next();
                break;
            }
            else {
                iterator.next();
            }
            i++;
        }

        return shot;
    }

    public boolean canShoot(int x, int y, final GameView game, EnemyBoard.Outcome[][] outcomes) {
        return game.isInBounds(x, y) && outcomes[x][y].equals(EnemyBoard.Outcome.UNKNOWN);
    }

    public int calculateFreeSpaces(Coordinate startPoint, int dx, int dy, GameView game, EnemyBoard.Outcome[][] outcomes) {
        int openSpaces = 0;
        int x = startPoint.getXCoordinate();
        int y = startPoint.getYCoordinate();
        while (canShoot(x + dx, y + dy, game, outcomes)) {
            openSpaces++;
            x += dx;
            y += dy;
        }
        return openSpaces;
    }

    public int calculateFreeSpaces(Coordinate startPoint, int dx, int dy, int limit, GameView game, EnemyBoard.Outcome[][] outcomes) {
        int openSpaces = 0;
        int x = startPoint.getXCoordinate();
        int y = startPoint.getYCoordinate();
        while (canShoot(x + dx, y + dy, game, outcomes) && limit > 0) {
            openSpaces++;
            x += dx;
            y += dy;
            limit--;
        }
        return openSpaces;
    }

    public int calculateOpenness(Coordinate coord, int limit, GameView game, EnemyBoard.Outcome[][] outcomes) {
        int openness = 0;
        openness += calculateFreeSpaces(coord, 1, 0, limit, game, outcomes);
        openness += calculateFreeSpaces(coord, -1, 0, limit, game, outcomes);
        openness += calculateFreeSpaces(coord, 0, 1, limit, game, outcomes);
        openness += calculateFreeSpaces(coord, 0, -1, limit, game, outcomes);
        return openness;
    }

    public int[][] updateWays(Coordinate lastMove, int[][] ways, int limit, GameView game, EnemyBoard.Outcome[][] outcomes) {
        // update spot to 0
        ways[lastMove.getXCoordinate()][lastMove.getYCoordinate()] = 0;

        // iterate in each direction until limit/boundary of the board/can't shoot anymore
        List<Coordinate> cardinalSteps = new ArrayList<>();

        // One step north
        cardinalSteps.add(new Coordinate(0, 1));
        // One step south
        cardinalSteps.add(new Coordinate(0, -1));
        // One step east
        cardinalSteps.add(new Coordinate(1, 0));
        // One step west
        cardinalSteps.add(new Coordinate(-1, 0));

        for (Coordinate coordinate : cardinalSteps) {
            int dx = coordinate.getXCoordinate();
            int dy = coordinate.getYCoordinate();
            int x = lastMove.getXCoordinate();
            int y = lastMove.getYCoordinate();
            // recalculate openness for affected squares
            while (canShoot(x + dx, y + dy, game, outcomes) && limit > 0) {
                x += dx;
                y += dy;
                limit--;
                ways[x][y] = calculateOpenness(new Coordinate(x, y), limit, game, outcomes);
            }
        }
        
        return ways;
    }
}
