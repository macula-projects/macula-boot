<!--
  - Copyright (c) 2023 Macula
  -   macula.dev, China
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -    http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<!--
 * @Descripttion: 状态指示器
 * @version: 1.0
 * @Author: sakuya
 * @Date: 2021年11月11日09:30:12
 * @LastEditors:
 * @LastEditTime:
-->

<template>
  <span :class="[{'sc-status-processing':pulse}, 'sc-state-bg--'+type]" class="sc-state"></span>
</template>

<script>
export default {
  props: {
    type: {type: String, default: "primary"},
    pulse: {type: Boolean, default: false}
  }
}
</script>

<style scoped>
.sc-state {
  display: inline-block;
  background: #000;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  vertical-align: middle;
}

.sc-status-processing {
  position: relative;
}

.sc-status-processing:after {
  position: absolute;
  top: 0px;
  left: 0px;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: inherit;
  content: '';
  animation: warn 1.2s ease-in-out infinite;
}

.sc-state-bg--primary {
  background: var(--el-color-primary);
}

.sc-state-bg--success {
  background: var(--el-color-success);
}

.sc-state-bg--warning {
  background: var(--el-color-warning);
}

.sc-state-bg--danger {
  background: var(--el-color-danger);
}

.sc-state-bg--info {
  background: var(--el-color-info);
}

@keyframes warn {
  0% {
    transform: scale(0.5);
    opacity: 1;
  }
  30% {
    opacity: 0.7;
  }
  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}
</style>
