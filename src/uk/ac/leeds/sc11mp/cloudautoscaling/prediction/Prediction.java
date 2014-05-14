package uk.ac.leeds.sc11mp.cloudautoscaling.prediction;

import java.io.File;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.arrayutil.NormalizationAction;
import org.encog.util.arrayutil.NormalizedField;

/**
 * Prediction module.
 * @author Martynas Puronas <sc11mp@leeds.ac.uk>
 */
public class Prediction {

    private BasicNetwork network;
    private NormalizedField normalisedField;
    private int windowSize;
    
    private double dataMin;
    private double dataMax;
    private double normalisedMin;
    private double normalisedMax;
    
    private static Logger logger = Logger.getLogger(Prediction.class);
    
    /**
     * @param networkPath Path to Network saved as file by the Encog framework
     * @param windowSize Size of the sliding window
     * @param dataMin The smallest value in the data
     * @param dataMax The largest value in the data
     * @param normalisedMin The smallest normalised value
     * @param normalisedMax The largest normalised value
     */
    public Prediction(String networkPath, int windowSize, double dataMin, double dataMax, 
            double normalisedMin, double normalisedMax) {
        
        network = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File(networkPath));
        
        this.dataMin = dataMin;
        this.dataMax = dataMax;
        this.normalisedMin = normalisedMin;
        this.normalisedMax = normalisedMax;
        
        this.windowSize = windowSize;
        
        createNormalisedField();
    }
    
    /**
     * Feeds the values in the neural network and gets its output
     * @param samples Input values. The size of the array must be equal to the Window Size
     * @return predicted value
     */
    public double predict(double[] samples) {
        MLData data = new BasicMLData(windowSize);
        
        for (double d : samples) {
            if (d > dataMax) {
                dataMax = d;
                createNormalisedField();
            }
        }
        
        for (int i = 0; i < windowSize; i++) {
            data.setData(i, normalisedField.normalize(samples[i]));
        }
        
        MLData prediction = network.compute(data);
        
        double result = normalisedField.deNormalize(prediction.getData(0)); 
        
        logger.debug("Predicted output for " + Arrays.toString(samples) + " is " + result);
        
        return result;
    }

    public double getDataMin() {
        return dataMin;
    }

    public void setDataMin(double dataMin) {
        this.dataMin = dataMin;
    }

    public double getDataMax() {
        return dataMax;
    }

    public void setDataMax(double dataMax) {
        this.dataMax = dataMax;
    }

    private void createNormalisedField() {
        normalisedField = new NormalizedField(NormalizationAction.Normalize, null, dataMax, dataMin, normalisedMax, normalisedMin);        
    }
    
}
