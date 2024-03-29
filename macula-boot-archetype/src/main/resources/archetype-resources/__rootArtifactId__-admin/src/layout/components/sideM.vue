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
  <div ref="" v-drag class="mobile-nav-button" draggable="false" @click="showMobileNav($event)">
    <el-icon>
      <el-icon-menu/>
    </el-icon>
  </div>

  <el-drawer ref="mobileNavBox" v-model="nav" :size="240" :with-header="false" destroy-on-close direction="ltr"
             title="移动端菜单">
    <el-container class="mobile-nav">
      <el-header>
        <div class="logo-bar"><img class="logo" src="/img/logo.png"><span>{{ $CONFIG.APP_NAME }}</span></div>
      </el-header>
      <el-main>
        <el-scrollbar>
          <el-menu :default-active="$route.meta.active || $route.fullPath" active-text-color="#409EFF" background-color="#212d3d"
                   router text-color="#fff" @select="select">
            <NavMenu :navMenus="menu"></NavMenu>
          </el-menu>
        </el-scrollbar>
      </el-main>
    </el-container>
  </el-drawer>

</template>

<script>
import NavMenu from './NavMenu.vue';

export default {
  components: {
    NavMenu
  },
  data() {
    return {
      nav: false,
      menu: []
    }
  },
  computed: {},
  created() {
    var menu = this.$router.sc_getMenu()
    this.menu = this.filterUrl(menu)
  },

  watch: {},
  methods: {
    showMobileNav(e) {
      var isdrag = e.currentTarget.getAttribute('drag-flag')
      if (isdrag == 'true') {
        return false;
      } else {
        this.nav = true;
      }

    },
    select() {
      this.$refs.mobileNavBox.handleClose()
    },
    //转换外部链接的路由
    filterUrl(map) {
      var newMap = []
      map && map.forEach(item => {
        item.meta = item.meta ? item.meta : {};
        //处理隐藏
        if (item.meta.hidden || item.meta.type == "BUTTON") {
          return false
        }
        //处理http
        if (item.meta.type == 'IFRAME') {
          item.path = `/i/${item.name}`;
        }
        //递归循环
        if (item.children && item.children.length > 0) {
          item.children = this.filterUrl(item.children);
        }
        newMap.push(item)
      })
      return newMap;
    }
  },
  directives: {
    drag(el) {
      let oDiv = el; //当前元素
      let firstTime = '', lastTime = '';
      //禁止选择网页上的文字
      // document.onselectstart = function() {
      // 	return false;
      // };
      oDiv.onmousedown = function (e) {
        //鼠标按下，计算当前元素距离可视区的距离
        let disX = e.clientX - oDiv.offsetLeft;
        let disY = e.clientY - oDiv.offsetTop;
        document.onmousemove = function (e) {
          oDiv.setAttribute('drag-flag', true);
          firstTime = new Date().getTime();
          //通过事件委托，计算移动的距离
          let l = e.clientX - disX;
          let t = e.clientY - disY;

          //移动当前元素

          if (t > 0 && t < document.body.clientHeight - 50) {
            oDiv.style.top = t + "px";
          }
          if (l > 0 && l < document.body.clientWidth - 50) {
            oDiv.style.left = l + "px";
          }


        }
        document.onmouseup = function () {
          lastTime = new Date().getTime();
          if ((lastTime - firstTime) > 200) {
            oDiv.setAttribute('drag-flag', false);
          }
          document.onmousemove = null;
          document.onmouseup = null;
        };
        //return false不加的话可能导致黏连，就是拖到一个地方时div粘在鼠标上不下来，相当于onmouseup失效
        return false;
      };
    }
  }
}
</script>

<style scoped>
.mobile-nav-button {
  position: fixed;
  bottom: 10px;
  left: 10px;
  z-index: 10;
  width: 50px;
  height: 50px;
  background: #409EFF;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mobile-nav-button i {
  color: #fff;
  font-size: 20px;
}

.mobile-nav {
  background: #212d3d;
}

.mobile-nav .el-header {
  background: transparent;
  border: 0;
}

.mobile-nav .el-main {
  padding: 0;
}

.mobile-nav .logo-bar {
  display: flex;
  align-items: center;
  font-weight: bold;
  font-size: 20px;
  color: #fff;
}

.mobile-nav .logo-bar img {
  width: 30px;
  margin-right: 10px;
}

.mobile-nav .el-submenu__title:hover {
  background: #fff !important;
}
</style>
