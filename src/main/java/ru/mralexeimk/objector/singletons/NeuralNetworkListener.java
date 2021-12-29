package ru.mralexeimk.objector.singletons;

import lombok.Data;
import ru.mralexeimk.objector.models.NeuralNetwork;
import ru.mralexeimk.objector.other.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class NeuralNetworkListener {
    private static NeuralNetwork neuralNetwork;
    private static int queueCount;
    private static Pair<String, Double> queryRes;

    public static void init(String id) {
        neuralNetwork = new NeuralNetwork(id);
        queueCount = 0;
        queryRes = new Pair<>("Nothing", 0.99);
    }

    public static NeuralNetwork get() {
        return neuralNetwork;
    }

    public static int getQueueCount() {
        return queueCount;
    }

    public static void increaseQueueCount() {
        queueCount++;
    }

    public static void save() {
        get().saveWeights(get().getId());
    }

    public static void threadTrain(List<Double> input, String id) {
        new Thread(() -> {
            get().train(input, id);
            queueCount--;
        }).start();
    }

    public static Pair<String, Double> getQueryResult() {
        return queryRes;
    }

    public static void threadQuery(List<Double> input) {
        new Thread(() -> queryRes = get().queryMaxPair(input)).start();
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
                    int green = 255 - (rgb >> 8) & 0xFF;
                    int blue = 255 - rgb & 0xFF;
                    res.add((double) (red+green+blue)/3.0);
                }
            }
            return res;
        } catch (Exception e) {
            return null;
        }
    }
}
