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
  <el-card header="版本信息" shadow="hover">
    <div style="height: 210px;text-align: center;">
      <img src="/img/ver.svg" style="height:140px"/>
      <h2 style="margin-top: 15px;">SCUI {{ $CONFIG.CORE_VER }}</h2>
      <p style="margin-top: 5px;">最新版本 {{ ver }}</p>
    </div>
    <div style="margin-top: 20px;">
      <el-button plain round type="primary" @click="golog">更新日志</el-button>
      <el-button plain round type="primary" @click="gogit">gitee</el-button>
    </div>
  </el-card>
</template>

<script>
export default {
  title: "版本信息",
  icon: "el-icon-monitor",
  description: "当前项目版本信息",
  data() {
    return {
      ver: 'loading...'
    }
  },
  mounted() {
    this.getVer()
  },
  methods: {
    async getVer() {
      this.ver = this.$CONFIG.APP_VER
    },
    golog() {
      window.open("https://gitee.com/lolicode/scui/releases")
    },
    gogit() {
      window.open("https://gitee.com/lolicode/scui")
    }
  }
}
</script>
