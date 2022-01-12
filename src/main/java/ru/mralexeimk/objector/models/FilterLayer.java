package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.singletons.NeuralNetworkListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FilterLayer extends Layer implements Serializable {
    private List<Matrix> data;
    private int K;

    public FilterLayer(int units, int size, int index, LayerType layerType) {
        super(units, size, index, layerType);
        data = new ArrayList<>();
        K = 2;
    }

    public List<Matrix> getData() {
        return data;
    }

    public void setData(List<Matrix> data) {
        this.data = data;
    }

    public void addData(List<Matrix> data, int index) {
        if(index == 0) {
            setData(data);
        }
        else {
            for(int i = 0; i < data.size(); ++i) {
                this.data.get(i).sum(data.get(i));
            }
        }
    }

    public int getK() {
        return K;
    }

    @Override
    public void toDefault() {
        K = getSize()/ getNextLayer().getSize();
    }

    @Override
    public void evaluate() {
        Layer next = getNextLayer();
        if(next instanceof PullingLayer pl) {
            pl.setData(NeuralNetworkListener.evaluateByPulling(data, K));
        }
    }
}
