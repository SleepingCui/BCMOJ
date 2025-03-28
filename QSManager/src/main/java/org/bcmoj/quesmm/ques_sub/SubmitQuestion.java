package org.bcmoj.quesmm.ques_sub;

import org.bcmoj.config.ConfigProcess;
import org.bcmoj.judgeserver.JudgeServer;
import org.bcmoj.quesmm.result_mm.JudgeResultManager;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.io.File;


public class SubmitQuestion {
    public static Logger LOGGER = LoggerFactory.getLogger(SubmitQuestion.class);
    public static void Submit(File CppFilePath, int ProblemId, int UserId) {  //入口
        MakeJudgeConfig makeJudgeConfig = new MakeJudgeConfig();
        String jsonConfig = makeJudgeConfig.GetDBQuestions(ProblemId, Boolean.parseBoolean(ConfigProcess.GetConfig("CodeSecurityCheck")));
        if (jsonConfig != null) {
            String result_cfg = JudgeServer.JServer(jsonConfig, CppFilePath);
            JudgeResultManager.saveJudgeResult(UserId,ProblemId, result_cfg);
            LOGGER.info("========== Done ==========");
        } else {
            LOGGER.error("Unknown Question ID :{}", ProblemId);
        }
    }
}