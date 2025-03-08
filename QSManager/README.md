# QSManage(题目管理)

> 题目管理部分源码

- db - 数据库操作
- result_mm - 判题结果管理
- ques_sub - 从数据库中拉取题目并提交给判题机
- config - 配置文件




### 数据库名称: `coding_problems`

#### 表1: `problems`
存储题目的基本信息。

| 字段名         | 数据类型        | 描述                  |
|----------------|----------------|-----------------------|
| `problem_id`   | INT            | 题目ID (主键, 自增)   |
| `title`        | VARCHAR(255)   | 题目名称              |
| `description`  | TEXT           | 题目介绍              |
| `time_limit`   | INT            | 时间限制（毫秒）      |

#### 表2: `examples`
存储每个题目的示例输入输出，检查点数量由示例的数量决定。

| 字段名         | 数据类型        | 描述                  |
|----------------|----------------|-----------------------|
| `example_id`   | INT            | 示例ID (主键, 自增)   |
| `problem_id`   | INT            | 题目ID (外键)         |
| `input`        | TEXT           | 示例输入              |
| `output`       | TEXT           | 示例输出              |