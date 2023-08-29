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
  <el-container>
    <el-header style="height:50px;">
      <div class="common-header-left">
        <div class="common-header-logo">
          <img :alt="$CONFIG.APP_NAME" src="/img/logo.png">
          <label>{{ $CONFIG.APP_NAME }}</label>
        </div>
        <div class="common-header-title">{{ title }}</div>
      </div>
      <div class="common-header-right">
        <router-link to="/login">返回登录</router-link>
      </div>
    </el-header>
    <el-main>
      <div class="common-container">
        <h2 class="common-title">{{ title }}</h2>
        <div class="common-main el-card">
          <slot></slot>
        </div>
      </div>
    </el-main>
  </el-container>
</template>

<script>
export default {
  props: {
    title: {type: String, default: ""}
  }
}
</script>

<style>
</style>
