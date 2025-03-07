package org.bcmoj.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bcmoj.config.ConfigProcess;

public class DBConfig {
    public static Logger LOGGER = LoggerFactory.getLogger(DBConfig.class);

    public static String db_questions_user = ConfigProcess.GetConfig("db_questions_user");
    public static String db_questions_password = ConfigProcess.GetConfig("db_questions_password");
    public static String db_results_user = ConfigProcess.GetConfig("db_results_user");
    public static String db_results_password = ConfigProcess.GetConfig("db_results_password");


    public static Connection db_questions_get_connction() throws ClassNotFoundException {
        Connection conn_questions;
        Class.forName("com.mysql.cj.jdbc.Driver");
        try{
            conn_questions = DriverManager.getConnection("jdbc:mysql://localhost:3306/coding_problems",db_questions_user,db_questions_password);
            LOGGER.info("Connected to database: coding_problems - " + conn_questions.getMetaData().getURL());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn_questions;

    }
    public Connection db_results_get_connction() throws ClassNotFoundException {
        Connection conn_results;
        Class.forName("com.mysql.cj.jdbc.Driver");
        try {
            conn_results = DriverManager.getConnection("jdbc:mysql://localhost:3306/results",db_results_user,db_results_password);
            LOGGER.info("Connected to database."+conn_results.getMetaData());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn_results;
    }


}
