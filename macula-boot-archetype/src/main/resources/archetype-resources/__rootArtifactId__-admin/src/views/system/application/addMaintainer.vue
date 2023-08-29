<template>
  <el-dialog v-model="visible" :title="titleMap[mode]" :width="800" destroy-on-close @closed="$emit('closed')">
    <el-container>
      <el-header>
        <div class="left-panel"/>
        <div class="right-panel">
          <div class="right-panel-search">
            <el-input v-model="search.keywords" clearable placeholder="用户名"></el-input>
            <el-button icon="el-icon-search" type="primary" @click="upsearch"></el-button>
          </div>
        </div>
      </el-header>
      <el-main class="nopadding">
        <sc-list-table ref="listable" :apiObj="apiObjUserList"
                       stripe
                       @select="onTableSelect"
                       @data-change="dataChange"
                       @select-all="onTableSelectAll">
          <el-table-column :reserve-selection="true" type="selection" width="50"></el-table-column>
          <el-table-column label="ID" prop="id" width="80"></el-table-column>
          <el-table-column label="用户名" prop="username" width="150"></el-table-column>
          <el-table-column label="用户昵称" prop="nickname" width="150"></el-table-column>
          <el-table-column label="所属角色" prop="roleNames" width="150"></el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="170"></el-table-column>
        </sc-list-table>
      </el-main>
    </el-container>
    <template #footer>
      <el-button @click="visible=false">取 消</el-button>
      <el-button :loading="isSaveing" type="primary" @click="submit()">保 存</el-button>
    </template>
  </el-dialog>
</template>

<script>
export default {
  emits: ['success', 'closed'],
  data() {
    return {
      mode: 'add',
      row: null,
      titleMap: {
        add: '添加维护人',
      },
      visible: false,
      isSaveing: false,
      isSearching: false,
      isInit: false,
      apiObjUserList: this.$API.system_user.user.list,
      tableListData: [],
      currentPageData: [],
      selectIds: [],
      search: {
        keywords: null
      }
    }
  },
  methods: {
    //显示
    open(row) {
      this.row = row;
      this.visible = true;
      this.isInit = true
      // return this
    },
    dataChange(data) {
      this.currentPageData = data.data.records
      if (!this.$refs.listable.isPagingSize || !this.$refs.listable.isPaging || this.isSearching) {
        this.toggleSelection(data.data.records, this.row.maintainer)
      }
    },
    toggleSelection(rows, currentRow) {
      var arrRow = []
      if (this.isInit) {
        if (currentRow !== null) {
          arrRow = currentRow.split(',')
          if (!this.isSearching) {
            this.selectIds = [...new Set(arrRow)]
          }
        }
        this.isInit = false
      } else {
        arrRow = this.selectIds
      }
      if (this.$refs.listable) {
        // 筛选当前应用对应的维护人
        arrRow.forEach((row) => {
          this.$nextTick(() => {
            var a = rows.find(r => r.id == row)
            if (a != null) {
              this.$refs.listable.toggleRowSelection(a, true)
            }
          })
        })
      }
    },
    //表单提交方法
    async submit() {
      if (this.selectIds.length > 0) {
        this.isSaveing = true;
        let params = {
          maintainer: ''
        }
        this.selectIds.forEach((item, index) => {
          if (index == 0) {
            params.maintainer = item
          } else {
            params.maintainer = params.maintainer + ',' + item
          }
        })
        var res = await this.$API.system_application.application.addMaintainer.put(params, this.row.id);
        this.isSaveing = false;
        if (res.code === '00000') {
          this.$emit('success', this.form, this.mode)
          this.visible = false;
          ElMessage.success("操作成功")
        } else {
          ElMessageBox.alert(res.message, "提示", {type: 'error'})
        }
      } else {
        return false;
      }
    },
    // 勾选数据行的 Checkbox 时触发的方法
    onTableSelect(rows, row) {
      //  判断是点击了表格勾选还是取消勾选，true为选中，0或false是取消选中
      const selected = rows.length && rows.indexOf(row) !== -1
      if (!selected) {
        if (row !== null) {
          // 如果点击取消勾选
          const index = this.selectIds.indexOf(row.id)
          this.selectIds.splice(index, 1) // 取消勾选，则删除id
        }
      } else {
        this.selectIds.push(row.id)
      }
    },
    // 全选时触发的方法
    onTableSelectAll(selection) {
      // 获取当前页码所显示的数据
      const a = this.currentPageData
      // 获取当前页勾选的数据
      const b = selection
      let flag_inCurrentPage
      selection.forEach((item) => {
        if (item.itemId === a[0].itemId) {
          flag_inCurrentPage = true
          return
        }
      })
      // true 为全选  false 为取消勾选
      const flag = a.length === b.length && flag_inCurrentPage
      if (flag) {
        // 全选
        this.currentPageData.forEach((item) => {
          if (this.selectIds.indexOf(item.id) === -1) {
            this.selectIds.push(item.id)
          }
        })
      } else {
        // 取消全勾选
        // 将当前页的数据进行移除
        this.selectIds = this.selectIds.filter(x => !this.currentPageData.some(y => y.id === x))
      }
    },
    //搜索
    upsearch() {
      this.isSearching = true
      this.$refs.listable.upData(this.search)
    },
  }
}
</script>

<style>
</style>
