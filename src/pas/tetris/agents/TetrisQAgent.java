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
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -i ./snn666.model

// Test game in the cs440 environment
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -s | tee testrun2.log

// TODO: ensure that buffer is large enough

public class TetrisQAgent
    extends QAgent
{
    // Constants for sizes of neural network (num of features)
    public static final int INPUT_SIZE = 5;
    public static final int HIDDEN_SIZE = 8;
    public static final int NUM_HIDDEN_LAYERS = 2;

    public static final double EXPLORATION_PROB = 0.05;

    // phase and game exp are the max prob at the start of each phase/epoch
    // exp probability is the sum of the 3
    public static final double PHASE_EXP = 0.055;
    public static final double GAME_EXP = 0.015;
    public static final double MIN_EXP = 0.03;
    // coincidentally, this will be an exp_prob of 0.05 halfway thru

    // constants used in the reward function to penalize total height, bumpiness, and number of holes
    public static final double HEIGHT_REWARD = -0.21;
    public static final double BUMPINESS_REWARD = -0.10;
    public static final double HOLES_REWARD = -0.95;
    public static final double SOLO_ROW_REWARD = -0.17;
    public static final double COMPLETE_ROW_REWARD = 9.0;

    public static final double REWARD_FACTOR = 1;

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
        // builds a 16x1 hidden layer neural network
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
        Matrix arrayImage = null;
        Matrix features = null;
        try
        {
            // 1.0 is current mino, 0.5 is previous minos, 0.0 is air
            arrayImage = game.getGrayscaleImage(potentialAction);
            
            int NUM_COLS = arrayImage.getShape().getNumCols();
            int NUM_ROWS = arrayImage.getShape().getNumRows();

            // New plan for features: total height (sum of cols' heights), bumpinesss, trapped air, number of covering pieces, number of complete rows
            features = Matrix.zeros(1, INPUT_SIZE);
            int totalHeight = NUM_COLS * NUM_ROWS;
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
                    totalHeight--;
                    row++;
                }

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

                covers += colCovers;
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

            // feature normalization

            // max totalHeight could be is 198 (each row can have up to 9)
            double heightFeature = totalHeight / 198.0;
            // max (sane) bumpiness is 20 * (3 or 4) ~= 70
            double bumpinessFeature = bumpiness / 70.0;
            // max number of holes without trying super hard would be roughly checkerboard
            double holesFeature = holes / 110.0;
            // max number of covers... ok this is a little trickier to visualize, I'm honestly just guessing here
            double coversFeature = covers / 60.0;
            // max number of complete rows... ah here we go, something that actually makes sense
            double completeRowsFeature = completeRows / 4.0;

            // add features to matrix
            features.set(0, 0, heightFeature);
            features.set(0, 1, bumpinessFeature);
            features.set(0, 2, holesFeature);
            features.set(0, 3, coversFeature);
            features.set(0, 4, completeRowsFeature);

            // System.out.println("TH: " + totalHeight + " Bmp: " + bumpiness + " Hol: " + holes + " Cov: " + covers + " CR: " + completeRows);
            // System.out.println(features);

        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        // System.out.print(features);
        return features;
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
        // TODO: change exploration function so that it is ALWAYS exploring at the start

        // make the exploration rate highest at the start of each phase and 
        // get the progress of this phase
        double gameProgress = (double) gameCounter.getCurrentGameIdx() / gameCounter.getNumTrainingGames();
        double phaseProgress = (double) gameCounter.getCurrentPhaseIdx() / gameCounter.getNumPhases();

        double exp_prob = MIN_EXP + PHASE_EXP * (1-phaseProgress) + GAME_EXP * (1-gameProgress);
        // System.out.println(exp_prob);

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
        // TODO: change choose exploration function so that it initially chooses "good" moves as deemed by us
        // Initially during training we should choose the best according to heuristics
        // Later on take more from what the qFunction determines is good (optional considering the shouldExplore prob is lower)

        // add teacher forcing so we select a winning move if it exists
        
        /*
        // iterate thru all final minos in coordinate order
        // and print qVal
        List<Mino> originalMinoList = game.getFinalMinoPositions();
        List<Mino> minoList = new ArrayList<>(originalMinoList);
        minoList.sort(Comparator.comparing(mino -> mino.getPivotBlockCoordinate().getXCoordinate()));

        Iterator<Mino> iterator = minoList.iterator();

        while (iterator.hasNext()) {
            Mino mino = iterator.next();
            try {
                System.out.println("Placing at " + mino.getPivotBlockCoordinate() + " facing " + mino.getOrientation() + " has qVal " + this.getQFunction().forward(getQFunctionInput(game, mino)));
                TimeUnit.SECONDS.sleep(4);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        */

        List<Mino> minosOriginal = game.getFinalMinoPositions();

        // so we don't divide by 0 later
        if (minosOriginal.size() == 1) {
            return minosOriginal.get(0);
        }
        List<Mino> minos = new ArrayList<>(minosOriginal);
        // tree map for automatic sorting
        Map<Double, Mino> minoMap = new TreeMap<>();
        for (Mino mino : minos) {
            try {
                minoMap.put(this.getQFunction().forward(getQFunctionInput(game, mino)).item(), mino);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        int numMinos = minoMap.size();
        Random random = new Random();
        // gives a distribution from 0 to inf
        double randomIndex = (1 / Math.log(random.nextDouble() + 1)) - 1;
        // note subtracting 1 from numMinos because we don't want the policy mino
        // add 1 at the front to offset so we have 1/2 chance of selecting 1, 1/4 of 2, etc.
        int randIdx = 1 + (int) (randomIndex % (numMinos - 1));

        // System.out.println("randIdx is: " + randIdx + " out of " + numMinos);

        // base implementation
        // int randIdx = this.getRandom().nextInt(game.getFinalMinoPositions().size());

        return game.getFinalMinoPositions().get(randIdx);
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
        // TODO: push rewards down so that score doesn't need to be multiplied by anything

        Board board = null;
        double reward = 0;

        try {
            board = game.getBoard();

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

        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        // Make points scored very important for the reward function
        reward += 2048 * game.getScoreThisTurn();

        reward = reward * REWARD_FACTOR;

        // System.out.print(reward);

        return reward;
    }

}
