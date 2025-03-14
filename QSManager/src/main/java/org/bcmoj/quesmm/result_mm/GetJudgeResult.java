package org.bcmoj.quesmm.result_mm;

import org.bcmoj.db.DBConnect;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GetJudgeResult {
    public static Logger LOGGER = LoggerFactory.getLogger(GetJudgeResult.class);
    // 读取判题结果并输出
    public static void readAndPrintJudgeResult(int userid, int problemid, int judgeid) {
        try (Connection conn = DBConnect.db_judge_results_get_connection()) {
            String querySQL;
            if (judgeid == 0) {
                // 查询所有结果
                querySQL = "SELECT j.result_id, c.checkpoint_id, c.result, c.time " +
                        "FROM judge_results j " +
                        "JOIN checkpoint_results c ON j.result_id = c.result_id " +
                        "WHERE j.userid = ? AND j.problemid = ?";
            } else {
                // 查询指定 judgeid 的结果
                querySQL = "SELECT j.result_id, c.checkpoint_id, c.result, c.time " +
                        "FROM judge_results j " +
                        "JOIN checkpoint_results c ON j.result_id = c.result_id " +
                        "WHERE j.userid = ? AND j.problemid = ? AND j.result_id = ?";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(querySQL)) {
                pstmt.setInt(1, userid);
                pstmt.setInt(2, problemid);
                if (judgeid != 0) {
                    pstmt.setInt(3, judgeid);
                }
                ResultSet rs = pstmt.executeQuery();
                LOGGER.info("========== Judge Results ==========");
                int currentJudgeId = -1;
                while (rs.next()) {
                    int resultId = rs.getInt("result_id");
                    int checkpointId = rs.getInt("checkpoint_id");
                    int result = rs.getInt("result");
                    float time = rs.getFloat("time");
                    if (resultId != currentJudgeId) {
                        if (currentJudgeId != -1) {
                            LOGGER.info("------------------------");
                        }
                        LOGGER.info("Judge ID: {}", resultId);
                        currentJudgeId = resultId;
                    }
                    LOGGER.info("Checkpoint {}: Result = {}, Time = {:.2f} ms", checkpointId, result, time);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read judge results: {}", e.getMessage());
        }
    }
}
