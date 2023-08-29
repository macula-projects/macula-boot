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
  <div class="user-bar">
    <div class="screen panel-item hidden-sm-and-down" @click="screen">
      <el-icon>
        <el-icon-full-screen/>
      </el-icon>
    </div>
    <tenant></tenant>
    <el-dropdown class="user panel-item" trigger="click" @command="handleUser">
      <div class="user-avatar">
        <el-avatar :size="30">{{ userNameF }}</el-avatar>
        <label>{{ userName }}</label>
        <el-icon class="el-icon--right">
          <el-icon-arrow-down/>
        </el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="clearCache">清除缓存</el-dropdown-item>
          <el-dropdown-item command="outLogin" divided>退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>

</template>

<script>
import tenant from './tenant'
import {useTenantStore} from '@/stores/tenant';
import {mapActions} from 'pinia';

export default {
  components: {
    tenant
  },
  data() {
    return {
      userName: "",
      userNameF: ""
    }
  },
  created() {
    var userInfo = this.$TOOL.data.get("USER_INFO");
    this.userName = userInfo.userName;
    this.userNameF = this.userName.substring(0, 1);
  },
  methods: {
    ...mapActions(useTenantStore, ['clearTenantOptions']),
    //个人信息
    handleUser(command) {
      if (command == "clearCache") {
        ElMessageBox.confirm('清除缓存会将系统初始化，包括登录状态、主题、语言设置等，是否继续？', '提示', {
          type: 'info',
        }).then(() => {
          const loading = ElLoading.service({fullscreen: true})
          this.$TOOL.data.clear()
          this.clearTenantOptions()
          this.$router.replace({path: '/login'})
          setTimeout(() => {
            loading.close()
            location.reload()
          }, 1000)
        }).catch(() => {
          //取消
        })
      }
      if (command == "outLogin") {
        ElMessageBox.confirm('确认是否退出当前用户？', '提示', {
          type: 'warning',
          confirmButtonText: '退出',
          confirmButtonClass: 'el-button--danger'
        }).then(() => {
          this.$router.replace({path: '/login'});
        }).catch(() => {
          //取消退出
        })
      }
    },
    //全屏
    screen() {
      var element = document.documentElement;
      this.$TOOL.screen(element)
    }
  }
}
</script>

<style scoped>
.user-bar {
  display: flex;
  align-items: center;
  height: 100%;
}

.user-bar .panel-item {
  padding: 0 10px;
  cursor: pointer;
  height: 100%;
  display: flex;
  align-items: center;
}

.user-bar .panel-item i {
  font-size: 16px;
}

.user-bar .panel-item:hover {
  background: rgba(0, 0, 0, 0.1);
}

.user-bar .user-avatar {
  height: 49px;
  display: flex;
  align-items: center;
}

.user-bar .user-avatar label {
  display: inline-block;
  margin-left: 5px;
  font-size: 12px;
  cursor: pointer;
}

.msg-list li {
  border-top: 1px solid #eee;
}

.msg-list li a {
  display: flex;
  padding: 20px;
}

.msg-list li a:hover {
  background: #ecf5ff;
}

.msg-list__icon {
  width: 40px;
  margin-right: 15px;
}

.msg-list__main {
  flex: 1;
}

.msg-list__main h2 {
  font-size: 15px;
  font-weight: normal;
  color: #333;
}

.msg-list__main p {
  font-size: 12px;
  color: #999;
  line-height: 1.8;
  margin-top: 5px;
}

.msg-list__time {
  width: 100px;
  text-align: right;
  color: #999;
}

.dark .msg-list__main h2 {
  color: #d0d0d0;
}

.dark .msg-list li {
  border-top: 1px solid #363636;
}

.dark .msg-list li a:hover {
  background: #383838;
}
</style>
