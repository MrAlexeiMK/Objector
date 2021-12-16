package ru.mralexeimk.objector.models;

import java.io.*;
import java.util.*;

public class NeuralNetwork implements Serializable {
    private int N;
    private List<Matrix> W;
    private double lr;
    private List<Object> layers;

    private final List<Object> def_layers = new ArrayList<>(Arrays.asList(1024, 150, 1));
    private final List<Object> def_layers_conv = new ArrayList<>(Arrays.asList(
            1024,
            Collections.nCopies(6, 784),
            Collections.nCopies(6, 196),
            Collections.nCopies(16, 100),
            Collections.nCopies(16, 25),
            120,
            84,
            1
            ));
    private final double def_lr = 0.1;

    public NeuralNetwork(int N, List<Object> layers, List<Matrix> W, double lr) {
        load(N, layers, W, lr);
    }

    public NeuralNetwork(String category, String id) {
        loadWeights(category, id);
    }

    public void load(int N, List<Object> layers, List<Matrix> W, double lr) {
        this.W = W;
        this.lr = lr;
        this.layers = layers;
        this.N = N;
    }

    public double getLearningRate() {
        return lr;
    }

    public int getN() {
        return N;
    }

    public List<Object> getLayers() {
        return layers;
    }

    public List<Matrix> getWeights() {
        return W;
    }

    public Matrix getWeights(int i) {
        return W.get(i);
    }

    public double activationFun(double x) {
        return 1.0/(1 + Math.exp(-x));
    }

    public Matrix activationFun(Matrix m) {
        Matrix res = new Matrix(m.getN(), m.getM());
        for(int x = 0; x < m.getN(); ++x) {
            for(int y = 0; y < m.getM(); ++y) {
                res.set(x, y, activationFun(m.get(x, y)));
            }
        }
        return res;
    }

    public void train(List<Double> input_list, List<Double> target_list) {
        train(new Matrix(input_list), new Matrix(target_list));
    }

    public void train(Matrix inputs, Matrix targets) {
        Matrix outputs = new Matrix(inputs);
        List<Matrix> outputs_array = new ArrayList<>();
        outputs_array.add(new Matrix(outputs));
        for(int i = 0; i < N; ++i) {
            outputs = activationFun(W.get(i).multiply(outputs));
            outputs_array.add(outputs);
        }
        Matrix errors = targets.minus(outputs);
        for(int i = N-1; i >= 0; --i) {
            Matrix dif = errors.multiply(outputs_array.get(i+1)).multiply(
                    outputs_array.get(i+1).getNegative().sum(1)
            ).multiply(
                    outputs_array.get(i).getTranspose()
            ).multiply(lr);
            W.set(i, W.get(i).sum(dif));
            errors = W.get(i).getTranspose().multiply(errors);
        }
    }

    public List<Double> query(List<Double> input_list) {
        return query(new Matrix(input_list));
    }

    public List<Double> query(Matrix inputs) {
        Matrix outputs = new Matrix(inputs);
        for(int i = 0; i < N; ++i) {
            outputs = activationFun(W.get(i).multiply(outputs));
        }
        List<Double> res = new ArrayList<>();
        for(int y = 0; y < outputs.getM(); ++y) {
            for(int x = 0; x < outputs.getN(); ++x) {
                res.add(outputs.get(x, y));
            }
        }
        return res;
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
                List<Double> targets = new ArrayList<>(Collections.nCopies(getOutputNeurons(), 0.01));
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
                List<Double> res = query(inputs);
                int target = 0;
                double max = 0;
                for(int i = 0; i < res.size(); ++i) {
                    if(res.get(i) > max) {
                        max = res.get(i);
                        target = i;
                    }
                }
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
            o.writeObject(new NeuralNetwork(N, layers, W, lr));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWeights(String category, String id) {
        File file = new File("weights/"+category+"/"+id+".w");
        try(FileInputStream fi = new FileInputStream(file.getPath()); ObjectInputStream oi = new ObjectInputStream(fi)) {
            NeuralNetwork nn = (NeuralNetwork) oi.readObject();
            load(nn.getN(), nn.getLayers(), nn.getWeights(), nn.getLearningRate());
        } catch (Exception e) {
            //e.printStackTrace();
            toDefault();
            saveWeights(category, id);
        }
    }

    public void saveWeightsReadable(String category, String id) {
        try {
            File file = new File("weights/"+category+"/"+id+".w");
            file.getParentFile().getParentFile().mkdirs();
            file.getParentFile().mkdirs();
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(N+"\n");
            out.write(lr + "\n");
            out.write("\n");
            for(int i = 0; i < N; ++i) {
                out.write(W.get(i).toString());
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWeightsReadable(String category, String id) {
        try {
            clear();
            File file = new File("weights/"+category+"/"+id+".w");
            if(!file.exists()) {
                toDefault();
                saveWeights(category, id);
                return;
            }
            Scanner sc = new Scanner(file);
            if(!sc.hasNextInt()) {
                toDefault();
                saveWeights(category, id);
                return;
            }
            N = sc.nextInt();
            lr = sc.nextDouble();
            for(int i = 0; i < N; ++i) {
                int a = sc.nextInt();
                int b = sc.nextInt();
                Matrix m = new Matrix(a, b);
                layers.add(a);
                if(i == N-1) layers.add(b);
                for(int y = 0; y < b; ++y) {
                    for(int x = 0; x < a; ++x) {
                        double val = Double.parseDouble(sc.next().replace(',','.'));
                        m.set(x, y, val);
                    }
                }
                W.add(m);
            }
            sc.close();
        } catch (Exception e) {
            toDefault();
            saveWeights(category, id);
        }
    }

    public void clear() {
        layers = new ArrayList<>();
        W = new ArrayList<>();
    }

    public void toDefault() {
        W = new ArrayList<>();
        layers = new ArrayList<>(def_layers);
        lr = def_lr;
        N = 2;
        for(int i = 0; i < N; ++i) {
            W.add(new Matrix((int)layers.get(i), (int)layers.get(i+1),
                    -1/Math.sqrt((int)layers.get(i+1)), 1/Math.sqrt((int)layers.get(i+1))));
        }
    }

    public int getInputNeurons() {
        return (int) layers.get(0);
    }

    public int getOutputNeurons() {
        return (int) layers.get(layers.size()-1);
    }

    public void print() {
        for(int i = 0; i < N; ++i) {
            W.get(i).print();
        }
    }
}
