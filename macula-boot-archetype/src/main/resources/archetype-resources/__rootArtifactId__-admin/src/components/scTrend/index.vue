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
 * @Descripttion: 趋势标记
 * @version: 1.0
 * @Author: sakuya
 * @Date: 2021年11月11日11:07:10
 * @LastEditors:
 * @LastEditTime:
-->

<template>
	<span :class="'sc-trend--'+type" class="sc-trend">
		<el-icon v-if="iconType=='P'" class="sc-trend-icon"><el-icon-top/></el-icon>
		<el-icon v-if="iconType=='N'" class="sc-trend-icon"><el-icon-bottom/></el-icon>
		<el-icon v-if="iconType=='Z'" class="sc-trend-icon"><el-icon-right/></el-icon>
		<em class="sc-trend-prefix">{{ prefix }}</em>
		<em class="sc-trend-value">{{ modelValue }}</em>
		<em class="sc-trend-suffix">{{ suffix }}</em>
	</span>
</template>

<script>
export default {
  props: {
    modelValue: {type: Number, default: 0},
    prefix: {type: String, default: ""},
    suffix: {type: String, default: ""},
    reverse: {type: Boolean, default: false}
  },
  computed: {
    absValue() {
      return Math.abs(this.modelValue);
    },
    iconType(v) {
      if (this.modelValue == 0) {
        v = 'Z'
      } else if (this.modelValue < 0) {
        v = 'N'
      } else if (this.modelValue > 0) {
        v = 'P'
      }
      return v
    },
    type(v) {
      if (this.modelValue == 0) {
        v = 'Z'
      } else if (this.modelValue < 0) {
        v = this.reverse ? 'P' : 'N'
      } else if (this.modelValue > 0) {
        v = this.reverse ? 'N' : 'P'
      }
      return v
    }
  }
}
</script>

<style scoped>
.sc-trend {
  display: flex;
  align-items: center;
}

.sc-trend-icon {
  margin-right: 2px;
}

.sc-trend em {
  font-style: normal;
}

.sc-trend-prefix {
  margin-right: 2px;
}

.sc-trend-suffix {
  margin-left: 2px;
}

.sc-trend--P {
  color: #f56c6c;
}

.sc-trend--N {
  color: #67c23a;
}

.sc-trend--Z {
  color: #555;
}
</style>
