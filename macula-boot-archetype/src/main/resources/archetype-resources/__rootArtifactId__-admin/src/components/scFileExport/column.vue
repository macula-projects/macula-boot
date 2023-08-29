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
  <el-table ref="table" :data="columnData" border row-key="prop" style="width: 100%">
    <el-table-column label="排序" prop="" width="60">
      <el-tag class="move" disable-transitions style="cursor: move;">
        <el-icon style="cursor: move;">
          <el-icon-d-caret/>
        </el-icon>
      </el-tag>
    </el-table-column>
    <el-table-column label="列名" prop="label">
      <template #default="scope">
        <el-tag :effect="scope.row.hide?'light':'dark'" :type="scope.row.hide?'info':''" disable-transitions round>
          {{ scope.row.label }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="显示" prop="hide" width="60">
      <template #default="scope">
        <el-switch v-model="scope.row.hide" :active-value="false" :inactive-value="true" size="small"/>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
import Sortable from 'sortablejs'

export default {
  emits: ['success'],
  props: {
    column: {type: Array, default: () => []}
  },
  data() {
    return {
      columnData: this.column
    }
  },
  mounted() {
    this.rowDrop()
  },
  methods: {
    rowDrop() {
      const _this = this
      const tbody = this.$refs.table.$el.querySelector('.el-table__body-wrapper tbody')
      Sortable.create(tbody, {
        handle: ".move",
        animation: 200,
        ghostClass: "ghost",
        onEnd({newIndex, oldIndex}) {
          const tableData = _this.columnData
          const currRow = tableData.splice(oldIndex, 1)[0]
          tableData.splice(newIndex, 0, currRow)
        }
      })
    }
  }
}
</script>
