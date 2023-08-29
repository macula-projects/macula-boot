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
        <sc-list-table ref="listable" :apiObj="obj"
                       :params="params"
                       stripe>
          <el-table-column label="ID" prop="id" width="80"></el-table-column>
          <el-table-column label="用户名" prop="username" width="150"></el-table-column>
          <el-table-column label="用户昵称" prop="nickname" width="150"></el-table-column>
          <el-table-column label="所属角色" prop="roleNames" width="150"></el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="170"></el-table-column>
        </sc-list-table>
      </el-main>
    </el-container>
  </el-dialog>
</template>

<script>
export default {
  emits: ['closed'],
  data() {
    return {
      mode: 'show',
      row: null,
      titleMap: {
        show: '查看维护人',
      },
      visible: false,
      isSaveing: false,
      isSearching: false,
      isInit: false,
      params: {
        ids: '-1'
      },
      obj: this.$API.system_user.user.listByIds,
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
      if (this.row.supervisor !== null && this.row.supervisor instanceof Array) {
        this.params.ids = this.row.supervisor.map(item => item.id).join(',')
      }
    },
    //搜索
    upsearch() {
      this.$refs.listable.upData(this.search)
    },
  }
}
</script>

<style>
</style>
