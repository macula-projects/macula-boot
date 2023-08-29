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
  <el-dialog v-model="visible" :title="titleMap[mode]" :width="330" destroy-on-close @closed="$emit('closed')">
    <el-form ref="dialogForm" :model="form" :rules="rules" label-position="left" label-width="80px">
      <el-form-item label="编码" prop="code">
        <el-input v-model="form.code" clearable placeholder="字典编码"></el-input>
      </el-form-item>
      <el-form-item label="字典名称" prop="name">
        <el-input v-model="form.name" clearable placeholder="字典显示名称"></el-input>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">正常</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
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
      mode: "add",
      titleMap: {
        add: '新增字典',
        edit: '编辑字典'
      },
      visible: false,
      isSaveing: false,
      form: {
        id: "",
        name: "",
        code: "",
        status: 1
      },
      rules: {
        code: [
          {required: true, message: '请输入编码'}
        ],
        name: [
          {required: true, message: '请输入字典名称'}
        ]
      },
      dic: [],
      dicProps: {
        value: "id",
        label: "name",
        emitPath: false,
        checkStrictly: true
      }
    }
  },
  mounted() {
    this.getDic()
  },
  methods: {
    //显示
    open(mode = 'add') {
      this.mode = mode;
      this.visible = true;
      return this;
    },
    //获取字典列表
    async getDic() {
      var res = await this.$API.system_dict.dict.typeListPages.get();
      this.dic = res.data.records;
    },
    //表单提交方法
    submit() {
      this.$refs.dialogForm.validate(async (valid) => {
        if (valid) {
          this.isSaveing = true;
          var res = null
          if (this.mode === 'add') {
            res = await this.$API.system_dict.dict.addType.post(this.form);
          } else {
            res = await this.$API.system_dict.dict.editType.put(this.form.id, this.form);
          }
          this.isSaveing = false;
          if (res.code === '00000') {
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
      this.form.status = data.status

      //可以和上面一样单个注入，也可以像下面一样直接合并进去
      //Object.assign(this.form, data)
    }
  }
}
</script>

<style>
</style>
