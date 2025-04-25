# 警告! 此模块代码不再维护!
### QSManage(题目管理)


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


### 数据库名称: `judge_results`

#### 表1: `judge_results`
存储用户和题目的基本信息。

| 字段名      | 数据类型 | 描述                  |
|-------------|----------|-----------------------|
| `result_id` | INT      | 结果ID (主键, 自增)   |
| `userid`    | INT      | 用户ID                |
| `problemid` | INT      | 题目ID                |

#### 表2: `checkpoint_results`
存储每个检查点的结果和时间。

| 字段名          | 数据类型 | 描述            |
|-----------------|----------|-----------------|
| `result_id`     | INT      | 结果ID (外键)   |
| `checkpoint_id` | INT      | 检查点ID        |
| `result`        | INT      | 检查点结果      |
| `time`          | FLOAT    | 检查点时间（ms）|