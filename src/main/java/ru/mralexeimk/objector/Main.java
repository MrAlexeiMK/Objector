package ru.mralexeimk.objector;

import ru.mralexeimk.objector.models.*;
import ru.mralexeimk.objector.other.LayerType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static List<Layer> layers = new ArrayList<>(Arrays.asList(
            new InputLayer(1, 28, LayerType.INPUT),
            new FilterLayer(8, 24, LayerType.FILTER),
            new PullingLayer(8, 12, LayerType.PULLING),
            new FilterLayer(32, 8, LayerType.FILTER),
            new PullingLayer(32, 4, LayerType.PULLING),
            new NeuronsLayer(32, 1, LayerType.NEURONS),
            new OutputLayer(10, 1, LayerType.OUTPUT)
    ));

    private static List<Layer> layers2 = new ArrayList<>(Arrays.asList(
            new InputLayer(1, 28, LayerType.INPUT),
            new NeuronsLayer(200, 1, LayerType.NEURONS),
            new OutputLayer(10, 1, LayerType.OUTPUT)
    ));
    public static void main(String[] args) {
        //MainApplication.main(args);
        NeuralNetwork nn = new NeuralNetwork(layers, 0.1);
        for(int i = 0; i < 5; ++i) nn.trainFromFile("/train/mnist_train_100.csv");
        nn.testFromFile("/test/mnist_test_10.csv");
        //nn.printData();
        //nn.printWeights();
    }
}
