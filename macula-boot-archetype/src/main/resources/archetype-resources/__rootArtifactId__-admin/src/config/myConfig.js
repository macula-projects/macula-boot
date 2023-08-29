/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//业务配置
//会合并至this.$CONFIG
//生产模式 public/config.js 同名key会覆盖这里的配置从而实现打包后的热更新
//为避免和SCUI框架配置混淆建议添加前缀 MY_
//全局可使用 this.$CONFIG.MY_KEY 访问

export default {
    //是否显示第三方授权登录
    MY_SHOW_LOGIN_OAUTH: true
}
