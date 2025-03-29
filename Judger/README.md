# Judger
> 判题机部分

- ### 示例配置
```json{
    "timeLimit": 3000,
    "checkpoints": {
        "1_in": "10\n1 2 4 5 6 7 8 9 0",
        "1_out": "1 2 4 5 6 7 8 9 0",
        "2_in": "2\n1 3",
        "2_out": "1 3",
        "3_in": "5\n10 20 30 40 50",
        "3_out": "10 20 30 40 50"
    }
}
```
- ### 代码安全检查
通过匹配关键字来确保递交代码的安全性

### 配置文件```keywords.txt```
```text
# Security check keywords list
# Lines starting with # are ignored

system
exec
fork
popen
pclose
chmod
chown
rmdir
unlink
kill
shutdown
reboot
sudo
su
rm

```