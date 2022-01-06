package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InputLayer extends Layer implements Serializable {
    private Matrix data;
    private List<Matrix> W;

    public InputLayer(int units, int size, int index, LayerType layerType) {
        super(units, size, index, layerType);
        data = new Matrix(size, size);
        W = new ArrayList<>();
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
        Layer next = getNextLayer();
        if(next instanceof FilterLayer) {
            int kernel = getSize()-next.getSize()+1;
            for(int i = 0; i < getNextLayer().getUnits(); ++i) {
                W.add(new Matrix(kernel, kernel, -1/Math.sqrt(next.getSize()*next.getSize()),
                        1/Math.sqrt(next.getSize()*next.getSize())));
            }
        }
        else if(next instanceof NeuronsLayer) {
            W.add(new Matrix(getSize()*getSize(), getNextLayer().getUnits(),
                    -1/Math.sqrt(getNextLayer().getUnits()), 1/Math.sqrt(getNextLayer().getUnits())));
        }
    }

    @Override
    public void evaluate() {
        Layer next = getNextLayer();
        if(next instanceof NeuronsLayer nl) {
            nl.setData(evaluateByDefault(data, W.get(0)));
        }
        else if(next instanceof FilterLayer fl) {
            fl.setData(evaluateByKernel(data, W));
        }
        else if(next instanceof PullingLayer pl) {
            pl.setData(evaluateByPulling(new ArrayList<>(List.of(data)), getSize()/pl.getSize()));
        }
    }
}
