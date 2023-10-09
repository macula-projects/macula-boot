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
  <!-- 通栏布局 -->
  <template v-if="layout=='header'">
    <header class="adminui-header">
      <div class="adminui-header-left">
        <div class="logo-bar">
          <img class="logo" src="/img/logo.png">
          <span>{{ $CONFIG.APP_NAME }}</span>
        </div>
        <ul v-if="!ismobile" class="nav">
          <li v-for="item in menu" :key="item" :class="pmenu.path==item.path?'active':''" @click="showMenu(item)">
            <el-icon>
              <component :is="item.meta.icon || 'el-icon-menu'"/>
            </el-icon>
            <span>{{ item.meta.title }}</span>
          </li>
        </ul>
      </div>
      <div class="adminui-header-right">
        <userbar></userbar>
      </div>
    </header>
    <section class="aminui-wrapper">
      <div v-if="!ismobile && nextMenu.length>0 || !pmenu.component"
           :class="menuIsCollapse?'aminui-side isCollapse':'aminui-side'">
        <div v-if="!menuIsCollapse" class="adminui-side-top">
          <h2>{{ pmenu.meta.title }}</h2>
        </div>
        <div class="adminui-side-scroll">
          <el-scrollbar>
            <el-menu :collapse="menuIsCollapse" :default-active="active" :unique-opened="$CONFIG.MENU_UNIQUE_OPENED"
                     router>
              <NavMenu :navMenus="nextMenu"></NavMenu>
            </el-menu>
          </el-scrollbar>
        </div>
        <div class="adminui-side-bottom" @click="toggleMenuIsCollapse()">
          <el-icon>
            <el-icon-expand v-if="menuIsCollapse"/>
            <el-icon-fold v-else/>
          </el-icon>
        </div>
      </div>
      <Side-m v-if="ismobile"></Side-m>
      <div class="aminui-body el-container">
        <Topbar v-if="!ismobile"></Topbar>
        <Tags v-if="!ismobile && layoutTags"></Tags>
        <div id="adminui-main" class="adminui-main">
          <router-view v-slot="{ Component }">
            <keep-alive :include="keepLiveRoute">
              <component :is="Component" v-if="routeShow" :key="$route.fullPath"/>
            </keep-alive>
          </router-view>
          <iframe-view></iframe-view>
        </div>
      </div>
    </section>
  </template>

  <!-- 经典布局 -->
  <template v-else-if="layout=='menu'">
    <header class="adminui-header">
      <div class="adminui-header-left">
        <div class="logo-bar">
          <img class="logo" src="/img/logo.png">
          <span>{{ $CONFIG.APP_NAME }}</span>
        </div>
      </div>
      <div class="adminui-header-right">
        <userbar></userbar>
      </div>
    </header>
    <section class="aminui-wrapper">
      <div v-if="!ismobile" :class="menuIsCollapse?'aminui-side isCollapse':'aminui-side'">
        <div class="adminui-side-scroll">
          <el-scrollbar>
            <el-menu :collapse="menuIsCollapse" :default-active="active" :unique-opened="$CONFIG.MENU_UNIQUE_OPENED"
                     router>
              <NavMenu :navMenus="menu"></NavMenu>
            </el-menu>
          </el-scrollbar>
        </div>
        <div class="adminui-side-bottom" @click="toggleMenuIsCollapse()">
          <el-icon>
            <el-icon-expand v-if="menuIsCollapse"/>
            <el-icon-fold v-else/>
          </el-icon>
        </div>
      </div>
      <Side-m v-if="ismobile"></Side-m>
      <div class="aminui-body el-container">
        <Topbar v-if="!ismobile"></Topbar>
        <Tags v-if="!ismobile && layoutTags"></Tags>
        <div id="adminui-main" class="adminui-main">
          <router-view v-slot="{ Component }">
            <keep-alive :include="keepLiveRoute">
              <component :is="Component" v-if="routeShow" :key="$route.fullPath"/>
            </keep-alive>
          </router-view>
          <iframe-view></iframe-view>
        </div>
      </div>
    </section>
  </template>

  <!-- 功能坞布局 -->
  <template v-else-if="layout=='dock'">
    <header class="adminui-header">
      <div class="adminui-header-left">
        <div class="logo-bar">
          <img class="logo" src="/img/logo.png">
          <span>{{ $CONFIG.APP_NAME }}</span>
        </div>
      </div>
      <div class="adminui-header-right">
        <div v-if="!ismobile" class="adminui-header-menu">
          <el-menu :default-active="active" active-text-color="var(--el-color-primary)" background-color="#222b45" mode="horizontal" router
                   text-color="#fff">
            <NavMenu :navMenus="menu"></NavMenu>
          </el-menu>
        </div>
        <Side-m v-if="ismobile"></Side-m>
        <userbar></userbar>
      </div>
    </header>
    <section class="aminui-wrapper">
      <div class="aminui-body el-container">
        <Tags v-if="!ismobile && layoutTags"></Tags>
        <div id="adminui-main" class="adminui-main">
          <router-view v-slot="{ Component }">
            <keep-alive :include="keepLiveRoute">
              <component :is="Component" v-if="routeShow" :key="$route.fullPath"/>
            </keep-alive>
          </router-view>
          <iframe-view></iframe-view>
        </div>
      </div>
    </section>
  </template>

  <!-- 默认布局 -->
  <template v-else>
    <section class="aminui-wrapper">
      <div v-if="!ismobile" class="aminui-side-split">
        <div class="aminui-side-split-top">
          <router-link :to="$CONFIG.DASHBOARD_URL">
            <img :title="$CONFIG.APP_NAME" class="logo" src="/img/logo-r.png">
          </router-link>
        </div>
        <div class="adminui-side-split-scroll">
          <el-scrollbar>
            <ul>
              <li v-for="item in menu" :key="item" :class="pmenu.path==item.path?'active':''"
                  @click="showMenu(item)">
                <el-icon>
                  <component :is="item.meta.icon || el-icon-menu"/>
                </el-icon>
                <p>{{ item.meta.title }}</p>
              </li>
            </ul>
          </el-scrollbar>
        </div>
      </div>
      <div v-if="(pmenu.meta) && (!ismobile && nextMenu.length>0 || !pmenu.component)"
           :class="menuIsCollapse?'aminui-side isCollapse':'aminui-side'">
        <div v-if="!menuIsCollapse" class="adminui-side-top">
          <h2>{{ pmenu.meta.title }}</h2>
        </div>
        <div class="adminui-side-scroll">
          <el-scrollbar>
            <el-menu :collapse="menuIsCollapse" :default-active="active" :unique-opened="$CONFIG.MENU_UNIQUE_OPENED"
                     router>
              <NavMenu :navMenus="nextMenu"></NavMenu>
            </el-menu>
          </el-scrollbar>
        </div>
        <div class="adminui-side-bottom" @click="toggleMenuIsCollapse()">
          <el-icon>
            <el-icon-expand v-if="menuIsCollapse"/>
            <el-icon-fold v-else/>
          </el-icon>
        </div>
      </div>
      <Side-m v-if="ismobile"></Side-m>
      <div class="aminui-body el-container">
        <Topbar>
          <userbar></userbar>
        </Topbar>
        <Tags v-if="!ismobile && layoutTags"></Tags>
        <div id="adminui-main" class="adminui-main">
          <router-view v-slot="{ Component }">
            <keep-alive :include="keepLiveRoute">
              <component :is="Component" v-if="routeShow" :key="$route.fullPath"/>
            </keep-alive>
          </router-view>
          <iframe-view></iframe-view>
        </div>
      </div>
    </section>
  </template>

  <div class="main-maximize-exit" @click="exitMaximize">
    <el-icon>
      <el-icon-close/>
    </el-icon>
  </div>

  <div class="layout-setting" @click="openSetting">
    <el-icon>
      <el-icon-brush-filled/>
    </el-icon>
  </div>

  <el-drawer v-model="settingDialog" :size="400" append-to-body destroy-on-close title="布局实时演示">
    <setting></setting>
  </el-drawer>
</template>

<script>
import SideM from './components/sideM.vue';
import Topbar from './components/topbar.vue';
import Tags from './components/tags.vue';
import NavMenu from './components/navMenu.vue';
import userbar from './components/userbar.vue';
import setting from './components/setting.vue';
import iframeView from './components/iframeView.vue';
import {mapState, mapActions} from 'pinia';
import {useGlobalStore} from '../stores/global';
import {useKeepAliveStore} from '../stores/keepAlive';

export default {
  name: 'index',
  components: {
    SideM,
    Topbar,
    Tags,
    NavMenu,
    userbar,
    setting,
    iframeView
  },
  data() {
    return {
      settingDialog: false,
      menu: [],
      nextMenu: [],
      pmenu: {},
      active: ''
    }
  },
  computed: {
    ...mapState(useGlobalStore, ['ismobile', 'layout', 'menuIsCollapse', 'layoutTags', 'theme']),
    ...mapState(useKeepAliveStore, ['keepLiveRoute', 'routeKey', 'routeShow']),
  },
  created() {
    this.onLayoutResize();
    window.addEventListener('resize', this.onLayoutResize);
    var menu = this.$router.sc_getMenu();
    this.menu = this.filterUrl(menu);
    this.showThis()
  },
  watch: {
    $route() {
      this.showThis()
    },
    layout: {
      handler(val) {
        document.body.setAttribute('data-layout', val)
      },
      immediate: true,
    }
  },
  methods: {
    ...mapActions(useGlobalStore, ['setIsMobile', 'toggleMenuIsCollapse']),
    openSetting() {
      this.settingDialog = true;
    },
    onLayoutResize() {
      this.setIsMobile(document.body.clientWidth < 992)
    },
    //路由监听高亮
    showThis() {
      this.pmenu = this.$route.meta.breadcrumb ? this.$route.meta.breadcrumb[0] : {}
      this.nextMenu = this.filterUrl(this.pmenu.children);
      this.$nextTick(() => {
        this.active = this.$route.meta.active || this.$route.fullPath;
      })
    },
    //点击显示
    showMenu(route) {
      this.pmenu = route;
      this.nextMenu = this.filterUrl(route.children);
      if ((!route.children || route.children.length == 0) && route.component) {
        this.$router.push({path: route.path})
      }
    },
    //转换外部链接的路由
    filterUrl(map) {
      var newMap = []
      map && map.forEach(item => {
        item.meta = item.meta ? item.meta : {};
        //处理隐藏
        if (!item.meta.visible || item.meta.type == "BUTTON") {
          return false
        }
        //处理http
        if (item.meta.type == 'IFRAME') {
          item.path = `/i/${item.meta.title}`;
        }
        //递归循环
        if (item.children && item.children.length > 0) {
          item.children = this.filterUrl(item.children)
        }


        newMap.push(item)
      })
      return newMap;
    },
    //退出最大化
    exitMaximize() {
      document.getElementById('app').classList.remove('main-maximize')
    }
  }
}
</script>
