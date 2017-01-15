/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tpl_demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.javatuples.Pair;
import org.ujmp.core.*;
import org.ujmp.core.calculation.Calculation.Ret;

/**
 *
 * @author tom
 */
public class MarkovChain {

    private String chain;
    private Matrix matrix;

    public MarkovChain() {
        chain = "";
        matrix = Matrix.Factory.emptyMatrix();
    }

    public MarkovChain(String seq) {
        chain = seq;
        matrix = firstOrderTransitionMatrix();
//        String[] seqs = chain.split(" ");
//        matrix = generateStochasticMatrix(seqs.length, seqs.length);
  
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public static String printMatrixInYangFormat(Matrix mx) {
        String result = "{";
        double[][] m = mx.toDoubleArray();
        for (int r = 0; r < mx.getRowCount(); r++) {
            result += "{";
            for (int c = 0; c < mx.getColumnCount(); c++) {
                result += m[r][c];
                if (c != mx.getColumnCount() - 1) {
                    result += ", ";
                }
            }
            result += "}";
            if (r != mx.getRowCount() - 1) {
                result += ", ";
            }
        }
        return result + "};";
    }

    /* Randomly generate a stochastic transition matrix represents temporal correlations */
    public static Matrix generateStochasticMatrix(int rows, int cols) {
        Matrix result = Matrix.Factory.rand(rows, cols);
        double[][] m = result.toDoubleArray();
        for (int r = 0; r < rows; r++) {
            double sum = 0;
            for (int c = 0; c < cols; c++) {
                sum += m[r][c];
            }
            for (int c = 0; c < cols; c++) {
                double v = Double.parseDouble(String.format("%.2f", m[r][c] / sum));
                result.setAsDouble(v, r, c);
            }
        }
        return result;
    }

    /* Delete rows with all zeros in transition matrix */
    public static Matrix deleteRowsWithAllZeros(Matrix mx) {
        List<Integer> del_rows = new ArrayList<>();
        for (int r = 0; r < mx.getRowCount(); r++) {
            double sum = mx.getRowList().get(r).getValueSum();
            if (sum == 0) {
                del_rows.add(r);
            }
        }
        return mx.deleteRows(Ret.NEW, del_rows);
    }

    /* Construct a first-order Markov chain transition matrix */
    private Matrix firstOrderTransitionMatrix() {
        String[] seqs = chain.split(" ");
        Set<Integer> states = new TreeSet<>();
        for (String s : seqs) {
            states.add(Integer.parseInt(s));
        }

        Map<Pair<String, String>, Integer> transition_ctr = new TreeMap<>();
        for (int a = 0, b = 1; a < seqs.length - 1 && b < seqs.length; a++, b++) {
            Pair<String, String> item = Pair.with(seqs[a], seqs[b]);
            if (transition_ctr.containsKey(item)) {
                int ctr = transition_ctr.get(item);
                transition_ctr.replace(item, ++ctr);
            } else {
                transition_ctr.put(item, 1);
            }
        }

        Matrix result = Matrix.Factory.zeros(states.size(), states.size());
        Map<String, Integer> state_index = new TreeMap<>();
        int idx = 0;
        for (Integer s : states) {
            String str = s.toString();
            state_index.put(str, idx);
            result.setRowLabel(idx, str);
            result.setColumnLabel(idx, str);
            idx++;
        }

        transition_ctr.forEach((k, v) -> {
            if (state_index.containsKey(k.getValue0()) && state_index.containsKey(k.getValue1())) {
                result.setAsDouble(v, state_index.get(k.getValue0()), state_index.get(k.getValue1()));
            }
        });

        double[][] m = result.toDoubleArray();
        for (int r = 0; r < result.getRowCount(); r++) {
            double sum = result.getRowList().get(r).getValueSum();
            if (sum != 0) {
                for (int c = 0; c < result.getColumnCount(); c++) {
                    result.setAsDouble(m[r][c] / sum, r, c);
                }
            }
        }
        return result;
    }

    /* Constructing a second-order Markov chain transition matrix */
    public Matrix secondOrderTransitionMatrix() {
        String[] seqs = chain.split(" ");
        // find all pois in the chain 
        Set<Integer> pois = new TreeSet<>();
        for (String s : seqs) {
            pois.add(Integer.parseInt(s));
        }

        // create row elements and column elements of the matrix
        Set<String> poi_cols = new TreeSet<>();
        pois.forEach((i) -> poi_cols.add(i.toString()));

        Set<String> poi_rows = new TreeSet<>();
        poi_cols.forEach((poi1) -> poi_cols.forEach((poi2) -> poi_rows.add(poi1 + " " + poi2)));

        // ((previous_states, predict_state), counter)
        Map<Pair<String, String>, Integer> transition_ctr = new TreeMap<>();
        int x = 0;
        int y = 1;
        int z = 2;
        while (x < seqs.length - 2 && y < seqs.length - 1 && z < seqs.length) {
            // (previous_states, predict_state)
            Pair<String, String> item = Pair.with(seqs[x] + " " + seqs[y], seqs[z]);
            if (transition_ctr.containsKey(item)) {
                int ctr = transition_ctr.get(item);
                transition_ctr.replace(item, ++ctr);
            } else {
                transition_ctr.put(item, 1);
            }
            x++;
            y++;
            z++;
        }
        
        Matrix result = Matrix.Factory.zeros(poi_rows.size(), poi_cols.size());    
        Map<String, Integer> rows_index = new TreeMap<>();
        int idx = 0;
        for (String s : poi_rows) {
            rows_index.put(s, idx);
            result.setRowLabel(idx, s);
            idx++;
        }

        Map<String, Integer> cols_index = new TreeMap<>();
        idx = 0;
        for (String s : poi_cols) {
            cols_index.put(s, idx);
            result.setColumnLabel(idx, s);
            idx++;
        }

        transition_ctr.forEach((k, v) -> {
            if (rows_index.containsKey(k.getValue0()) && cols_index.containsKey(k.getValue1())) {
                result.setAsDouble(v, rows_index.get(k.getValue0()), cols_index.get(k.getValue1()));
            }
        });

        double[][] m = result.toDoubleArray();
        for (int r = 0; r < result.getRowCount(); r++) {
            double sum = result.getRowList().get(r).getValueSum();
            if (sum == 0) {
                for (int c = 0; c < result.getColumnCount(); c++) {
                    result.setAsDouble(m[r][c] / sum, r, c);
                }
            }
        }
        return result;
    }

}
