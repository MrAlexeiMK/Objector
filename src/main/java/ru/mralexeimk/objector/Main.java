package ru.mralexeimk.objector;

import ru.mralexeimk.objector.models.*;
import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.singletons.SettingsListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    //MNIST test
    private static List<List<Layer>> layers = new ArrayList<>(Arrays.asList(
            List.of(new InputLayer(1, 28, 0, LayerType.INPUT)),
            List.of(new FilterLayer(8, 26, 0, LayerType.FILTER)),
            List.of(new PullingLayer(8, 13, 0, LayerType.PULLING)),
            List.of(new FilterLayer(16, 10, 0, LayerType.FILTER)),
            List.of(new PullingLayer(16, 5, 0, LayerType.PULLING)),
            List.of(new NeuronsLayer(400, 1, LayerType.NEURONS)),
            List.of(new OutputLayer(10, 1, LayerType.OUTPUT))
    ));

    private static List<List<Layer>> layers2 = new ArrayList<>(Arrays.asList(
            List.of(new InputLayer(1, 28, 0, LayerType.INPUT)),
            List.of(new NeuronsLayer(150, 1, LayerType.NEURONS)),
            List.of(new OutputLayer(10, 1, LayerType.OUTPUT))
    ));

    public static void main(String[] args) {
        //SettingsListener.init();
        //mnistTest();
        MainApplication.main(args);
    }
    //93.68%
    public static void mnistTest() {
        NeuralNetwork nn = new NeuralNetwork(layers, 0.05);
        nn.printData();
        nn.printWeights();
        for(int i = 0; i < 30; ++i) nn.trainFromFile("/train/mnist_train_100.csv");
        nn.printData();
        nn.printWeights();
        nn.testFromFile("/test/mnist_test_10.csv");
        nn.saveWeights("mnist");
    }
}
