# OpenCLI 1.8.8 错误输出采样

采样日期为 2026-11-18。运行环境使用 Node 24.18.0，npm 依赖安装在临时目录，三个样本各用隔离的 HOME。来源为 npm 包 `@jackwener/opencli@1.8.8`，包的发布校验值为 `sha512-T0rp89sVWMKdnQHoYwkRDuACy7k/lCvVScOZmpqnBhd1AnXG6XLYSYO6U0D/Zdj8qq9kQqgop3qmElNcbMr+sw==`。相关源码见已锁定的 [Adapter 渲染器](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/commanderAdapter.ts#L159) 和 [错误模型](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/errors.ts)。

| 样本 | 执行方式 | 进程退出码 | YAML `error.exitCode` | stdout |
|---|---|---:|---:|---:|
| `error-argument.yaml` | 原生只读命令 `opencli 12306 stations '' -f json`，空参数在请求站点前被拒绝 | 2 | 2 | 0 字节 |
| `error-auth-required.yaml` | 官方 Adapter 渲染受控 `AuthRequiredError` | 77 | 77 | 0 字节 |
| `error-empty-result-autofix.yaml` | 官方 Adapter 渲染受控 `EmptyResultError` | 66 | 66 | 0 字节 |

样本保存在 `src/test/resources/opencli-contracts/v1`，stderr 按字节保存，摘要见同目录的 `SHA256SUMS`。即使指定 `-f json`，三例的 stderr 均为 YAML。官方渲染器为 `EMPTY_RESULT` 附加 `# AutoFix` 注释，`AUTH_REQUIRED` 的样本没有该注释。如果后续 decoder 测试组合这两个字段，应标注为合成回归样本。

## 复核受控样本

在临时目录安装锁定包后，将下列内容保存为 `capture.mjs`，分别设置 `OPENCLI_FIXTURE=auth` 或 `OPENCLI_FIXTURE=empty`，用 Node 运行并独立收集 stdout、stderr 与退出码。

```javascript
import { Command } from 'commander';
import { registerCommandToProgram } from './node_modules/@jackwener/opencli/dist/src/commanderAdapter.js';
import { AuthRequiredError, EmptyResultError } from './node_modules/@jackwener/opencli/dist/src/errors.js';

const program = new Command().name('opencli');
registerCommandToProgram(program.command('fixture'), {
  site: 'fixture',
  name: 'check',
  description: 'Offline fixture',
  args: [],
  browser: false,
  func() {
    if (process.env.OPENCLI_FIXTURE === 'auth') {
      throw new AuthRequiredError('example.invalid');
    }
    throw new EmptyResultError('fixture/check', 'No rows in the offline fixture');
  },
});
await program.parseAsync(['fixture', 'check', '-f', 'json'], { from: 'user' });
```

两个受控 Adapter 均在本地抛出错误。未执行登录、真实站点请求、写操作或注释中的修复命令。本次证据只完成 C03 任务 1.1，decoder 的实现和验收仍待评审。
