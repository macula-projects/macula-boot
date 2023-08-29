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
 * loadJS 异步加载远程JS
 * @constructor
 * @param {string} src - 必填，需要加载的URL路径
 * @param {string} keyName - 必填，唯一key和JS返回的全局的对象名
 * @param {string} callbackName - 非必填，如果远程JS有callback，则可更有效的判断是否完成加载
 */
export function loadJS(src, keyName, callbackName) {
    return new Promise((resolve, reject) => {
        let has = document.head.querySelector("script[loadKey=" + keyName + "]")
        if (has) {
            return resolve(window[keyName])
        }
        let script = document.createElement("script")
        script.type = "text/javascript"
        script.src = src
        script.setAttribute("loadKey", keyName)
        document.head.appendChild(script)
        script.onload = () => {
            if (callbackName) {
                window[callbackName] = () => {
                    return resolve(window[keyName])
                }
            } else {
                setTimeout(() => {
                    return resolve(window[keyName])
                }, 50)
            }
        }
        script.onerror = (err) => {
            return reject(err)
        }
    })
}

/**
 * loadCSS 异步加载远程css
 * @constructor
 * @param {string} src - 必填，需要加载的URL路径
 * @param {string} keyName - 必填，唯一key
 */
export function loadCSS(src, keyName) {
    return new Promise((resolve, reject) => {
        let has = document.head.querySelector("link[loadKey=" + keyName + "]")
        if (has) {
            return resolve()
        }
        let link = document.createElement('link')
        link.rel = "stylesheet"
        link.href = src
        link.setAttribute("loadKey", keyName)
        document.head.appendChild(link)
        link.onload = () => {
            return resolve()
        }
        link.onerror = (err) => {
            return reject(err)
        }
    })
}
