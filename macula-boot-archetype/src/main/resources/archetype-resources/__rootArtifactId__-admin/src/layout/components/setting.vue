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
  <el-form ref="form" label-position="left" label-width="120px" style="padding:0 20px;">
    <el-alert :closable="false"
              title="以下配置可实时预览，开发者可在 config/index.js 中配置默认值，非常不建议在生产环境下开放布局设置" type="error"></el-alert>
    <el-divider></el-divider>
    <el-form-item :label="$t('user.nightmode')">
      <el-switch v-model="dark"></el-switch>
    </el-form-item>
    <el-form-item :label="$t('user.language')">
      <el-select v-model="lang">
        <el-option label="简体中文" value="zh-cn"></el-option>
        <el-option label="English" value="en"></el-option>
      </el-select>
    </el-form-item>
    <el-divider></el-divider>
    <el-form-item label="主题颜色">
      <el-color-picker v-model="colorPrimary" :predefine="colorList">></el-color-picker>
    </el-form-item>
    <el-divider></el-divider>
    <el-form-item label="框架布局">
      <el-select v-model="layout" placeholder="请选择">
        <el-option label="默认" value="default"></el-option>
        <el-option label="通栏" value="header"></el-option>
        <el-option label="经典" value="menu"></el-option>
        <el-option label="功能坞" value="dock"></el-option>
      </el-select>
    </el-form-item>
    <el-form-item label="折叠菜单">
      <el-switch v-model="menuIsCollapse"></el-switch>
    </el-form-item>
    <el-form-item label="标签栏">
      <el-switch v-model="layoutTags"></el-switch>
    </el-form-item>
    <el-divider></el-divider>
  </el-form>
</template>

<script>
import colorTool from '@/utils/color'
import {mapWritableState} from 'pinia'
import {useGlobalStore} from '../../stores/global'

export default {
  data() {
    return {
      lang: this.$TOOL.data.get('APP_LANG') || this.$CONFIG.LANG,
      dark: this.$TOOL.data.get('APP_DARK') || false,
      colorList: ['#409EFF', '#009688', '#536dfe', '#ff5c93', '#c62f2f', '#fd726d'],
      colorPrimary: this.$TOOL.data.get('APP_COLOR') || this.$CONFIG.COLOR || '#409EFF'
    }
  },
  computed: {
    ...mapWritableState(useGlobalStore, ['layout', 'menuIsCollapse', 'layoutTags'])
  },
  watch: {
    layout(val) {
      this.layout = val
    },
    dark(val) {
      if (val) {
        document.documentElement.classList.add("dark")
        this.$TOOL.data.set("APP_DARK", val)
      } else {
        document.documentElement.classList.remove("dark")
        this.$TOOL.data.remove("APP_DARK")
      }
    },
    lang(val) {
      this.$i18n.locale = val
      this.$TOOL.data.set("APP_LANG", val);
    },
    colorPrimary(val) {
      if (!val) {
        val = '#409EFF'
        this.colorPrimary = '#409EFF'
      }
      document.documentElement.style.setProperty('--el-color-primary', val);
      for (let i = 1; i <= 9; i++) {
        document.documentElement.style.setProperty(`--el-color-primary-light-${i}`, colorTool.lighten(val, i / 10));
      }
      for (let i = 1; i <= 9; i++) {
        document.documentElement.style.setProperty(`--el-color-primary-dark-${i}`, colorTool.darken(val, i / 10));
      }
      this.$TOOL.data.set("APP_COLOR", val);
    }
  }
}
</script>

<style>
</style>
