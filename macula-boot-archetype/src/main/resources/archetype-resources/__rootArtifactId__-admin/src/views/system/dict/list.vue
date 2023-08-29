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
  <el-dialog v-model="visible" :title="titleMap[mode]" :width="400" destroy-on-close @closed="$emit('closed')">
    <el-form ref="dialogForm" :model="form" :rules="rules" label-position="left" label-width="100px">
      <el-form-item label="所属字典" prop="dic">
        <el-cascader v-model="form.dic" :options="dic" :props="dicProps" :show-all-levels="false"
                     clearable></el-cascader>
      </el-form-item>
      <el-form-item label="项名称" prop="name">
        <el-input v-model="form.name" clearable></el-input>
      </el-form-item>
      <el-form-item label="键值" prop="value">
        <el-input v-model="form.value" clearable></el-input>
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
        add: '新增项',
        edit: '编辑项'
      },
      visible: false,
      isSaveing: false,
      form: {
        id: "",
        dic: "",
        name: "",
        value: 0,
        typeCode: null,
        oldTypeCode: null,
        status: 1
      },
      rules: {
        dic: [
          {required: true, message: '请选择所属字典'}
        ],
        name: [
          {required: true, message: '请输入项名称'}
        ],
        value: [
          {required: true, message: '请输入键值'}
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
    if (this.params) {
      this.form.dic = this.params.code
    }
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
          var dt = this.dic.find(d => d.id == this.form.dic)
          this.form.typeCode = dt.code
          if (this.mode === 'add') {
            res = await this.$API.system_dict.dict.addItem.post(this.form);
          } else {
            res = await this.$API.system_dict.dict.editItem.put(this.form.id, this.form);
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
      this.form.value = data.value
      if (this.mode === 'edit') {
        this.form.status = data.status
      }
      this.form.typeCode = data.code
      this.form.oldTypeCode = data.code
      this.form.dic = data.dic
    }
  }
}
</script>

<style>
</style>
