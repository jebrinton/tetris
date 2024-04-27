package src.pas.tetris.agents;


// SYSTEM IMPORTS
import java.util.Iterator;
import java.util.List;
import java.util.Random;


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

// COMPILE
// javac -cp "./lib/*:." @tetris.srcs

// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 100 -v 50 -g 0.99 -n 0.01 -b 5000 -c 1000000000 -s | tee run1.log

// more phases/games
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -p 20000 -t 400 -v 100 -g 0.99 -n 0.00001 -u 5 -b 500000 -c 1000000000 -s | tee run5.log

// IP address of system
// ssh jbrin@10.210.1.208

// Command for just running a test game
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent

// Test game in the cs440 environment
// java -cp "lib/*:." edu.bu.tetris.Main -q src.pas.tetris.agents.TetrisQAgent -s | tee testrun2.log


public class TetrisQAgent
    extends QAgent
{

    public static final double EXPLORATION_PROB = 0.05;

    // phase and game exp are the max prob at the start of each phase/epoch
    // exp probability is the sum of the 3
    public static final double PHASE_EXP = 0.045;
    public static final double GAME_EXP = 0.015;
    public static final double MIN_EXP = 0.03;

    private Random random;

    public TetrisQAgent(String name)
    {
        super(name);
        this.random = new Random(12345); // optional to have a seed
    }

    public Random getRandom() { return this.random; }

    @Override
    public Model initQFunction()
    {
        // builds a neural network with 3 hidden layers
        final int inputSize = 2 * Board.NUM_COLS;
        final int hiddenDim = 4 * Board.NUM_COLS;
        final int outDim = 1;
        final int numHiddenLayers = 3;

        Sequential qFunction = new Sequential();
        qFunction.add(new Dense(inputSize, hiddenDim));
        // note this loop starts at 1
        for (int i = 1; i < numHiddenLayers; i++) {
            qFunction.add(new ReLU());
            qFunction.add(new Dense(hiddenDim, hiddenDim));
        }
        qFunction.add(new ReLU());
        qFunction.add(new Dense(hiddenDim, outDim));

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
            arrayImage = game.getGrayscaleImage(potentialAction);
            // System.out.print("\n\n\narray");
            // System.out.print(arrayImage);

            int NUM_COLS = arrayImage.getShape().getNumCols();
            int NUM_ROWS = arrayImage.getShape().getNumRows();

            // create a flattened matrix as output
            // 2 features per column (topAir and density)
            features = Matrix.zeros(1, 2 * NUM_COLS);

            // get the number of 
            for (int col = 0; col < NUM_COLS; col++) {
                int topAir = 0;
                // loop through top air blocks
                int row = 0;
                while (row < NUM_ROWS && arrayImage.get(row, col) == 0.0) {
                    topAir++;
                    row++;
                }

                // System.out.println("r c " + NUM_ROWS + " " + NUM_COLS);

                // System.out.println("r- " + row);

                double density = 1;
                if (row == NUM_ROWS) {
                    density = 1;
                }
                else {
                    // loop through remaining blocks
                    int midAir = 0;
                    int midBlock = 0;
                    while (row < NUM_ROWS) {
                        // air
                        if (arrayImage.get(row, col) == 0.0) {
                            midAir++;
                        }
                        else {
                            midBlock++;
                        }
                        row++;
                    }
                    density = (double) midBlock / (midAir + midBlock);
                }
                features.set(0, 2*col, topAir);
                features.set(0, 2*col + 1, density);

                // if (col == 0) {
                //     System.out.println("\ntopAir is " + topAir + " and density is " + density);
                // }
            }

            // I wanna do something for how filled in the rows are

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
        // System.out.println("cur game idx: " + gameCounter.getCurrentGameIdx());
        // System.out.println("cur phase idx: " + gameCounter.getCurrentPhaseIdx());
        // System.out.println("tot game count: " + gameCounter.getNumTrainingGames());
        // System.out.println("phase count: " + gameCounter.getNumPhases());

        // make the exploration rate highest at the start of each phase and 
        // get the progress of this phase
        double gameProgress = (double) gameCounter.getCurrentGameIdx() / gameCounter.getNumTrainingGames();
        double phaseProgress = (double) gameCounter.getCurrentPhaseIdx() / gameCounter.getNumPhases();

        // System.out.println("pP: " + gameProgress + " tP: " + phaseProgress);

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
        // add forced teaching so we select a winning move if it exists

        int randIdx = this.getRandom().nextInt(game.getFinalMinoPositions().size());
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
        Board board = null;
        double reward = 0;

        try {
            board = game.getBoard();
            
            for (int col = 0; col < Board.NUM_COLS; col++) {
                int topAir = 0;
                // loop through top air blocks
                int row = 0;
                // this is using row is x and col is y
                while (board.isInBounds(new Coordinate(col, row)) && !board.isCoordinateOccupied(col, row)) {
                    topAir++;
                    row++;
                }

                double density = 1;
                // increment row and check if we went out of bounds
                row++;
                if (!board.isInBounds(new Coordinate(col, row))) {
                    density = 1;
                }
                else {
                    // loop through remaining blocks
                    int midAir = 0;
                    int midBlock = 0;
                    while (board.isInBounds(new Coordinate(col, row))) {
                        // air
                        if (!board.isCoordinateOccupied(col, row)) {
                            midAir++;
                        }
                        else {
                            midBlock++;
                        }
                        row++;
                    }
                    density = (double) midBlock / (midAir + midBlock);
                }

                // if (col == 0) {
                //     System.out.println("\ntopAir is " + topAir + " and density is " + density);
                // }

                // total reward will be the amount of air above the blocks but give a disadvantage if there's a lower density below the blocks
                reward += topAir * Math.pow(Math.E, density - 0.67);
                // reward += topAir * density;
            }

        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        // Make points scored very important for the reward function
        reward += 2048 * game.getScoreThisTurn();

        // System.out.println("Re- " + reward);
        return reward;
    }

}
