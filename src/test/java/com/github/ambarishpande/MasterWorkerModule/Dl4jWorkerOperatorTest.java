package com.github.ambarishpande.MasterWorkerModule;

/**
 * Created by @ambarishpande on 14/1/17.
 */

import com.datatorrent.lib.testbench.CollectorTestSink;
import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;


public class Dl4jWorkerOperatorTest {

//    private static Logger log = (Logger) LoggerFactory.getLogger(Dl4jWorkerOperatorTest.class);

    public MultiLayerConfiguration conf;
    public DataSet dataset;
    public IrisDataSetIterator dataSetIterator;
    public Dl4jWorkerOperator worker;
    private static CollectorTestSink<Object> sink;
    @Before
    public void setup()
    {
        dataSetIterator = new IrisDataSetIterator(1,150);

        conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(0.01)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(4).nOut(4)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER)
                        .nIn(4).nOut(3).build())
                .pretrain(false).backprop(true).build();

        worker = new Dl4jWorkerOperator();
        worker.setConf(conf);
        System.out.println("Before Setup function complete...");
        worker.setup(null);
        // Create a dummy sink to simulate the output port and set it as the output port of the operator
        sink = new CollectorTestSink<>();
        //TestUtils.setSink(wordCountOperator.output, sink);
        worker.output.setSink(sink);


    }

    @Test
    public void testProcess()
    {

        int windowId = 0;
        int batchSize = 10;
        System.out.println("Number of Examples" + dataSetIterator.numExamples());
        DataSet d;
        for (windowId = 0; windowId<15; windowId++)
        {
            System.out.println("Window : " + windowId);
            worker.beginWindow(windowId);
            for(int i = windowId*batchSize; i< windowId*batchSize+batchSize;i++)
            {
                d = dataSetIterator.next();

                System.out.println("Examples per Dataset : " + d.numExamples());
                System.out.println("Tuple No :" + i );
                worker.dataPort.process(d);
            }
            worker.endWindow();
            System.out.println("Received parameters from worker : " + sink.collectedTuples.toString());
        }


    }

    @Test
    public void testTrainedModel(){

        Evaluation eval = new Evaluation(3); //create an evaluation object with 10 possible classes
        dataSetIterator.reset();
        while(dataSetIterator.hasNext()){
            DataSet next = dataSetIterator.next();
            INDArray output = worker.model.output(next.getFeatureMatrix()); //get the networks prediction
            eval.eval(next.getLabels(), output); //check the prediction against the true class
        }
        System.out.println(eval.stats());


    }
}

