<template>
  <el-container>
    <el-header>
      <div class="left-panel">
        <el-date-picker v-model="search.date" end-placeholder="结束日期" range-separator="至"
                        start-placeholder="开始日期" type="datetimerange" value-format="YYYY-MM-DD"></el-date-picker>
      </div>
      <div class="right-panel">
        <div class="right-panel-search">
          <el-input v-model="search.keywords" clearable placeholder="操作人/请求接口/操作名称"></el-input>
          <el-button icon="el-icon-search" type="primary" @click="upsearch"></el-button>
        </div>
      </div>
    </el-header>
    <el-main class="nopadding">
      <scTable ref="table" :apiObj="apiObj" highlightCurrentRow stripe>
        <el-table-column label="操作名称" prop="opTitle" width="150"></el-table-column>
        <el-table-column label="请求接口" prop="opUrl" width="150"></el-table-column>
        <el-table-column label="请求方法" prop="opRequestMethod" width="150"></el-table-column>
        <el-table-column label="操作人" prop="opName" width="150"></el-table-column>
        <el-table-column label="客户端IP" prop="opIp" width="150"></el-table-column>
        <el-table-column label="日志时间" prop="createTime" width="170"></el-table-column>
        <el-table-column :show-overflow-tooltip="true" label="响应结果" min-width="150" prop="jsonResult"
                         width="170"></el-table-column>
        <el-table-column :show-overflow-tooltip='true' label="异常信息" prop="errorMsg" width="170"></el-table-column>
      </scTable>
    </el-main>
  </el-container>
</template>

<script>

export default {
  name: 'log',
  data() {
    return {
      infoDrawer: false,
      date: [],
      apiObj: this.$API.system_log.log.list,
      search: {
        keywords: null,
        date: null
      },
      searchParam: {
        keywords: null,
        beginDate: null,
        endDate: null,
      }
    }
  },
  methods: {
    upsearch() {
      if (this.search.date != null) {
        this.searchParam.beginDate = this.search.date[0]
        this.searchParam.endDate = this.search.date[1]
      }
      this.searchParam.keywords = this.search.keywords
      this.$refs.table.upData(this.searchParam)
    }
  }
}
</script>

<style>
</style>