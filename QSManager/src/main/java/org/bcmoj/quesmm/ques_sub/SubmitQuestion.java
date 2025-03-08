package org.bcmoj.quesmm.ques_sub;

import org.bcmoj.judgeserver.JudgeServer;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.io.File;

public class SubmitQuestion {
    public static Logger LOGGER = LoggerFactory.getLogger(SubmitQuestion.class);
    public static void Sub(File CppFilePath, int ProblemId) {  //入口
        MakeJudgeConfig makeJudgeConfig = new MakeJudgeConfig();
        String jsonConfig = makeJudgeConfig.GetDBQuestions(ProblemId);
        if (jsonConfig != null) {
            LOGGER.debug("从数据库获取的 JSON 配置: {}", jsonConfig);
            JudgeServer.JServer(jsonConfig, CppFilePath);
            LOGGER.info("判题结束");
        } else {
            LOGGER.error("未找到题目ID为 " + ProblemId + " 的题目。");
        }
    }

}