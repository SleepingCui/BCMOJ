package org.bcmoj.db;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bcmoj.config.ConfigProcess;

public class DBConnect {
    public static Logger LOGGER = LoggerFactory.getLogger(DBConnect.class);

    public static String db_user = ConfigProcess.GetConfig("db_user");
    public static String db_password = ConfigProcess.GetConfig("db_password");
    public static String db_port = ConfigProcess.GetConfig("db_port");

    public static Connection db_connection(String db_name) throws ClassNotFoundException {
        Connection conn = null;
        Class.forName("com.mysql.cj.jdbc.Driver");
        try {
            String url = "jdbc:mysql://localhost:" + db_port + "/" + db_name;
            conn = DriverManager.getConnection(url, db_user, db_password);
            LOGGER.info("Connected to database: {} - {}", db_name, conn.getMetaData().getURL());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return conn;

    }
    @Test
    public void test() throws ClassNotFoundException {
        db_connection("judge_results");

    }

}
