package org.bcmoj.quesmm.result_mm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bcmoj.db.DBConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class JudgeResultManager {
    public static Logger LOGGER = LoggerFactory.getLogger(JudgeResultManager.class);
    public static void saveJudgeResult(int userid, int problemid, String jsonResult) {
        ObjectMapper mapper = new ObjectMapper();
        try (Connection conn = DBConnect.db_judge_results_get_connection()) {
            JsonNode rootNode = mapper.readTree(jsonResult);
            String insertJudgeResultSQL = "INSERT INTO judge_results (userid, problemid) VALUES (?, ?)";
            int resultId;
            try (PreparedStatement pstmt = conn.prepareStatement(insertJudgeResultSQL, Statement.RETURN_GENERATED_KEYS)) {
                LOGGER.info("Inserting judge result into database...");
                pstmt.setInt(1, userid);
                pstmt.setInt(2, problemid);
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        resultId = rs.getInt(1);
                        LOGGER.info("Result ID: {}", resultId);
                    } else {
                        throw new SQLException("Failed to get generated result_id.");
                    }
                }
            }
            // 插入检查点结果
            String insertCheckpointSQL = "INSERT INTO checkpoint_results (result_id, checkpoint_id, result, time) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertCheckpointSQL)) {
                for (int i = 1; rootNode.has(i + "_res"); i++) {
                    int result = rootNode.get(i + "_res").asInt();
                    float time = (float) rootNode.get(i + "_time").asDouble();
                    pstmt.setInt(1, resultId);
                    pstmt.setInt(2, i); // checkpoint_id
                    pstmt.setInt(3, result);
                    pstmt.setFloat(4, time);
                    pstmt.executeUpdate();
                }
            }
            LOGGER.info("Completed");
        } catch (Exception e) {
            LOGGER.error("Unable to manage database: {}", e.getMessage());
        }

    }


}

