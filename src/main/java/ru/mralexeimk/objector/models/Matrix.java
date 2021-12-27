package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.Pair;

import java.io.Serializable;
import java.util.*;

public class Matrix implements Serializable {
    private List<List<Double>> data;
    private int N, M;

    public Matrix() {
        Scanner sc = new Scanner(System.in);
        N = sc.nextInt();
        M = sc.nextInt();
        data = new ArrayList<>();
        double temp = 0;
        for(int i = 0; i < M; ++i) {
            ArrayList<Double> arr = new ArrayList<>();
            for(int j = 0; j < N; ++j) {
                temp = sc.nextDouble();
                arr.add(temp);
            }
            data.add(arr);
        }
        sc.close();
    }

    public Matrix(int N, int M) {
        this.N = N;
        this.M = M;
        data = new ArrayList<>();
        for(int i = 0; i < M; ++i) {
            data.add(new ArrayList<>(Collections.nCopies(N, 0.0)));
        }
    }

    public Matrix(int N, int M, double from, double to) {
        this.N = N;
        this.M = M;
        data = new ArrayList<>();
        for(int y = 0; y < M; ++y) {
            List<Double> arr = new ArrayList<>();
            for(int x = 0; x < N; ++x) {
                arr.add(from + new Random().nextDouble() * (to - from));
            }
            data.add(arr);
        }
    }

    public Matrix(Matrix m) {
        assign(m);
    }

    public Matrix(List<Double> vector) {
        N = 1;
        M = vector.size();
        data = new ArrayList<>();
        for(int y = 0; y < M; ++y) {
            data.add(new ArrayList<>(Arrays.asList(vector.get(y))));
        }
    }

    public List<Double> toList() {
        List<Double> res = new ArrayList<>();
        for(int y = 0; y < getM(); ++y) {
            for(int x = 0; x < getN(); ++x) {
                res.add(get(x, y));
            }
        }
        return res;
    }

    public void assign(Matrix m) {
        this.N = m.getN();
        this.M = m.getM();
        data = new ArrayList<>();
        for(int i = 0; i < M; ++i) {
            data.add(new ArrayList<>());
        }
        for(int x = 0; x < m.getN(); ++x) {
            for(int y = 0; y < m.getM(); ++y) {
                data.get(y).add(m.get(x, y));
            }
        }
    }

    public int getN() {
        return N;
    }

    public int getM() {
        return M;
    }

    public double get(int x, int y) {
        return data.get(y).get(x);
    }

    public List<Double> getLine(int y) {
        return data.get(y);
    }

    public void setLine(int y, List<Double> list) {
        data.set(y, list);
    }

    public void setColumn(int x, List<Double> list) {
        for(int y = 0; y < M; ++y) {
            set(x, y, list.get(y));
        }
    }

    public List<Double> getColumn(int x) {
        List<Double> res = new ArrayList<>();
        for(int y = 0; y < M; ++y) {
            res.add(get(x, y));
        }
        return res;
    }

    public void set(int x, int y, double value) {
        data.get(y).set(x, value);
    }

    public Matrix getNegative() {
        Matrix res = new Matrix(this);
        for(int x = 0; x < N; ++x) {
            for(int y = 0; y < M; ++y) {
                res.set(x, y, -get(x, y));
            }
        }
        return res;
    }

    public void toNull() {
        for(int x = 0; x < N; ++x) {
            for(int y = 0; y < M; ++y) {
                set(x, y, 0);
            }
        }
    }

    public void toIdentity(boolean fillZeroes) {
        if(fillZeroes) {
            for (int x = 0; x < N; ++x) {
                for (int y = 0; y < M; ++y) {
                    if (x != y) {
                        set(x, y, 0);
                    } else set(x, y, 1);
                }
            }
        }
        else {
            int size = Math.min(N, M);
            for(int i = 0; i < size; ++i) {
                set(i, i, 1);
            }
        }
    }

    public void swapLines(int l1, int l2) {
        List<Double> temp = new ArrayList<>(getLine(l1));
        setLine(l1, getLine(l2));
        setLine(l2, temp);
    }

    public void swapColumns(int c1, int c2) {
        List<Double> temp = new ArrayList<>(getColumn(c1));
        setColumn(c1, getColumn(c2));
        setColumn(c2, temp);
    }

    public void transpose() {
        if(N == M) {
            for (int x = 0; x < N; ++x) {
                for (int y = 0; y < M; ++y) {
                    set(y, x, get(x, y));
                }
            }
        }
        else {
            Matrix res = new Matrix(M, N);
            for (int x = 0; x < N; ++x) {
                for (int y = 0; y < M; ++y) {
                    res.set(y, x, get(x, y));
                }
            }
            assign(res);
        }
    }

    public void convertByKernel(Matrix K) {
        if(K.getN() <= getN() && K.getM() <= getM()) {
            Matrix res = new Matrix(getN() - K.getN() + 1, getM() - K.getM() + 1);
            for(int y = 0; y < res.getM(); ++y) {
                for(int x = 0; x < res.getN(); ++x) {
                    double val = 0;
                    for(int x1 = x; x1 < x+K.getN(); ++x1) {
                        for(int y1 = y; y1 < y + K.getM(); ++y1) {
                            val += get(x1, y1)*K.get(x1-x, y1-y);
                        }
                    }
                    res.set(x, y, val);
                }
            }
            assign(res);
        }
    }

    public void resize(int K) { //divide by K
        if(K <= getN() && K <= getM()) {
            Matrix res = new Matrix(getN()/K, getM()/K);
            for(int y = 0; y < res.getM(); ++y) {
                for(int x = 0; x < res.getN(); ++x) {
                    double val = 0;
                    for(int x1 = K*x; x1 < K*x+K; x1++) {
                        for(int y1 = K*y; y1 < K*y+K; y1++) {
                            val = Math.max(val, get(x1, y1));
                        }
                    }
                    res.set(x, y, val);
                }
            }
            assign(res);
        }
    }

    public List<Double> getSquareValues(int K, int index) {
        List<Double> res = new ArrayList<>();
        for(Pair<Integer, Integer> pair : getSquare(K, index)) {
            res.add(get(pair.getFirst(), pair.getSecond()));
        }
        return res;
    }

    public List<Pair<Integer, Integer>> getSquare(int K, int index) {
        List<Pair<Integer, Integer>> coords = new ArrayList<>();
        if(N%K == 0 && M%K == 0) {
            int nextN = N/K;
            int x = (index%nextN) * K;
            int y = (index/nextN) * K;
            for(int x1 = x; x1 < x + K; ++x1) {
                for(int y1 = y; y1 < y + K; ++y1) {
                    coords.add(new Pair<>(x1, y1));
                }
            }
        }
        else throw new IndexOutOfBoundsException("N%K != 0 or M%K != 0");
        return coords;
    }

    public void sumIntoSquare(int K, int index, double error) {
        if(N%K == 0 && M%K == 0) {
            int x = (index%N) * K;
            int y = (index/N) * K;
            for(int x1 = x; x1 < x + K; ++x1) {
                for(int y1 = y; y1 < y + K; ++y1) {
                    set(x1, y1, get(x1, y1) + error);
                }
            }
        }
        else throw new IndexOutOfBoundsException("N%K != 0 or M%K != 0");
    }

    public double getAverage() {
        double res = 0;
        for(int y = 0; y < getM(); ++y) {
            for(int x = 0; x < getN(); ++x) {
                res += get(x, y);
            }
        }
        res /= getN()*getM();
        return res;
    }

    public Matrix getTranspose() {
        Matrix res = new Matrix(this);
        res.transpose();
        return res;
    }

    public Matrix getSubMatrix(int c1, int c2) {
        Matrix res = new Matrix(c2-c1+1, getM());
        for(int y = 0; y < M; ++y) {
            for(int x = c1; x <= c2; ++x) {
                res.set(x - c1, y, get(x, y));
            }
        }
        return res;
    }

    public int toTriangular(boolean down) {
        int swaps = 0;
        int size = Math.min(M, N);
        double l1, l2;
        if(down) {
            for (int L1 = 0; L1 < size; ++L1) {
                for (int L2 = L1 + 1; L2 < size; ++L2) {
                    l1 = get(L1, L1);
                    l2 = get(L1, L2);

                    if (l2 == 0) continue;
                    else if (l1 == 0) {
                        swapLines(L1, L2);
                        ++swaps;
                        l1 = get(L1, L1);
                        l2 = get(L1, L2);
                    }
                    double val = -(l2 / l1);
                    set(L1, L2, 0);
                    for (int x = L1 + 1; x < N; ++x) {
                        set(x, L2, get(x, L2) + get(x, L1) * val);
                    }
                }
            }
        }
        else {
            for (int L1 = 1; L1 <= size-1; ++L1) {
                for (int L2 = L1 - 1; L2 >= 0; --L2) {
                    l1 = get(L1, L1);
                    l2 = get(L1, L2);

                    if (l2 == 0) continue;
                    else if (l1 == 0) {
                        swapLines(L1, L2);
                        ++swaps;
                        l1 = get(L1, L1);
                        l2 = get(L1, L2);
                    }
                    double val = -(l2 / l1);
                    set(L1, L2, 0);
                    for (int x = L1 + 1; x < N; ++x) {
                        set(x, L2, get(x, L2) + get(x, L1) * val);
                    }
                }
            }
        }
        return swaps;
    }

    public double getTrack() {
        double ans = get(0, 0);
        for(int i = 1; i < Math.min(N, M); ++i) {
            ans *= get(i, i);
        }
        return ans;
    }

    public double getDeterminant() {
        Matrix A = new Matrix(this);
        int swaps = A.toTriangular(true);
        double ans = A.getTrack();
        return (swaps%2 == 0) ? ans : -ans;
    }

    public void toUnit() {
        toTriangular(true);
        toTriangular(false);
        int size = Math.min(N, M);
        for(int i = 0; i < size; ++i) {
            double el = get(i, i);
            if(el != 0 && el != 1) {
                for(int x = 0; x < N; ++x) {
                    set(x, i, get(x, i)/el);
                }
            }
        }
    }

    public void joinRight(Matrix m) {
        if(M == m.getM()) {
            N += m.getN();
            for(int y = 0; y < M; ++y) {
                for(double el : m.getLine(y)) {
                    getLine(y).add(el);
                }
            }
        }
    }

    public Matrix getInverse() {
        if(N != M) throw new IndexOutOfBoundsException("Matrix must be square");
        Matrix E = new Matrix(N, M);
        E.toIdentity(false);
        Matrix A = new Matrix(this);
        A.joinRight(E);
        A.toUnit();
        for(int i = 0; i < A.getM(); ++i) {
            if (A.get(i, i) == 0) throw new RuntimeException("Determinant mustn't be 0");
        }
        return A.getSubMatrix(N, 2*N-1);
    }

    public void print() {
        System.out.println("("+N+";"+M+"):");
        for(int y = 0; y < M; ++y) {
            for(int x = 0; x < N; ++x) {
                System.out.print(Math.round(get(x, y)*100.0)/100.0+"|");
            }
            System.out.println();
        }
    }

    public String toString() {
        String res = N+" " + M + "\n";
        for(int y = 0; y < M; ++y) {
            for(int x = 0; x < N; ++x) {
                res += get(x, y)+" ";
            }
            res += "\n";
        }
        return res;
    }

    public Matrix sum(double val) {
        Matrix res = new Matrix(getN(), getM());
        for(int x = 0; x < N; ++x) {
            for(int y = 0; y < M; ++y) {
                res.set(x, y, get(x, y) + val);
            }
        }
        return res;
    }

    public Matrix sum(Matrix m) {
        Matrix res = new Matrix(getN(), getM());
        for(int x = 0; x < N; ++x) {
            for(int y = 0; y < M; ++y) {
                res.set(x, y, get(x, y) + m.get(x, y));
            }
        }
        return res;
    }

    public Matrix minus(double val) {
        return sum(-val);
    }

    public Matrix minus(Matrix m) {
        return sum(m.getNegative());
    }

    public Matrix multiply(double val) {
        Matrix res = new Matrix(getN(), getM());
        for(int x = 0; x < N; ++x) {
            for(int y = 0; y < M; ++y) {
                res.set(x, y, get(x, y) * val);
            }
        }
        return res;
    }

    public Matrix multiply(Matrix m) {
        if(N == m.getM()) {
            Matrix res = new Matrix(m.getN(), M);
            for (int y = 0; y < M; ++y) {
                for (int x = 0; x < m.getN(); ++x) {
                    for(int i = 0; i < N; ++i) {
                        res.set(x, y, res.get(x, y) + get(i, y)*m.get(x, i));
                    }
                }
            }
            return res;
        }
        else if(N == m.getN() && M == m.getM()) {
            Matrix res = new Matrix(N, M);
            for(int y = 0; y < M; ++y) {
                for(int x = 0; x < N; ++x) {
                    res.set(x, y, get(x, y) * m.get(x, y));
                }
            }
            return res;
        }
        return null;
    }
}
