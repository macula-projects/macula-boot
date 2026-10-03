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
  <el-form ref="loginForm" :model="form" :rules="rules" label-width="0" size="large">
    <el-form-item prop="user">
      <el-input v-model="form.user" :placeholder="$t('login.userPlaceholder')" clearable prefix-icon="el-icon-user">
        <template #append>
          <el-select v-model="userType" style="width: 130px;">
            <el-option :label="$t('login.admin')" value="admin"></el-option>
            <el-option :label="$t('login.user')" value="user"></el-option>
          </el-select>
        </template>
      </el-input>
    </el-form-item>
    <el-form-item prop="password">
      <el-input v-model="form.password" :placeholder="$t('login.PWPlaceholder')" clearable prefix-icon="el-icon-lock"
                show-password></el-input>
    </el-form-item>
    <el-form-item style="margin-bottom: 10px;">
      <el-col :span="12">
        <el-checkbox v-model="form.autologin" :label="$t('login.rememberMe')"></el-checkbox>
      </el-col>
      <el-col :span="12" class="login-forgot">
        <router-link to="/reset_password">{{ $t('login.forgetPassword') }}？</router-link>
      </el-col>
    </el-form-item>
    <el-form-item>
      <el-button :loading="islogin" round style="width: 100%;" type="primary" @click="login">{{
          $t('login.signIn')
        }}
      </el-button>
    </el-form-item>
    <div class="login-reg">
      {{ $t('login.noAccount') }}
      <router-link to="/user_register">{{ $t('login.createAccount') }}</router-link>
    </div>
  </el-form>
</template>

<script>

import config from "@/config"

export default {
  data() {
    return {
      userType: 'admin',
      form: {
        user: config.DEMO_USERNAME,
        password: config.DEMO_PASSWORD,
        autologin: false
      },
      rules: {
        user: [
          {required: true, message: this.$t('login.userError'), trigger: 'blur'}
        ],
        password: [
          {required: true, message: this.$t('login.PWError'), trigger: 'blur'}
        ]
      },
      islogin: false,
    }
  },
  watch: {
    userType(val) {
      if (val == 'admin') {
        this.form.user = config.DEMO_USERNAME
        this.form.password = config.DEMO_PASSWORD
      } else if (val == 'user') {
        this.form.user = 'user'
        this.form.password = 'user'
      }
    }
  },
  mounted() {

  },
  methods: {
    async login() {

      var validate = await this.$refs.loginForm.validate().catch(() => {
      })
      if (!validate) {
        return false
      }

      this.islogin = true
      var data = {
        username: this.form.user,
        password: this.form.password,
        grant_type: 'password',
        client_id: config.OAUTH_CLIENT_ID,
        client_secret: config.OAUTH_CLIENT_SECRET,
        scope: config.OAUTH_SCOPE
      }
      try {
        // 获取 token
        var user = await this.$API.common_auth.systemToken.post(data)
        if (!user?.access_token) {
          var tokenError = new Error('登录失败，请检查用户名和密码')
          tokenError.data = user || {}
          throw tokenError
        }
        this.$TOOL.cookie.set("TOKEN", user.access_token, {
          expires: 24 * 60 * 60
        })

        var userInfo = await this.$API.common_auth.getUserInfo.get()
        if (!userInfo?.success) {
          var userInfoError = new Error('用户信息加载失败')
          userInfoError.data = userInfo || {}
          throw userInfoError
        }
        this.$TOOL.data.set("USER_INFO", userInfo.data)

        // 处理菜单
        // 用户的角色是否包含路由返回菜单对应的角色
        var res = await this.$API.common_auth.getRoutes.get()
        if (!res?.success) {
          var routesError = new Error('菜单加载失败')
          routesError.data = res || {}
          throw routesError
        }
        var routes = res.data
        var roles = userInfo.data.roles
        var perms = userInfo.data.perms
        var menu = this.$TOOL.treeFilter(routes, node => {
          return !node.meta.roles || node.meta.roles.length === 0 || node.meta.roles.some(item => roles.includes(item))
          })

        this.$TOOL.data.set("MENU", menu)
        this.$TOOL.data.set("PERMISSIONS", perms)

        this.$router.replace({
          path: '/'
        })
        ElMessage.success("Login Success 登录成功")
      } catch (error) {
        this.$TOOL.cookie.remove("TOKEN")
        this.$TOOL.data.remove("USER_INFO")
        this.$TOOL.data.remove("MENU")
        this.$TOOL.data.remove("PERMISSIONS")
        var errorData = error?.data || {}
        ElMessage.warning(errorData.msg || errorData.message || errorData.error_description ||
            errorData.error || error?.message || '登录失败，请检查用户名和密码')
        return false
      } finally {
        this.islogin = false
      }
    }
  }
}

</script>

<style></style>
