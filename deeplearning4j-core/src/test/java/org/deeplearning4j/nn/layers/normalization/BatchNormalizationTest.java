package org.deeplearning4j.nn.layers.normalization;

import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.layers.factory.LayerFactories;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.params.BatchNormalizationParamInitializer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class BatchNormalizationTest {
    protected INDArray dnnInput = Nd4j.create(new double[] {
            4.,4.,4.,4.,8.,8.,8.,8.,4.,4.,4.,4.,8.,8.,8.,8.,
            2.,2.,2.,2.,4.,4.,4.,4.,2.,2.,2.,2.,4.,4.,4.,4.
    },new int[]{2, 16});


    protected INDArray dnnEpsilon = Nd4j.create(new double[] {
            1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,
            -1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,
    },new int[]{2, 16});


    protected INDArray cnnInput = Nd4j.create(new double[] {
            4.,4.,4.,4.,8.,8.,8.,8.,4.,4.,4.,4.,8.,8.,8.,8.,4.,4.
            ,4.,4.,8.,8.,8.,8.,4.,4.,4.,4.,8.,8.,8.,8,
            2.,2.,2.,2.,4.,4.,4.,4.,2.,2.,2.,2.,4.,4.,4.,4.,
            2.,2.,2.,2.,4.,4.,4.,4.,2.,2.,2.,2.,4.,4.,4.,4.
    },new int[]{2, 2, 4, 4});


    protected INDArray cnnEpsilon = Nd4j.create(new double[] {
            1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,
            1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,1.,
            -1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,
            -1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,-1.,
    },new int[]{2, 2, 4, 4});

    @Before
    public void doBefore() {
    }

    protected Layer setupActivations(int nIn, int nOut){
        BatchNormalization bN = new BatchNormalization.Builder().nIn(nIn).nOut(nOut).build();
        NeuralNetConfiguration layerConf = new NeuralNetConfiguration.Builder()
                .iterations(1).layer(bN).build();
        return LayerFactories.getFactory(layerConf).create(layerConf);
    }

    @Test
    public void testCnnBatchNormForward() {
        Layer layer = setupActivations(2*4*4, 2);
        INDArray cnnActivationsActual = layer.preOutput(cnnInput);
        INDArray activationsExpected = cnnEpsilon;
        assertEquals(activationsExpected, cnnActivationsActual);
        assertArrayEquals(activationsExpected.shape(), cnnActivationsActual.shape());
    }

    @Test
    public void testCnnBatchNormBack(){
        Layer layer = setupActivations(2*4*4, 2);
        layer.preOutput(cnnInput);
        Pair<Gradient, INDArray> actualOut = layer.backpropGradient(cnnEpsilon);

        INDArray expectedEpsilonOut = Nd4j.create(new double[] {
                0. ,  0. ,  0. ,  0. , -0.5, -0.5, -0.5, -0.5,  0. ,  0. ,  0. ,
                0. , -0.5, -0.5, -0.5, -0.5,  0. ,  0. ,  0. ,  0. , -0.5, -0.5,
                -0.5, -0.5,  0. ,  0. ,  0. ,  0. , -0.5, -0.5, -0.5, -0.5, -2. ,
                -2. , -2. , -2. , -1.5, -1.5, -1.5, -1.5, -2. , -2. , -2. , -2. ,
                -1.5, -1.5, -1.5, -1.5, -2. , -2. , -2. , -2. , -1.5, -1.5, -1.5,
                -1.5, -2. , -2. , -2. , -2. , -1.5, -1.5, -1.5, -1.5
        },new int[]{2, 2, 4, 4});

//        INDArray expectedGGamma = Nd4j.create(new double[]
//                { 2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,
//                  2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.,2.
//                }, new int[] {1, 32});
//
//        INDArray expectedBeta = Nd4j.create(new double[]
//                { 0.}, new int[] {1, 1});

        // arrays are the same assert does not see that
        assertEquals(expectedEpsilonOut, actualOut.getSecond());
//        assertEquals(expectedBeta, actualOut.getFirst().getGradientFor("beta"));
//        assertEquals(expectedGGamma, actualOut.getFirst().getGradientFor("gamma"));

    }

    @Test
    public void testMultiCNNLayer() throws Exception {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
                .iterations(1)
                .seed(123)
                .list()
                .layer(0, new ConvolutionLayer.Builder().nIn(1).nOut(6).weightInit(WeightInit.XAVIER).activation("relu").build())
                .layer(1, new BatchNormalization.Builder().build())
                .layer(2, new DenseLayer.Builder().nOut(2).build())
                .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .weightInit(WeightInit.XAVIER)
                        .activation("softmax")
                        .nIn(2).nOut(10).build())
                .backprop(true).pretrain(false)
                .cnnInputSize(28,28,1)
                .build();
        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();
        DataSetIterator iter = new MnistDataSetIterator(2, 2);
        DataSet next = iter.next();

        network.setInput(next.getFeatureMatrix());
        INDArray activationsActual = network.preOutput(next.getFeatureMatrix());
        assertEquals(10, activationsActual.shape()[1], 1e-2);

        network.fit(next);
        INDArray actualGammaParam = network.getLayer(1).getParam(BatchNormalizationParamInitializer.GAMMA);
        INDArray actualBetaParam = network.getLayer(1).getParam(BatchNormalizationParamInitializer.BETA);
        assertTrue(actualGammaParam != null);
        assertTrue(actualBetaParam != null);


    }

}
