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
