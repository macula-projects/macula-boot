# Intent: Binlog4j 使用 Jackson 3
Status: accepted

用户于 2026-10-08 明确确认上一轮 Jackson 3 替换方案：“好，按照这个方案”。

移除 Binlog4j Starter 对 fastjson2 的直接依赖，复用项目 Jackson 3 技术栈，保持下划线列名映射、消费位点存储和事件接口。同步示例、README 与回归测试。
