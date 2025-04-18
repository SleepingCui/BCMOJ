package org.bcmoj.quesmm.ques_sub;

import org.bcmoj.db.DBConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MakeJudgeConfig {
    public Logger LOGGER = LoggerFactory.getLogger(getClass());
    // 从数据库获取题目数据
    public String GetDBQuestions(int problemId,boolean securityCheck) {
        int timeLimit;
        Map<String, String> checkpoints = new HashMap<>();
        try (Connection conn = DBConnect.db_connection("coding_problems")) {
            String problemQuery = "SELECT time_limit FROM problems WHERE problem_id = ?";
            PreparedStatement problemStmt = conn.prepareStatement(problemQuery);
            problemStmt.setInt(1, problemId);
            ResultSet problemRs = problemStmt.executeQuery();
            if (problemRs.next()) {
                timeLimit = problemRs.getInt("time_limit");
                String exampleQuery = "SELECT input, output FROM examples WHERE problem_id = ?";
                PreparedStatement exampleStmt = conn.prepareStatement(exampleQuery);
                exampleStmt.setInt(1, problemId);
                ResultSet exampleRs = exampleStmt.executeQuery();
                int checkpointNumber = 1;
                while (exampleRs.next()) {
                    String input = exampleRs.getString("input");
                    String output = exampleRs.getString("output");
                    checkpoints.put(checkpointNumber + "_in", input);
                    checkpoints.put(checkpointNumber + "_out", output);
                    checkpointNumber++;
                }
            } else {
                LOGGER.error("Question {} not found.", problemId);
                return null;
            }
        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.error("Unable to manage database: {}", e.getMessage());
            return null;
        }
        return buildJsonConfig(timeLimit, checkpoints, securityCheck);
    }
    private String buildJsonConfig(int timeLimit, Map<String, String> checkpoints, boolean securityCheck) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"timeLimit\": ").append(timeLimit).append(",");
        jsonBuilder.append("\"checkpoints\": {");
        int count = 0;
        for (Map.Entry<String, String> entry : checkpoints.entrySet()) {
            if (count > 0) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\"").append(entry.getKey()).append("\": \"")
                    .append(escapeJson(entry.getValue())).append("\"");
            count++;
        }
        jsonBuilder.append("},");
        jsonBuilder.append("\"securityCheck\": ").append(securityCheck);
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}