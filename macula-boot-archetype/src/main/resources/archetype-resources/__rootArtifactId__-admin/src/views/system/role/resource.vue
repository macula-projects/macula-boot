<template>
  <el-dialog v-model="visible" :width="500" destroy-on-close title='角色权限设置' @close="$emit('closed')">
    <template #default>
      <el-container>
        <el-tabs v-model="activeName" style="width: 100%;">
          <el-tab-pane label="菜单权限" name="menu">
            <div class="treeMain">
              <el-tree
                  ref="menuTree"
                  :data="menuList"
                  :default-checked-keys="selectMenuList"
                  :default-expanded-keys="selectMenuList"
                  :filter-node-method="menuFilterNode"
                  :props="defaultProps"
                  check-strictly
                  highlight-current
                  node-key="id"
                  show-checkbox
                  @check-change="nodeCheck"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-container>
    </template>
    <template #footer>
      <el-button @click="visible=false">取 消</el-button>
      <el-button :loading="isSaveing" type="primary" @click="submit()">保 存</el-button>
    </template>
  </el-dialog>
</template>

<script>
export default {
  emits: ['success', 'closed'],
  props: {
    dataScopeEnum: {type: Array, default: []}
  },
  data() {
    return {
      visible: false,
      isSaveing: false,
      activeName: 'menu',
      menuFilterText: '',
      defaultProps: {
        label: (data) => {
          return data.name
        }
      },
      menuList: [],
      selectMenuList: [],
      roleId: null,
      menuTreeNodeMap: {}
    }
  },
  async created() {

  },
  watch: {},
  methods: {
    //加载菜单树数据
    async getMenu(params) {
      this.menuloading = true
      var res = await this.$API.system_menu.menu.list.get(params)
      this.menuloading = false
      if (res.code === '00000') {
        this.menuList = res.data
        this.$nextTick(() => {
          this.$refs.menuTree.filter()
        })
      }
    },
    //树过滤
    menuFilterNode(value, data, node) {
      this.menuTreeNodeMap[data.id] = {
        'cur': node,
        'parent': this.getParentNode(data),
        'children': this.getChildNode(node)
      }
      return true;
    },
    getParentNode(data) {
      let parentNodes = []
      this.loopLoadParentNode(data, parentNodes)
      return parentNodes
    },
    loopLoadParentNode(data, parentNodes) {
      if (data.parentId && this.menuTreeNodeMap[data.parentId]) {
        let parentNode = this.menuTreeNodeMap[data.parentId]['cur']
        this.loopLoadParentNode(parentNode.data, parentNodes)
        parentNodes.push(parentNode)
      }
    },
    getChildNode(node) {
      let childrenNodes = []
      this.loopLoadChildNode(childrenNodes, node)
      return childrenNodes
    },
    loopLoadChildNode(childrenNodes, node) {
      if (node && node.childNodes.length > 0) {
        node.childNodes.forEach(tmpNode => {
          this.loopLoadChildNode(childrenNodes, tmpNode)
          childrenNodes.push(tmpNode)
        })
      }
    },
    nodeCheck(data, curNodeState) {
      if (curNodeState) {
        this.menuTreeNodeMap[data.id]['parent'].forEach(parentNode => this.$refs.menuTree.setChecked(parentNode.data, curNodeState))
      } else {
        this.menuTreeNodeMap[data.id]['children'].forEach(childrenNode => this.$refs.menuTree.setChecked(childrenNode.data, curNodeState))
      }
    },
    open() {
      this.visible = true
      return this
    },
    async refreshResource(row) {
      this.roleId = row.id
      this.dataScope = row.dataScope
      const roleMenuIdsRes = await this.$API.system_role.role.getRoleMenuIds.get(this.roleId)
      if (roleMenuIdsRes.code === '00000') {
        this.selectMenuList = roleMenuIdsRes.data
      }
      this.getMenu()
    },
    async submit() {
      if (this.roleId) {
        this.isSaveing = true
        await this.updateMenuIds()
        this.isSaveing = false
        ElMessage.success('保存成功！')
        this.visible = false;
        this.$emit('success')
      } else {
        ElMessage.warning('数据加载中，请稍后重试或重新加载！')
      }
    },
    async updateMenuIds() {
      this.selectMenuList.length = 0
      this.$refs.menuTree.getCheckedNodes(false, true).forEach(item => this.selectMenuList.push(item.id))
      const putMenusRes = await this.$API.system_role.role.updateRoleMenus.put(this.roleId, this.selectMenuList)
      if (putMenusRes.code === '00000') {
        return
      }
    }
  }
}
</script>

<style scoped>
.treeMain {
  height: 320px;
  overflow: auto;
  border: 1px solid #dcdfe6;
  margin-bottom: 10px;
}
</style>
