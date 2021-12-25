package ru.mralexeimk.objector.singletons;
import ru.mralexeimk.objector.Main;
import ru.mralexeimk.objector.models.NeuralNetwork;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public class Objects {
    private static HashMap<String, NeuralNetwork> nn;
    private static String category;

    private static int queue_count;
    private static Result query_res;

    public static void init(String category) {
        queue_count = 0;
        query_res = null;
        Objects.category = category;
        Set<String> list = getObjectsInDirectory();
        nn = new HashMap<>();
        for(String id : list) {
            addObject(id);
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

    public static void addObject(String name) {
        new Thread(() -> {
            NeuralNetwork neuralNetwork = null;
            if (!getObjects().contains(name)) {
                neuralNetwork = new NeuralNetwork(category, name);
                nn.put(name, neuralNetwork);
            }
        }).start();
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
        List<Double> output = new ArrayList<>(Arrays.asList(0.01));
        List<Double> output2 = new ArrayList<>(Arrays.asList(0.99));
        if (getNeuralNetworks().containsKey(id)) {
            NeuralNetwork neuralNetwork = getNeuralNetwork(id);
            neuralNetwork.train(input, output2);
            for (String name : getNeuralNetworks().keySet()) {
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
        new Thread(() -> {
            for (String obj : getObjects()) {
                NeuralNetwork n = getNeuralNetwork(obj);
                n.saveWeights(category, obj);
            }
        }).start();
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
        if(input != null && !input.isEmpty()) {
            for (String name : getNeuralNetworks().keySet()) {
                List<Double> output = getNeuralNetwork(name).query(input);
                if (output.get(0) > max) {
                    max = output.get(0);
                    res = name;
                }
            }
        }
        return new Result(res, max);
    }

    public static Result getQueryResult() {
        return query_res;
    }

    public static void threadQuery(List<Double> input) {
        new Thread(() -> {
            query_res = query(input);
        }).start();
    }

    public static void trainFromFile(String path) {
        try {
            File file = new File(Main.class.getResource(path).toURI());
            Scanner sc = new Scanner(file);
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
                train(inputs, id);
                System.out.println(j);
            }
            sc.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void testFromFile(String path) {
        try {
            File file = new File(Main.class.getResource(path).toURI());
            Scanner sc = new Scanner(file);
            String line;
            int j = 0;
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
                if(correct == target) ++count_correct;
            }
            System.out.println((double)100*count_correct/count + "%");
            sc.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    public static List<Double> parseImage(BufferedImage im, int newW, int newH) {
        try {
            List<Double> res = new ArrayList<>();
            im = resize(im, newW, newH);
            for (int y = 0; y < newH; ++y) {
                for (int x = 0; x < newW; ++x) {
                    int rgb = im.getRGB(x, y);
                    int red = 255 - (rgb >> 16) & 0xFF;
                    res.add((double) red);
                }
            }
            return res;
        } catch (Exception e) {
            return null;
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

    public static HashMap<String, NeuralNetwork> getNeuralNetworks() {
        return nn;
    }
}
