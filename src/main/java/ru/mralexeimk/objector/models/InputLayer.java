package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.singletons.NeuralNetworkListener;
import ru.mralexeimk.objector.singletons.SettingsListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InputLayer extends Layer implements Serializable {
    private Matrix data;
    private List<Matrix> W;
    private List<Double> biases;

    public InputLayer(int units, int size, int index, LayerType layerType) {
        super(units, size, index, layerType);
        data = new Matrix(size, size);
        biases = new ArrayList<>();
        W = new ArrayList<>();
    }

    public List<Double> getBiases() {
        return biases;
    }

    public Matrix getData() {
        return data;
    }

    public List<Matrix> getW() {
        return W;
    }

    public void setW(int index, Matrix W) {
        this.W.set(index, W);
    }

    public void setData(List<Double> data) {
        int i = 0;
        for(int y = 0; y < getSize(); ++y) {
            for(int x = 0; x < getSize(); ++x) {
                try {
                    this.data.set(x, y, data.get(i));
                    ++i;
                } catch (IndexOutOfBoundsException ex) {
                    System.out.println("Input image size error");
                }
            }
        }
    }

    @Override
    public void toDefault() {
        W.clear();
        biases.clear();
        Layer next = getNextLayer();
        if(next instanceof FilterLayer) {
            int kernel = getSize()-next.getSize()+1;
            for(int i = 0; i < getNextLayer().getUnits(); ++i) {
                if (SettingsListener.get().isDefaultKernels() && kernel == 3) {
                    W.add(NeuralNetworkListener.getDefaultKernel(i));
                }
                else {
                    W.add(new Matrix(kernel, kernel, -1 / Math.sqrt(next.getSize() * next.getSize()),
                            1 / Math.sqrt(next.getSize() * next.getSize())));
                }
            }
        }
        else if(next instanceof NeuronsLayer) {
            W.add(new Matrix(getSize() * getSize(), getNextLayer().getUnits(),
                        -1 / Math.sqrt(getNextLayer().getUnits()), 1 / Math.sqrt(getNextLayer().getUnits())));
        }
        biases = new ArrayList<>(Collections.nCopies(W.size(), 0.0));
    }

    @Override
    public void evaluate() {
        Layer next = getNextLayer();
        if(next instanceof NeuronsLayer nl) {
            nl.setData(NeuralNetworkListener.evaluateByDefault(data, W.get(0)));
        }
        else if(next instanceof FilterLayer fl) {
            fl.addData(NeuralNetworkListener.evaluateByKernel(data, W, biases), getIndex());
        }
        else if(next instanceof PullingLayer pl) {
            pl.setData(NeuralNetworkListener.evaluateByPulling(new ArrayList<>(List.of(data)), getSize()/pl.getSize()));
        }
    }
}
