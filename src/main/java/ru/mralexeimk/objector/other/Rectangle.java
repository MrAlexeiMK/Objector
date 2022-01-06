package ru.mralexeimk.objector.other;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rectangle {
    private int x, y;
    private int lenX, lenY;
    private Pair<String, Double> queryRes;

    public void addLenX(int perLen) {
        lenX += perLen;
    }

    public void addLenY(int perLen) {
        lenY += perLen;
    }
}
