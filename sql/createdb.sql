
CREATE TABLE IF NOT EXISTS users (
                                     userid INT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    passwd VARCHAR(255) NOT NULL,
    usergroup VARCHAR(255) NOT NULL,
    avatar VARCHAR(255)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS problems (
                                        problem_id INT AUTO_INCREMENT PRIMARY KEY,
                                        title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    time_limit INT NOT NULL DEFAULT 1000 COMMENT '时间限制（毫秒）'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS judge_results (
                                             result_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '结果ID',
                                             userid INT NOT NULL COMMENT '用户ID',
                                             problemid INT NOT NULL COMMENT '题目ID',
                                             time DATETIME COMMENT '提交时间',
                                             filepath VARCHAR(1024) COMMENT '提交文件路径',
    FOREIGN KEY (userid) REFERENCES users(userid) ON DELETE CASCADE,
    FOREIGN KEY (problemid) REFERENCES problems(problem_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS checkpoint_results (
                                                  result_id INT NOT NULL COMMENT '结果ID',
                                                  checkpoint_id INT NOT NULL COMMENT '检查点ID',
                                                  result INT COMMENT '检查点结果',
                                                  time FLOAT COMMENT '检查点时间（ms）',
                                                  PRIMARY KEY (result_id, checkpoint_id),
    FOREIGN KEY (result_id) REFERENCES judge_results(result_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS examples (
                                        example_id INT AUTO_INCREMENT PRIMARY KEY,
                                        problem_id INT NOT NULL,
                                        input TEXT NOT NULL,
                                        output TEXT NOT NULL,
                                        FOREIGN KEY (problem_id) REFERENCES problems(problem_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始用户admin 密码123456
INSERT INTO users (username, email, passwd, avatar)
VALUES (
           'admin',
           'admin@example.com',
           SHA1('123456'),
           NULL
       );
