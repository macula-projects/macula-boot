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
  <div class="adminui-topbar">
    <div class="left-panel">
      <el-breadcrumb class="hidden-sm-and-down" separator-icon="el-icon-arrow-right">
        <transition-group mode="out-in" name="breadcrumb">
          <template v-for="item in breadList" :key="item.title">
            <el-breadcrumb-item v-if="item.path!='/' &&  !item.meta.hiddenBreadcrumb" :to="toPath(item)">
              <el-icon v-if="item.meta.icon" class="icon">
                <component :is="item.meta.icon"/>
              </el-icon>
              {{ item.meta.title }}
            </el-breadcrumb-item>
          </template>
        </transition-group>
      </el-breadcrumb>
    </div>
    <div class="center-panel">

    </div>
    <div class="right-panel">
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      breadList: []
    }
  },
  created() {
    this.getBreadcrumb();
  },
  watch: {
    $route() {
      this.getBreadcrumb();
    }
  },
  methods: {
    getBreadcrumb() {
      let matched = this.$route.meta.breadcrumb;
      this.breadList = matched;
    },
    toPath(item) {
      return item.component && item.component.name != 'empty' ? (item.fullPath ? item.fullPath : item.path) : null
    }
  }
}
</script>

<style scoped>
.el-breadcrumb {
  margin-left: 15px;
}

.el-breadcrumb .el-breadcrumb__inner .icon {
  font-size: 14px;
  margin-right: 5px;
  float: left;
}

.breadcrumb-enter-active, .breadcrumb-leave-active {
  transition: all 0.3s;
}

.breadcrumb-enter-from, .breadcrumb-leave-active {
  opacity: 0;
  transform: translateX(20px);
}

.breadcrumb-leave-active {
  position: absolute;
}
</style>
