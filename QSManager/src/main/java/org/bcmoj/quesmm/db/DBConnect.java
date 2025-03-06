package org.bcmoj.quesmm.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bcmoj.quesmm.cfg.ConfigProcess;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnect {
    public static Logger LOGGER = LoggerFactory.getLogger(DBConnect.class);
    Connection conn_questions;

    public static String db_questions_user = ConfigProcess.ConfigProcess("db_questions_user");
    public static String db_questions_password = ConfigProcess.ConfigProcess("db_questions_password");

    public Connection db_questions_get_connction() throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try{
            conn_questions = DriverManager.getConnection("jdbc:mysql://localhost:3306/questions?useUnicode=true&characterEncoding=gbk",db_questions_user,db_questions_password);
            LOGGER.info("Connected to database.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn_questions;

    }

}
