/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tpl_demo;

import java.sql.*;
import java.io.*;
import org.apache.logging.log4j.*;
import org.ujmp.core.Matrix;

/**
 *
 * @author tom
 */
public class TPL_Demo {

    private static Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    private static final int usr_num = 1000;
    private static final int run_times = 60;

    public static void main(String[] args)
            throws ClassNotFoundException, SQLException, IOException {

        String uid = "7105"; //args[0];
        String time = "2008-02-02 14:%";

        /* ********************** output to log_file *********************** */
        File file = new File("log_file.txt");
        FileOutputStream fis = new FileOutputStream(file);
        PrintStream out = new PrintStream(fis);
        System.setOut(out);
        /* ***************************************************************** */

        // t_poi4
        
        TrajectoryAnalyse t_dr = new TrajectoryAnalyse("t_drive.sqlite", "t_poi4", usr_num, run_times);
//        t_dr.writeAllUsersTPLsToCSV();
//        t_dr.writeAllUsersTrajectoriesToCSV();

//        t_dr.evaluateTemporalCorrelation();
        t_dr.writeStatsUsersToCSV("users.csv");
//        t_dr.writeUserTPLToCSV("tpl.csv", uid);
//        t_dr.writeUserTrajectoryToCSV("trajectory.csv", uid);
//        t_dr.writePOICountPerTimeToCSV("count.csv", time);



//        Matrix pb = MarkovChain.generateStochasticMatrix(2, 2);
//        pb.setAsDouble(0.8, 0, 0);
//        pb.setAsDouble(0.2, 0, 1);
//        pb.setAsDouble(0.1, 1, 0);
//        pb.setAsDouble(0.9, 1, 1);
//
//        Matrix pf = MarkovChain.generateStochasticMatrix(2, 2);
//        pf.setAsDouble(0.8, 0, 0);
//        pf.setAsDouble(0.2, 0, 1);
//        pf.setAsDouble(0.1, 1, 0);
//        pf.setAsDouble(0.9, 1, 1);
//        
//        t_dr.findUpperBound(pb, pf, 2);

    } // main

}
