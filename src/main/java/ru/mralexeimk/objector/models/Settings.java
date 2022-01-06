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
    }

    public void toDefault() {
        rewriteWeights = false;
        onlyMoving = false;
        detectColors = false;
        severalObjects = false;
        webCamQualityTrain = new Pair<>(160, 90);
        webCamQualityQuery = new Pair<>(160, 90);
        lr = 0.1;
        exampleSelected = 1;
        toDefaultConfiguration();
    }

    public void chooseConv(boolean isColored) {
        if(!isColored) {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 28, 0, LayerType.INPUT)),
                    List.of(new FilterLayer(8, 24, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(8, 12, 0, LayerType.PULLING)),
                    List.of(new FilterLayer(32, 8, 0, LayerType.FILTER)),
                    List.of(new PullingLayer(32, 4, 0, LayerType.PULLING)),
                    List.of(new NeuronsLayer(512, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
        else {
            configuration = new ArrayList<>(Arrays.asList(
                    List.of(new InputLayer(1, 28, 0, LayerType.INPUT),
                            new InputLayer(1, 28, 1, LayerType.INPUT),
                            new InputLayer(1, 28, 2, LayerType.INPUT)),
                    List.of(new FilterLayer(8, 24, 0, LayerType.FILTER),
                            new FilterLayer(8, 24, 1, LayerType.FILTER),
                            new FilterLayer(8, 24, 2, LayerType.FILTER)),
                    List.of(new PullingLayer(8, 12, 0, LayerType.PULLING),
                            new PullingLayer(8, 12, 1, LayerType.PULLING),
                            new PullingLayer(8, 12, 2, LayerType.PULLING)),
                    List.of(new FilterLayer(32, 8, 0, LayerType.FILTER),
                            new FilterLayer(32, 8, 1, LayerType.FILTER),
                            new FilterLayer(32, 8, 2, LayerType.FILTER)),
                    List.of(new PullingLayer(32, 4, 0, LayerType.PULLING),
                            new PullingLayer(32, 4, 1, LayerType.PULLING),
                            new PullingLayer(32, 4, 2, LayerType.PULLING)),
                    List.of(new NeuronsLayer(1536, 1, LayerType.NEURONS)),
                    List.of(new OutputLayer(1, 1, LayerType.OUTPUT))
            ));
        }
    }

    public void chooseDefault(boolean isColored) {
        if(!isColored) {
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
            case 0:
                chooseConv(false);
                webCamQualityTrain = new Pair<>(160, 90);
                webCamQualityQuery = new Pair<>(160, 90);
                detectColors = false;
                severalObjects = false;
                break;
            case 1:
                chooseConv(true);
                webCamQualityTrain = new Pair<>(160, 90);
                webCamQualityQuery = new Pair<>(160, 90);
                detectColors = true;
                severalObjects = false;
                break;
            case 2:
                chooseDefault(false);
                webCamQualityTrain = new Pair<>(160, 90);
                webCamQualityQuery = new Pair<>(160, 90);
                detectColors = false;
                severalObjects = false;
                break;
            case 3:
                chooseDefault(true);
                webCamQualityTrain = new Pair<>(160, 90);
                webCamQualityQuery = new Pair<>(160, 90);
                detectColors = true;
                severalObjects = false;
                break;
            case 4:
                chooseConv(false);
                webCamQualityTrain = new Pair<>(180, 180);
                webCamQualityQuery = new Pair<>(1440, 810);
                detectColors = false;
                severalObjects = true;
                break;
            case 5:
                chooseConv(true);
                webCamQualityTrain = new Pair<>(180, 180);
                webCamQualityQuery = new Pair<>(1440, 810);
                detectColors = true;
                severalObjects = true;
                break;
        }
    }

    public void toDefaultConfiguration() {
        if(!detectColors) {
            selectConfiguration(0);
        }
        else {
            selectConfiguration(1);
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
