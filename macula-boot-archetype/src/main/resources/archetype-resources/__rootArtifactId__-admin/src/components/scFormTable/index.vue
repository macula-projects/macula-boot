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
 * @Descripttion: 表单表格
 * @version: 1.3
 * @Author: sakuya
 * @Date: 2023年2月9日12:32:26
 * @LastEditors: sakuya
 * @LastEditTime: 2023年2月17日10:41:47
-->

<template>
  <div ref="scFormTable" class="sc-form-table">
    <el-table ref="table" :data="data" border stripe>
      <el-table-column fixed="left" type="index" width="50">
        <template #header>
          <el-button v-if="!hideAdd" circle icon="el-icon-plus" size="small" type="primary" @click="rowAdd"></el-button>
        </template>
        <template #default="scope">
          <div :class="['sc-form-table-handle', {'sc-form-table-handle-delete':!hideDelete}]">
            <span>{{ scope.$index + 1 }}</span>
            <el-button v-if="!hideDelete" circle icon="el-icon-delete" plain size="small" type="danger"
                       @click="rowDel(scope.row, scope.$index)"></el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="dragSort" label="" width="50">
        <template #default>
          <div class="move" style="cursor: move;">
            <el-icon-d-caret style="width: 1em; height: 1em;"/>
          </div>
        </template>
      </el-table-column>
      <slot></slot>
      <template #empty>
        {{ placeholder }}
      </template>
    </el-table>
  </div>
</template>

<script>
import Sortable from 'sortablejs'

export default {
  props: {
    modelValue: {type: Array, default: () => []},
    addTemplate: {
      type: Object, default: () => {
      }
    },
    placeholder: {type: String, default: "暂无数据"},
    dragSort: {type: Boolean, default: false},
    hideAdd: {type: Boolean, default: false},
    hideDelete: {type: Boolean, default: false}
  },
  data() {
    return {
      data: []
    }
  },
  mounted() {
    this.data = this.modelValue
    if (this.dragSort) {
      this.rowDrop()
    }
  },
  watch: {
    modelValue() {
      this.data = this.modelValue
    },
    data: {
      handler() {
        this.$emit('update:modelValue', this.data);
      },
      deep: true
    }
  },
  methods: {
    rowDrop() {
      const _this = this
      const tbody = this.$refs.table.$el.querySelector('.el-table__body-wrapper tbody')
      Sortable.create(tbody, {
        handle: ".move",
        animation: 300,
        ghostClass: "ghost",
        onEnd({newIndex, oldIndex}) {
          _this.data.splice(newIndex, 0, _this.data.splice(oldIndex, 1)[0])
          const newArray = _this.data.slice(0)
          const tmpHeight = _this.$refs.scFormTable.offsetHeight
          _this.$refs.scFormTable.style.setProperty('height', tmpHeight + 'px')
          _this.data = []
          _this.$nextTick(() => {
            _this.data = newArray
            _this.$nextTick(() => {
              _this.$refs.scFormTable.style.removeProperty('height')
            })

          })
        }
      })
    },
    rowAdd() {
      const temp = JSON.parse(JSON.stringify(this.addTemplate))
      this.data.push(temp)
    },
    rowDel(row, index) {
      this.data.splice(index, 1)
    }
  }
}
</script>

<style scoped>
.sc-form-table {
  width: 100%;
}

.sc-form-table .sc-form-table-handle {
  text-align: center;
}

.sc-form-table .sc-form-table-handle span {
  display: inline-block;
}

.sc-form-table .sc-form-table-handle button {
  display: none;
}

.sc-form-table .hover-row .sc-form-table-handle span {
  display: none;
}

.sc-form-table .hover-row .sc-form-table-handle button {
  display: inline-block;
}
</style>
