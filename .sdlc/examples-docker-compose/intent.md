# Intent: Examples Docker Compose
Author: rainsoft. Status: accepted.

## Problem
项目开发者和希望快速了解 Macula Boot 的人员目前无法便捷地运行 Alibaba 与 Tencent 示例链路。示例依赖 MySQL、Redis、Nacos、Polaris 等多项基础设施，手工准备环境和逐项启动耗时，增加了开发和初次体验的门槛。

## Proposed outcome
使用者能够按 Alibaba 或 Tencent 场景运行对应的 gateway、provider、consumer 示例，并可选择完整运行基础依赖与示例应用，或仅运行基础依赖、在本机通过 Maven 或 IDE 启动示例应用。首次体验和日常开发所需的环境准备步骤与时间显著减少。

## Affected users and systems
受影响用户包括 Macula Boot 项目开发者、贡献者以及希望快速体验项目的人员。受影响范围为 `macula-boot-examples` 中 Alibaba 和 Tencent 的 gateway、provider、consumer 示例及其所需的 MySQL、Redis、Nacos、Polaris 基础依赖；首期不覆盖 task 和 binlog4j 示例。

## Constraints
必须包含 MySQL、Redis、Nacos、Polaris；兼容 Docker Compose v2；支持 macOS 和 Windows，并兼容 x86 与 ARM64 架构。需要同时支持示例应用容器化运行和仅启动基础依赖两种使用方式。没有明确截止时间。

## Open questions
完整链路的具体可观察验收行为和入口尚未确认。
