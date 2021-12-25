package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PullingLayer extends Layer implements Serializable {
    private List<Matrix> data;
    private List<List<Matrix>> W;

    public PullingLayer(int units, int size, LayerType layerType) {
        super(units, size, layerType);
        data = new ArrayList<>();
        W = new ArrayList<>();
    }

    public List<Matrix> getData() {
        return data;
    }

    public void setData(List<Matrix> data) {
        this.data = data;
    }

    public List<List<Matrix>> getW() {
        return W;
    }

    public void setW(List<List<Matrix>> W) {
        this.W = W;
    }

    public void setWeightOfUnit(int unit, List<Matrix> W) {
        getW().get(unit).clear();
        getW().get(unit).addAll(W);
    }

    public void setWW(int unit, int kernel, Matrix W) {
        getW().get(unit).set(kernel, W);
    }

    public Matrix getWW(int unit, int kernel) {
        return getW().get(unit).get(kernel);
    }

    public List<Matrix> getWeightsOfUnit(int unit) {
        return W.get(unit);
    }

    @Override
    public void toDefault() {
        Layer next = getNextLayer();
        if(next instanceof FilterLayer) {
            int kernel = getSize()-next.getSize()+1;
            int val = getNextLayer().getUnits()/getUnits();
            for(int i = 0; i < getUnits(); ++i) {
                List<Matrix> res = new ArrayList<>();
                for (int j = 0; j < val; ++j) {
                    res.add(new Matrix(kernel, kernel, -0.99, 0.99));
                }
                W.add(res);
            }
        }
    }

    @Override
    public void evaluate() {
        Layer next = getNextLayer();
        if(next instanceof NeuronsLayer nl) {
            if(getUnits() == next.getUnits()) {
                List<Double> res = new ArrayList<>();
                for(Matrix m : getData()) {
                    res.add(m.getAverage());
                }
                nl.setData(res);
            }
            else {
                throw new IndexOutOfBoundsException("Pulling layer and NeuronsLayer has different units");
            }
        }
        else if(next instanceof FilterLayer fl) {
            List<Matrix> res = new ArrayList<>();
            for(int i = 0; i < getUnits(); ++i) {
                res.addAll(evaluateByKernel(data.get(i), W.get(i)));
            }
            fl.setData(res);
        }
    }
}
