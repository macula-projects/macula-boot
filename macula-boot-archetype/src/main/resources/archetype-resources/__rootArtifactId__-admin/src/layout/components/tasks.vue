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
  <el-container v-loading="loading">
    <el-main>
      <el-empty v-if="tasks.length==0" :image-size="120">
        <template #description>
          <h2>没有正在执行的任务</h2>
        </template>
        <p style="font-size: 14px;color: #999;line-height: 1.5;margin: 0 40px;">
          在处理耗时过久的任务时为了不阻碍正在处理的工作，可在任务中心进行异步执行。</p>
      </el-empty>
      <el-card v-for="task in tasks" :key="task.id" class="user-bar-tasks-item" shadow="hover">
        <div class="user-bar-tasks-item-body">
          <div class="taskIcon">
            <el-icon v-if="task.type=='export'" :size="20">
              <el-icon-paperclip/>
            </el-icon>
            <el-icon v-if="task.type=='report'" :size="20">
              <el-icon-dataAnalysis/>
            </el-icon>
          </div>
          <div class="taskMain">
            <div class="title">
              <h2>{{ task.taskName }}</h2>
              <p><span v-time.tip="task.createDate"></span> 创建</p>
            </div>
            <div class="bottom">
              <div class="state">
                <el-tag v-if="task.state=='0'" type="info">执行中</el-tag>
                <el-tag v-if="task.state=='1'">完成</el-tag>
              </div>
              <div class="handler">
                <el-button v-if="task.state=='1'" circle icon="el-icon-download" type="primary"
                           @click="download(task)"></el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </el-main>
    <el-footer style="padding:10px;text-align: right;">
      <el-button circle icon="el-icon-refresh" @click="refresh"></el-button>
    </el-footer>
  </el-container>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      tasks: []
    }
  },
  mounted() {
    this.getData()
  },
  methods: {
    async getData() {
      this.loading = true
      var res = await this.$API.system.tasks.list.get()
      this.tasks = res.data
      this.loading = false
    },
    refresh() {
      this.getData()
    },
    download(row) {
      let a = document.createElement("a")
      a.style = "display: none"
      a.target = "_blank"
      a.href = row.result
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    }
  }
}
</script>

<style scoped>
.user-bar-tasks-item {
  margin-bottom: 10px;
}

.user-bar-tasks-item:hover {
  border-color: var(--el-color-primary);
}

.user-bar-tasks-item-body {
  display: flex;
}

.user-bar-tasks-item-body .taskIcon {
  width: 45px;
  height: 45px;
  background: var(--el-color-primary-light-9);
  margin-right: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--el-color-primary);
  border-radius: 20px;
}

.user-bar-tasks-item-body .taskMain {
  flex: 1;
}

.user-bar-tasks-item-body .title h2 {
  font-size: 15px;
}

.user-bar-tasks-item-body .title p {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.user-bar-tasks-item-body .bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
}
</style>
