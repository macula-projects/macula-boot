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

/**
 * @description 自动import导入所有 api 模块
 */

const files = import.meta.globEager('./model/*/*.js')
const modules = {}
Object.keys(files).forEach(key => {
    modules[key.replace(/^\.\/model\/(.*)\/(.*)\.js$/g, '$1_$2')] = files[key].default
})
export default modules
