USE coding_problems;

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

