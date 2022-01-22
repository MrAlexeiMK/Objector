package ru.mralexeimk.objector.models;

import lombok.Data;
import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.other.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class Settings implements Serializable {
    private boolean rewriteWeights;
    private Pair<Integer, Integer> webCamQualityTrain;
    private Pair<Integer, Integer> webCamQualityQuery;
    private boolean onlyMoving, detectColors;
    private List<List<Layer>> configuration;
    private double lr;
    private boolean severalObjects;
    private int exampleSelected;
    private double separate;
    private boolean isSeparated;
    private boolean isDefaultKernels;
    private boolean trainKernels;

    public Settings() {
        loadFromFile();
    }

    public Settings(Settings settings) {
        load(settings);
    }

    public void load(Settings settings) {
        this.rewriteWeights = settings.rewriteWeights;
        this.configuration = settings.configuration;
        this.onlyMoving = settings.onlyMoving;
        this.webCamQualityTrain = settings.webCamQualityTrain;
        this.webCamQualityQuery = settings.webCamQualityQuery;
        this.detectColors = settings.detectColors;
        this.severalObjects = settings.severalObjects;
        this.lr = settings.lr;
        this.separate = settings.separate;
        this.isSeparated = settings.isSeparated;
        this.isDefaultKernels = settings.isDefaultKernels;
        this.trainKernels = settings.trainKernels;
    }

    public void toDefault() {
        rewriteWeights = false;
        onlyMoving = false;
        detectColors = true;
        severalObjects = false;
        webCamQualityTrain = new Pair<>(320, 180);
        webCamQualityQuery = new Pair<>(320, 180);
        isSeparated = true;
        isDefaultKernels = true;
        trainKernels = true;
        lr = 0.05;
        separate = 0.7;
        exampleSelected = 7;
        toDefaultConfiguration();
    }

    public void chooseDetermined() {
        if(!detectColors) {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 38, 0, LayerType.INPUT)),
                    List.of(new FilterLayer(8, 36, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(8, 18, 0, LayerType.PULLING)),
                    List.of(new FilterLayer(16, 16, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(16, 8, 0, LayerType.PULLING)),
                    List.of(new NeuronsLayer(1024, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
        else {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 38, 0, LayerType.INPUT),
                            new InputLayer(1, 38, 1, LayerType.INPUT),
                            new InputLayer(1, 38, 2, LayerType.INPUT)),
                    List.of(new FilterLayer(8, 36, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(8, 18, 0, LayerType.PULLING)),
                    List.of(new FilterLayer(16, 16, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(16, 8, 0, LayerType.PULLING)),
                    List.of(new NeuronsLayer(1024, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
    }

    public void chooseAdvanced() {
        if(!detectColors) {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 60, 0, LayerType.INPUT)),
                    List.of(new FilterLayer(8, 40, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(8, 20, 0, LayerType.PULLING)),
                    List.of(new FilterLayer(16, 12, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(16, 6, 0, LayerType.PULLING)),
                    List.of(new NeuronsLayer(576, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
        else {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 60, 0, LayerType.INPUT),
                            new InputLayer(1, 60, 1, LayerType.INPUT),
                            new InputLayer(1, 60, 2, LayerType.INPUT)),
                    List.of(new FilterLayer(8, 40, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(8, 20, 0, LayerType.PULLING)),
                    List.of(new FilterLayer(16, 12, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(16, 6, 0, LayerType.PULLING)),
                    List.of(new NeuronsLayer(576, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
    }

    public void chooseConv() {
        if(!detectColors) {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 28, 0, LayerType.INPUT)),
                    List.of(new FilterLayer(8, 24, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(8, 12, 0, LayerType.PULLING)),
                    List.of(new FilterLayer(16, 8, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(16, 4, 0, LayerType.PULLING)),
                    List.of(new NeuronsLayer(256, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
        else {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 28, 0, LayerType.INPUT),
                            new InputLayer(1, 28, 1, LayerType.INPUT),
                            new InputLayer(1, 28, 2, LayerType.INPUT)),
                    List.of(new FilterLayer(8, 24, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(8, 12, 0, LayerType.PULLING)),
                    List.of(new FilterLayer(16, 8, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(16, 4, 0, LayerType.PULLING)),
                    List.of(new NeuronsLayer(256, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
    }

    public void chooseDefault() {
        if(!detectColors) {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 28, 0, LayerType.INPUT)),
                    List.of(new NeuronsLayer(200, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
        else {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 28, 0, LayerType.INPUT)),
                    List.of(new NeuronsLayer(600, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
    }

    public void selectConfiguration(int index) {
        setExampleSelected(index);
        switch (index) {
            case 0 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = false;
                severalObjects = false;
                trainKernels = true;
                chooseConv();
            }
            case 1 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = true;
                severalObjects = false;
                trainKernels = true;
                chooseConv();
            }
            case 2 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = false;
                severalObjects = false;
                trainKernels = true;
                chooseDefault();
            }
            case 3 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = true;
                severalObjects = false;
                trainKernels = true;
                chooseDefault();
            }
            case 4 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = false;
                severalObjects = false;
                trainKernels = true;
                chooseAdvanced();
            }
            case 5 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = true;
                severalObjects = false;
                trainKernels = true;
                chooseAdvanced();
            }
            case 6 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = false;
                severalObjects = false;
                trainKernels = true;
                chooseDetermined();
            }
            case 7 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = true;
                severalObjects = false;
                trainKernels = true;
                chooseDetermined();
            }
            case 8 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = false;
                severalObjects = false;
                trainKernels = false;
                chooseDetermined();
            }
            case 9 -> {
                webCamQualityTrain = new Pair<>(320, 180);
                webCamQualityQuery = new Pair<>(320, 180);
                detectColors = true;
                severalObjects = false;
                trainKernels = false;
                chooseDetermined();
            }
        }
    }

    public void toDefaultConfiguration() {
        if(!detectColors) {
            selectConfiguration(6);
        }
        else {
            selectConfiguration(7);
        }
    }

    public List<List<Layer>> getConfigurationLayers() {
        return configuration;
    }

    public String getConfiguration() {
        String res = "";
        for(List<Layer> layers : configuration) {
            for(int i = 0; i < layers.size(); ++i) {
                Layer layer = layers.get(i);
                res += layer.toString();
                if(i < layers.size()-1) res += " + ";
            }
            res += "\n";
        }
        return res;
    }

    public boolean setConfiguration(String conf) {
        try {
            String[] arr = conf.split("\n");
            configuration = new ArrayList<>();
            for (String lay : arr) {
                List<Layer> ll = new ArrayList<>();
                String[] spl = lay.split(" \\+ ");
                for(int index = 0; index < spl.length; ++index) {
                    String row = spl[index];
                    int units = Integer.parseInt(row.split("@")[0]);
                    int size = Integer.parseInt(row.split("x")[0].split("@")[1]);
                    String layer = row.split(", ")[1];
                    if (layer.equalsIgnoreCase("INPUT")) {
                        ll.add(new InputLayer(units, size, index, LayerType.INPUT));
                    } else if (layer.equalsIgnoreCase("FILTER")) {
                        ll.add(new FilterLayer(units, size, index, LayerType.FILTER));
                    } else if (layer.equalsIgnoreCase("PULLING")) {
                        ll.add(new PullingLayer(units, size, index, LayerType.PULLING));
                    } else if (layer.equalsIgnoreCase("NEURONS")) {
                        ll.add(new NeuronsLayer(units, size, LayerType.NEURONS));
                    } else if (layer.equalsIgnoreCase("OUTPUT")) {
                        ll.add(new OutputLayer(units, size, LayerType.OUTPUT));
                    }
                }
                configuration.add(ll);
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public void saveToFile() {
        File file = new File("settings.objector");
        try {
            file.createNewFile();
        } catch (Exception ignored) {}

        try (FileOutputStream f = new FileOutputStream(file.getPath()); ObjectOutputStream o = new ObjectOutputStream(f)) {
            o.writeObject(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFromFile() {
        File file = new File("settings.objector");
        try(FileInputStream fi = new FileInputStream(file.getPath()); ObjectInputStream oi = new ObjectInputStream(fi)) {
            Settings settings = (Settings) oi.readObject();
            load(settings);
        } catch (Exception e) {
            toDefault();
            saveToFile();
        }
    }
}
