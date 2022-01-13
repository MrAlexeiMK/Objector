package ru.mralexeimk.objector.singletons;

import lombok.Data;
import ru.mralexeimk.objector.models.Matrix;
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
    private static List<Matrix> kernels = new ArrayList<>(List.of(
            new Matrix("0.0625,0.125,0.0625|0.125,0.25,0.125|0.0625,0.125,0.0625"),
            new Matrix("-1,-2,-1|0,0,0|1,2,1"),
            new Matrix("-2,-1,0|-1,1,1|0,1,2"),
            new Matrix("0,0,0|0,1,0|0,0,0"),
            new Matrix("1,0,-1|2,0,-2|1,0,-1"),
            new Matrix("-1,-1,-1|-1,8,-1|-1,-1,-1"),
            new Matrix("-1,0,1|-2,0,2|-1,0,1"),
            new Matrix("0,-1,0|-1,5,-1|0,-1,0"),
            new Matrix("1,2,1|0,0,0|-1,-2,-1")
    ));

    public static void init(String id) {
        queueCount = 0;
        isFinished = true;
        cells = new ArrayList<>();
        queryRes = new Pair<>("Nothing", 0.99);
        rectangles = new ArrayList<>();
        neuralNetwork = new NeuralNetwork(id);
    }

    public static Matrix getDefaultKernel(int index) {
        return kernels.get(index%getDefaultKernelsSize());
    }

    public static int getDefaultKernelsSize() {
        return kernels.size();
    }

    public static double activationFunSig(double x) {
        return 1.0/(1 + Math.exp(-x));
    }

    public static double activationFunSoftPlus(double x) {
        return Math.min(Math.log(1 + Math.exp(x)), 0.99);
    }

    public static double activationFunReLU(double x) {
        if(x >= 0) return Math.min(x, 0.99);
        return 0.001*x;
    }

    public static double activationFunTanh(double x) {
        return (Math.exp(x) - Math.exp(-x))/(Math.exp(x) + Math.exp(-x));
    }

    public static Matrix activationFun(Matrix m) {
        Matrix res = new Matrix(m.getN(), m.getM());
        for(int x = 0; x < m.getN(); ++x) {
            for(int y = 0; y < m.getM(); ++y) {
                res.set(x, y, activationFunSig(m.get(x, y)));
            }
        }
        return res;
    }

    public static List<Double> evaluateByDefault(List<Double> data, Matrix W) {
        return evaluateByDefault(new Matrix(data), W);
    }

    public static List<Double> evaluateByDefault(Matrix data, Matrix W) {
        Matrix outputs = new Matrix(data.toList());
        outputs = activationFun(W.multiply(outputs));
        List<Double> res = new ArrayList<>();
        for(int y = 0; y < outputs.getM(); ++y) {
            for(int x = 0; x < outputs.getN(); ++x) {
                res.add(outputs.get(x, y));
            }
        }
        return res;
    }

    public static List<Matrix> evaluateByKernel(Matrix A, List<Matrix> kernels, List<Double> biases) {
        List<Matrix> res = new ArrayList<>();
        for (int i = 0; i < kernels.size(); ++i) {
            Matrix K = kernels.get(i);
            Matrix B = new Matrix(A);
            B.convertByKernel(K);
            B.sum(biases.get(i));
            B = activationFun(B);
            res.add(B);
        }
        return res;
    }

    public static Matrix evaluateByPulling(Matrix A, int K) {
        Matrix B = new Matrix(A);
        B.resize(K);
        return B;
    }

    public static List<Matrix> evaluateByPulling(List<Matrix> A, int K) {
        List<Matrix> res = new ArrayList<>();
        for(Matrix m : A) {
            Matrix B = new Matrix(m);
            B.resize(K);
            res.add(B);
        }
        return res;
    }

    public static void setQueueCount(int count) {
        queueCount = count;
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

    public static void decreaseQueueCount() {
        queueCount--;
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
        if(!SettingsListener.get().isSeparated()) return a;
        if(a <= SettingsListener.get().getSeparate()) return 0.01;
        return 0.99;
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

    public static List<Double> pixelConvert(int rgb) {
        int red = 255 - (rgb >> 16) & 0xFF;
        int green = 255 - (rgb >> 8) & 0xFF;
        int blue = 255 - rgb & 0xFF;
        double r = colorConvert((red/255.0)*0.99 + 0.01);
        double g = colorConvert((green/255.0)*0.99 + 0.01);
        double b = colorConvert((blue/255.0)*0.99 + 0.01);
        return new ArrayList<>(List.of(r, g, b));
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
