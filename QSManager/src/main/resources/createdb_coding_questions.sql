CREATE DATABASE IF NOT EXISTS coding_problems;
USE coding_problems;

-- 创建 problems 表
CREATE TABLE problems (
                          problem_id INT AUTO_INCREMENT PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          description TEXT NOT NULL,
                          time_limit INT NOT NULL DEFAULT 1000 COMMENT '时间限制（毫秒）'
);

-- 创建 examples 表
CREATE TABLE examples (
                          example_id INT AUTO_INCREMENT PRIMARY KEY,
                          problem_id INT NOT NULL,
                          input TEXT NOT NULL,
                          output TEXT NOT NULL,
                          FOREIGN KEY (problem_id) REFERENCES problems(problem_id) ON DELETE CASCADE
);
-- 插入题目
INSERT INTO problems (title, description, time_limit) VALUES ('两数之和', '给定一个整数数组和一个目标值，找出数组中和为目标值的两个数。', 2000);
INSERT INTO problems (title, description, time_limit) VALUES ('反转字符串', '给定一个字符串，将其反转。', 1000);
INSERT INTO problems (title, description, time_limit) VALUES ('A+B Problem','输入两个整数 a,b，输出它们的和。',1000);

-- 插入示例输入输出
INSERT INTO examples (problem_id, input, output) VALUES (1, '[2, 7, 11, 15], 9', '[0, 1]');
INSERT INTO examples (problem_id, input, output) VALUES (1, '[3, 2, 4], 6', '[1, 2]');
INSERT INTO examples (problem_id, input, output) VALUES (2, '"hello"', '"olleh"');
INSERT INTO examples (problem_id, input, output) VALUES (2, '"world"', '"dlrow"');
INSERT INTO examples (problem_id, input, output) VALUES (3, '1 1','2');
INSERT INTO examples (problem_id, input, output) VALUES (3, '1 2','3');



SELECT
    p.problem_id,
    p.title,
    p.description,
    p.time_limit,
    COUNT(e.example_id) AS checkpoint_count
FROM
    problems p
        LEFT JOIN
    examples e ON p.problem_id = e.problem_id
GROUP BY
    p.problem_id;