# Judge Server
> 判题服务器

- ### 示例配置
```json
{"timeLimit": 2000,"checkpoints": {"1_in": "[2, 7, 11, 15], 9","2_in": "[3, 2, 4], 6","2_out": "[1, 2]","1_out": "[0, 1]"},"securityCheck": false}
```
- ### 示例输出
```json
{"1_res":-4,"1_time":0.0,"2_res":-4,"2_time":0.0}
```
#### 其中
```
-5 -> Security Check Failed
-4 -> Compile Error
-3 -> Wrong Answer
2 -> Real Time Limit Exceeded
4 -> Runtime Error
5 -> System Error
1 -> Accepted
```
##### tip:支持`\n`等转义字符

- ### 代码安全检查
通过匹配关键字来确保递交代码的安全性,支持正则

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