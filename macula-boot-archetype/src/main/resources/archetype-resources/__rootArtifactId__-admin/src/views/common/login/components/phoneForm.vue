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
    <el-form-item prop="phone">
      <el-input v-model="form.phone" :placeholder="$t('login.mobilePlaceholder')" clearable
                prefix-icon="el-icon-iphone">
        <template #prepend>+86</template>
      </el-input>
    </el-form-item>
    <el-form-item prop="yzm" style="margin-bottom: 35px;">
      <div class="login-msg-yzm">
        <el-input v-model="form.yzm" :placeholder="$t('login.smsPlaceholder')" clearable
                  prefix-icon="el-icon-unlock"></el-input>
        <el-button :disabled="disabled" @click="getYzm">{{ this.$t('login.smsGet') }}<span
            v-if="disabled"> ({{ time }})</span></el-button>
      </div>
    </el-form-item>
    <el-form-item>
      <el-button :loading="islogin" round style="width: 100%;" type="primary" @click="login">{{ $t('login.signIn') }}
      </el-button>
    </el-form-item>
    <div class="login-reg">
      {{ $t('login.noAccount') }}
      <router-link to="/user_register">{{ $t('login.createAccount') }}</router-link>
    </div>
  </el-form>
</template>

<script>
export default {
  data() {
    return {
      form: {
        phone: "",
        yzm: "",
      },
      rules: {
        phone: [
          {required: true, message: this.$t('login.mobileError')}
        ],
        yzm: [
          {required: true, message: this.$t('login.smsError')}
        ]
      },
      disabled: false,
      time: 0,
      islogin: false,
    }
  },
  mounted() {

  },
  methods: {
    async getYzm() {
      var validate = await this.$refs.loginForm.validateField("phone").catch(() => {
      })
      if (!validate) {
        return false
      }

      ElMessage.success(this.$t('login.smsSent'))
      this.disabled = true
      this.time = 60
      var t = setInterval(() => {
        this.time -= 1
        if (this.time < 1) {
          clearInterval(t)
          this.disabled = false
          this.time = 0
        }
      }, 1000)
    },
    async login() {
      var validate = await this.$refs.loginForm.validate().catch(() => {
      })
      if (!validate) {
        return false
      }
    }
  }
}
</script>

<style>

</style>
