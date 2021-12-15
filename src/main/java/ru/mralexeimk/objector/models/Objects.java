package ru.mralexeimk.objector.models;
import ru.mralexeimk.objector.models.NeuralNetwork;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class Objects {
    private HashMap<String, NeuralNetwork> nn;
    private String category;

    public Objects(String category) {
        this.category = category;
        Set<String> list = getObjectsInDirectory();
        nn = new HashMap<>();
        for(String id : list) {
            NeuralNetwork neuralNetwork = new NeuralNetwork(category, id);
            nn.put(id, neuralNetwork);
        }
    }

    public Set<String> getObjects() {
        return nn.keySet();
    }

    public Set<String> getObjectsInDirectory() {
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

    public NeuralNetwork addObject(String name) {
        NeuralNetwork neuralNetwork = null;
        if(!getObjects().contains(name)) {
            neuralNetwork = new NeuralNetwork(category, name);
            nn.put(name, neuralNetwork);
        }
        return neuralNetwork;
    }

    public void deleteObject(String name) {
        if(getObjects().contains(name)) {
            nn.remove(name);
            File file = new File("weights/"+category+"/"+name+".w");
            file.delete();
        }
    }

    public void update() {
        for(String name : nn.keySet()) {
            NeuralNetwork neuralNetwork = nn.get(name);
            neuralNetwork.toDefault();
            nn.replace(name, neuralNetwork);
        }
    }

    public String getCategory() {
        return category;
    }

    public int getCount() {
        return nn.size();
    }

    public void train(List<Double> input, String id) {
        List<Double> output = new ArrayList<>(Arrays.asList(0.01, 0.99));
        List<Double> output2 = new ArrayList<>(Arrays.asList(0.99, 0.01));
        if(getNeuralNetworks().containsKey(id)) {
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

    public String query(List<Double> input) {
        String res = null;
        double max = 0;
        for(String name : getNeuralNetworks().keySet()) {
            List<Double> output = getNeuralNetwork(name).query(input);
            if(output.get(0) > max) {
                max = output.get(0);
                res = name;
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
                String correct = spl[0];
                List<Double> inputs = new ArrayList<>();
                for(int i = 1; i < spl.length; ++i) {
                    double t = Double.parseDouble(spl[i]);
                    t = (t/255.0)*0.99 + 0.01;
                    inputs.add(t);
                }
                String target = query(inputs);
                System.out.println("Correct: " + correct + ", Output: " + target);
                if(correct == target) ++count_correct;
            }
            System.out.println((double)100*count_correct/count + "%");
            sc.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public List<Double> parseImage(String path) {
        List<Double> res = new ArrayList<>();
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(getClass().getResource(path).toURI()));
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

    public NeuralNetwork getNeuralNetwork(String id) {
        return nn.get(id);
    }

    public HashMap<String, NeuralNetwork> getNeuralNetworks() {
        return nn;
    }
}
