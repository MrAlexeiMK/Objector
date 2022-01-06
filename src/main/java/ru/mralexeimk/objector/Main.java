package ru.mralexeimk.objector;

import ru.mralexeimk.objector.models.*;
import ru.mralexeimk.objector.other.LayerType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    //MNIST test
    private static List<List<Layer>> layers = new ArrayList<>(Arrays.asList(
            List.of(new InputLayer(1, 28, 0, LayerType.INPUT)),
            List.of(new FilterLayer(8, 24, 0, LayerType.FILTER)),
            List.of(new PullingLayer(8, 12, 0, LayerType.PULLING)),
            List.of(new FilterLayer(32, 8, 0, LayerType.FILTER)),
            List.of(new PullingLayer(32, 4, 0, LayerType.PULLING)),
            List.of(new NeuronsLayer(512, 1, LayerType.NEURONS)),
            List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
    ));

    private static List<List<Layer>> layers2 = new ArrayList<>(Arrays.asList(
            List.of(new InputLayer(1, 28, 0, LayerType.INPUT)),
            List.of(new NeuronsLayer(120, 1, LayerType.NEURONS)),
            List.of(new OutputLayer(10, 1, LayerType.OUTPUT))
    ));

    public static void main(String[] args) {
        MainApplication.main(args);
        //mnistTest();
    }

    public static void mnistTest() {
        NeuralNetwork nn = new NeuralNetwork(layers, 0.1);
        nn.printData();
        nn.printWeights();
        for(int i = 0; i < 10; ++i) nn.trainFromFile("/train/mnist_train.csv");
        nn.printData();
        nn.printWeights();
        nn.testFromFile("/test/mnist_test.csv");
        nn.saveWeights("mnist");
    }
}
