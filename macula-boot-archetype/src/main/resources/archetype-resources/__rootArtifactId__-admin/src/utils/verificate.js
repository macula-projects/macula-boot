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

//验证手机号
export function verifyPhone(rule, value, callback) {
    let reg = /^[1][3, 4, 5, 6, 7, 8, 9][0-9]{9}$/
    if (!reg.test(value)) {
        return callback(new Error('请输入正确的手机号码'))
    }
    callback()
}

//车牌号码
export function verifyCars(rule, value, callback) {
    let reg = /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$/
    if (!reg.test(value)) {
        return callback(new Error('请输入正确的车牌号码'))
    }
    callback()
}
