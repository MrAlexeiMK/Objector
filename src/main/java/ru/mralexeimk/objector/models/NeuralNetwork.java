package ru.mralexeimk.objector.models;

import lombok.Data;
import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.other.Pair;
import ru.mralexeimk.objector.singletons.NeuralNetworkListener;
import ru.mralexeimk.objector.singletons.SettingsListener;

import java.io.*;
import java.util.*;

@Data
public class NeuralNetwork implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id = "";
    private List<List<Layer>> layers;
    private double lr;
    private List<String> objects = new ArrayList<>();

    private final String defaultId = "default";
    private final List<String> defaultObjects = new ArrayList<>(List.of("Nothing"));

    public NeuralNetwork(String id, List<List<Layer>> layers, double lr, List<String> objects) {
        load(id, layers, lr, objects);
    }

    public NeuralNetwork(List<List<Layer>> layers, double lr) {
        load(defaultId, layers, lr, defaultObjects);
        init();
    }

    public NeuralNetwork(double lr) {
        this.layers = new ArrayList<>(SettingsListener.get().getConfigurationLayers());
        this.lr = lr;
        init();
    }

    public NeuralNetwork() {
        load(defaultId,
                new ArrayList<>(SettingsListener.get().getConfigurationLayers()), SettingsListener.get().getLr(), defaultObjects);
        init();
    }

    public NeuralNetwork(String category, String id) {
        loadWeights(id);
    }

    public NeuralNetwork(String id) {
        loadWeights(id);
    }

    public void load(String id, List<List<Layer>> layers, double lr, List<String> objects) {
        this.id = id;
        this.layers = layers;
        this.lr = lr;
        this.objects = objects;
    }

    public void init() {
        for(int i = 0; i < layers.size(); ++i) {
            for(int j = 0; j < layers.get(i).size(); ++j) {
                if (i + 1 < layers.size()) {
                    int v = j;
                    if(j >= layers.get(i+1).size()) v = 0;
                    layers.get(i).get(j).setNextLayer(layers.get(i + 1).get(v));
                }
                layers.get(i).get(j).toDefault();
            }
        }
    }

    public void addObject(String objectId) {
        objects.add(objectId);
        int units = getOutputLayer().getUnits();
        layers.remove(layers.size()-1);
        OutputLayer ol = new OutputLayer(units+1, 1, LayerType.OUTPUT);
        layers.add(new ArrayList<>(List.of(ol)));
        if(layers.get(layers.size()-2).get(0) instanceof NeuronsLayer) {
            layers.get(layers.size()-2).get(0).setNextLayer(ol);
            if(SettingsListener.get().isRewriteWeights()) {
                layers.get(layers.size()-2).get(0).toDefault();
            }
            else {
                layers.get(layers.size()-2).get(0).addRow();
            }
        }
        saveWeights(id);
    }

    public void removeObject(String objectId) {
        int index = 0;
        for(String obj : objects) {
            if(obj.equals(objectId)) break;
            ++index;
        }
        objects.remove(objectId);
        int units = getOutputLayer().getUnits();
        layers.remove(layers.size()-1);
        OutputLayer ol = new OutputLayer(units-1, 1, LayerType.OUTPUT);
        layers.add(new ArrayList<>(List.of(ol)));
        if(layers.get(layers.size()-2).get(0) instanceof NeuronsLayer) {
            layers.get(layers.size()-2).get(0).setNextLayer(ol);
            if(SettingsListener.get().isRewriteWeights()) {
                layers.get(layers.size()-2).get(0).toDefault();
            }
            else {
                layers.get(layers.size()-2).get(0).removeRow(index);
            }
        }
        saveWeights(id);
    }

    public boolean isSeveralInputs() {
        return layers.get(0).size() > 1;
    }

    public double getLearningRate() {
        return lr;
    }

    public List<List<Layer>> getLayers() {
        return layers;
    }

    public String getConfiguration() {
        String res = "";
        for(List<Layer> list : layers) {
            for(int i = 0; i < list.size(); ++i) {
                Layer layer = list.get(i);
                res += layer.toString();
                if(i < list.size()-1) res += " + ";
            }
            res += "\n";
        }
        return res;
    }

    public void setInputLayerData(List<List<Double>> data) {
        for(int i = 0; i < data.size(); ++i) {
            getInputLayer(i).setData(data.get(i));
        }
    }

    public InputLayer getInputLayer(int index) {
        return (InputLayer) layers.get(0).get(index);
    }

    public OutputLayer getOutputLayer() {
        return (OutputLayer) layers.get(layers.size()-1).get(0);
    }

    public void printWeights() {
        for (List<Layer> layerList : layers) {
            Layer layer = layerList.get(0);
            System.out.println(layer.toString());
            if (layer instanceof InputLayer il) {
                System.out.println(il.getBiases());
                for (Matrix m : il.getW()) m.print();
            } else if (layer instanceof PullingLayer pl) {
                System.out.println(pl.getBiases());
                for (List<Matrix> mm : pl.getW()) {
                    for (Matrix m : mm) {
                        m.print();
                    }
                }
            } else if (layer instanceof NeuronsLayer nl) {
                if (nl.getW() != null) nl.getW().print();
            }
        }
    }

    public void printData() {
        for (List<Layer> layerList : layers) {
            Layer layer = layerList.get(0);
            System.out.println(layer.toString());
            if (layer instanceof InputLayer il) {
                il.getData().print();
            } else if (layer instanceof FilterLayer fl) {
                for (Matrix m : fl.getData()) {
                    m.print();
                }
            } else if (layer instanceof PullingLayer pl) {
                for (Matrix m : pl.getData()) {
                    m.print();
                }
            } else if (layer instanceof NeuronsLayer nl) {
                System.out.println(nl.getData());
            } else if (layer instanceof OutputLayer ol) {
                System.out.println(ol.getData());
            }
        }
    }

    public void evaluate() {
        for (List<Layer> layerList : layers) {
            for (Layer layer : layerList) {
                layer.evaluate();
            }
        }
    }

    public void train(List<List<Double>> input_list, String objectId) {
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

    public synchronized void train(List<List<Double>> input_list, List<Double> target_list) {
        setInputLayerData(input_list);
        evaluate();
        Matrix outputs = new Matrix(getOutputLayer().getData());
        Matrix targets = new Matrix(target_list);
        Matrix errors = targets.minus(outputs);
        List<Double> temp = new ArrayList<>();
        for(int i = layers.size()-2; i >= 0; --i) {
            int len = layers.get(i).size();
            for (int j = 0; j < len; ++j) {
                Layer layer = layers.get(i).get(j);
                Layer nextLayer = layer.getNextLayer();
                if (layer instanceof NeuronsLayer nl) {
                    Matrix I = new Matrix(nl.getData());
                    Matrix O = null;
                    if (nextLayer instanceof OutputLayer) {
                        O = new Matrix(outputs);
                    } else if (nextLayer instanceof NeuronsLayer nl2) {
                        O = new Matrix(nl2.getData());
                    }
                    if (O != null) {
                        Matrix dif = errors
                                .multiply(O)
                                .multiply(O.getNegative().sum(1))
                                .multiply(I.getTranspose())
                                .multiply(lr);
                        nl.setW(nl.getW().sum(dif));
                        errors = nl.getW().getTranspose().multiply(errors);
                    }
                } else if (layer instanceof PullingLayer pl) {
                    if(!SettingsListener.get().isTrainKernels()) continue;
                    if (nextLayer instanceof FilterLayer fl) {
                        int connections = fl.getUnits() / pl.getUnits();
                        int lenErrorsPerConnect = errors.getM() / (pl.getUnits() * connections);
                        for (int unit = 0; unit < pl.getUnits(); ++unit) {
                            double average = 0;
                            Matrix I = pl.getData().get(unit);
                            for (int kernel = 0; kernel < connections; ++kernel) {
                                int val = unit * connections + kernel;
                                double bias = pl.getBiases().get(unit).get(kernel);
                                Matrix O = fl.getData().get(val);
                                Matrix K = pl.getWW(unit, kernel);
                                List<Double> errorList = new ArrayList<>();
                                double bias_error = 0, bias_input = 0, bias_output = 0;
                                double bias_input_count = 0, bias_output_count = 0;
                                for (int e = val * lenErrorsPerConnect; e < lenErrorsPerConnect + val * lenErrorsPerConnect; ++e) {
                                    errorList.add(errors.get(0, e));
                                }
                                for (int e = 0; e < errorList.size(); ++e) {
                                    double error = errorList.get(e);
                                    average += error;
                                    bias_error += error;
                                    for (Pair<Integer, Integer> pair : O.getSquare(fl.getK(), e)) {
                                        double outputValue = O.get(pair.getFirst(), pair.getSecond());
                                        bias_output += outputValue;
                                        bias_output_count++;
                                        for (int y = pair.getSecond(); y < pair.getSecond() + K.getM(); ++y) {
                                            for (int x = pair.getFirst(); x < pair.getFirst() + K.getN(); ++x) {
                                                double inputValue = I.get(x, y);
                                                bias_input += inputValue;
                                                bias_input_count++;
                                                int kX = x - pair.getFirst();
                                                int kY = y - pair.getSecond();
                                                K.set(kX, kY, K.get(kX, kY) + lr * outputValue * (1 - outputValue) * inputValue * error);
                                            }
                                        }
                                    }
                                }
                                bias_input /= bias_input_count;
                                bias_output /= bias_output_count;
                                pl.getBiases().get(unit).set(kernel, bias+lr*bias_output*(1-bias_output)*bias_input*bias_error);
                                pl.setWW(unit, kernel, K);
                            }
                            average /= (connections * lenErrorsPerConnect);
                            temp.add(average);
                        }
                        errors = new Matrix(temp);
                        temp.clear();
                    }
                } else if (layer instanceof InputLayer il) {
                    Matrix IVector = new Matrix(il.getData().toList());
                    Matrix I = il.getData();
                    if (nextLayer instanceof FilterLayer fl) {
                        if(!SettingsListener.get().isTrainKernels()) continue;
                        int connections = fl.getUnits();
                        for (int kernel = 0; kernel < connections; ++kernel) {
                            Matrix O = new Matrix(I);
                            Matrix K = il.getW().get(kernel);
                            double bias = il.getBiases().get(kernel);
                            O.convertByKernel(K);
                            O = NeuralNetworkListener.activationFun(O);
                            double error = errors.get(0, kernel);
                            double bias_input = 0, bias_output = 0;
                            double bias_input_count = 0, bias_output_count = 0;
                            for (int y = 0; y < O.getM(); ++y) {
                                for (int x = 0; x < O.getN(); ++x) {
                                    double outputValue = O.get(x, y);
                                    bias_output += outputValue;
                                    bias_output_count++;
                                    for (int y1 = y; y1 < y + K.getM(); ++y1) {
                                        for (int x1 = x; x1 < x + K.getN(); ++x1) {
                                            double inputValue = I.get(x1, y1);
                                            bias_input += inputValue;
                                            bias_input_count++;
                                            int kX = x1 - x;
                                            int kY = y1 - y;
                                            K.set(kX, kY, K.get(kX, kY) + lr * outputValue * (1 - outputValue) * inputValue * error);
                                        }
                                    }
                                }
                            }
                            bias_input /= bias_input_count;
                            bias_output /= bias_output_count;
                            il.getBiases().set(kernel, bias + lr*bias_output*(1-bias_output)*bias_input*error);
                            il.setW(kernel, K);
                        }
                    } else if (nextLayer instanceof NeuronsLayer nl) {
                        Matrix O = new Matrix(nl.getData());
                        Matrix dif = errors
                                .multiply(O)
                                .multiply(O.getNegative().sum(1))
                                .multiply(IVector.getTranspose())
                                .multiply(lr);
                        il.setW(j, il.getW().get(j).sum(dif));
                    }
                }
            }
        }
    }

    public List<Double> query(List<List<Double>> input_list) {
        setInputLayerData(input_list);
        evaluate();
        return getOutputLayer().getData();
    }

    public int queryMax(List<List<Double>> input_list) {
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

    public Pair<String, Double> queryMaxPair(List<List<Double>> input_list) {
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

    public List<Pair<String, Double>> queryPairs(List<List<Double>> input_list) {
        List<Double> list = query(input_list);
        List<Pair<String, Double>> res = new ArrayList<>();
        for(int i = 0; i < list.size(); ++i) {
            if(list.get(i) > 0.5) {
                res.add(new Pair<>(objects.get(i), list.get(i)));
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
                train(new ArrayList<>(List.of(inputs)), targets);
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
                int target = queryMax(new ArrayList<>(List.of(inputs)));
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
        load(defaultId, new ArrayList<>(SettingsListener.get().getConfigurationLayers()), SettingsListener.get().getLr(), defaultObjects);
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
