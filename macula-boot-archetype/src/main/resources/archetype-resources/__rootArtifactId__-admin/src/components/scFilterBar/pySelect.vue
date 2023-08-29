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
 * @Descripttion: 二次封装el-select 支持拼音
 * @version: 1.0
 * @Author: sakuya
 * @Date: 2021年7月31日22:26:56
 * @LastEditors:
 * @LastEditTime:
-->

<template>
  <el-select :filter-method="filterMethod" v-bind="$attrs" @visible-change="visibleChange">
    <el-option v-for="field in optionsList" :key="field.value" :disabled="isDisabled(field.value)" :label="field.label"
               :value="field"></el-option>
  </el-select>
</template>

<script>
import pinyin from 'pinyin-match'

export default {
  props: {
    options: {type: Array, default: () => []},
    filter: {type: Array, default: () => []}
  },
  data() {
    return {
      optionsList: [],
      optionsList_: []
    }
  },
  mounted() {
    this.optionsList = this.options
    this.optionsList_ = [...this.options]
  },
  methods: {
    filterMethod(keyword) {
      if (keyword) {
        this.optionsList = this.optionsList_
        this.optionsList = this.optionsList.filter((item) =>
            pinyin.match(item.label, keyword)
        );
      } else {
        this.optionsList = this.optionsList_
      }
    },
    visibleChange(isopen) {
      if (isopen) {
        this.optionsList = this.optionsList_
      }
    },
    isDisabled(key) {
      if (this.filter.find(item => item.field.value == key && !item.field.repeat)) {
        return true
      } else {
        return false
      }
    }
  }
}
</script>
