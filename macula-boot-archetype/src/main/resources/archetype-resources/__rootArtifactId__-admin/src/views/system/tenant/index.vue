<template>
  <el-container>
    <el-header>
      <div class="left-panel">
        <el-button icon="el-icon-plus" type="primary" @click="add"></el-button>
        <el-button :disabled="selection.length==0" icon="el-icon-delete" plain type="danger"
                   @click="batch_del"></el-button>
      </div>
      <div class="right-panel">
        <div class="right-panel-search">
          <el-input v-model="keyWord" clearable placeholder="租户名称"></el-input>
          <el-button icon="el-icon-search" type="primary" @click="searchTenant"></el-button>
        </div>
      </div>
    </el-header>
    <el-main class="nopadding">
      <scTable ref="table" :apiObj="apiObj" row-key="id" stripe @selection-change="selectionChange">
        <el-table-column type="selection" width="50"></el-table-column>
        <el-table-column label="#" type="index" width="50"></el-table-column>
        <el-table-column label="租户名称" prop="name" width="250"></el-table-column>
        <el-table-column label="租户编码" prop="code" width="250"></el-table-column>
        <el-table-column label="负责人" prop="supervisor" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.supervisor.map(item => item.username).join(',') }}
          </template>
        </el-table-column>
        <el-table-column label="描述" prop="description" show-overflow-tooltip width="120"></el-table-column>
        <el-table-column align="right" fixed="right" label="操作" width="250">
          <template #default="scope">
            <el-button-group>
              <el-button size="small" text type="primary" @click="table_show(scope.row, scope.$index)">查看负责人
              </el-button>
              <el-button size="small" text type="primary" @click="show(scope.row)">查看</el-button>
              <el-button size="small" text type="primary" @click="edit(scope.row, scope.$index)">编辑</el-button>
              <el-popconfirm title="确定删除吗？" @confirm="del(scope.row, scope.$index)">
                <template #reference>
                  <el-button size="small" text type="danger">删除</el-button>
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
  <show-maintainer-dialog v-if="dialog.show" ref="showMaintainerDialog"
                          @closed="dialog.show=false"></show-maintainer-dialog>
</template>

<script>
import saveDialog from './save'
import showMaintainerDialog from './showMaintainer'

export default {
  name: 'tenant',
  data() {
    let that = this
    return {
      keyWord: '',
      apiObj: this.$API.system_tenant.tenant.pages,
      selection: [],
      dialog: {
        save: false,
        resource: false,
        show: false
      }
    }
  },
  components: {
    saveDialog,
    showMaintainerDialog
  },
  methods: {
    table_show(row, rowIndex) {
      this.dialog.show = true
      this.$nextTick(() => {
        this.$refs.showMaintainerDialog.open(row)
      })
    },
    add() {
      this.dialog.save = true
      this.$nextTick(() => {
        this.$refs.saveDialog.open()
      })
    },
    show(row) {
      this.dialog.save = true
      this.$nextTick(() => {
        this.$refs.saveDialog.open("show").setData(row)
      })
    },
    edit(row, index) {
      this.dialog.save = true
      this.$nextTick(() => {
        this.$refs.saveDialog.open("edit").setData(row)
      })
    },
    async del(row, index) {
      var res = await this.$API.system_tenant.tenant.del.delete(row.id)
      if (res.code === "00000") {
        this.$refs.table.refresh()
        ElMessage.success("删除成功")
      } else {
        ElMessageBox.alert(res.message, "提示", {type: 'error'})
      }
    },
    async batch_del() {
      const msgBox = await ElMessageBox.confirm(`确定删除选中的 ${this.selection.length} 项吗？如果删除项中含有子集将会被一并删除`, '提示', {
        type: 'warning'
      }).catch(() => {
      });
      if (msgBox === 'confirm') {
        let loading = ElLoading.service({fullscreen: true})
        await this.$API.system_tenant.tenant.del.delete(this.selection.map(item => item.id))
        this.$refs.table.refresh()
        loading.close()
        ElMessage.success("操作成功")
      }
    },
    searchTenant() {
      this.$refs.table.upData({keywords: this.keyWord})
    },
    //表格选择后回调事件
    selectionChange(selection) {
      this.selection = selection
    },
    handleSaveSuccess() {
      this.$refs.table.refresh()
    }
  }
}
</script>

<style>
</style>
