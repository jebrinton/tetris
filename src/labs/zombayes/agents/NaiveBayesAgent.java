package src.labs.zombayes.agents;


// SYSTEM IMPORTS
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.labs.zombayes.agents.SurvivalAgent;
import edu.bu.labs.zombayes.features.Features.FeatureType;
import edu.bu.labs.zombayes.linalg.Matrix;
import edu.bu.labs.zombayes.utils.Pair;



public class NaiveBayesAgent
    extends SurvivalAgent
{

    public static class NaiveBayes
        extends Object
    {

        public static final FeatureType[] FEATURE_HEADER = {FeatureType.CONTINUOUS,
                                                            FeatureType.CONTINUOUS,
                                                            FeatureType.DISCRETE,
                                                            FeatureType.DISCRETE};

        private Map<Integer, Double> priorProbabilities;
        private Map<Integer, Map<Integer, double[]>> gaussianParameters; // Mean and standard deviation for each feature and class

        /*
         * each row of matrix X corresponds to a human/zombie
         * each column is the feature of that human/zombie
         * and y_gt is the matrix ground truth where 0 represents not a zombie and 1 represents a zombie so:
         * each row of y_gt tells u whether or not a row in the original matrix X is a zombie or human
         * 
         * feature 3 has a domain of {0, 1, 2}
         * feature 4 has a domain of {0, 1, 2, 3}
         */
        // TODO: complete me!
        public NaiveBayes()
        {
            priorProbabilities = new HashMap<>();
            gaussianParameters = new HashMap<>();
        }

        // TODO: complete me!
        public void fit(Matrix X, Matrix y_gt)
        {
            // convert matrix X to array
            int rowDim = X.getShape().getNumRows();
            int colDim = X.getShape().getNumCols();

            double[][] x = new double[rowDim][colDim];

            for (int i = 0; i < rowDim; i++) {
                for (int j = 0; j < colDim; j++) {
                    x[i][j] = X.get(i, j);
                }
            }

            // convert vector y_gt to array
            int y_gt_length = y_gt.getShape().getNumRows();

            int[] yGt = new int[y_gt_length];

            for (int i = 0; i < yGt.length; i++) {
                yGt[i] = (int) y_gt.get(i, 0);
            }

            // int numSamples = x.length;
            // just do first two samples for now
            int numSamples = 2;
            int numFeatures = x[0].length;

            // Calculate prior probabilities
            for (int i = 0; i < numSamples; i++) {
                int className = yGt[i];
                priorProbabilities.put(className, priorProbabilities.getOrDefault(className, 0.0) + 1.0);
            }
            for (int className : priorProbabilities.keySet()) {
                priorProbabilities.put(className, priorProbabilities.get(className) / numSamples);
            }

            // Calculate Gaussian parameters (mean and standard deviation) for each feature and class
            for (int i = 0; i < numSamples; i++) {
                int className = yGt[i];
                if (!gaussianParameters.containsKey(className)) {
                    gaussianParameters.put(className, new HashMap<>());
                }
                for (int j = 0; j < numFeatures; j++) {
                    if (!gaussianParameters.get(className).containsKey(j)) {
                        gaussianParameters.get(className).put(j, new double[2]); // 0: mean, 1: standard deviation
                    }
                    gaussianParameters.get(className).get(j)[0] += x[i][j]; // Accumulate sum for mean calculation
                }
            }
            for (int className : gaussianParameters.keySet()) {
                for (int j = 0; j < numFeatures; j++) {
                    double[] parameters = gaussianParameters.get(className).get(j);
                    parameters[0] /= priorProbabilities.get(className); // Calculate mean
                    for (int i = 0; i < numSamples; i++) {
                        parameters[1] += Math.pow(x[i][j] - parameters[0], 2); // Accumulate sum for variance calculation
                    }
                    parameters[1] = Math.sqrt(parameters[1] / priorProbabilities.get(className)); // Calculate standard deviation
                }
            }

            // // This'll update the values of the classifier using the training data + ground truth
            // System.out.print(X.toString() + y_gt);

            // // Filter the rows so we have a matrix of human data and a matrix of zombie data
            // // Make the rowMask
            // Matrix humanRowMask = y_gt.getRowMaskEq(0.0, 0);
            // // Matrix zombieRowMask = y_gt.getRowMaskEq(1.0, 0);

            // Matrix humanMatrix = null;
            // try {
            //     humanMatrix = X.filterRows(humanRowMask);
            // } catch (Exception e) {
            //     // TODO: handle exception
            //     System.out.println(e);
            // }

            // // System.out.print(humanMatrix);
            
            // Matrix col0 = humanMatrix.getCol(0);
            // Matrix col1 = humanMatrix.getCol(1);

            // System.out.print(col0.toString() + col1);
            return;
        }

        // TODO: complete me!
        public int predict(Matrix x)
        {
            int sampleLength = x.getShape().getNumRows();
            // change to 2 for now
            sampleLength = 2;
            double[] sample = new double[sampleLength];

            double maxPosterior = Double.NEGATIVE_INFINITY;
            int predictedClass = -1;
            for (int className : priorProbabilities.keySet()) {
                double posterior = Math.log(priorProbabilities.get(className));
                for (int i = 0; i < sample.length; i++) {
                    double[] parameters = gaussianParameters.get(className).get(i);
                    double mean = parameters[0];
                    double stdDev = parameters[1];
                    posterior += Math.log(gaussianProbability(sample[i], mean, stdDev));
                }
                if (posterior > maxPosterior) {
                    maxPosterior = posterior;
                    predictedClass = className;
                }
            }
            return predictedClass;
        }

        // calculate prob using gaussian formula
        private double gaussianProbability(double x, double mean, double stdDev) {
            return Math.exp(-Math.pow(x - mean, 2) / (2 * Math.pow(stdDev, 2))) / (stdDev * Math.sqrt(2 * Math.PI));
        }

    }
    
    private NaiveBayes model;

    public NaiveBayesAgent(int playerNum, String[] args)
    {
        super(playerNum, args);
        this.model = new NaiveBayes();
    }

    public NaiveBayes getModel() { return this.model; }

    @Override
    public void train(Matrix X, Matrix y_gt)
    {
        System.out.println(X.getShape() + " " + y_gt.getShape());
        this.getModel().fit(X, y_gt);
    }

    @Override
    public int predict(Matrix featureRowVector)
    {
        return this.getModel().predict(featureRowVector);
    }

}
