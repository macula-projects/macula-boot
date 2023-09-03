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
 * @Descripttion: 处理iframe持久化，涉及store(VUEX)
 * @version: 1.0
 * @Author: sakuya
 * @Date: 2021年6月30日13:20:41
 * @LastEditors:
 * @LastEditTime:
-->

<template>
  <div v-show="$route.meta.type=='IFRAME'" class="iframe-pages">
    <iframe v-for="item in this.iframeStore.iframeList" v-show="$route.meta.url==item.meta.url" :key="item.meta.url"
            :src="item.meta.url"
            frameborder='0'></iframe>
  </div>
</template>

<script>
import {mapActions, mapStores} from 'pinia';
import {useGlobalStore} from '../../stores/global';
import {useIframeStore} from '../../stores/iframe';

export default {
  data() {
    return {}
  },
  watch: {
    $route(e) {
      this.push(e)
    },
  },
  created() {
    this.push(this.$route);
  },
  computed: {
    ...mapStores(useIframeStore, useGlobalStore),
  },
  mounted() {

  },
  methods: {
    ...mapActions(useIframeStore, ['setIframeList', 'pushIframeList', 'clearIframeList']),
    push(route) {
      if (route.meta.type == 'IFRAME') {
        if (this.ismobile || !this.layoutTags) {
          this.$store.commit("setIframeList", route)
        } else {
          this.pushIframeList(route)
        }
      } else {
        if (this.globalStore.ismobile || !this.globalStore.layoutTags) {
          this.clearIframeList()
        }
      }
    }
  }
}
</script>

<style scoped>
.iframe-pages {
  width: 100%;
  height: 100%;
  background: #fff;
}

iframe {
  border: 0;
  width: 100%;
  height: 100%;
  display: block;
}
</style>
