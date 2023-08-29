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
  <div v-if="pageLoading">
    <el-main>
      <el-card shadow="never">
        <el-skeleton :rows="1"></el-skeleton>
      </el-card>
      <el-card shadow="never" style="margin-top: 15px;">
        <el-skeleton></el-skeleton>
      </el-card>
    </el-main>
  </div>
  <work v-if="dashboard=='1'" @on-mounted="onMounted"></work>
  <widgets v-else @on-mounted="onMounted"></widgets>
</template>

<script>
import {defineAsyncComponent} from 'vue';

const work = defineAsyncComponent(() => import('./work'));
const widgets = defineAsyncComponent(() => import('./widgets'));

export default {
  name: "dashboard",
  components: {
    work,
    widgets
  },
  data() {
    return {
      pageLoading: true,
      dashboard: '0'
    }
  },
  created() {
    this.dashboard = this.$TOOL.data.get("USER_INFO").dashboard || '0';
  },
  mounted() {

  },
  methods: {
    onMounted() {
      this.pageLoading = false
    }
  }
}
</script>

<style>
</style>
