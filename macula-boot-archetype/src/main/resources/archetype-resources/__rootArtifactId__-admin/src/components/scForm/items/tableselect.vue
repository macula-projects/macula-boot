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
  <sc-table-select v-model="value" :apiObj="apiObj" :multiple="item.options.multiple" :props="item.options.props"
                   :table-width="600" style="width: 100%;">
    <el-table-column v-for="(_item, _index) in item.options.column" :key="_index" :label="_item.label"
                     :prop="_item.prop" :width="_item.width"></el-table-column>
  </sc-table-select>
</template>

<script>
export default {
  name: 'uploadRender',
  props: {
    modelValue: [String, Number, Boolean, Date, Object, Array],
    item: {
      type: Object, default: () => {
      }
    }
  },
  data() {
    return {
      value: this.modelValue,
      apiObj: this.getApiObj()
    }
  },
  watch: {
    value(val) {
      this.$emit("update:modelValue", val)
    }
  },
  mounted() {

  },
  methods: {
    getApiObj() {
      return eval(`this.` + this.item.options.apiObj)
    }
  }
}
</script>

<style>
</style>
