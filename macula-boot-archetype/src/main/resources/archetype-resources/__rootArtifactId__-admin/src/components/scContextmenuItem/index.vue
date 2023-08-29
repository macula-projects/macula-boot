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

<!--
 * @Descripttion: scContextmenuItem组件
 * @version: 1.2
 * @Author: sakuya
 * @Date: 2021年7月23日16:29:36
 * @LastEditors: sakuya
 * @LastEditTime: 2022年2月8日15:51:07
-->

<template>
  <hr v-if="divided">
  <li :class="disabled?'disabled':''" @mouseenter="openSubmenu($event)" @mouseleave="closeSubmenu($event)"
      @click.stop="liClick">
		<span class="title">
			<el-icon class="sc-contextmenu__icon"><component :is="icon" v-if="icon"/></el-icon>
			{{ title }}
		</span>
    <span class="sc-contextmenu__suffix">
			<el-icon v-if="$slots.default"><el-icon-arrow-right/></el-icon>
			<template v-else>{{ suffix }}</template>
		</span>
    <ul v-if="$slots.default" class="sc-contextmenu__menu">
      <slot></slot>
    </ul>
  </li>
</template>

<script>
export default {
  props: {
    command: {type: String, default: ""},
    title: {type: String, default: ""},
    suffix: {type: String, default: ""},
    icon: {type: String, default: ""},
    divided: {type: Boolean, default: false},
    disabled: {type: Boolean, default: false},
  },
  inject: ['menuClick'],
  methods: {
    liClick() {
      if (this.$slots.default) {
        return false
      }
      if (this.disabled) {
        return false
      }
      this.menuClick(this.command)
    },
    openSubmenu(e) {
      var menu = e.target.querySelector('ul')
      if (!menu) {
        return false
      }
      menu.style.display = 'inline-block'
      var rect = menu.getBoundingClientRect()
      var menuX = rect.left
      var menuY = rect.top
      var innerWidth = window.innerWidth
      var innerHeight = window.innerHeight
      var menuHeight = menu.offsetHeight
      var menuWidth = menu.offsetWidth
      if (menuX + menuWidth > innerWidth) {
        menu.style.left = 'auto'
        menu.style.right = '100%'
      }
      if (menuY + menuHeight > innerHeight) {
        menu.style.top = 'auto'
        menu.style.bottom = '0'
      }
    },
    closeSubmenu(e) {
      var menu = e.target.querySelector('ul')
      if (!menu) {
        return false
      }
      menu.removeAttribute("style")
      menu.style.display = 'none'
    }
  }
}
</script>

<style>

</style>
