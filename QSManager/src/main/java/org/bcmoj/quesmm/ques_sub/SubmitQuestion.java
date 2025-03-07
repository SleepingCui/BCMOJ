package org.bcmoj.quesmm.ques_sub;

import org.bcmoj.db.DBConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SubmitQuestion {
    public Logger LOGGER = LoggerFactory.getLogger(getClass());
    //从数据库获取题目数据
    public void GetDBQuestions(int problemId) {
        try (Connection conn = DBConfig.db_questions_get_connction()) {
            // 查询题目基本信息
            String problemQuery = "SELECT title, description FROM problems WHERE problem_id = ?";
            PreparedStatement problemStmt = conn.prepareStatement(problemQuery);
            problemStmt.setInt(1, problemId);
            ResultSet problemRs = problemStmt.executeQuery();

            if (problemRs.next()) {
                String title = problemRs.getString("title");
                String description = problemRs.getString("description");

                // 输出题目基本信息
                LOGGER.info("题目ID: " + problemId);
                LOGGER.info("题目名称: " + title);
                LOGGER.info("题目介绍: " + description);
                LOGGER.info("------------------------");

                // 查询检查点及其示例输入输出
                String checkpointQuery = "SELECT c.checkpoint, e.input, e.output " +
                        "FROM checkpoints c " +
                        "JOIN examples e ON c.checkpoint_id = e.checkpoint_id " +
                        "WHERE c.problem_id = ?";
                PreparedStatement checkpointStmt = conn.prepareStatement(checkpointQuery);
                checkpointStmt.setInt(1, problemId);
                ResultSet checkpointRs = checkpointStmt.executeQuery();

                int checkpointNumber = 1;
                while (checkpointRs.next()) {
                    String checkpoint = checkpointRs.getString("checkpoint");
                    String input = checkpointRs.getString("input");
                    String output = checkpointRs.getString("output");

                    // 输出检查点及其示例
                    LOGGER.info("检查点 " + checkpointNumber + ": " + checkpoint);
                    LOGGER.info("示例输入: " + input);
                    LOGGER.info("示例输出: " + output);
                    LOGGER.info("------------------------");
                    checkpointNumber++;
                }
            } else {
                LOGGER.info("未找到题目ID为 " + problemId + " 的题目。");
            }
        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
    }
    @Test
    public void test(){
        GetDBQuestions(1);
    }

    //TODO:提交给判题机
}
