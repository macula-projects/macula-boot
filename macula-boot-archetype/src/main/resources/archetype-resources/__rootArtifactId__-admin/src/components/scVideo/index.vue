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
 * @Descripttion: xgplayer二次封装
 * @version: 1.1
 * @Author: sakuya
 * @Date: 2021年11月29日12:10:06
 * @LastEditors: sakuya
 * @LastEditTime: 2022年5月30日21:02:50
-->

<template>
  <div ref="scVideo" class="sc-video"></div>
</template>

<script>
import Player from 'xgplayer'
import HlsPlayer from 'xgplayer-hls'

export default {
  props: {
    src: {type: String, required: true, default: ""},
    autoplay: {type: Boolean, default: false},
    controls: {type: Boolean, default: true},
    loop: {type: Boolean, default: false},
    isLive: {type: Boolean, default: false},
    options: {
      type: Object, default: () => {
      }
    }
  },
  data() {
    return {
      player: null
    }
  },
  watch: {
    src(val) {
      if (this.player.hasStart) {
        this.player.src = val
      } else {
        this.player.start(val)
      }
    }
  },
  mounted() {
    if (this.isLive) {
      this.initHls()
    } else {
      this.init()
    }
  },
  methods: {
    init() {
      this.player = new Player({
        el: this.$refs.scVideo,
        url: this.src,
        autoplay: this.autoplay,
        loop: this.loop,
        controls: this.controls,
        fluid: true,
        lang: 'zh-cn',
        ...this.options
      })
    },
    initHls() {
      this.player = new HlsPlayer({
        el: this.$refs.scVideo,
        url: this.src,
        autoplay: this.autoplay,
        loop: this.loop,
        controls: this.controls,
        fluid: true,
        isLive: true,
        ignores: ['time', 'progress'],
        lang: 'zh-cn',
        ...this.options
      })
    }
  }
}
</script>

<style scoped>
.sc-video:deep(.danmu) > * {
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  text-shadow: 1px 1px 0 #000, -1px -1px 0 #000, -1px 1px 0 #000, 1px -1px 0 #000;
}

.sc-video:deep(.xgplayer-controls) {
  background-image: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.3));
}

.sc-video:deep(.xgplayer-progress-tip) {
  border: 0;
  color: #fff;
  background: rgba(0, 0, 0, .5);
  line-height: 25px;
  padding: 0 10px;
  border-radius: 25px;
}

.sc-video:deep(.xgplayer-enter-spinner) {
  width: 50px;
  height: 50px;
}
</style>
