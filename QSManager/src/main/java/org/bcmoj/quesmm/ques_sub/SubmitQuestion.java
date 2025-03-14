package org.bcmoj.quesmm.ques_sub;

import org.bcmoj.judgeserver.JudgeServer;
import org.bcmoj.quesmm.result_mm.JudgeResultManager;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.io.File;


public class SubmitQuestion {
    public static Logger LOGGER = LoggerFactory.getLogger(SubmitQuestion.class);
    public static void Sub(File CppFilePath, int ProblemId, int UserId) {  //入口
        MakeJudgeConfig makeJudgeConfig = new MakeJudgeConfig();
        String jsonConfig = makeJudgeConfig.GetDBQuestions(ProblemId);
        if (jsonConfig != null) {
            LOGGER.debug("Question Config: {}", jsonConfig);
            String result_cfg = JudgeServer.JServer(jsonConfig, CppFilePath);
            LOGGER.debug("Result Config: {}",result_cfg);
            JudgeResultManager.saveJudgeResult(UserId,ProblemId, result_cfg);
            LOGGER.info("========== Done ==========");

        } else {
            LOGGER.error("Unknown Question ID :{}", ProblemId);
        }
    }


}