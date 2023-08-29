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
    <el-aside v-loading="menuloading" width="300px">
      <el-container>
        <el-header>
          <el-input v-model="menuFilterText" clearable placeholder="输入关键字进行过滤"></el-input>
        </el-header>
        <el-main class="nopadding">
          <el-tree ref="menu" :data="menuList" :default-expanded-keys="defaultExpandedIds" :expand-on-click-node="false" :props="menuProps"
                   check-strictly class="menu" draggable highlight-current
                   node-key="id" show-checkbox @node-click="menuClick" @node-drop="nodeDrop">
            <template #default="{node, data}">
							<span class="custom-tree-node el-tree-node__label">
								<span class="label">
									{{ node.label }}
								</span>
								<span v-if="data.type !== 'BUTTON' && data.type !== 'IFRAME' && data.type !== 'LINK'" class="do">
									<el-icon @click.stop="add(node, data)"><el-icon-plus/></el-icon>
								</span>
							</span>
            </template>
          </el-tree>
        </el-main>
        <el-footer style="height:51px;">
          <el-button icon="el-icon-plus" size="small" type="primary" @click="add()"></el-button>
          <el-button icon="el-icon-delete" plain size="small" type="danger" @click="delMenu"></el-button>
        </el-footer>
      </el-container>
    </el-aside>
    <el-container>
      <el-main ref="main" class="nopadding" style="padding:20px;">
        <save ref="save" :menu="menuList"></save>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
let newMenuIndex = 1;
import save from './save'

export default {
  name: "settingMenu",
  components: {
    save
  },
  data() {
    return {
      menuloading: false,
      menuList: [],
      menuProps: {
        label: (data) => {
          return data.name
        }
      },
      menuFilterText: "",
      defaultExpandedIds: [],
      curMenuType: ""
    }
  },
  watch: {
    async menuFilterText(val) {
      await this.getMenu({keywords: this.menuFilterText})
    }
  },
  mounted() {
    this.getMenu()
  },
  methods: {
    //加载树数据
    async getMenu(params) {
      this.menuloading = true
      var res = await this.$API.system_menu.menu.list.get(params)
      this.menuloading = false
      if (res.code === '00000') {
        this.menuList = res.data
      }
    },
    //树点击
    menuClick(data, node) {
      var pid = node.level == 1 ? '0' : node.parent.data.id
      this.curMenuType = data.type
      this.$refs.save.setData(data, pid)
      this.$refs.main.$el.scrollTop = 0
    },
    //树拖拽
    nodeDrop(draggingNode, dropNode, dropType) {
      this.$refs.save.setData({})
      ElMessage(`拖拽对象：${draggingNode.data.meta.title}, 释放对象：${dropNode.data.meta.title}, 释放对象的位置：${dropType}`)
    },
    //增加
    async add(node, data) {
      var newMenuName = "未命名" + newMenuIndex++;
      var newMenuData = {
        id: "",
        parentId: data ? data.id : "0",
        name: newMenuName,
        path: "/",
        component: "",
        type: data ? (data.type === 'MENU' ? 'BUTTON' : 'CATALOG') : 'CATALOG',
        icon: '',
        visible: 1,
        sort: 0,
        perm: '',
        redirect: ''
      }
      var res = await this.$API.system_menu.menu.add.post(newMenuData)
      if (res.code === '00000' && data) {
        if (newMenuData.type === 'BUTTON' && this.curMenuType === 'CATALOG') {
          //目录后选择菜单然后创建下级菜单将自动更新
          data.createTime = ''
          data.path = data.routePath
          await this.$API.system_menu.menu.add.post(data)
        }
        data.children = data.children || []
        data.children.push(newMenuData)
        this.$refs.save.setData(data, data.parentId || "0")
      }
      this.getMenu()
      this.defaultExpandedIds = []
      this.loopPushDefaultExpandedIds(node)
    },
    loopPushDefaultExpandedIds(node) {
      if (!node) {
        return
      }
      if (node.data && node.data.id) {
        this.defaultExpandedIds.push(node.data.id)
      }
      if (node.parent) {
        this.loopPushDefaultExpandedIds(node.parent)
      }
    },
    //删除菜单
    async delMenu() {
      var CheckedNodes = this.$refs.menu.getCheckedNodes()
      if (CheckedNodes.length == 0) {
        ElMessage.warning("请选择需要删除的项")
        return false;
      }

      var confirm = await ElMessageBox.confirm('确认删除已选择的菜单吗？', '提示', {
        type: 'warning',
        confirmButtonText: '删除',
        confirmButtonClass: 'el-button--danger'
      }).catch(() => {
      })
      if (confirm != 'confirm') {
        return false
      }

      this.menuloading = true
      var reqData = []
      CheckedNodes.map(item => {
        this.loadingSubMenuId(item, reqData)
      })
      var res = await this.$API.system_menu.menu.del.delete(reqData)
      await this.delMenuPermission(reqData)
      this.menuloading = false

      if (res.code === "00000") {
        CheckedNodes.forEach(item => {
          var node = this.$refs.menu.getNode(item)
          if (node && node.isCurrent) {
            this.$refs.save.setData({})
          }
          this.$refs.menu.remove(item)
        })
      } else {
        ElMessage.warning(res.message)
      }
    },
    loadingSubMenuId(item, reqData) {
      reqData.push(item.id)
      if (item.children && item.children.length > 0) {
        item.children.forEach(subItem => this.loadingSubMenuId(subItem, reqData))
      }
    },
    async delMenuPermission(reqData) {
      if (reqData.length === 0) {
        return
      }
      let permIds = []
      for (var i = 0; i < reqData.length; i++) {
        let permListRes = await this.$API.system_permission.permission.list.get({menuId: reqData[i]})
        if (permListRes.code === '00000' && permListRes.data.length > 0) {
          permListRes.data.forEach(item => permIds.push(item.id))
        }
      }
      if (permIds.length !== 0) {
        return await this.$API.system_permission.permission.del.delete(permIds.join(","))
      }
      return
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

.custom-tree-node .label {
  display: flex;
  align-items: center;;
  height: 100%;
}

.custom-tree-node .label .el-tag {
  margin-left: 5px;
}

.custom-tree-node .do {
  display: none;
}

.custom-tree-node .do i {
  margin-left: 5px;
  color: #999;
}

.custom-tree-node .do i:hover {
  color: #333;
}

.custom-tree-node:hover .do {
  display: inline-block;
}
</style>
