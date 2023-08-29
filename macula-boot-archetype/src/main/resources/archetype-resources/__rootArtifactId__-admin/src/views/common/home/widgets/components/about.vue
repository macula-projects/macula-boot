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
  <el-card class="item-background" header="关于项目" shadow="hover">
    <p>高性能 / 精致 / 优雅。基于Vue3 + Element-Plus 的中后台前端解决方案，如果喜欢就点个星星支持一下。</p>
    <p>
      <a href='https://gitee.com/lolicode/scui' target="_blank">
        <img alt='star' src='https://gitee.com/lolicode/scui/badge/star.svg?theme=dark' style="vertical-align: middle">
      </a>
    </p>
  </el-card>
</template>

<script>
export default {
  title: "关于项目",
  icon: "el-icon-setting",
  description: "点个星星支持一下",
  data() {
    return {}
  }
}
</script>

<style scoped>
.item-background p {
  color: #999;
  margin-top: 10px;
  line-height: 1.8;
}
</style>
