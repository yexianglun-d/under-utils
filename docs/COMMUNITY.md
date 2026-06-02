# 社区参与入口

Under-Utils 优先接收可复用工程模式、可复现 Bug 和文档改进。使用问题、功能想法和确定的缺陷分开处理，避免 issue 列表变成聊天入口。

## 使用问题

安装、配置、示例工程和模块用法问题，优先使用 GitHub Discussions 的 Q&A 分类：

https://github.com/yexianglun-d/under-utils/discussions/categories/q-a

提问时建议包含：

- 使用的模块和版本。
- 最小配置或最小代码片段。
- 期望行为和实际结果。
- 已经看过的 README、模块文档或 API 页面。

## 功能想法

还不确定是否应该进入 public API 的想法，先放到 GitHub Discussions 的 Ideas 分类：

https://github.com/yexianglun-d/under-utils/discussions/categories/ideas

功能想法需要先说明：

- 这个场景是否跨服务、跨项目重复出现。
- 为什么 JDK、Spring、Hutool、Apache Commons、Guava 或现有模块不能直接覆盖。
- 哪些行为属于公共库，哪些必须留在业务项目内。
- 失败语义、线程安全、外部依赖和测试方式。

## Bug 和可接手任务

能稳定复现的问题使用 Bug issue。已经确认适合贡献者接手的任务会打标签：

- `good first issue`：边界清楚，适合第一次贡献。
- `help wanted`：需要真实使用反馈、文档补充或独立实现。
- `roadmap`：和近期路线图相关，但仍需要拆成可 review 的小步。

提交 PR 前请阅读 [CONTRIBUTING.md](../CONTRIBUTING.md) 和 [docs/COMPATIBILITY.md](https://github.com/yexianglun-d/under-utils/blob/main/docs/COMPATIBILITY.md)。

## Community Entry Points

Usage questions should start in GitHub Discussions Q&A. Early feature ideas should start in Discussions Ideas. Reproducible bugs belong in Issues. Approachable contribution tasks use the `good first issue`, `help wanted`, or `roadmap` labels.
