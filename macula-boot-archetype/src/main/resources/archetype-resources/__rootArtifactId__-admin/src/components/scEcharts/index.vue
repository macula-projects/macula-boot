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
  <div ref="scEcharts" :style="{height:height, width:width}"></div>
</template>

<script>
import * as echarts from 'echarts';
import T from './echarts-theme-T.js';

echarts.registerTheme('T', T);
const unwarp = (obj) => obj && (obj.__v_raw || obj.valueOf() || obj);

export default {
  ...echarts,
  name: "scEcharts",
  props: {
    height: {type: String, default: "100%"},
    width: {type: String, default: "100%"},
    nodata: {type: Boolean, default: false},
    option: {
      type: Object, default: () => {
      }
    }
  },
  data() {
    return {
      isActivat: false,
      myChart: null
    }
  },
  watch: {
    option: {
      deep: true,
      handler(v) {
        unwarp(this.myChart).setOption(v);
      }
    }
  },
  computed: {
    myOptions: function () {
      return this.option || {};
    }
  },
  activated() {
    if (!this.isActivat) {
      this.$nextTick(() => {
        this.myChart.resize()
      })
    }
  },
  deactivated() {
    this.isActivat = false;
  },
  mounted() {
    this.isActivat = true;
    this.$nextTick(() => {
      this.draw();
    })
  },
  methods: {
    draw() {
      var myChart = echarts.init(this.$refs.scEcharts, 'T');
      myChart.setOption(this.myOptions);
      this.myChart = myChart;
      window.addEventListener('resize', () => myChart.resize());
    }
  }
}
</script>
