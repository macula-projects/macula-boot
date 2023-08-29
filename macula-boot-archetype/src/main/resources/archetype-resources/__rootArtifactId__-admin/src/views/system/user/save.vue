<template>
  <el-dialog v-model="visible" :title="titleMap[mode]" :width="500" destroy-on-close @closed="$emit('closed')">
    <el-form ref="dialogForm" :disabled="mode=='show'" :model="form" :rules="rules" label-position="left"
             label-width="100px">
      <template v-if="mode!='resetPassword'">
        <el-form-item label="头像">
          <sc-upload v-model="form.avatar" title="上传头像"></sc-upload>
        </el-form-item>
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" clearable placeholder="用于登录系统"></el-input>
        </el-form-item>
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="form.nickname" clearable placeholder="请输入完整的真实姓名"></el-input>
        </el-form-item>
        <el-form-item label="所属部门" prop="dept">
          <el-cascader v-model="form.dept" :options="depts" :placeholder="form.dept" :props="deptsProps"
                       clearable style="width: 100%;" @change="handleChangeDept"></el-cascader>
        </el-form-item>
        <el-form-item label="所属角色">
          <el-cascader v-model="form.roleNames" :options="roleNames" :placeholder="form.roleNames"
                       :props="roleNamesProps" clearable style="width: 100%;"
                       @change="handleChangeRole"></el-cascader>
        </el-form-item>
      </template>
      <template v-if="mode=='resetPassword'">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" clearable show-password type="password"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" clearable show-password type="password"></el-input>
        </el-form-item>
      </template>
      <template v-if="mode=='add'">
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" clearable show-password type="password"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="password2">
          <el-input v-model="form.password2" clearable show-password type="password"></el-input>
        </el-form-item>
      </template>
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
  data() {
    return {
      mode: "add",
      titleMap: {
        add: '新增用户',
        edit: '编辑用户',
        show: '查看',
        resetPassword: '重置密码'
      },
      visible: false,
      isSaveing: false,
      //表单数据
      form: {
        id: "",
        username: "",
        avatar: "",
        nickname: "",
        dept: "",
        roleNames: "",
        deptId: null,
        password: "",
        roleIds: []
      },
      //验证规则
      rules: {
        // avatar:[
        //   {required: true, message: '请上传头像'}
        // ],
        role: [
          {required: true, message: '请选择所属角色'}
        ],
        username: [
          {required: true, message: '请输入登录账号'}
        ],
        nickname: [
          {required: true, message: '请输入真实姓名'}
        ],
        password: [
          {required: true, message: '请输入登录密码'},
          {
            validator: (rule, value, callback) => {
              if (this.form.password2 !== '') {
                this.$refs.dialogForm.validateField('password2');
              }
              callback();
            }
          }
        ],
        password2: [
          {required: true, message: '请再次输入密码'},
          {
            validator: (rule, value, callback) => {
              if (value !== this.form.password) {
                callback(new Error('两次输入密码不一致!'));
              } else {
                callback();
              }
            }
          }
        ],
        dept: [
          {required: true, message: '请选择所属部门'}
        ]
      },
      //所需数据选项
      roleNames: [],
      roleNamesProps: {
        value: "value",
        multiple: true,
        checkStrictly: true
      },
      depts: [],
      deptsProps: {
        children: "children",
        label: "name",
        value: "id",
        checkStrictly: true
      }
    }
  },
  mounted() {
    this.getRole()
    this.getDept()
  },
  methods: {
    //显示
    open(mode = 'add') {
      this.mode = mode;
      this.visible = true;
      return this
    },
    //加载树数据
    async getRole() {
      var res = await this.$API.system_role.role.options.get();
      this.roleNames = res.data;
      console.log("res_role", res);
    },
    async getDept() {
      var res = await this.$API.system_dept.dept.list.get();
      this.depts = res.data;
      console.log("depts", this.depts)
    },
    handleChangeDept(val) {
      this.form.deptId = val[1];
    },
    handleChangeRole(val) {
      let roleIdArray = [];
      val.forEach(item => {
        roleIdArray.push(item[0]);
      });
      this.form.roleIds = roleIdArray;
    },
    //表单提交方法
    submit() {
      // 获取部门id和角色id;
      this.$refs.dialogForm.validate(async (valid) => {
        if (valid) {
          this.isSaveing = true;
          if (this.mode == 'add') {
            var res = await this.$API.system_user.user.add.post(this.form);
          } else if (this.mode == 'edit') {
            var res = await this.$API.system_user.user.edit.put(this.form, this.form.id);
          } else if (this.mode == 'resetPassword') {
            this.form.password = this.form.newPassword;
            console.log("form", this.form)
            var res = await this.$API.system_user.user.resetPassword.patch(this.form);
          }
          this.isSaveing = false;
          if (res.code === '00000') {
            this.$emit('success', this.form, this.mode)
            this.visible = false;
            ElMessage.success("操作成功");

          } else {
            ElMessageBox.alert(res.message, "提示", {type: 'error'})
          }
        } else {
          return false;
        }
      })
    },
    //表单注入数据
    setData(data) {
      this.form.id = data.id
      this.form.username = data.username
      this.form.avatar = data.avatar
      this.form.nickname = data.nickname
      this.form.roleNames = data.roleNames
      this.form.dept = data.deptName
      this.form.password = data.password
    }
  }
}
</script>

<style>
</style>
