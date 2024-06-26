package src.pas.tetris.agents;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
// SYSTEM IMPORTS
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

// JAVA PROJECT IMPORTS
import edu.bu.tetris.agents.QAgent;
import edu.bu.tetris.agents.TrainerAgent.GameCounter;
import edu.bu.tetris.game.Block;
import edu.bu.tetris.game.Board;
import edu.bu.tetris.game.Game.GameView;
import edu.bu.tetris.game.minos.Mino;
import edu.bu.tetris.game.minos.Mino.MinoType;
import edu.bu.tetris.linalg.Matrix;
import edu.bu.tetris.nn.Model;
import edu.bu.tetris.nn.LossFunction;
import edu.bu.tetris.nn.Optimizer;
import edu.bu.tetris.nn.models.Sequential;
import edu.bu.tetris.nn.layers.Dense; // fully connected layer
import edu.bu.tetris.nn.layers.ReLU;  // some activations (below too)
import edu.bu.tetris.nn.layers.Tanh;
import edu.bu.tetris.nn.layers.Sigmoid;
import edu.bu.tetris.training.data.Dataset;
import edu.bu.tetris.utils.Coordinate;
import edu.bu.tetris.utils.Pair;

// IP address of system
// ssh jbrin@10.210.1.208

// COMPILE
// javac -cp "./lib/*:." @tetris.srcs

// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 100 -v 50 -g 0.99 -n 0.01 -b 5000 -c 1000000000 -s | tee run1.log

// more phases/games
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 20000 -t 200 -v 100 -g 0.99 -n 0.00001 -u 4 -b 500000 -c 1000000000 -s | tee run7.log

// Ok using the training rate provided from Piazza
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 100 -v 50 -g 0.99 -n 0.01 -b 50000 -c 1000000000 -s | tee run8.log

// Fast training rate which somehow worked well (now I'm realizing it was just the seed honestly) (so I changed it back)
// Ok now this one is practically the same as the Piazza one it's just with the correct ins and outs
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 100 -v 50 -g 0.99 -n 0.01 -b 500000 -o ./n1params/q -i ./n1params/q1.model --outOffset 900000 -s | tee n1run1.log

// Slower training rate but start with a good seed???
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 100 -v 50 -g 0.99 -n 0.001 -b 500000 -o ./n-3params/q -i ./n1params/q1.model --outOffset 300000 -s | tee n-3run1.log

// SmallNN one
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 100 -v 50 -g 0.99 -n 0.001 -b 500000 -o ./snn-params/q -s | tee snn-run1.log

// Even slower training with u set higher
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 100 -v 50 -g 0.99 -n 0.0001 -u 7 -b 500000 -o ./n-4params/q -i ./n1params/q1.model --outOffset 400000 -s | tee n-4run1.log

// Command for just running a test game
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent

// Test game using a certain model
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -i ./8-2-3-good.model

// Test game in the cs440 environment
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -s | tee testrun2.log

// NOTE: on the server params, etc will be stored like hiddenSize-numLayers-learningRatePower ex: 8-2-3-params

// NEW HOPE runs
// All of these don't have the crazy high clip value

// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 400 -v 100 -g 0.99 -n 0.001 -b 50000 -o ./8-2-3-params/q -s | tee 8-2-3.log

// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 200 -v 100 -g 0.99 -n 0.0001 -b 50000 -o ./8-2-4-params/q -s | tee 8-2-4.log

// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 200 -v 100 -g 0.99 -n 0.000001 -u 5 -b 50000 -o ./8-2-6-params/q -s | tee 8-2-6.log

// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 400 -v 100 -g 0.99 -c 1000000000 -n 0.001 -b 50000 -o ./16-1-3-params/q -s | tee 16-1-3.log

// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 800 -v 200 -g 0.99 -c 1000000000 -n 0.0001 -b 50000 -o ./16-1-4-params/q -s | tee 16-1-4.log

// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 200 -v 80 -g 0.99 -c 1000000000 -n 0.000001 -u 10 -b 50000 -o ./16-1-6-params/q -s | tee 16-1-6.log

// Now let's just get crazy, only 5 inner nodes
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 800 -v 200 -g 0.99 -c 1000000000 -n 0.0001 -b 50000 -o ./5-1-4-params/q -s | tee 5-1-4.log

// 12x2
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 200 -v 80 -g 0.99 -c 1000000000 -n 0.000001 -u 5 -b 50000 -o ./12-2-6-params/q -s | tee 12-2-6.log

// Realized I need something that will go quick...
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 1000 -t 20 -v 10 -g 0.99 -c 1000000000 -n 0.000001 -u 3 -b 50000 -o ./12-1-6-params/q -s | tee 12-1-6.log

// TODO: ensure that buffer is large enough
// TODO: fix Features class so it can operate on Boards (or just make Board2Matrix tbh)
// TODO: add perfect clear/Tetris/T-spin

// Perfect clear is 6pts and is when you completely clear the board. Always go for this
// Tetris is just clearing 4 lines; 4 pts
// So basically score is 2^(numLines - 2)
// Clear 2 lines - 1 pt
// Clear 3 lines - 2 pts
// Clear 4 lines - 4 pts

public class TetrisQAgent
    extends QAgent
{
    // Constants for sizes of neural network (num of features)
    public static final int INPUT_SIZE = 7;
    public static final int HIDDEN_SIZE = 7;
    public static final int NUM_HIDDEN_LAYERS = 2;

    public static final double MIN_EXP = 0.04;

    // constants used in the reward function to penalize total height, bumpiness, number of holes, covers, and solo rows

    // Some (actually not anymore but it was inspiration) of these are approximated from: https://codemyroad.wordpress.com/2013/04/14/tetris-ai-the-near-perfect-player/
    public static final double HEIGHT_REWARD = -0.00005;
    public static final double MAX_HEIGHT_REWARD = -0.00019;
    public static final double BUMPINESS_REWARD = -0.00018;
    public static final double HOLES_REWARD = -0.00036;
    public static final double COVERS_REWARD = -0.00027;
    // this is changed to 0 b/c the scoring changed
    public static final double SOLO_ROW_REWARD = -0.00000;
    // note: this reward is quadratic
    public static final double COMPLETE_ROW_REWARD = 0.20000;
    public static final double PERFECT_CLEAR_REWARD = 6.00000;
    public static final double LOSE_REWARD = -999.0;
    // the holy grail: find a way to locate t-spins

    private Random random;

    public TetrisQAgent(String name)
    {
        super(name);
        // old seed was 12345
        this.random = new Random(12345); // optional to have a seed
    }

    public Random getRandom() { return this.random; }

    @Override
    public Model initQFunction()
    {
        final int outDim = 1;

        Sequential qFunction = new Sequential();
        qFunction.add(new Dense(INPUT_SIZE, HIDDEN_SIZE));
        // note this loop starts at 1
        for (int i = 1; i < NUM_HIDDEN_LAYERS; i++) {
            qFunction.add(new ReLU());
            qFunction.add(new Dense(HIDDEN_SIZE, HIDDEN_SIZE));
        }
        qFunction.add(new ReLU());
        qFunction.add(new Dense(HIDDEN_SIZE, outDim));

        return qFunction;
    }

    /**
        This function is for you to figure out what your features
        are. This should end up being a single row-vector, and the
        dimensions should be what your qfunction is expecting.
        One thing we can do is get the grayscale image
        where squares in the image are 0.0 if unoccupied, 0.5 if
        there is a "background" square (i.e. that square is occupied
        but it is not the current piece being placed), and 1.0 for
        any squares that the current piece is being considered for.
        
        We can then flatten this image to get a row-vector, but we
        can do more than this! Try to be creative: how can you measure the
        "state" of the game without relying on the pixels? If you were given
        a tetris game midway through play, what properties would you look for?
     */
    @Override
    public Matrix getQFunctionInput(final GameView game,
                                    final Mino potentialAction)
    {
        Features ftrs = null;

        try {
            ftrs = new Features(game.getGrayscaleImage(potentialAction));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("Error in getting grayscale image for features");
        }

        // feature normalization

        // max totalHeight could be is 198 (each row can have up to 9)
        double heightFeature = ftrs.getTotalHeight() / 198.0;
        double maxHeightFeature = ftrs.getMaxHeight() / 22.0;
        // max (sane) bumpiness is 20 * (3 or 4) ~= 70
        double bumpinessFeature = ftrs.getBumpiness() / 70.0;
        // max number of holes without trying super hard would be roughly checkerboard
        double holesFeature = ftrs.getHoles() / 110.0;
        // max number of covers... ok this is a little trickier to visualize, I'm honestly just guessing here
        double coversFeature = ftrs.getCovers() / 60.0;
        // max number of complete rows... ah here we go, something that actually makes sense
        double completeRowsFeature = ftrs.getCompleteRows() / 4.0;
        double perfectClearFeature = (double) ftrs.getPerfectClear();

        // load qInput
        Matrix qIn = Matrix.zeros(1, INPUT_SIZE);
        qIn.set(0, 0, heightFeature);
        qIn.set(0, 1, maxHeightFeature);
        qIn.set(0, 2, bumpinessFeature);
        qIn.set(0, 3, holesFeature);
        qIn.set(0, 4, coversFeature);
        qIn.set(0, 5, completeRowsFeature);
        qIn.set(0, 6, perfectClearFeature);

        return qIn;
    }

    /**
     * This method is used to decide if we should follow our current policy
     * (i.e. our q-function), or if we should ignore it and take a random action
     * (i.e. explore).
     *
     * Remember, as the q-function learns, it will start to predict the same "good" actions
     * over and over again. This can prevent us from discovering new, potentially even
     * better states, which we want to do! So, sometimes we should ignore our policy
     * and explore to gain novel experiences.
     *
     * The current implementation chooses to ignore the current policy around 5% of the time.
     * While this strategy is easy to implement, it often doesn't perform well and is
     * really sensitive to the EXPLORATION_PROB. I would recommend devising your own
     * strategy here.
     */
    @Override
    public boolean shouldExplore(final GameView game,
                                 final GameCounter gameCounter)
    {
        // New function: remaining phase ratio to the 5th power plus a small offset 
        // (At the start, will explore a ton before going down)
        double numPhases = gameCounter.getNumPhases();
        double curPhase = gameCounter.getCurrentPhaseIdx();

        // Note: due to MIN_EXP, will be above 1 for a few phases
        double exp_prob = Math.pow((numPhases - curPhase) / numPhases, 5) + MIN_EXP;

        return this.getRandom().nextDouble() <= exp_prob;
    }

    /**
     * This method is a counterpart to the "shouldExplore" method. Whenever we decide
     * that we should ignore our policy, we now have to actually choose an action.
     *
     * You should come up with a way of choosing an action so that the model gets
     * to experience something new. The current implemention just chooses a random
     * option, which in practice doesn't work as well as a more guided strategy.
     * I would recommend devising your own strategy here.
     */
    @Override
    public Mino getExplorationMove(final GameView game)
    {
        // choose the best move according to our reward function
        // to get this, we simulate each move/get the corresponding reward

        List<Mino> minos = game.getFinalMinoPositions();

        // tree map for automatic sorting
        // note that it's in reverse order so we can choose the most positive reward at the end!
        TreeMap<Double, Mino> minoMap = new TreeMap<>(Comparator.reverseOrder());
        for (Mino mino : minos) {
            try {
                // get the features of this move and its reward
                Features features = new Features(game.getGrayscaleImage(mino));
                // printMove(game.getGrayscaleImage(mino));
                // System.out.println(features);

                // add a small fluctuation so we don't always choose the latest of same reward moves
                minoMap.put(features.calculateReward() + (random.nextDouble() / 100_000), mino);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        // TODO: maybe add stochasticity so we don't always choose the best move

        // return best move
        return minoMap.get(minoMap.firstKey());
    }

    /**
     * This method is called by the TrainerAgent after we have played enough training games.
     * In between the training section and the evaluation section of a phase, we need to use
     * the exprience we've collected (from the training games) to improve the q-function.
     *
     * You don't really need to change this method unless you want to. All that happens
     * is that we will use the experiences currently stored in the replay buffer to update
     * our model. Updates (i.e. gradient descent updates) will be applied per minibatch
     * (i.e. a subset of the entire dataset) rather than in a vanilla gradient descent manner
     * (i.e. all at once)...this often works better and is an active area of research.
     *
     * Each pass through the data is called an epoch, and we will perform "numUpdates" amount
     * of epochs in between the training and eval sections of each phase.
     */
    @Override
    public void trainQFunction(Dataset dataset,
                               LossFunction lossFunction,
                               Optimizer optimizer,
                               long numUpdates)
    {
        for(int epochIdx = 0; epochIdx < numUpdates; ++epochIdx)
        {
            dataset.shuffle();
            Iterator<Pair<Matrix, Matrix> > batchIterator = dataset.iterator();

            while(batchIterator.hasNext())
            {
                Pair<Matrix, Matrix> batch = batchIterator.next();

                try
                {
                    Matrix YHat = this.getQFunction().forward(batch.getFirst());

                    optimizer.reset();
                    this.getQFunction().backwards(batch.getFirst(),
                                                  lossFunction.backwards(YHat, batch.getSecond()));
                    optimizer.step();
                } catch(Exception e)
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This method is where you will devise your own reward signal. Remember, the larger
     * the number, the more "pleasurable" it is to the model, and the smaller the number,
     * the more "painful" to the model.
     *
     * This is where you get to tell the model how "good" or "bad" the game is.
     * Since you earn points in this game, the reward should probably be influenced by the
     * points, however this is not all. In fact, just using the points earned this turn
     * is a **terrible** reward function, because earning points is hard!!
     *
     * I would recommend you to consider other ways of measuring "good"ness and "bad"ness
     * of the game. For instance, the higher the stack of minos gets....generally the worse
     * (unless you have a long hole waiting for an I-block). When you design a reward
     * signal that is less sparse, you should see your model optimize this reward over time.
     */
    @Override
    public double getReward(final GameView game)
    {
        Board board = null;
        double reward = 0;

        try {
            board = game.getBoard();
            reward = calculateBoardReward(board);
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        reward += game.getScoreThisTurn();

        return reward;
    }

    public static double calculateBoardReward(Board board) {
        if (board == null) {
            System.out.println("null board, giving reward 0");
            return 0;
        }
        double reward = 0;

        // New plan for features: total height (sum of cols' heights), bumpinesss, trapped air, number of complete rows
        int totalHeight = Board.NUM_COLS * Board.NUM_ROWS;
        int bumpiness = 0;
        int holes = 0;
        int completeRows = 0;

        // used to calculate bumpiness
        int lastColAir = 0;

        // loop through cols then rows from NW to SE
        for (int col = 0; col < Board.NUM_COLS; col++) {
            int row = 0;

            // Make reward very low if we lost the game
            if (board.isCoordinateOccupied(col, row + 2)) {
                reward -= 2048;
                // System.out.println("GAME OVER");
            }

            // initial top air
            while (board.isInBounds(new Coordinate(col, row)) && !board.isCoordinateOccupied(col, row)) {
                totalHeight--;
                row++;
            }

            // now row is at the index of the first block
            // increment bumpiness by the absolute difference between the last row height and this row's height
            if (col > 0) {
                bumpiness += Math.abs(lastColAir - row);
            }
            lastColAir = row;

            // continue to bottom
            while (row < Board.NUM_ROWS) {
                // count number of holes (air)
                if (!board.isCoordinateOccupied(col, row)) {
                    holes++;
                }
                row++;
            }
        }

        // loop thru all rows
        // if there is a row with no air blocks, increment completeRows
        for (int row = 0; row < Board.NUM_ROWS; row++) {
            boolean isFull = true;
            for (int col = 0; col < Board.NUM_COLS; col++) {
                if (!board.isCoordinateOccupied(col, row)) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                completeRows++;
            }
        }

        // System.out.println("TH: " + totalHeight + " Bmp: " + bumpiness + " Hol: " + holes + " CR: " + completeRows);

        reward = HEIGHT_REWARD * totalHeight + BUMPINESS_REWARD * bumpiness + HOLES_REWARD * holes;

        // don't want to reward just clearing a single row
        if (completeRows == 1) {
            reward += SOLO_ROW_REWARD;
        }
        else if (completeRows > 1) {
            reward += COMPLETE_ROW_REWARD * Math.pow(completeRows, 2);
        }

        // System.out.println("RE- " + reward);
        
        return reward;
    }

    class Features {
        // TODO: add maxTrough (depth of trough created which an I-tetronimo could fit into)
        // add t-flip!
        // add the weighted height; i.e. the square root of the squares of the heights (euclidean??)
        // also update covers so it is more of covering-a-place-we-could-have-scored metric (this could be epic)
        int totalHeight;
        int maxHeight;
        int bumpiness;
        int holes;
        int covers;
        int completeRows;
        int perfectClear;

        /*
         * Constructor given the 2D grayscale array
         */
        public Features(Matrix arrayImage) {
            int NUM_COLS = arrayImage.getShape().getNumCols();
            int NUM_ROWS = arrayImage.getShape().getNumRows();

            int totalHeight = 0;
            int maxHeight = 0;
            int bumpiness = 0;
            int holes = 0;
            int covers = 0;
            int completeRows = 0;

            // Possible new features: number of type of Mino in the queue (one-hot encoding); number of covers by the specific Mino
            // number of 1-missing rows
            
            // Another crazy thing we could try: only base it off what the specific mino does
            // Such as: height relative to mean height of old minos
            // number of holes created
            // bumpiness added
            // rows completed

            // used to calculate bumpiness
            int lastColAir = 0;

            // loop through cols then rows from NW to SE
            for (int col = 0; col < NUM_COLS; col++) {
                // cover total for this column
                int colCovers = 0;

                int row = 0;
                // initial top air
                while (row < NUM_ROWS && arrayImage.get(row, col) == 0.0) {
                    row++;
                }

                int colHeight = NUM_ROWS - row;

                // now row is at the index of the first block
                // increment bumpiness by the absolute difference between the last row height and this row's height
                if (col > 0) {
                    bumpiness += Math.abs(lastColAir - row);
                }
                lastColAir = row;

                // while squares are covered (we're only measuring the top cover)
                while (row < NUM_ROWS && arrayImage.get(row, col) != 0.0) {
                    colCovers++;
                    row++;
                }

                boolean isTrueCover = false;

                // continue to bottom
                while (row < NUM_ROWS) {
                    // count number of holes (air)
                    if (arrayImage.get(row, col) == 0.0) {
                        // check that it actually covers
                        isTrueCover = true;
                        holes++;
                    }
                    row++;
                }

                // reset covers
                if (!isTrueCover) {
                    colCovers = 0;
                }

                totalHeight += colHeight;
                covers += colCovers;
                maxHeight = Math.max(maxHeight, colHeight);
            }

            // loop thru all rows
            // if there is a row with no air blocks, increment completeRows
            for (int row = 0; row < NUM_ROWS; row++) {
                boolean isFull = true;
                for (int col = 0; col < NUM_COLS; col++) {
                    if (arrayImage.get(row, col) == 0.0) {
                        isFull = false;
                        break;
                    }
                }
                if (isFull) {
                    completeRows++;
                }
            }

            int perfectClear = 0;
            if (completeRows == maxHeight) {
                perfectClear = 1;
            }

            this.totalHeight = totalHeight;
            this.maxHeight = maxHeight;
            this.bumpiness = bumpiness;
            this.holes = holes;
            this.covers = covers;
            this.completeRows = completeRows;
            this.perfectClear = perfectClear;
        }

        public double calculateReward() {
            double reward = 0;

            if (maxHeight > 20) {
                return LOSE_REWARD;
            }

            if (perfectClear == 1) {
                return PERFECT_CLEAR_REWARD;
            }

            // ensure to not reward a single row clear
            if (completeRows == 1) {
                reward += SOLO_ROW_REWARD;
            }
            else {
                reward += COMPLETE_ROW_REWARD * Math.pow(completeRows, 2);
            }

            // linear combination of the rest of the rewards
            reward += HEIGHT_REWARD * totalHeight + MAX_HEIGHT_REWARD * maxHeight + BUMPINESS_REWARD * bumpiness + HOLES_REWARD * holes + COVERS_REWARD * covers;
            
            return reward;
        }

        @Override
        public String toString() {
            return "tHeight: " + totalHeight + " maxH: " + maxHeight + " bump: " + bumpiness +
            " holes: " + holes + " covers: " +
            covers + " compRows: " + completeRows + " perfClear: " + perfectClear;
        }

        // Getter for totalHeight
        public int getTotalHeight() {
            return totalHeight;
        }

        // Getter for maxHeight
        public int getMaxHeight() {
            return maxHeight;
        }

        // Getter for bumpiness
        public int getBumpiness() {
            return bumpiness;
        }

        // Getter for holes
        public int getHoles() {
            return holes;
        }

        // Getter for covers
        public int getCovers() {
            return covers;
        }

        // Getter for completeRows
        public int getCompleteRows() {
            return completeRows;
        }

        // Getter for completeRows
        public int getPerfectClear() {
            return perfectClear;
        }
    }

    public void printMove(Matrix grayscaleMatrix) {
        for (int row = 0; row < grayscaleMatrix.getShape().getNumRows(); row++) {
            for (int col = 0; col < grayscaleMatrix.getShape().getNumCols(); col++) {
                System.out.print(" ");
                if (grayscaleMatrix.get(row, col) == 0.0) {
                    System.out.print("-");
                }
                else if (grayscaleMatrix.get(row, col) == 0.5) {
                    System.out.print("\u2592");
                }
                else if (grayscaleMatrix.get(row, col) == 1.0) {
                    System.out.print("\u2587");
                }
                else {
                    System.out.print("Not a gameboard");
                }
                System.out.print(" ");
            }
            System.out.println();
        }
    }
}
