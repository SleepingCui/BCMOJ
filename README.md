<p align="center">
  <img src="https://raw.githubusercontent.com/SleepingCui/BCMOJ/master/imgs/logo.png" alt="logo" width=30%/>
</p>

![GitHub License](https://img.shields.io/github/license/SleepingCui/BCMOJ)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues-pr/SleepingCui/BCMOJ)
![GitHub last commit (branch)](https://img.shields.io/github/last-commit/SleepingCui/BCMOJ/master)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/SleepingCui/BCMOJ/maven.yml)
![GitHub Release](https://img.shields.io/github/v/release/SleepingCui/BCMOJ)

<br></br>

> [!WARNING]
> **这个项目目前处于开发阶段,可能存在未经查明的bug
> 如果发现bug,请提出[issue](https://github.com/SleepingCui/BCMOJ/issues)**
### **BCMOJ** ———一个轻量化的在线代码评测系统 ~~(两位初中生花100+h堆石山代码堆出来的玩意)~~
---

## 项目架构

```mermaid
flowchart TD
    A[用户浏览器] -->|提交代码| B[Flask Web服务器]
    B -->|保存记录| C[(MySQL数据库)]
    B -->|转发请求| D[Java判题服务器]
    D -->|获取测试用例| C
    D --> E{安全检查}
    E -->|安全| F[编译代码]
    E -->|危险| G[返回错误]
    F --> H[执行测试用例]
    H --> I[结果比对]
    I --> J[生成报告]
    J -->|存储结果| C
    J -->|返回结果| B
    B -->|显示结果| A
```

---

## 如何使用

### 详见[Wiki](https://github.com/SleepingCui/BCMOJ/wiki)

---

## ScreenShots

<p align="center">
  <img src="https://raw.githubusercontent.com/SleepingCui/BCMOJ/master/imgs/ss1.png" alt="ss1"/>
  <img src="https://raw.githubusercontent.com/SleepingCui/BCMOJ/master/imgs/ss2.png" alt="ss2"/>
  <img src="https://raw.githubusercontent.com/SleepingCui/BCMOJ/master/imgs/ss3.png" alt="ss3"/>
</p>
