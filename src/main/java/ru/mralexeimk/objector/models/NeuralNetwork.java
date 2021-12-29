package ru.mralexeimk.objector.models;

import lombok.Data;
import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.other.Pair;

import java.io.*;
import java.util.*;

@Data
public class NeuralNetwork implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id = "";
    private List<Layer> layers;
    private double lr;
    private List<String> objects = new ArrayList<>();

    private final String defaultId = "default";
    private final List<String> defaultObjects = new ArrayList<>(List.of("Nothing"));
    private final List<Layer> defaultLayers = new ArrayList<>(Arrays.asList(
            new InputLayer(1, 28, LayerType.INPUT),
            new FilterLayer(8, 24, LayerType.FILTER),
            new PullingLayer(8, 12, LayerType.PULLING),
            new FilterLayer(32, 8, LayerType.FILTER),
            new PullingLayer(32, 4, LayerType.PULLING),
            new NeuronsLayer(512, 1, LayerType.NEURONS),
            new OutputLayer(1, 1, LayerType.OUTPUT)
    ));
    private static List<Layer> defaultLayers2 = new ArrayList<>(Arrays.asList(
            new InputLayer(1, 28, LayerType.INPUT),
            new NeuronsLayer(120, 1, LayerType.NEURONS),
            new OutputLayer(1, 1, LayerType.OUTPUT)
    ));
    private final double defaultLr = 0.1;

    public NeuralNetwork(String id, List<Layer> layers, double lr, List<String> objects) {
        load(id, layers, lr, objects);
    }

    public NeuralNetwork(List<Layer> layers, double lr) {
        load(defaultId, layers, lr, defaultObjects);
        init();
    }

    public NeuralNetwork(double lr) {
        this.layers = defaultLayers;
        this.lr = lr;
        init();
    }

    public NeuralNetwork() {
        load(defaultId, defaultLayers, defaultLr, defaultObjects);
        init();
    }

    public NeuralNetwork(String category, String id) {
        loadWeights(id);
    }

    public NeuralNetwork(String id) {
        loadWeights(id);
    }

    public void load(String id, List<Layer> layers, double lr, List<String> objects) {
        this.id = id;
        this.layers = layers;
        this.lr = lr;
        this.objects = objects;
    }

    public void init() {
        for(int i = 0; i < layers.size(); ++i) {
            if(i+1 < layers.size()) {
                layers.get(i).setNextLayer(layers.get(i + 1));
            }
            layers.get(i).toDefault();
        }
    }

    public void addObject(String objectId) {
        objects.add(objectId);
        int units = getOutputLayer().getUnits();
        layers.remove(layers.size()-1);
        OutputLayer ol = new OutputLayer(units+1, 1, LayerType.OUTPUT);
        layers.add(ol);
        if(layers.get(layers.size()-2) instanceof NeuronsLayer) {
            layers.get(layers.size()-2).setNextLayer(ol);
            layers.get(layers.size()-2).toDefault();
        }
        saveWeights(id);
    }

    public void removeObject(String objectId) {
        objects.remove(objectId);
        int units = getOutputLayer().getUnits();
        layers.remove(layers.size()-1);
        OutputLayer ol = new OutputLayer(units-1, 1, LayerType.OUTPUT);
        layers.add(ol);
        if(layers.get(layers.size()-2) instanceof NeuronsLayer nl) {
            nl.setNextLayer(ol);
            nl.toDefault();
        }
        saveWeights(id);
    }

    public double getLearningRate() {
        return lr;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public String getConfiguration() {
        String res = "";
        for(Layer layer : layers) {
            res += layer.toString() + "\n";
        }
        return res;
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

    public void printWeights() {
        for(Layer layer : getLayers()) {
            System.out.println(layer.toString());
            if(layer instanceof InputLayer il) {
                for(Matrix m : il.getW()) m.print();
            }
            else if(layer instanceof PullingLayer pl) {
                for(List<Matrix> mm : pl.getW()) {
                    for(Matrix m : mm) {
                        m.print();
                    }
                }
            }
            else if(layer instanceof NeuronsLayer nl) {
                if(nl.getW() != null) nl.getW().print();
            }
        }
    }

    public void printData() {
        for(Layer layer : layers) {
            System.out.println(layer.toString());
            if(layer instanceof InputLayer il) {
                il.getData().print();
            }
            else if(layer instanceof FilterLayer fl) {
                for(Matrix m : fl.getData()) {
                    m.print();
                }
            }
            else if(layer instanceof PullingLayer pl) {
                for(Matrix m : pl.getData()) {
                    m.print();
                }
            }
            else if(layer instanceof NeuronsLayer nl) {
                System.out.println(nl.getData());
            }
            else if(layer instanceof OutputLayer ol) {
                System.out.println(ol.getData());
            }
        }
    }

    public void train(List<Double> input_list, String objectId) {
        if(objects.contains(objectId)) {
            List<Double> target_list = new ArrayList<>(Collections.nCopies(objects.size(), 0.01));
            int index = 0;
            for(String obj : objects) {
                if(obj.equals(objectId)) {
                    break;
                }
                ++index;
            }
            target_list.set(index, 0.99);
            train(input_list, target_list);
        }
    }

    public synchronized void train(List<Double> input_list, List<Double> target_list) {
        setInputLayerData(input_list);
        for(Layer layer : getLayers()) {
            layer.evaluate();
        }
        Matrix outputs = new Matrix(getOutputLayer().getData());
        Matrix targets = new Matrix(target_list);
        Matrix errors = targets.minus(outputs);
        for(int i = layers.size()-2; i >= 0; --i) {
            Layer layer = getLayers().get(i);
            Layer nextLayer = layer.getNextLayer();
            if(layer instanceof NeuronsLayer nl) {
                Matrix I = new Matrix(nl.getData());
                Matrix O = null;
                if(nextLayer instanceof OutputLayer) {
                    O = new Matrix(outputs);
                }
                else if(nextLayer instanceof NeuronsLayer nl2) {
                    O = new Matrix(nl2.getData());
                }
                if(O != null) {
                    Matrix dif = errors
                            .multiply(O)
                            .multiply(O.getNegative().sum(1))
                            .multiply(I.getTranspose())
                            .multiply(lr);
                    nl.setW(nl.getW().sum(dif));
                    errors = nl.getW().getTranspose().multiply(errors);
                }
            }
            else if(layer instanceof PullingLayer pl) {
                if(nextLayer instanceof FilterLayer fl) {
                    int connections = fl.getUnits()/pl.getUnits();
                    List<Double> new_errors = new ArrayList<>();
                    int lenErrorsPerConnect = errors.getM()/(pl.getUnits()*connections);
                    for(int unit = 0; unit < pl.getUnits(); ++unit) {
                        double average = 0;
                        Matrix I = pl.getData().get(unit);
                        for(int kernel = 0; kernel < connections; ++kernel) {
                            int val = unit*connections + kernel;
                            Matrix O = fl.getData().get(val);
                            Matrix K = pl.getWW(unit, kernel);
                            List<Double> errorList = new ArrayList<>();
                            for(int e = val*lenErrorsPerConnect; e < lenErrorsPerConnect+val*lenErrorsPerConnect; ++e) {
                                errorList.add(errors.get(0, e));
                            }
                            for(int e = 0; e < errorList.size(); ++e) {
                                double error = errorList.get(e);
                                average += error;
                                for(Pair<Integer, Integer> pair : O.getSquare(fl.getK(), e)) {
                                    double outputValue = O.get(pair.getFirst(), pair.getSecond());
                                    for(int y = pair.getSecond(); y < pair.getSecond()+K.getM(); ++y) {
                                        for(int x = pair.getFirst(); x < pair.getFirst()+K.getN(); ++x) {
                                            double inputValue = I.get(x, y);
                                            int kX = x-pair.getFirst();
                                            int kY = y-pair.getSecond();
                                            K.set(kX, kY, K.get(kX, kY) + lr*outputValue*(1-outputValue)*inputValue*error);
                                        }
                                    }
                                }
                            }
                            pl.setWW(unit, kernel, K);
                        }
                        average /= (connections * lenErrorsPerConnect);
                        new_errors.add(average);
                    }
                    errors = new Matrix(new_errors);
                }
            }
            else if(layer instanceof InputLayer il) {
                Matrix IVector = new Matrix(il.getData().toList());
                Matrix I = il.getData();
                if(nextLayer instanceof FilterLayer fl) {
                    int connections = fl.getUnits();
                    for(int kernel = 0; kernel < connections; ++kernel) {
                        Matrix O = fl.getData().get(kernel);
                        Matrix K = il.getW().get(kernel);
                        double error = errors.get(0, kernel);
                        for(int y = 0; y < O.getM(); ++y) {
                            for(int x = 0; x < O.getN(); ++x) {
                                double outputValue = O.get(x, y);
                                for(int y1 = y; y1 < y+K.getM(); ++y1) {
                                    for(int x1 = x; x1 < x+K.getN(); ++x1) {
                                        double inputValue = I.get(x1, y1);
                                        int kX = x1-x;
                                        int kY = y1-y;
                                        K.set(kX, kY, K.get(kX, kY) + lr*outputValue*(1-outputValue)*inputValue*error);
                                    }
                                }
                            }
                        }
                        il.setW(kernel, K);
                    }
                }
                else if(nextLayer instanceof NeuronsLayer nl) {
                    Matrix O = new Matrix(nl.getData());
                    Matrix dif = errors
                            .multiply(O)
                            .multiply(O.getNegative().sum(1))
                            .multiply(IVector.getTranspose())
                            .multiply(lr);
                    nl.setW(nl.getW().sum(dif));
                    errors = nl.getW().getTranspose().multiply(errors);
                }
            }
        }
    }

    public void train(Matrix inputs, Matrix targets) {
        train(inputs.toList(), targets.toList());
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

    public Pair<String, Double> queryMaxPair(List<Double> input_list) {
        List<Double> list = query(input_list);
        int res = 0;
        double max = 0;
        for(int i = 0; i < list.size(); ++i) {
            if(list.get(i) > max) {
                max = list.get(i);
                res = i;
            }
        }
        return new Pair<>(objects.get(res), max);
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

    public void toDefault() {
        load(defaultId, defaultLayers, defaultLr, defaultObjects);
        init();
    }

    public synchronized void saveWeights(String id) {
        this.id = id;
        File file = new File("weights/"+id+".w");
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (Exception ignored) {}

        try (FileOutputStream f = new FileOutputStream(file.getPath()); ObjectOutputStream o = new ObjectOutputStream(f)) {
            o.writeObject(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWeights(String id) {
        this.id = id;
        File file = new File("weights/"+id+".w");
        try(FileInputStream fi = new FileInputStream(file.getPath()); ObjectInputStream oi = new ObjectInputStream(fi)) {
            NeuralNetwork nn = (NeuralNetwork) oi.readObject();
            load(nn.getId(), nn.getLayers(), nn.getLearningRate(), nn.getObjects());
        } catch (Exception e) {
            toDefault();
            saveWeights(id);
        }
    }
}
