-- 1. 创建数据库，并设置默认字符集为 utf8mb4（支持中文、emoji）
CREATE DATABASE IF NOT EXISTS BCMOJ CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE BCMOJ;

-- 2. 创建 users 表（先创建，因为其他表可能引用它）
CREATE TABLE users (
    userid INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    passwd VARCHAR(255) NOT NULL,
    avatar VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 创建 problems 表（先于 examples 表创建，因为 examples 引用它）
CREATE TABLE problems (
    problem_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    time_limit INT NOT NULL DEFAULT 1000 COMMENT '时间限制（毫秒）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 创建 judge_results 表（引用 users 和 problems）
CREATE TABLE judge_results (
    result_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '结果ID',
    userid INT NOT NULL COMMENT '用户ID',
    problemid INT NOT NULL COMMENT '题目ID',
    FOREIGN KEY (userid) REFERENCES users(userid) ON DELETE CASCADE,
    FOREIGN KEY (problemid) REFERENCES problems(problem_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 创建 checkpoint_results 表（引用 judge_results）
CREATE TABLE checkpoint_results (
    result_id INT NOT NULL COMMENT '结果ID',
    checkpoint_id INT NOT NULL COMMENT '检查点ID',
    result INT COMMENT '检查点结果',
    time FLOAT COMMENT '检查点时间（ms）',
    PRIMARY KEY (result_id, checkpoint_id),
    FOREIGN KEY (result_id) REFERENCES judge_results(result_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 创建 examples 表（引用 problems）
CREATE TABLE examples (
    example_id INT AUTO_INCREMENT PRIMARY KEY,
    problem_id INT NOT NULL,
    input TEXT NOT NULL,
    output TEXT NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(problem_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 插入数据（确保引用的 problem_id 存在）
INSERT INTO problems (title, description, time_limit) VALUES 
('两数之和', '给定一个整数数组和一个目标值，找出数组中和为目标值的两个数。', 2000),
('反转字符串', '给定一个字符串，将其反转。', 1000),
('A+B Problem', '输入两个整数 a,b，输出它们的和。', 1000);

INSERT INTO examples (problem_id, input, output) VALUES 
(1, '[2,7,11,15]\n9', '[0,1]'),
(1, '[3,2,4]\n6', '[1,2]'),
(2, '"hello"', '"olleh"'),
(2, '"world"', '"dlrow"'),
(3, '1 1', '2'),
(3, '1 2', '3');