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
  <el-card class="item-background" header="时钟" shadow="hover">
    <div class="time">
      <h2>{{ time }}</h2>
      <p>{{ day }}</p>
    </div>
  </el-card>
</template>

<script>
export default {
  title: "时钟",
  icon: "el-icon-clock",
  description: "演示部件效果",
  data() {
    return {
      time: '',
      day: ''
    }
  },
  mounted() {
    this.showTime()
    setInterval(() => {
      this.showTime()
    }, 1000)
  },
  methods: {
    showTime() {
      this.time = this.$TOOL.dateFormat(new Date(), 'hh:mm:ss')
      this.day = this.$TOOL.dateFormat(new Date(), 'yyyy年MM月dd日')
    }
  }
}
</script>

<style scoped>
.item-background {
  background: linear-gradient(to right, #8E54E9, #4776E6);
  color: #fff;
}

.time h2 {
  font-size: 40px;
}

.time p {
  font-size: 14px;
  margin-top: 13px;
  opacity: 0.7;
}
</style>
