package ru.mralexeimk.objector.singletons;

import lombok.Data;
import ru.mralexeimk.objector.models.NeuralNetwork;
import ru.mralexeimk.objector.other.Pair;
import ru.mralexeimk.objector.other.Rectangle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NeuralNetworkListener {
    private static NeuralNetwork neuralNetwork;
    private static int queueCount;
    private static Pair<String, Double> queryRes;
    private static List<Rectangle> rectangles;
    private static boolean isFinished;
    private static List<List<List<Pair<String, Double>>>> cells;

    public static void init(String id) {
        queueCount = 0;
        isFinished = true;
        cells = new ArrayList<>();
        queryRes = new Pair<>("Nothing", 0.99);
        rectangles = new ArrayList<>();
        neuralNetwork = new NeuralNetwork(id);
    }

    public static NeuralNetwork get() {
        return neuralNetwork;
    }

    public static List<Rectangle> getRectangles() {
        return rectangles;
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

    public static void threadTrain(List<List<Double>> input, String id) {
        new Thread(() -> {
            get().train(input, id);
            if(queueCount > 0) queueCount--;
        }).start();
    }

    public static Pair<String, Double> getQueryResult() {
        return queryRes;
    }

    public static void threadQuery(List<List<Double>> input) {
        if(isFinished) {
            new Thread(() -> {
                isFinished = false;
                queryRes = get().queryMaxPair(input);
                isFinished = true;
            }).start();
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

    public static double colorConvert(double a) {
        if(a <= 0.4) return 0.01;
        else if(a <= 0.7) return 0.5;
        else return 0.99;
    }

    public static List<Rectangle> querySeveralObjects(BufferedImage im, int perLen) {
        List<Rectangle> res = new ArrayList<>();
        cells = new ArrayList<>();
        for(int yStart = 0; yStart <= im.getHeight()-perLen; yStart+=perLen) {
            List<List<Pair<String, Double>>> cell = new ArrayList<>();
            for(int xStart = 0; xStart <= im.getWidth()-perLen; xStart+=perLen) {
                BufferedImage crop = cropImage(im, xStart, yStart, perLen, perLen);
                List<List<Double>> data = parseImage(crop, get().getInputLayer(0).getSize());
                List<Pair<String, Double>> pair = get().queryPairs(data);
                cell.add(pair);
            }
            cells.add(cell);
        }
        for(int y = 0; y < cells.size(); ++y) {
            for(int x = 0; x < cells.get(y).size(); ++x) {
                List<Pair<String, Double>> pairs = new ArrayList<>(cells.get(y).get(x));
                Pair<String, Double> maxPair = null;
                double max = 0;
                for(Pair<String, Double> pair : pairs) {
                    if(pair.getSecond() > max) {
                        max = pair.getSecond();
                        maxPair = pair;
                    }
                }
                if(maxPair != null && !maxPair.getFirst().equals("Nothing")) {
                    Rectangle rect = new Rectangle(x*perLen, y*perLen, perLen, perLen, maxPair);
                    res.add(rect);
                }
            }
        }
        return res;
    }

    public static void threadQuerySeveralObjects(BufferedImage im, int perLen) {
        if(isFinished) {
            new Thread(() -> {
                isFinished = false;
                rectangles = querySeveralObjects(im, perLen);
                isFinished = true;
            }).start();
        }
    }

    public static List<List<Double>> extractData(BufferedImage im, int xStart, int yStart, int perLen) {
        List<List<Double>> res = new ArrayList<>();
        for(int i = 0; i < neuralNetwork.getLayers().get(0).size(); ++i) {
            res.add(new ArrayList<>());
        }
        for (int y = yStart; y < yStart+perLen; ++y) {
            for (int x = xStart; x < xStart+perLen; ++x) {
                int rgb = im.getRGB(x, y);
                int red = 255 - (rgb >> 16) & 0xFF;
                int green = 255 - (rgb >> 8) & 0xFF;
                int blue = 255 - rgb & 0xFF;
                if(neuralNetwork.isSeveralInputs()) {
                    double r = (red/255.0)*0.99 + 0.01;
                    double g = (green/255.0)*0.99 + 0.01;
                    double b = (blue/255.0)*0.99 + 0.01;
                    res.get(0).add(colorConvert(r));
                    res.get(1).add(colorConvert(g));
                    res.get(2).add(colorConvert(b));
                }
                else {
                    double mid = (red+green+blue)/3.0;
                    mid = (mid/255.0)*0.99 + 0.01;
                    res.get(0).add(colorConvert(mid));
                }
            }
        }
        return res;
    }

    public static BufferedImage cropImage(BufferedImage src, int x, int y, int xLen, int yLen) {
        BufferedImage dest = new BufferedImage(xLen, yLen, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = dest.getGraphics();
        g.drawImage(src, 0, 0, xLen, yLen, x, y, x+xLen, y+yLen, null);
        g.dispose();
        return dest;
    }

    public static List<List<Double>> parseImage(BufferedImage im, int len) {
        try {
            im = resize(im, len, len);
            return extractData(im, 0, 0, len);
        } catch (Exception e) {
            return null;
        }
    }
}
