package ru.mralexeimk.objector.models;

import lombok.Data;
import ru.mralexeimk.objector.other.LayerType;

import java.io.*;
import java.util.*;

@Data
public class NeuralNetwork implements Serializable {
    private double lr;
    private List<Layer> layers;

    private final List<Layer> defaultLayers = new ArrayList<>(Arrays.asList(
            new InputLayer(1, 32, LayerType.INPUT),
            new FilterLayer(8, 28, LayerType.FILTER),
            new PullingLayer(8, 14, LayerType.PULLING),
            new FilterLayer(128, 10, LayerType.FILTER),
            new PullingLayer(128, 5, LayerType.PULLING),
            new NeuronsLayer(128, 1, LayerType.NEURONS),
            new NeuronsLayer(80, 1, LayerType.NEURONS),
            new OutputLayer(1, 1, LayerType.OUTPUT)
    ));
    private final double defaultLr = 0.1;

    public NeuralNetwork(List<Layer> layers, double lr) {
        load(layers, lr);
    }

    public NeuralNetwork(double lr) {
        this.layers = defaultLayers;
        this.lr = lr;
        init();
    }

    public NeuralNetwork() {
        this.layers = defaultLayers;
        this.lr = defaultLr;
        init();
    }

    public void load(List<Layer> layers, double lr) {
        this.layers = layers;
        this.lr = lr;
        init();
    }

    public void init() {
        for(int i = 0; i < layers.size(); ++i) {
            if(i+1 < layers.size()) {
                layers.get(i).setNextLayer(layers.get(i + 1));
            }
            layers.get(i).toDefault();
        }
    }

    public NeuralNetwork(String category, String id) {
        loadWeights(category, id);
    }

    public double getLearningRate() {
        return lr;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public void setInputLayerData(List<Double> data) {
        getInputLayer().setData(data);
    }

    public InputLayer getInputLayer() {
        return (InputLayer) layers.get(0);
    }

    public OutputLayer getOutputLayer() {
        return (OutputLayer) layers.get(layers.size()-1);
    }

    public void train(List<Double> input_list, List<Double> target_list) {
        train(new Matrix(input_list), new Matrix(target_list));
    }

    public void train(Matrix inputs, Matrix targets) {
        setInputLayerData(inputs.toList());
        for(Layer layer : getLayers()) {
            layer.evaluate();
        }
        Matrix outputs = new Matrix(getOutputLayer().getData());
        Matrix errors = targets.minus(outputs);
        for(int i = getLayers().size()-2; i >= 0; --i) {
            Layer layer = getLayers().get(i);
            Layer nextLayer = layer.getNextLayer();
            if(layer instanceof NeuronsLayer nl) {
                Matrix I = new Matrix(nl.getData());
                Matrix O = null;
                if(nextLayer instanceof OutputLayer ol) {
                    O = new Matrix(ol.getData());
                }
                else if(nextLayer instanceof NeuronsLayer nl2) {
                    O = new Matrix(nl2.getData());
                }
                if(O != null) {
                    Matrix dif = errors.multiply(O).multiply(
                            O.getNegative().sum(1).multiply(
                                    I.getTranspose().multiply(lr)
                            )
                    );
                    nl.setW(nl.getW().sum(dif));
                    errors = nl.getW().getTranspose().multiply(errors);
                }
            }
            else if(layer instanceof PullingLayer pl) {
                if(nextLayer instanceof FilterLayer fl) {
                    int connections = fl.getUnits()/pl.getUnits();
                    List<Double> new_errors = new ArrayList<>();
                    for(int unit = 0; unit < pl.getUnits(); ++unit) {
                        Matrix I = new Matrix(pl.getData().get(unit).toList());
                        double average = 0;
                        for(int kernel = 0; kernel < connections; ++kernel) {
                            int val = unit*connections + kernel;
                            Matrix K = pl.getWW(unit, kernel);
                            Matrix O = new Matrix(fl.getData().get(val).toList());
                            double error = errors.get(0, val);
                            average += error;
                            Matrix dif = O.multiply(
                                    O.getNegative().sum(1).multiply(
                                            I.getTranspose().multiply(lr*error)
                                    )
                            );
                            K = K.sum(dif);
                            pl.setWW(unit, kernel, K);

                        }
                        average /= connections;
                        new_errors.add(average);
                    }
                    errors = new Matrix(new_errors);
                }
            }
            else if(layer instanceof InputLayer il) {
                if(nextLayer instanceof FilterLayer fl) {
                    int connections = fl.getUnits();
                    Matrix I = new Matrix(il.getData().toList());
                    for(int kernel = 0; kernel < connections; ++kernel) {
                        Matrix O = new Matrix(fl.getData().get(kernel).toList());
                        Matrix K = il.getW().get(kernel);
                        double error = errors.get(0, kernel);
                        Matrix dif = O.multiply(
                                O.getNegative().sum(1).multiply(
                                        I.getTranspose().multiply(lr*error)
                                )
                        );
                        K = K.sum(dif);
                        il.setW(kernel, K);
                    }
                }
            }
        }
    }

    public List<Double> query(List<Double> input_list) {
        setInputLayerData(input_list);
        for(Layer layer : getLayers()) {
            layer.evaluate();
        }
        return getOutputLayer().getData();
    }

    public List<Double> query(Matrix inputs) {
        return query(inputs.toList());
    }

    public int queryMax(List<Double> input_list) {
        List<Double> list = query(input_list);
        int res = 0;
        double max = 0;
        for(int i = 0; i < list.size(); ++i) {
            if(list.get(i) > max) {
                max = list.get(i);
                res = i;
            }
        }
        return res;
    }

    public void trainFromFile(String path) {
        try {
            File file = new File(getClass().getResource(path).toURI());
            Scanner sc = new Scanner(file);
            String line;
            int j = 0;
            while(sc.hasNextLine()) {
                line = sc.nextLine();
                String[] spl = line.split(",");
                int target = Integer.parseInt(spl[0]);
                List<Double> inputs = new ArrayList<>();
                for(int i = 1; i < spl.length; ++i) {
                    double t = Double.parseDouble(spl[i]);
                    t = (t/255.0)*0.99 + 0.01;
                    inputs.add(t);
                }
                List<Double> targets = new ArrayList<>(Collections.nCopies(getOutputLayer().getUnits(), 0.01));
                targets.set(target, 0.99);
                train(inputs, targets);
                ++j;
                System.out.println(j);
            }
            sc.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void testFromFile(String path) {
        try {
            File file = new File(getClass().getResource(path).toURI());
            Scanner sc = new Scanner(file);
            String line;
            int j = 0;
            int count = 0;
            int count_correct = 0;
            while(sc.hasNextLine()) {
                line = sc.nextLine();
                ++count;
                String[] spl = line.split(",");
                int correct = Integer.parseInt(spl[0]);
                List<Double> inputs = new ArrayList<>();
                for(int i = 1; i < spl.length; ++i) {
                    double t = Double.parseDouble(spl[i]);
                    t = (t/255.0)*0.99 + 0.01;
                    inputs.add(t);
                }
                int target = queryMax(inputs);
                System.out.println("Correct: " + correct + ", Output: " + target);
                if(correct == target) ++count_correct;
            }
            System.out.println((double)100*count_correct/count + "%");
            sc.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveWeights(String category, String id) {
        File file = new File("weights/"+category+"/"+id+".w");
        file.getParentFile().getParentFile().mkdirs();
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (Exception e) {}

        try (FileOutputStream f = new FileOutputStream(file.getPath()); ObjectOutputStream o = new ObjectOutputStream(f)) {
            o.writeObject(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWeights(String category, String id) {
        File file = new File("weights/"+category+"/"+id+".w");
        try(FileInputStream fi = new FileInputStream(file.getPath()); ObjectInputStream oi = new ObjectInputStream(fi)) {
            NeuralNetwork nn = (NeuralNetwork) oi.readObject();
            load(nn.getLayers(), nn.getLearningRate());
        } catch (Exception e) {
            load(defaultLayers, defaultLr);
            saveWeights(category, id);
        }
    }
}
