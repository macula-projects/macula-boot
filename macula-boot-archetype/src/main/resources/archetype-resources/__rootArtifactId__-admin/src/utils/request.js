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

import axios from 'axios';
import {ElMessageBox, ElNotification} from 'element-plus';
import sysConfig from "@/config";
import tool from '@/utils/tool';
import router from '@/router';

axios.defaults.baseURL = ''

axios.defaults.timeout = sysConfig.TIMEOUT
let loadQuitMsgBox = false
let menuRouteSuffixReg = /\/menus\/(my|routes)$/

// HTTP request 拦截器
axios.interceptors.request.use(
    (config) => {
        let token = tool.cookie.get("TOKEN");
        if (token) {
            config.headers[sysConfig.TOKEN_NAME] = sysConfig.TOKEN_PREFIX + token
        }
        if (!sysConfig.REQUEST_CACHE && config.method == 'get') {
            config.params = config.params || {};
            config.params['_'] = new Date().getTime();
        }
        if (config.url && (!config.url.endsWith(import.meta.env.VITE_APP_MENU_PATH)) && tool.data.get(sysConfig.TENANT_ID)) {
            // tool.cookie含有tenantId, 则遍历config的参数，如果包含tenantId则不添加，不包含则从tool.cookie中获取并添加到header属下
            config.headers[sysConfig.TENANT_ID] = tool.data.get(sysConfig.TENANT_ID)
        }

        Object.assign(config.headers, sysConfig.HEADERS)
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// HTTP response 拦截器
axios.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response) {
            if (error.response.status == 404) {
                ElNotification.error({
                    title: '请求错误',
                    message: "Status:404，正在请求不存在的服务器记录！"
                });
            } else if (error.response.status == 500) {
                ElNotification.error({
                    title: '请求错误',
                    message: error.response.data.message || "Status:500，服务器发生错误！"
                });
            } else if (error.response.status == 401) {
                if (!loadQuitMsgBox) {
                    loadQuitMsgBox = true
                    ElMessageBox.confirm('当前用户已被登出或无权限访问当前资源，请尝试重新登录后再操作。', '无权限访问', {
                        type: 'error',
                        closeOnClickModal: false,
                        center: true,
                        confirmButtonText: '重新登录'
                    }).then(() => {
                        loadQuitMsgBox = false
                        router.replace({path: '/login'});
                    }).catch(() => {
                        loadQuitMsgBox = false
                    })
                }
            } else {
                ElNotification.error({
                    title: '请求错误',
                    message: error.message || `Status:${error.response.status}，未知错误！`
                });
            }
        } else {
            ElNotification.error({
                title: '请求错误',
                message: "请求服务器无响应！"
            });
        }

        return Promise.reject(error.response);
    }
);

var http = {

    /** get 请求
     * @param  {接口地址} url
     * @param  {请求参数} params
     * @param  {参数} config
     */
    get: function (url, params = {}, config = {}) {
        return new Promise((resolve, reject) => {
            axios({
                method: 'get',
                url: url,
                params: params,
                ...config
            }).then((response) => {
                resolve(response.data);
            }).catch((error) => {
                reject(error);
            })
        })
    },

    /** post 请求
     * @param  {接口地址} url
     * @param  {请求参数} data
     * @param  {参数} config
     */
    post: function (url, data = {}, config = {}) {
        return new Promise((resolve, reject) => {
            axios({
                method: 'post',
                url: url,
                data: data,
                ...config
            }).then((response) => {
                resolve(response.data);
            }).catch((error) => {
                reject(error);
            })
        })
    },

    /** put 请求
     * @param  {接口地址} url
     * @param  {请求参数} data
     * @param  {参数} config
     */
    put: function (url, data = {}, config = {}) {
        return new Promise((resolve, reject) => {
            axios({
                method: 'put',
                url: url,
                data: data,
                ...config
            }).then((response) => {
                resolve(response.data);
            }).catch((error) => {
                reject(error);
            })
        })
    },

    /** patch 请求
     * @param  {接口地址} url
     * @param  {请求参数} data
     * @param  {参数} config
     */
    patch: function (url, data = {}, config = {}) {
        return new Promise((resolve, reject) => {
            axios({
                method: 'patch',
                url: url,
                data: data,
                ...config
            }).then((response) => {
                resolve(response.data);
            }).catch((error) => {
                reject(error);
            })
        })
    },

    /** delete 请求
     * @param  {接口地址} url
     * @param  {请求参数} data
     * @param  {参数} config
     */
    delete: function (url, data = {}, config = {}) {
        return new Promise((resolve, reject) => {
            axios({
                method: 'delete',
                url: url,
                data: data,
                ...config
            }).then((response) => {
                resolve(response.data);
            }).catch((error) => {
                reject(error);
            })
        })
    },

    /** jsonp 请求
     * @param  {接口地址} url
     * @param  {JSONP回调函数名称} name
     */
    jsonp: function (url, name = 'jsonp') {
        return new Promise((resolve) => {
            var script = document.createElement('script')
            var _id = `jsonp${Math.ceil(Math.random() * 1000000)}`
            script.id = _id
            script.type = 'text/javascript'
            script.src = url
            window[name] = (response) => {
                resolve(response)
                document.getElementsByTagName('head')[0].removeChild(script)
                try {
                    delete window[name];
                } catch (e) {
                    window[name] = undefined;
                }
            }
            document.getElementsByTagName('head')[0].appendChild(script)
        })
    }
}

export default http;
