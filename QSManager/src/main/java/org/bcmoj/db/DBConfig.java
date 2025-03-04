package org.bcmoj.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBConfig {
    public static Logger LOGGER = LoggerFactory.getLogger(DBConfig.class);
    Connection conn_questions;

    //TODO: 后面会使用配置文件代替
    public static String db_questions_user = "root";
    public static String db_questions_password = "sino22";

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
