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
  <el-main>
    <el-alert style="margin-bottom:20px;"
              title="根据角色配置,可让不同角色访问不同的控制台视图,参数值在登录成功后返回 dashboard:{type}" type="success"></el-alert>
    <el-row :gutter="15">
      <el-col :lg="24">
        <el-card header="我的常用" shadow="never">
          <myapp></myapp>
        </el-card>
      </el-col>
    </el-row>
  </el-main>
</template>

<script>
import myapp from './components/myapp';

export default {
  components: {
    myapp
  },
  data() {
    return {}
  },
  mounted() {
    this.$emit('on-mounted')
  },
  methods: {}
}
</script>

<style scoped>

</style>
