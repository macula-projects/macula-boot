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
 * 全局代码错误捕捉
 * 比如 null.length 就会被捕捉到
 */

export default (error, vm) => {
    console.log('error', error)
    //过滤HTTP请求错误
    if (!error || error.status || error.status == 0) {
        return false
    }

    var errorMap = {
        InternalError: "Javascript引擎内部错误",
        ReferenceError: "未找到对象",
        TypeError: "使用了错误的类型或对象",
        RangeError: "使用内置对象时，参数超范围",
        SyntaxError: "语法错误",
        EvalError: "错误的使用了Eval",
        URIError: "URI错误"
    }
    var errorName = errorMap[error.name] || "未知错误"

    console.warn(`[SCUI error]: ${error}`);
    console.error(error);
    //throw error;

    vm.$nextTick(() => {
        vm.$notify.error({
            title: errorName,
            message: error
        });
    })
}
