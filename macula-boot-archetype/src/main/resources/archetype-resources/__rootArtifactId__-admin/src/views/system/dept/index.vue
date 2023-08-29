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
    <el-header>
      <div class="left-panel">
        <el-button icon="el-icon-plus" type="primary" @click="add"></el-button>
      </div>
      <div class="right-panel">
        <div class="right-panel-search">
          <el-select
              v-model="search.status"
              clearable
              placeholder="部门状态"
          >
            <el-option :value="1" label="正常"/>
            <el-option :value="0" label="禁用"/>
          </el-select>
          <el-input v-model="search.keywords" clearable placeholder="部门名称"></el-input>
          <el-button icon="el-icon-search" type="primary" @click="upsearch"></el-button>
        </div>
      </div>
    </el-header>
    <el-main class="nopadding">
      <scTable ref="table" :apiObj="apiObj" hidePagination row-key="id" @selection-change="selectionChange">
        <el-table-column type="selection" width="50"></el-table-column>
        <el-table-column label="部门名称" prop="name" width="250"></el-table-column>
        <el-table-column label="排序" prop="sort" width="150"></el-table-column>
        <el-table-column label="状态" prop="status" width="150">
          <template #default="scope">
            <el-tag v-if="scope.row.status==1" type="success">启用</el-tag>
            <el-tag v-if="scope.row.status==0" type="danger">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180"></el-table-column>
        <el-table-column align="right" fixed="right" label="操作" width="170">
          <template #default="scope">
            <el-button-group>
              <el-button size="small" text type="primary" @click="table_edit(scope.row, scope.$index)">编辑</el-button>
              <el-popconfirm title="确定删除吗？" @confirm="table_del(scope.row, scope.$index)">
                <template #reference>
                  <el-button size="small" text type="primary">删除</el-button>
                </template>
              </el-popconfirm>
            </el-button-group>
          </template>
        </el-table-column>
      </scTable>
    </el-main>
  </el-container>

  <save-dialog v-if="dialog.save" ref="saveDialog" @closed="dialog.save=false"
               @success="handleSaveSuccess"></save-dialog>

</template>

<script>
import saveDialog from './save'

export default {
  name: 'dept',
  components: {
    saveDialog
  },
  data() {
    return {
      dialog: {
        save: false
      },
      apiObj: this.$API.system_dept.dept.list,
      selection: [],
      search: {
        keywords: null,
        status: null
      }
    }
  },
  methods: {
    //添加
    add() {
      this.dialog.save = true
      this.$nextTick(() => {
        this.$refs.saveDialog.open()
      })
    },
    //编辑
    table_edit(row) {
      this.dialog.save = true
      this.$nextTick(() => {
        this.$refs.saveDialog.open('edit').setData(row)
      })
    },
    //查看
    table_show(row) {
      this.dialog.save = true
      this.$nextTick(() => {
        this.$refs.saveDialog.open('show').setData(row)
      })
    },
    //删除
    async table_del(row) {
      var reqData = row.id
      var res = await this.$API.system_dept.dept.del.delete(reqData);
      if (res.code === '00000') {
        this.$refs.table.refresh()
        ElMessage.success("删除成功")
      } else {
        ElMessageBox.alert(res.message, "提示", {type: 'error'})
      }
    },
    async batch_del() {
      ElMessageBox.confirm(`确定删除选中的 ${this.selection.length} 项吗？如果删除项中含有子集将会被一并删除`, '提示', {
        type: 'warning'
      }).then(() => {
        const loading = this.$loading();
        this.$refs.table.refresh()
        loading.close();
        ElMessage.success("操作成功")
      }).catch(() => {

      })
    },
    //表格选择后回调事件
    selectionChange(selection) {
      this.selection = selection;
    },
    //搜索
    upsearch() {
      this.$refs.table.upData(this.search)
    },
    //根据ID获取树结构
    filterTree(id) {
      var target = null;

      function filter(tree) {
        tree.forEach(item => {
          if (item.id == id) {
            target = item
          }
          if (item.children) {
            filter(item.children)
          }
        })
      }

      filter(this.$refs.table.tableData)
      return target
    },
    //本地更新数据
    handleSaveSuccess(data, mode) {
      this.$refs.table.refresh()
    }
  }
}
</script>

<style>
</style>
