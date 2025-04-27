CREATE DATABASE IF NOT EXISTS judge_results;
USE judge_results;

-- 创建 judge_results 表
CREATE TABLE judge_results (
                               result_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '结果ID',
                               userid INT NOT NULL COMMENT '用户ID',
                               problemid INT NOT NULL COMMENT '题目ID'
);

-- 创建 checkpoint_results 表
CREATE TABLE checkpoint_results (
                                    result_id INT NOT NULL COMMENT '结果ID',
                                    checkpoint_id INT NOT NULL COMMENT '检查点ID',
                                    result INT COMMENT '检查点结果',
                                    time FLOAT COMMENT '检查点时间（ms）',
                                    PRIMARY KEY (result_id, checkpoint_id),
                                    FOREIGN KEY (result_id) REFERENCES judge_results(result_id) ON DELETE CASCADE
);