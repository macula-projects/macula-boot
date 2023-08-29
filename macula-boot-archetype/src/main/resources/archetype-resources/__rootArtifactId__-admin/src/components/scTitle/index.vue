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
  <div class="sc-title">
    {{ title }}
  </div>
</template>

<script>
export default {
  props: {
    title: {type: String, required: true, default: ""},
  },
  data() {
    return {}
  },
  computed: {}
}
</script>

<style scoped>
.sc-title {
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
  font-size: 17px;
  padding-bottom: 15px;
  color: #3c4a54;
  font-weight: bold;
}
</style>
