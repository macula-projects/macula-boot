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
  <el-card v-loading="loading" header="实时收入" shadow="hover">
    <scEcharts ref="c1" :option="option" height="300px"></scEcharts>
  </el-card>
</template>

<script>
export default {
  title: "实时收入",
  icon: "el-icon-data-line",
  description: "Echarts组件演示",
  data() {
    return {
      loading: true,
      option: {}
    }
  },
  created() {
    var _this = this;
    setTimeout(function () {
      _this.loading = false
    }, 500);

    var option = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        boundaryGap: false,
        type: 'category',
        data: (function () {
          var now = new Date();
          var res = [];
          var len = 30;
          while (len--) {
            res.unshift(now.toLocaleTimeString().replace(/^\D*/, ''));
            now = new Date(now - 2000);
          }
          return res;
        })()
      },
      yAxis: [{
        type: 'value',
        name: '价格',
        "splitLine": {
          "show": false
        }
      }],
      series: [
        {
          name: '收入',
          type: 'line',
          symbol: 'none',
          lineStyle: {
            width: 1,
            color: '#409EFF'
          },
          areaStyle: {
            opacity: 0.1,
            color: '#79bbff'
          },
          data: (function () {
            var res = [];
            var len = 30;
            while (len--) {
              res.push(Math.round(Math.random() * 0));
            }
            return res;
          })()
        },
      ],
    };
    this.option = option;

  },
  mounted() {
    var _this = this;
    setInterval(function () {
      var o = _this.option;

      o.series[0].data.shift()
      o.series[0].data.push(Math.round(Math.random() * 100));

      o.xAxis.data.shift();
      o.xAxis.data.push((new Date()).toLocaleTimeString().replace(/^\D*/, ''));


      //_this.$refs.c1.myChart.setOption(o)
    }, 2100)

  },
}
</script>
