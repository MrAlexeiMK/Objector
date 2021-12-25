package ru.mralexeimk.objector.models;

import lombok.Data;
import ru.mralexeimk.objector.other.LayerType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public abstract class Layer implements Serializable {
    private int units;
    private int size;
    private LayerType layerType;
    private Layer nextLayer;

    public Layer(int units, int size, LayerType layerType) {
        this.units = units;
        this.size = size;
        this.layerType = layerType;
        nextLayer = null;
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

    public List<Double> evaluateByDefault(List<Double> data, Matrix W) {
        return evaluateByDefault(new Matrix(data), W);
    }

    public List<Double> evaluateByDefault(Matrix data, Matrix W) {
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

    public List<Matrix> evaluateByKernel(Matrix A, List<Matrix> kernels) {
        List<Matrix> res = new ArrayList<>();
        for(int i = 0; i < kernels.size(); ++i) {
            Matrix K = kernels.get(i);
            Matrix B = new Matrix(A);
            B.convertByKernel(K);
            B = new Matrix(activationFun(B));
            res.add(B);
        }
        return res;
    }

    public Matrix evaluateByPulling(Matrix A, int K) {
        Matrix B = new Matrix(A);
        B.resize(K);
        return B;
    }

    public List<Matrix> evaluateByPulling(List<Matrix> A, int K) {
        List<Matrix> res = new ArrayList<>();
        for(Matrix m : A) {
            Matrix B = new Matrix(m);
            B.resize(K);
            res.add(B);
        }
        return res;
    }

    public String toString() {
        return getUnits()+"@"+getSize()+"x"+getSize() +", "+getLayerType().toString();
    }

    public void toDefault() {};

    public void evaluate() {};
}
