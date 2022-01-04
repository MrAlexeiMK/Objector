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
    private Pair<Integer, Integer> webCamQuality;
    private boolean onlyMoving;
    private List<Layer> configuration;

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
        this.webCamQuality = settings.webCamQuality;
    }

    public void toDefault() {
        rewriteWeights = false;
        onlyMoving = true;
        webCamQuality = new Pair<>(160, 90);
        toDefaultConfiguration();
    }

    public void toDefaultConfiguration() {
        configuration = new ArrayList<>(Arrays.asList(
                new InputLayer(1, 28, LayerType.INPUT),
                new FilterLayer(8, 24, LayerType.FILTER),
                new PullingLayer(8, 12, LayerType.PULLING),
                new FilterLayer(32, 8, LayerType.FILTER),
                new PullingLayer(32, 4, LayerType.PULLING),
                new NeuronsLayer(512, 1, LayerType.NEURONS),
                new OutputLayer(1, 1, LayerType.OUTPUT)
        ));
    }

    public List<Layer> getConfigurationLayers() {
        return configuration;
    }

    public String getConfiguration() {
        String res = "";
        for(Layer layer : configuration) {
            res += layer.toString() + "\n";
        }
        return res;
    }

    public boolean setConfiguration(String conf) {
        try {
            String[] arr = conf.split("\n");
            configuration = new ArrayList<>();
            for (String row : arr) {
                int units = Integer.parseInt(row.split("@")[0]);
                int size = Integer.parseInt(row.split("x")[0].split("@")[1]);
                String layer = row.split(", ")[1];
                if(layer.equalsIgnoreCase("INPUT")) {
                    configuration.add(new InputLayer(units, size, LayerType.INPUT));
                }
                else if(layer.equalsIgnoreCase("FILTER")) {
                    configuration.add(new FilterLayer(units, size, LayerType.FILTER));
                }
                else if(layer.equalsIgnoreCase("PULLING")) {
                    configuration.add(new PullingLayer(units, size, LayerType.PULLING));
                }
                else if(layer.equalsIgnoreCase("NEURONS")) {
                    configuration.add(new NeuronsLayer(units, size, LayerType.NEURONS));
                }
                else if(layer.equalsIgnoreCase("OUTPUT")) {
                    configuration.add(new OutputLayer(units, size, LayerType.OUTPUT));
                }
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
