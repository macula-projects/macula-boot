<template>
  <el-dialog v-model="visible" :title="titleMap[mode]" :width="500" destroy-on-close @closed="$emit('closed')">
    <el-form ref="dialogForm" :disabled="mode=='show'" :model="form" :rules="rules" label-position="left"
             label-width="100px">
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="form.name" clearable></el-input>
      </el-form-item>
      <el-form-item label="角色编码" prop="code">
        <el-input v-model="form.code" clearable></el-input>
      </el-form-item>
      <el-form-item label="数据权限" prop="dataScope">
        <el-select v-model="form.dataScope" placeholder="请选择角色数据权限">
          <el-option v-for="(item, index) in dataScopeEnum" :key="index" :label="item.label"
                     :value="item.value"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" controls-position="right"></el-input-number>
        <div class="el-form-item-msg">角色排序越小越前</div>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-switch v-model="form.status" active-value="1" inactive-value="0"></el-switch>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取 消</el-button>
      <el-button v-if="mode!='show'" :loading="isSaveing" type="primary" @click="submit()">保 存</el-button>
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
    let that = this
    return {
      mode: "add",
      titleMap: {
        add: '新增',
        edit: '编辑',
        show: '查看'
      },
      visible: false,
      isSaveing: false,
      //表单数据
      form: {
        id: "",
        name: "",
        code: "",
        sort: 1,
        status: '1',
        dataScope: "ALL"
      },
      //验证规则
      rules: {
        sort: [
          {required: true, message: '请输入排序', trigger: 'change'}
        ],
        name: [
          {required: true, validator: that.validtorRoleName, trigger: 'blur'}
        ],
        code: [
          {required: true, validator: that.validtorRoleCode, trigger: 'blur'}
        ],
        dataScope: [
          {required: true, message: '请选择角色数据权限'}
        ]
      }
    }
  },
  mounted() {

  },
  methods: {
    //显示
    open(mode = 'add') {
      this.mode = mode;
      this.visible = true;
      return this
    },
    async validtorRoleName(rule, value, callback) {
      if (value.trim().length === 0) {
        callback(new Error('请输入角色名称'))
      }
      const params = {
        id: this.form.id ? this.form.id : null,
        name: value,
      }
      const res = await this.$API.system_role.role.validtorRoleName.get(params)
      if (res.code === "00000" && res.data) {
        callback()
      }
      callback(new Error('角色名称已存在！'))
    },
    async validtorRoleCode(rule, value, callback) {
      if (value.trim().length === 0) {
        callback(new Error('请输入角色编码'))
      }
      const regx = /^[A-Z]+_*[A-Z]*$/g
      if (!regx.test(value)) {
        callback(new Error('角色编码由大写字母及下划线组成且必须以大写字母开头'))
      }
      const params = {
        id: this.form.id ? this.form.id : null,
        code: value,
      }
      const res = await this.$API.system_role.role.validtorRoleCode.get(params)
      if (res.code === "00000" && res.data) {
        callback()
      }
      callback(new Error('角色编码已存在！'))
    },
    //表单提交方法
    submit() {
      this.$refs.dialogForm.validate(async (valid) => {
        if (valid) {
          this.isSaveing = true;
          var res = await this.$API.system_role.role.post.post(this.form);
          this.isSaveing = false;
          if (res.code === "00000") {
            this.$emit('success', this.form, this.mode)
            this.visible = false;
            ElMessage.success("操作成功")
          } else {
            ElMessageBox.alert(res.message, "提示", {type: 'error'})
          }
        }
      })
    },
    //表单注入数据
    setData(data) {
      this.form.id = data.id
      this.form.name = data.name
      this.form.code = data.code
      this.form.sort = data.sort
      this.form.status = String(data.status)
      this.form.dataScope = data.dataScope
    }
  }
}
</script>

<style>
</style>
