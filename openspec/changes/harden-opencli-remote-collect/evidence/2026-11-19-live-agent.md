# OpenCLI 1.8.8 与 opencli-admin Agent 回环联调

使用 [partme-ai/opencli-admin 的锁定源码](https://github.com/partme-ai/opencli-admin/blob/f3879eb2e30f5dedb4df249f0b15a7d007bbfe9e/backend/agent_server.py)。`backend/agent_server.py` 的 SHA-256 为 `a9a53e003234b3615d2097e96bbdceea6642febf89b29f32ae321d8b7fff3530`。使用 OpenCLI 1.8.8、Node 24.18.0、Python 3.14.8、FastAPI 0.165.0、Pydantic 2.14.3、Uvicorn 0.54.0 和 PyYAML 6.0.3。Agent 仅监听回环地址，禁用中心注册。OpenCLI 使用隔离用户、配置和缓存目录。

| 请求 | 观察结果 |
| --- | --- |
| 默认 Agent 桥接端口 19825，查询 npm search 帮助 | OpenCLI 1.8.8 拒绝该端口并退出 78，Agent 返回 HTTP 200 和业务失败。三个 Java 客户端均观察到业务失败。 |
| Agent 桥接端口 61735，查询 npm search 帮助 | HTTP 200，Agent 返回实际 OpenCLI 的结构化帮助，SDK 标记业务成功。 |
| 同一端口，调用 npm search 时省略必填位置参数 | OpenCLI 退出 1，Agent 返回 HTTP 200 和业务失败。SDK 保留 HTTP 200，将兼容退出值标明为合成值，并保留业务错误。 |
| 同一端口，再次查询帮助并显式启用原文捕获 | SDK 保留原始 Agent 响应。关闭捕获的成功及失败请求均验证原文为空。 |

带 61735 配置的一轮在 Java 8 `8da04d3`、Java 17 `4fb8c73` 和 Java 21 `df3b58c` 各通过三项。Agent 日志共记录六次成功及三次缺参失败。验证范围为明文回环 legacy 收集协议、CLI 帮助及参数校验。生产 TLS 和认证由独立部署环境验收。此次联调没有调用 npm 注册表、浏览器或用户账户。

## 复跑

从上述提交提取 `backend/agent_server.py` 到临时目录。从已安装的 OpenCLI 1.8.8 包取得 `dist/src/main.js` 的绝对路径，作为 `OPENCLI_ENTRY`。在临时目录保存 `isolate.mjs`，将 `os.homedir()` 指向 `OPENCLI_FIXTURE_HOME`。

```javascript
import os from 'node:os';
import { syncBuiltinESMExports } from 'node:module';
os.homedir = () => process.env.OPENCLI_FIXTURE_HOME;
syncBuiltinESMExports();
```

在同一目录保存可执行文件 `opencli-isolated`。使用临时目录及回环端口启动 Agent。

```sh
#!/bin/sh
exec node --import "$OPENCLI_FIXTURE_HOME/isolate.mjs" "$OPENCLI_ENTRY" "$@"
```

```sh
chmod 700 opencli-isolated
export OPENCLI_FIXTURE_HOME="$PWD" OPENCLI_CONFIG_DIR="$PWD/config" OPENCLI_CACHE_DIR="$PWD/cache"
export OPENCLI_BIN="$PWD/opencli-isolated" CI=1 NO_COLOR=1
export AGENT_REGISTER=off CENTRAL_API_URL='' AGENT_DEPLOY_TYPE=shell OPENCLI_DAEMON_PORT=61735
uv run --no-project --with fastapi==0.165.0 --with pydantic==2.14.3 \
  --with uvicorn==0.54.0 --with pyyaml==6.0.3 \
  python -m uvicorn agent_server:app --host 127.0.0.1 --port 19823
```

在 SDK 功能分支以对应 JDK 执行 `OpenCliRemoteAgentHttpClientTest`，从生成的 Surefire XML 读取 `java.class.path`。用该 classpath 编译 [OpenCliAdminLoopbackProbe](fixtures/OpenCliAdminLoopbackProbe.java)，再用同一 classpath 加上探针输出目录运行，唯一参数为 `http://127.0.0.1:19823`。复现旧端口问题时，在无其他请求的临时 Agent 上改用 19825 并执行同一探针，首个成功断言会得到业务失败，Agent 日志会显示子进程退出 78。
