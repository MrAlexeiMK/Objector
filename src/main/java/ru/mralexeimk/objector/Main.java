package ru.mralexeimk.objector;

import ru.mralexeimk.objector.models.*;
import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.singletons.Objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    //MNIST test
    private static List<Layer> layers = new ArrayList<>(Arrays.asList(
            new InputLayer(1, 28, LayerType.INPUT),
            new FilterLayer(8, 24, LayerType.FILTER),
            new PullingLayer(8, 12, LayerType.PULLING),
            new FilterLayer(32, 8, LayerType.FILTER),
            new PullingLayer(32, 4, LayerType.PULLING),
            new NeuronsLayer(512, 1, LayerType.NEURONS),
            new OutputLayer(10, 1, LayerType.OUTPUT)
    ));

    private static List<Layer> layers2 = new ArrayList<>(Arrays.asList(
            new InputLayer(1, 28, LayerType.INPUT),
            new NeuronsLayer(60, 1, LayerType.NEURONS),
            new OutputLayer(10, 1, LayerType.OUTPUT)
    ));
    public static void main(String[] args) {
        //MainApplication.main(args);
        mnistTestMultiNet();
    }

    public static void mnistTest() {
        NeuralNetwork nn = new NeuralNetwork(layers, 0.2);
        nn.printData();
        nn.printWeights();
        for(int i = 0; i < 50; ++i) nn.trainFromFile("/train/mnist_train_100.csv");
        nn.printData();
        nn.printWeights();
        nn.testFromFile("/test/mnist_test_10.csv");
    }

    public static void mnistTestMultiNet() {
        Objects.init("MNIST");
        Objects.addAllObjects(new ArrayList<>(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")));
        for(int i = 0; i < 100; ++i) Objects.trainFromFile("/train/mnist_train_100.csv");
        Objects.testFromFile("/test/mnist_test_10.csv");
        Objects.saveToFilesCurrentThread();
    }
}
