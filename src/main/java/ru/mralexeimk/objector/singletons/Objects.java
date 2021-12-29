package ru.mralexeimk.objector.singletons;
import ru.mralexeimk.objector.Main;
import ru.mralexeimk.objector.models.NeuralNetwork;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Objects {
    private static ConcurrentHashMap<String, NeuralNetwork> nn;
    private static String category;

    private static int queue_count;
    private static Result query_res;

    public static void init(String category) {
        queue_count = 0;
        query_res = null;
        Objects.category = category;
        Set<String> list = getObjectsInDirectory();
        nn = new ConcurrentHashMap<>();
        for(String id : list) {
            addCurrentThreadObject(id);
        }
    }

    public static Set<String> getObjects() {
        return nn.keySet();
    }

    public static int getQueueCount() {
        return queue_count;
    }

    public static Set<String> getObjectsInDirectory() {
        File file = new File("weights/"+category+"/");
        file.mkdirs();
        Set<String> list = new HashSet<>();
        for(File fe : file.listFiles()) {
            if(!fe.isDirectory()) {
                String id = fe.getName();
                String[] spl = id.split("\\.");
                if(spl.length > 1 && spl[spl.length-1].equalsIgnoreCase("w")) {
                    id = "";
                    for(int i = 0; i < spl.length-1; ++i) {
                        id += spl[i];
                    }
                    list.add(id);
                }
            }
        }
        return list;
    }

    public static void addCurrentThreadObject(String name) {
        if (!getObjects().contains(name)) {
            NeuralNetwork neuralNetwork = new NeuralNetwork(category, name);
            nn.put(name, neuralNetwork);
        }
    }

    public static void addObject(String name) {
        new Thread(() -> {
            addCurrentThreadObject(name);
        }).start();
    }

    public static void addAllObjects(List<String> names) {
        for(String name : names) addCurrentThreadObject(name);
    }

    public static void deleteObject(String name) {
        new Thread(() -> {
            if (getObjects().contains(name)) {
                nn.remove(name);
                File file = new File("weights/" + category + "/" + name + ".w");
                file.delete();
            }
        }).start();
    }

    public static void update() {
        for(String name : nn.keySet()) {
            NeuralNetwork neuralNetwork = nn.get(name);
            neuralNetwork.toDefault();
            nn.replace(name, neuralNetwork);
        }
    }

    public static String getCategory() {
        return category;
    }

    public static int getCount() {
        return nn.size();
    }

    public static synchronized void train(List<Double> input, String id) {
        currentThreadTrain(input, id);
    }

    public static void currentThreadTrain(List<Double> input, String id) {
        List<Double> output = new ArrayList<>(List.of(0.99/getNeuralNetworks().size()));
        List<Double> output2 = new ArrayList<>(List.of(0.99));
        if (getNeuralNetworks().containsKey(id)) {
            NeuralNetwork neuralNetwork = getNeuralNetwork(id);
            neuralNetwork.train(input, output2);
            Iterator<String> it = getNeuralNetworks().keySet().iterator();
            while(it.hasNext()) {
                String name = it.next();
                NeuralNetwork n = getNeuralNetwork(name);
                if (!name.equals(id)) {
                    n.train(input, output);
                }
            }
        }
    }

    public static void threadTrain(List<Double> input, String id) {
        queue_count++;
        Thread th = new Thread(() -> {
            train(input, id);
            queue_count--;
        });
        th.start();
    }

    public static void saveToFiles() {
        new Thread(Objects::saveToFilesCurrentThread).start();
    }

    public static void saveToFilesCurrentThread() {
        for (String obj : getObjects()) {
            NeuralNetwork n = getNeuralNetwork(obj);
            n.saveWeights(obj);
        }
    }

    public static class Result {
        private String id;
        private double prob;

        public Result(String id, double prob) {
            this.id = id;
            this.prob = prob;
        }

        public String getId() {
            return id;
        }

        public double getProb() {
            return prob;
        }
    }

    public static Result query(List<Double> input) {
        String res = null;
        double max = 0;
        System.out.println("============");
        if(input != null && !input.isEmpty()) {
            for (String name : getNeuralNetworks().keySet()) {
                List<Double> output = getNeuralNetwork(name).query(input);
                if (output.get(0) > max) {
                    max = output.get(0);
                    res = name;
                }
                System.out.println(name + " , " + output.get(0));
            }
        }
        System.out.println("============");
        return new Result(res, max);
    }

    public static Result getQueryResult() {
        return query_res;
    }

    public static void threadQuery(List<Double> input) {
        new Thread(() -> query_res = query(input)).start();
    }

    public static void trainFromFile(String path) {
        File file = null;
        try {
            file = new File(Main.class.getResource(path).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try (Scanner sc = new Scanner(file)) {
            String line;
            int j = 0;
            while(sc.hasNextLine()) {
                line = sc.nextLine();
                String[] spl = line.split(",");
                String id = spl[0];
                List<Double> inputs = new ArrayList<>();
                for(int i = 1; i < spl.length; ++i) {
                    double t = Double.parseDouble(spl[i]);
                    t = (t/255.0)*0.99 + 0.01;
                    inputs.add(t);
                }
                ++j;
                currentThreadTrain(inputs, id);
                System.out.println(j);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void testFromFile(String path) {
        File file = null;
        try {
            file = new File(Main.class.getResource(path).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try (Scanner sc = new Scanner(file)) {
            String line;
            int count = 0;
            int count_correct = 0;
            while(sc.hasNextLine()) {
                line = sc.nextLine();
                ++count;
                String[] spl = line.split(",");
                String correct = spl[0];
                List<Double> inputs = new ArrayList<>();
                for(int i = 1; i < spl.length; ++i) {
                    double t = Double.parseDouble(spl[i]);
                    t = (t/255.0)*0.99 + 0.01;
                    inputs.add(t);
                }
                String target = query(inputs).getId();
                System.out.println("Correct: " + correct + ", Output: " + target);
                if(correct.equals(target)) ++count_correct;
            }
            System.out.println((double)100*count_correct/count + "%");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Double> parseImage(String path) {
        List<Double> res = new ArrayList<>();
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(Main.class.getResource(path).toURI()));
            for(int y = 0; y < bufferedImage.getHeight(); ++y) {
                for(int x = 0; x < bufferedImage.getWidth(); ++x) {
                    int rgb = bufferedImage.getRGB(x, y);
                    int red = 255 - (rgb >> 16) & 0xFF;
                    //if(red < 5) System.out.print("0");
                    //else System.out.print("1");
                    res.add(Double.valueOf(red));
                }
                //System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static NeuralNetwork getNeuralNetwork(String id) {
        return nn.get(id);
    }

    public static ConcurrentHashMap<String, NeuralNetwork> getNeuralNetworks() {
        return nn;
    }
}
