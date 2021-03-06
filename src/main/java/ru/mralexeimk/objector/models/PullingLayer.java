package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.singletons.NeuralNetworkListener;
import ru.mralexeimk.objector.singletons.SettingsListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PullingLayer extends Layer implements Serializable {
    private List<Matrix> data;
    private List<List<Matrix>> W;
    private List<List<Double>> biases;

    public PullingLayer(int units, int size, int index, LayerType layerType) {
        super(units, size, index, layerType);
        data = new ArrayList<>();
        biases = new ArrayList<>();
        W = new ArrayList<>();
    }

    public List<List<Double>> getBiases() {
        return biases;
    }

    public List<Matrix> getData() {
        return data;
    }

    public List<List<Matrix>> getW() {
        return W;
    }

    public void setData(List<Matrix> data) {
        this.data = data;
    }

    public void setWW(int unit, int kernel, Matrix W) {
        this.W.get(unit).set(kernel, W);
    }

    public Matrix getWW(int unit, int kernel) {
        return W.get(unit).get(kernel);
    }

    @Override
    public void toDefault() {
        W.clear();
        biases.clear();
        Layer next = getNextLayer();
        if(next instanceof FilterLayer) {
            int kernel = getSize()-next.getSize()+1;
            int val = getNextLayer().getUnits()/getUnits();
            for(int i = 0; i < getUnits(); ++i) {
                List<Matrix> res = new ArrayList<>();
                for (int j = 0; j < val; ++j) {
                    if(SettingsListener.get().isDefaultKernels() && kernel == 3) {
                        res.add(NeuralNetworkListener.getDefaultKernel(i*getUnits()+j));
                    }
                    else {
                        res.add(new Matrix(kernel, kernel, -1 / Math.sqrt(next.getSize() * next.getSize()),
                                1 / Math.sqrt(next.getSize() * next.getSize())));
                    }
                }
                W.add(res);
                biases.add(new ArrayList<>(Collections.nCopies(res.size(), 0.0)));
            }
        }
    }

    @Override
    public void evaluate() {
        Layer next = getNextLayer();
        if(next instanceof NeuronsLayer nl) {
            List<Double> res = new ArrayList<>();
            if(getUnits() == nl.getUnits()) {
                for(Matrix m : getData()) {
                    res.add(m.getAverage());
                }
                nl.setData(res);
            }
            else {
                for(Matrix m : getData()) {
                    res.addAll(m.toList());
                }
                if(nl.getUnits() == getUnits()*getSize()*getSize()) {
                    nl.setData(res);
                }
                else {
                    nl.addData(res);
                }
            }
        }
        else if(next instanceof FilterLayer fl) {
            List<Matrix> res = new ArrayList<>();
            for(int i = 0; i < getUnits(); ++i) {
                res.addAll(NeuralNetworkListener.evaluateByKernel(data.get(i), W.get(i), biases.get(i)));
            }
            fl.setData(res);
        }
    }
}
