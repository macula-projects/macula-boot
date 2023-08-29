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
    <el-aside v-loading="showDicloading" width="300px">
      <el-container>
        <el-header>
          <el-input v-model="dicFilterText" clearable placeholder="输入关键字进行过滤"></el-input>
        </el-header>
        <el-main class="nopadding">
          <el-tree ref="dic" :data="dicList" :expand-on-click-node="false" :filter-node-method="dicFilterNode" :highlight-current="true" :props="dicProps"
                   class="menu" node-key="id" @node-click="dicClick">
            <template #default="{node, data}">
							<span class="custom-tree-node">
								<span class="label">{{ node.label }}</span>
								<span class="code">{{ data.code }}</span>
								<span class="status">
									<el-tag v-if="data.status === 1" type="success">启用</el-tag>
									<el-tag v-else type="info">禁用</el-tag>
								</span>
								<span class="do">
									<el-button-group>
										<el-button icon="el-icon-edit" size="small" @click.stop="dicEdit(data)"></el-button>
										<el-button icon="el-icon-delete" size="small" @click.stop="dicDel(node, data)"></el-button>
									</el-button-group>
								</span>
							</span>
            </template>
          </el-tree>
        </el-main>
        <el-footer style="height:51px;">
          <el-button icon="el-icon-plus" size="small" style="width: 100%;" type="primary" @click="addDic">字典分类
          </el-button>
        </el-footer>
      </el-container>
    </el-aside>
    <el-container class="is-vertical">
      <el-header>
        <div class="left-panel">
          <el-button icon="el-icon-plus" type="primary" @click="addInfo"></el-button>
          <el-button :disabled="selection.length==0" icon="el-icon-delete" plain type="danger"
                     @click="batch_del"></el-button>
        </div>
      </el-header>
      <el-main class="nopadding">
        <scTable ref="table" :apiObj="listApi" :paginationLayout="'prev, pager, next'" :params="listApiParams" row-key="id"
                 stripe @selection-change="selectionChange">
          <el-table-column type="selection" width="50"></el-table-column>
          <el-table-column label="名称" prop="name" width="150"></el-table-column>
          <el-table-column label="键值" prop="value" width="150"></el-table-column>
          <el-table-column label="是否有效" prop="status" width="100">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 1" type="success">启用</el-tag>
              <el-tag v-else type="info">禁用</el-tag>
            </template>
          </el-table-column>
          <el-table-column align="right" fixed="right" label="操作" width="120">
            <template #default="scope">
              <el-button-group>
                <el-button size="small" text type="primary" @click="table_edit(scope.row, scope.$index)">编辑
                </el-button>
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
  </el-container>

  <dic-dialog v-if="dialog.dic" ref="dicDialog" @closed="dialog.dic=false" @success="handleDicSuccess"></dic-dialog>

  <list-dialog v-if="dialog.list" ref="listDialog" @closed="dialog.list=false"
               @success="handleListSuccess"></list-dialog>

</template>

<script>
import dicDialog from './dic'
import listDialog from './list'
import Sortable from 'sortablejs'

export default {
  name: 'dic',
  components: {
    dicDialog,
    listDialog
  },
  data() {
    return {
      dialog: {
        dic: false,
        info: false
      },
      showDicloading: true,
      dicList: [],
      dicFilterText: '',
      dicProps: {
        label: 'name'
      },
      listApi: null,
      listApiParams: {},
      selection: []
    }
  },
  watch: {
    dicFilterText(val) {
      this.$refs.dic.filter(val);
    }
  },
  mounted() {
    this.getDic()
  },
  methods: {
    //加载树数据
    async getDic() {
      var res = await this.$API.system_dict.dict.typeListPages.get();
      this.showDicloading = false;
      this.dicList = res.data.records;
      //获取第一个节点,设置选中 & 加载明细列表
      var firstNode = this.dicList[0];
      if (firstNode) {
        this.$nextTick(() => {
          this.$refs.dic.setCurrentKey(firstNode.id)
        })
        this.listApiParams = {
          typeCode: firstNode.code
        }
        this.listApi = this.$API.system_dict.dict.itemsListPages;
      }
    },
    //树过滤
    dicFilterNode(value, data) {
      if (!value) return true;
      var targetText = data.name + data.code;
      return targetText.indexOf(value) !== -1;
    },
    //树增加
    addDic() {
      this.dialog.dic = true
      this.$nextTick(() => {
        this.$refs.dicDialog.open()
      })
    },
    //编辑树
    dicEdit(data) {
      this.dialog.dic = true
      this.$nextTick(() => {
        this.$refs.dicDialog.open('edit').setData(data)
      })
    },
    //树点击事件
    dicClick(data) {
      this.$refs.table.reload({
        typeCode: data.code
      })
    },
    //删除树
    async dicDel(node, data) {
      this.showDicloading = true;
      var res = await this.$API.system_dict.dict.delType.delete(data.id);
      if (res.code === '00000') {
        var dicCurrentKey = this.$refs.dic.getCurrentKey();
        this.$refs.dic.remove(data.id)
        if (dicCurrentKey == data.id) {
          var firstNode = this.dicList[0];
          if (firstNode) {
            this.$refs.dic.setCurrentKey(firstNode.id);
            this.$refs.table.upData({
              code: firstNode.code
            })
          } else {
            this.listApi = null;
            this.$refs.table.tableData = []
          }
        }
        this.showDicloading = false;
        ElMessage.success("删除成功")
      } else {
        ElMessageBox.alert(res.message, "提示", {type: 'error'})
      }
    },
    //添加明细
    addInfo() {
      this.dialog.list = true
      this.$nextTick(() => {
        var dicCurrentKey = this.$refs.dic.getCurrentKey();
        var code = null
        if (this.dicList.length > 0) {
          var t = this.dicList.find(d => d.id == dicCurrentKey)
          code = t.code
        }
        const data = {
          dic: dicCurrentKey,
          code: code
        }
        this.$refs.listDialog.open().setData(data)
      })
    },
    //编辑明细
    table_edit(row) {
      this.dialog.list = true
      this.$nextTick(() => {
        var dicCurrentKey = this.$refs.dic.getCurrentKey();
        row.dic = dicCurrentKey

        var t = this.dicList.find(d => d.id == dicCurrentKey)
        row.code = t.code
        this.$refs.listDialog.open('edit').setData(row)
      })
    },
    //删除明细
    async table_del(row, index) {
      var res = await this.$API.system_dict.dict.delItem.delete(row.id);
      if (res.code === '00000') {
        this.$refs.table.tableData.splice(index, 1);
        ElMessage.success("删除成功")
      } else {
        ElMessageBox.alert(res.message, "提示", {type: 'error'})
      }
    },
    //批量删除
    async batch_del() {
      ElMessageBox.confirm(`确定删除选中的 ${this.selection.length} 项吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        this.selection.forEach(item => {
          this.$refs.table.tableData.forEach((itemI, indexI) => {
            if (item.id === itemI.id) {
              var res = this.$API.system_dict.dict.delItem.delete(itemI.id);
              this.$refs.table.tableData.splice(indexI, 1)
            }
          })
        })
        ElMessage.success("操作成功")
      }).catch(() => {

      })
    },
    //提交明细
    saveList() {
      this.$refs.listDialog.submit(async (formData) => {
        this.isListSaveing = true;
        var res = await this.$API.demo.post.post(formData);
        this.isListSaveing = false;
        if (res.code == 200) {
          //这里选择刷新整个表格 OR 插入/编辑现有表格数据
          this.listDialogVisible = false;
          ElMessage.success("操作成功")
        } else {
          ElMessageBox.alert(res.message, "提示", {type: 'error'})
        }
      })
    },
    //表格选择后回调事件
    selectionChange(selection) {
      this.selection = selection;
    },
    //本地更新数据
    handleDicSuccess(data, mode) {
      if (mode == 'add') {
        this.getDic()
      } else if (mode == 'edit') {
        var editNode = this.$refs.dic.getNode(data.id);
        //判断是否移动？
        var editNodeParentId = editNode.level == 1 ? undefined : editNode.parent.data.id
        if (editNodeParentId != data.parentId) {
          var obj = editNode.data;
          this.$refs.dic.remove(data.id)
          this.$refs.dic.append(obj, data.parentId[0])
        }
        Object.assign(editNode.data, data)
      }
    },
    //本地更新数据
    handleListSuccess(data, mode) {
      this.$refs.table.reload({
        typeCode: data.oldTypeCode
      })
    }
  }
}
</script>

<style scoped>
.custom-tree-node {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 24px;
  height: 100%;
}

.custom-tree-node .code {
  font-size: 12px;
  color: #999;
}

.custom-tree-node .status {
  font-size: 12px;
  color: #999;
}

.custom-tree-node .do {
  display: none;
}

.custom-tree-node:hover .code {
  display: none;
}

.custom-tree-node:hover .status {
  display: none;
}

.custom-tree-node:hover .do {
  display: inline-block;
}
</style>
