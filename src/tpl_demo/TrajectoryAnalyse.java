/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tpl_demo;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.javatuples.Pair;
import org.ujmp.core.Matrix;
import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.type.OctaveDouble;

/**
 *
 * @author tom
 */
public class TrajectoryAnalyse {

    private String db;
    private String tab;
    private int users;
    private int times;

    public TrajectoryAnalyse(String db_name, String tab_name, int usr_num, int run_times) {
        db = db_name;
        tab = tab_name;
        users = usr_num;
        times = run_times;
    }

    private static double addLaplaceNoise(double val, double budget, double sensitivity) {
        double scale = sensitivity / budget;
        int result = (int) (new LaplaceDistribution(0, scale).sample() + val);
        if (result < 0) {
            result = 0;
        }
        return result;
    }

    private Pair<Double, Double> findQD(Matrix p, double a) {
        double result = 0;
        double q_val = 0;
        double d_val = 0;
        double[][] m = p.toDoubleArray();
        for (double[] q : m) {
            for (double[] d : m) {
                boolean isSame = true;
                for (int c = 0; c < p.getColumnCount(); c++) {
                    if (q[c] != d[c]) {
                        isSame = false;
                        break;
                    }
                }

                if (!isSame) { // for two different rows : q and d
                    List<Integer> plus_set = new ArrayList<>(); // index of elements in q+ and d+
                    for (int j = 0; j < p.getColumnCount(); j++) { // for each column in row 
                        if (q[j] > d[j]) {
                            plus_set.add(j);
                        }
                    }

                    boolean update = false;
                    double q_sum = 0;
                    double d_sum = 0;
                    do {
                        for (int j : plus_set) {
                            q_sum += q[j];
                            d_sum += d[j];
                        }

                        double maxVal = maxValue(q_sum, d_sum, a);
                        for (Iterator<Integer> itr = plus_set.iterator(); itr.hasNext();) {
                            int j = itr.next();
                            Double tmp = q[j] / d[j];
                            if (tmp <= maxVal) {
                                itr.remove();
                            }
                        }
                    } while (update);

                    double logVal = Math.log(maxValue(q_sum, d_sum, a));
                    if (result < logVal) {
                        result = logVal;
                        q_val = q_sum;
                        d_val = d_sum;
                    }
                } // q != d		
            } // for d
        } // for q

        return Pair.with(q_val, d_val);
    }

    /* Theorem 5: supremum value of BPL (or FPL) over time */
    private double supremumValue(double q, double d, double a) {
        double result = 0;

        OctaveEngine octave = new OctaveEngineFactory().getScriptEngine();
        octave.eval("pkg load symbolic");
        octave.eval("syms x");
        octave.eval("q = " + q);
        octave.eval("d = " + d);
        octave.eval("a = " + a);

        if (d != 0) {
            octave.eval("solve(log((sqrt(rat(4*d*(1-q))*exp(x)+(rat(d-1)+rat(q)*exp(x)).^2)+rat(d-1)+rat(q)*exp(x))/rat(2*d)) == rat(a), x)");
            octave.eval("eps = double(ans)");
            OctaveDouble eps = octave.get(OctaveDouble.class, "eps");
            result = eps.get(1);
        }
        if (d == 0 && q != 1) {
            octave.eval("solve(log((rat(1-q)*exp(x))/(1-rat(q)*exp(x))) == rat(a), x)");
            octave.eval("eps = double(ans)");
            OctaveDouble eps = octave.get(OctaveDouble.class, "eps");
            int i = 1;
            result = eps.get(i);
            System.out.println("log(1/q) = " + Math.log(1 / q));
            while (result > Math.log(1 / q)) {
                result = eps.get(i++);
            }
        }
        octave.close();
        return result;
    }

    /* Releasing Data by upper bound */
    public double findUpperBound(Matrix pb, Matrix pf, double a) {
        double ab = a / 2;
        Pair<Double, Double> qdb = findQD(pb, ab);
        double eb = supremumValue(qdb.getValue0(), qdb.getValue1(), ab);

        double af = a - ab + eb;
        Pair<Double, Double> qdf = findQD(pf, af);
        double ef = supremumValue(qdf.getValue0(), qdf.getValue1(), af);

        if (eb < ef) {
            System.out.println("##########################  eb < ef");
            return findUpperBound(pb, pf, ab + (a - ab) / 2);
        }
        if (eb > ef) {
            System.out.println("##########################  eb > ef");
            return findUpperBound(pb, pf, ab / 2);
        }
        if (Math.abs(eb / ef - 1) < 0.001) {
            System.out.println("##########################  eb == ef");
            return eb;
        }

        return 0;
    }

    public double findUpperBoundForAllUsers(double a) throws ClassNotFoundException, SQLException {
        double result = Double.MAX_VALUE;
        String clause = "SELECT poi_id FROM " + tab + " WHERE uid=";
        String[] uids = sortUidByChainLength().split(" ");

        for (int i = 0; i < users; i++) {
            String chain = findChain(clause + uids[i]);
            Matrix matrix = new MarkovChain(chain).getMatrix();
            Matrix p = MarkovChain.deleteRowsWithAllZeros(matrix);
            double min = findUpperBound(p, p, a);
            if (result > min) {
                result = min;
            }
        } // for i
        return result;
    }

    /* Releasing Data by quantification */
    private double findQuantification(Matrix pb, Matrix pf, double a) {
        double ab = a / 2;
        double e1 = ab;
        Pair<Double, Double> qdb = findQD(pb, ab);
        double eb = e1 - Math.log(maxValue(qdb.getValue0(), qdb.getValue1(), e1));

        double et = a - e1 + eb;
        double af = et;
        Pair<Double, Double> qdf = findQD(pf, af);
        double ef = et - Math.log(maxValue(qdb.getValue0(), qdb.getValue1(), et));

        if (eb < ef) {
            return findUpperBound(pb, pf, ab + (a - ab) / 2);
        } else if (eb > ef) {
            return findUpperBound(pb, pf, ab / 2);
        } else {
            return eb;
        }
    }

    public double findQuantificationForAllUsers(double a) throws ClassNotFoundException, SQLException {
        double result = Double.MAX_VALUE;
        String clause = "SELECT poi_id FROM " + tab + " WHERE uid=";
        String[] uids = sortUidByChainLength().split(" ");

        for (int i = 0; i < users; i++) {
            String chain = findChain(clause + uids[i]);
            Matrix matrix = new MarkovChain(chain).getMatrix();
            Matrix p = MarkovChain.deleteRowsWithAllZeros(matrix);
            double min = findQuantification(p, p, a);
            if (result > min) {
                result = min;
            }
        } // for i
        return result;
    }

    /* Theorem 4: maximum value of the objective function */
    private double maxValue(double q, double d, double a) {
        double tmp = (Math.exp(a) - 1);
        return (q * tmp + 1) / (d * tmp + 1);
    }

    /* Find Backward / Forward Privacy Leakage */
    private double findPrivacyLeakage(Matrix p, double a, double eps) {
        double result = 0;

        double[][] m = p.toDoubleArray();
        for (double[] q : m) {
            for (double[] d : m) {
                boolean isSame = true;
                for (int c = 0; c < p.getColumnCount(); c++) {
                    if (q[c] != d[c]) {
                        isSame = false;
                        break;
                    }
                }

                if (!isSame) { // for two different rows : q and d
                    List<Integer> plus_set = new ArrayList<>(); // index of elements in q+ and d+
                    for (int j = 0; j < p.getColumnCount(); j++) { // for each column in row 
                        if (q[j] > d[j]) {
                            plus_set.add(j);
                        }
                    }

                    boolean update = false;
                    double q_sum = 0;
                    double d_sum = 0;
                    do {
                        for (int j : plus_set) {
                            q_sum += q[j];
                            d_sum += d[j];
                        }

                        double maxVal = maxValue(q_sum, d_sum, a);
                        for (Iterator<Integer> itr = plus_set.iterator(); itr.hasNext();) {
                            int j = itr.next();
                            Double tmp = q[j] / d[j];
                            if (tmp <= maxVal) {
                                itr.remove();
                            }
                        }
                    } while (update);

                    double logVal = Math.log(maxValue(q_sum, d_sum, a));
                    if (result < logVal) {
                        result = logVal;
                    }
                } // q != d		
            } // for d
        } // for q

        return (result + eps);
    }

    private String predictNextState(Matrix mx, String now) {
        int row = (int) mx.getRowForLabel(now);
//        System.out.println(mx.getRowCount() + "x" + mx.getColumnCount() + " now = " + now + " row = " + row);

        if (row < 0) {
            return "";
        } else if (row < mx.getRowCount()) {
            int col = 0;
            double[][] m = mx.toDoubleArray();
            double max = 0;
            for (int c = 0; c < mx.getColumnCount(); c++) {
                if (m[row][c] > max) {
                    max = m[row][c];
                    col = c;
                }
            }
            return mx.getColumnLabel(col);
        } else {
            return "";
        }

    }

    /* The accuracy is the ratio between the number of 
     * correct predictions over the total number of predictions */
    private double calculateAccuracy(Matrix mx, String seq) {
        if (seq.isEmpty()) {
            return 1;
        }

        String[] seqs = seq.split(" ");
        List<Pair<String, String>> transition = new ArrayList<>();
        for (int a = 0, b = 1; a < seqs.length - 1 && b < seqs.length; a++, b++) {
            Pair<String, String> item = Pair.with(seqs[a], seqs[b]);
            transition.add(item);
        }

        int total = seqs.length - 1;
        int correct = 0;

        for (Pair<String, String> p : transition) {
            String next = predictNextState(mx, p.getValue0());
            if (p.getValue1().equalsIgnoreCase(next)) {
                correct++;
            }
        }

//        System.out.println("correct = " + correct + " total = " + total + " Accuracy = " + (1.0 * correct) / total);
        return (1.0 * correct) / total;
    }

    private double[] calculateFPL(Matrix mx, double a, double[] eps, int times) {
        double[] result = new double[times];
        result[times - 1] = a;
        for (int i = times - 2; i >= 0; i--) {
            double n = findPrivacyLeakage(mx, result[i + 1], eps[i]);
            result[i] = (Double.parseDouble(String.format("%.3f", n)));
        } // for t
//        System.out.println(Arrays.toString(result));
        return result;
    }

    private double[] calculateBPL(Matrix mx, double a, double[] eps, int times) {
        double[] result = new double[times];
        result[0] = a;
        for (int i = 1; i < times; i++) {
            double n = findPrivacyLeakage(mx, result[i - 1], eps[i]);
            result[i] = (Double.parseDouble(String.format("%.3f", n)));
        } // for t
//        System.out.println(Arrays.toString(result));
        return result;
    }

    private double[] calculateTPL(Matrix mx, double a, double[] eps, int times) {
        double[] result = new double[times];
        double[] bpl = calculateBPL(mx, a, eps, times);
        double[] fpl = calculateFPL(mx, a, eps, times);
        for (int i = 0; i < times; i++) {
            double n = bpl[i] + fpl[i] - eps[i];
            result[i] = Double.parseDouble(String.format("%.3f", n));
        }
//        System.out.println(Arrays.toString(result));
        return result;
    }

    private double[] calculateTPL(double[] bpl, double[] fpl, double[] eps, int times) {
        double[] result = new double[times];
        for (int i = 0; i < times; i++) {
            double n = bpl[i] + fpl[i] - eps[i];
            result[i] = Double.parseDouble(String.format("%.3f", n));
        }
//        System.out.println(Arrays.toString(result));
        return result;
    }

    /* Mehtods for t_drive dataset */
    private String sortUidByChainLength()
            throws ClassNotFoundException, SQLException {
        String result = "";

        String clause = "SELECT uid, COUNT(poi_id) AS ChainLen FROM " + tab + " GROUP BY uid ORDER BY ChainLen DESC";
        Class.forName("org.sqlite.JDBC");
        Connection cnt = DriverManager.getConnection("jdbc:sqlite:" + db);
        cnt.setAutoCommit(false);
        Statement stm = cnt.createStatement();
        ResultSet rs = stm.executeQuery(clause);
        while (rs.next()) {
            String usr = rs.getString("uid") + " ";
            result += usr;
        }

        rs.close();
        stm.close();
        cnt.close();

        return result.trim();
    }

    /* Mehtods for t_drive dataset */
    private String findChain(String clause)
            throws ClassNotFoundException, SQLException {
        String result = "";

        Class.forName("org.sqlite.JDBC");
        Connection cnt = DriverManager.getConnection("jdbc:sqlite:" + db);
        cnt.setAutoCommit(false);
        Statement stm = cnt.createStatement();
        ResultSet rs = stm.executeQuery(clause);
        while (rs.next()) {
            String pos = rs.getString("poi_id") + " ";
            result += pos;
        }

        rs.close();
        stm.close();
        cnt.close();

        return result.trim();
    }

    public void writeUserTPLToCSV(String csv, String uid)
            throws ClassNotFoundException, SQLException, IOException {

        String clause = "SELECT poi_id FROM " + tab + " WHERE uid=" + uid;
        String chain = findChain(clause);
        Matrix matrix = new MarkovChain(chain).getMatrix();
        Matrix p = MarkovChain.deleteRowsWithAllZeros(matrix);
//        System.out.println(p);

        double a = 0.1;
        double[] eps = new double[times];
        for (int i = 0; i < times; i++) {
            eps[i] = 0.1;
        }
        double[] bpl = calculateBPL(p, a, eps, times);
        double[] fpl = calculateFPL(p, a, eps, times);
        double[] tpl = calculateTPL(bpl, fpl, eps, times);

        CSVWriter writer = new CSVWriter(new FileWriter("csv/" + csv), ',');
        List<String[]> newRows = new ArrayList<>();

        String[] title = new String[4];
        title[0] = "time";
        title[1] = "bpl";
        title[2] = "fpl";
        title[3] = "tpl";
        newRows.add(title);

        for (int i = 0; i < times; i++) {
            String[] rows = new String[4];
            rows[0] = "" + i;
            rows[1] = "" + bpl[i];
            rows[2] = "" + fpl[i];
            rows[3] = "" + tpl[i];
            newRows.add(rows);
        }

        writer.writeAll(newRows);
        writer.close();
    }

    public void writeAllUsersTPLsToCSV()
            throws ClassNotFoundException, SQLException, IOException {
        String[] uids = sortUidByChainLength().split(" ");
        for (int i = 0; i < users; i++) {
            writeUserTPLToCSV("tpl" + i + ".csv", uids[i]);
        }
    }

    public void writeUserTrajectoryToCSV(String csv, String uid)
            throws ClassNotFoundException, SQLException, IOException {

        CSVWriter writer = new CSVWriter(new FileWriter("csv/" + csv), ',');
        List<String[]> newRows = new ArrayList<>();

        String[] title = new String[3];
        title[0] = "timestamp";
        title[1] = "lat";
        title[2] = "lng";
        newRows.add(title);

        String clause = "SELECT timestamp, lat, lng FROM " + tab + " WHERE uid=" + uid;
        Class.forName("org.sqlite.JDBC");
        Connection cnt = DriverManager.getConnection("jdbc:sqlite:" + db);
        cnt.setAutoCommit(false);
        Statement stm = cnt.createStatement();
        ResultSet rs = stm.executeQuery(clause);
        while (rs.next()) {
            String[] rows = new String[3];
            rows[0] = rs.getString("timestamp");
            rows[1] = rs.getString("lat");
            rows[2] = rs.getString("lng");
            newRows.add(rows);
        }

        rs.close();
        stm.close();
        cnt.close();

        writer.writeAll(newRows);
        writer.close();
    }

    public void writeAllUsersTrajectoriesToCSV()
            throws ClassNotFoundException, SQLException, IOException {
        String[] uids = sortUidByChainLength().split(" ");
        for (int i = 0; i < users; i++) {
            writeUserTrajectoryToCSV("trajectory" + i + ".csv", uids[i]);
        }
    }

    /* conducted to evaluate the accuracy of our prediction algorithm */
    public void evaluateTemporalCorrelation()
            throws ClassNotFoundException, SQLException {

        String clause = "SELECT poi_id FROM " + tab + " WHERE uid=";

        double total_accuracy = 0;
        String[] uids = sortUidByChainLength().split(" ");

        double a = 0.1;
        double[] eps = new double[times];
        for (int t = 0; t < times; t++) {
            eps[t] = 0.1;
        }

        for (int i = 0; i < users; i++) {
            String chain = findChain(clause + uids[i]);
            Matrix matrix = new MarkovChain(chain).getMatrix();
            Matrix p = MarkovChain.deleteRowsWithAllZeros(matrix);
            double[] tpl = calculateTPL(p, a, eps, times);

            // conducted to evaluate the accuracy of our prediction algorithm
            double accuracy = calculateAccuracy(matrix, chain);
            total_accuracy += accuracy;
            System.out.println("UID:" + uids[i] + " DistinctPOI: " + matrix.getColumnCount()
                    + " ChainLen = " + chain.split(" ").length
                    + " Accuracy = " + String.format("%.3f", accuracy)
                    + " TPL = " + tpl[times - 1]);
        } // for i

        System.out.println("Average accuracy = " + String.format("%.3f", total_accuracy / users));
    }

    public void writeStatsUsersToCSV(String csv)
            throws ClassNotFoundException, SQLException, IOException {

        String clause = "SELECT poi_id FROM " + tab + " WHERE uid=";
        String[] uids = sortUidByChainLength().split(" ");

        double a = 0.1;
        double[] eps = new double[times];
        for (int t = 0; t < times; t++) {
            eps[t] = 0.1;
        }

        CSVWriter writer = new CSVWriter(new FileWriter("csv/" + csv), ',');
        List<String[]> newRows = new ArrayList<>();

        String[] title = new String[2];
        title[0] = "uid";
        title[1] = "tpl";
        newRows.add(title);

        for (int i = 0; i < users; i++) {
            String chain = findChain(clause + uids[i]);
            Matrix matrix = new MarkovChain(chain).getMatrix();
            Matrix p = MarkovChain.deleteRowsWithAllZeros(matrix);
            double[] tpl = calculateTPL(p, a, eps, times);
            double accuracy = calculateAccuracy(matrix, chain);
            if (accuracy >= 0.9) {
                int tpl_val = (int) tpl[times - 1];
                String[] rows = new String[2];
                rows[0] = uids[i];
                rows[1] = "" + tpl_val;
                newRows.add(rows);
            }
//            if (tpl[i] < 10) {
//                System.out.println(uids[i]);
//                System.out.println(matrix);
//            }
//            String[] rows = new String[2];
//            rows[0] = uids[i];
//            rows[1] = "" + tpl[times - 1];
//            newRows.add(rows);
        } // for i

        writer.writeAll(newRows);
        writer.close();
    }

    public void writePOICountPerTimeToCSV(String csv, String time)
            throws ClassNotFoundException, SQLException, IOException {

        CSVWriter writer = new CSVWriter(new FileWriter("csv/" + csv), ',');
        List<String[]> newRows = new ArrayList<>();

        Class.forName("org.sqlite.JDBC");
        Connection cnt = DriverManager.getConnection("jdbc:sqlite:" + db);
        cnt.setAutoCommit(false);
        Statement stm = cnt.createStatement();

        String[] title = new String[6];
        title[0] = "timestamp";
        title[1] = "poi_id";
        title[2] = "uid_counts";
        title[3] = "noise_counts_1";
        title[4] = "noise_counts_2";
        title[5] = "noise_counts_3";
        newRows.add(title);

        String clause = "SELECT timestamp, poi_id, COUNT(uid) AS uid_counts FROM " + tab
                + " WHERE timestamp LIKE \"" + time
                + "\" GROUP BY timestamp, poi_id ORDER BY timestamp, poi_id asc";
        ResultSet rs = stm.executeQuery(clause);
        while (rs.next()) {
            String[] rows = new String[6];
            rows[0] = rs.getString("timestamp");
            rows[1] = rs.getString("poi_id");
            rows[2] = rs.getString("uid_counts");
            rows[3] = "" + addLaplaceNoise(Double.parseDouble(rows[2]), 0.1, 1.0);
            rows[4] = "" + addLaplaceNoise(Double.parseDouble(rows[2]), 0.5, 1.0);
            rows[5] = "" + addLaplaceNoise(Double.parseDouble(rows[2]), 0.9, 1.0);
            newRows.add(rows);
        }

        rs.close();
        stm.close();
        cnt.close();

        writer.writeAll(newRows);
        writer.close();
    }
}
