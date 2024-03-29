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
  <div class="node-wrap">
    <div class="node-wrap-box" @click="show">
      <div class="title" style="background: #3296fa;">
        <el-icon class="icon">
          <el-icon-promotion/>
        </el-icon>
        <span>{{ nodeConfig.nodeName }}</span>
        <el-icon class="close" @click.stop="delNode()">
          <el-icon-close/>
        </el-icon>
      </div>
      <div class="content">
        <span v-if="toText(nodeConfig)">{{ toText(nodeConfig) }}</span>
        <span v-else class="placeholder">请选择人员</span>
      </div>
    </div>
    <add-node v-model="nodeConfig.childNode"></add-node>
    <el-drawer v-model="drawer" :size="500" append-to-body destroy-on-close title="抄送人设置">
      <template #header>
        <div class="node-wrap-drawer__title">
          <label v-if="!isEditTitle" @click="editTitle">{{ form.nodeName }}
            <el-icon class="node-wrap-drawer__title-edit">
              <el-icon-edit/>
            </el-icon>
          </label>
          <el-input v-if="isEditTitle" ref="nodeTitle" v-model="form.nodeName" clearable @blur="saveTitle"
                    @keyup.enter="saveTitle"></el-input>
        </div>
      </template>
      <el-container>
        <el-main style="padding:0 20px 20px 20px">
          <el-form label-position="top">
            <el-form-item label="选择要抄送的人员">
              <el-button icon="el-icon-plus" round type="primary" @click="selectHandle(1, form.nodeUserList)">选择人员
              </el-button>
              <div class="tags-list">
                <el-tag v-for="(user, index) in form.nodeUserList" :key="user.id" closable @close="delUser(index)">
                  {{ user.name }}
                </el-tag>
              </div>
            </el-form-item>
            <el-form-item label="">
              <el-checkbox v-model="form.userSelectFlag" label="允许发起人自选抄送人"></el-checkbox>
            </el-form-item>
          </el-form>
        </el-main>
        <el-footer>
          <el-button type="primary" @click="save">保存</el-button>
          <el-button @click="drawer=false">取消</el-button>
        </el-footer>
      </el-container>
    </el-drawer>
  </div>
</template>

<script>
import addNode from './addNode'

export default {
  inject: ['select'],
  props: {
    modelValue: {
      type: Object, default: () => {
      }
    }
  },
  components: {
    addNode
  },
  data() {
    return {
      nodeConfig: {},
      drawer: false,
      isEditTitle: false,
      form: {}
    }
  },
  watch: {
    modelValue() {
      this.nodeConfig = this.modelValue
    }
  },
  mounted() {
    this.nodeConfig = this.modelValue
  },
  methods: {
    show() {
      this.form = {}
      this.form = JSON.parse(JSON.stringify(this.nodeConfig))
      this.drawer = true
    },
    editTitle() {
      this.isEditTitle = true
      this.$nextTick(() => {
        this.$refs.nodeTitle.focus()
      })
    },
    saveTitle() {
      this.isEditTitle = false
    },
    save() {
      this.$emit("update:modelValue", this.form)
      this.drawer = false
    },
    delNode() {
      this.$emit("update:modelValue", this.nodeConfig.childNode)
    },
    delUser(index) {
      this.form.nodeUserList.splice(index, 1)
    },
    selectHandle(type, data) {
      this.select(type, data)
    },
    toText(nodeConfig) {
      if (nodeConfig.nodeUserList && nodeConfig.nodeUserList.length > 0) {
        const users = nodeConfig.nodeUserList.map(item => item.name).join("、")
        return users
      } else {
        if (nodeConfig.userSelectFlag) {
          return "发起人自选"
        } else {
          return false
        }

      }
    }
  }
}
</script>

<style>
</style>
