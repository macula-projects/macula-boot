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

<template>
  <el-card header="进度环" shadow="hover">
    <div class="progress">
      <el-progress :percentage="85.5" :width="160" type="dashboard">
        <template #default="{ percentage }">
          <div class="percentage-value">{{ percentage }}%</div>
          <div class="percentage-label">当前进度</div>
        </template>
      </el-progress>
    </div>

  </el-card>
</template>

<script>
export default {
  title: "进度环",
  icon: "el-icon-odometer",
  description: "进度环原子组件演示",
  data() {
    return {}
  }
}
</script>

<style scoped>
.progress {
  text-align: center;
}

.progress .percentage-value {
  font-size: 28px;
}

.progress .percentage-label {
  font-size: 12px;
  margin-top: 10px;
}
</style>
