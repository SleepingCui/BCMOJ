package org.bcmoj.quesmm.result_mm;

import org.bcmoj.db.DBConnect;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GetJudgeResult {
    public static Logger LOGGER = LoggerFactory.getLogger(GetJudgeResult.class);

    // 状态码
    public static final int COMPILE_ERROR = -4;
    public static final int WRONG_ANSWER = -3;
    public static final int REAL_TIME_LIMIT_EXCEEDED = 2;
    public static final int RUNTIME_ERROR = 4;
    public static final int SYSTEM_ERROR = 5;
    public static final int ACCEPTED = 1;
    public static final int SECURITY_CHECK_FAILED = -5;

    public static void readAndPrintJudgeResult(int userid, int problemid, int judgeid) {
        try (Connection conn = DBConnect.db_connection("judge_results")) {
            String querySQL;
            if (judgeid == 0) {
                querySQL = "SELECT j.result_id, c.checkpoint_id, c.result, c.time " +
                        "FROM judge_results j " +
                        "JOIN checkpoint_results c ON j.result_id = c.result_id " +
                        "WHERE j.userid = ? AND j.problemid = ?";
            } else {
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
                    LOGGER.info("Checkpoint {}: Result = {}, Time = {} ms", checkpointId, getStatusDescription(result), time);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read judge results: {}", e.getMessage());
        }
    }

    private static String getStatusDescription(int statusCode) {
        return switch (statusCode) {
            case COMPILE_ERROR -> "Compile Error";
            case WRONG_ANSWER -> "Wrong Answer";
            case REAL_TIME_LIMIT_EXCEEDED -> "Real Time Limit Exceeded";
            case RUNTIME_ERROR -> "Runtime Error";
            case SYSTEM_ERROR -> "System Error";
            case ACCEPTED -> "Accepted";
            case SECURITY_CHECK_FAILED -> "Security Check Failed";
            default -> "Unknown Status";
        };
    }
}
