/* Copyright (c) 2026 Macula. Licensed under the Apache License, Version 2.0. */
(function (root) {
    'use strict';
    const axios = typeof module === 'object' ? require('./vendor/axios.min.js') : root.axios;
    const crypto = typeof module === 'object' ? require('./crypto-client.js') : root.MaculaCrypto;
    const KEY_PATH = '/gateway/protect/key';
    const URLS_PATH = '/gateway/protect/urls';

    /**
     * 创建统一 Axios 请求实例：业务使用 get/post，拦截器完成参数加密和 data 解密。
     * 密钥使用 WeakMap 按 Axios config 隔离，不挂在可序列化的 config 或诊断信息中。
     */
    function createRequest({baseURL = root.location ? root.location.origin : 'http://localhost', getToken = () => '',
        onTrace, adapter = 'fetch', timeout = 20000} = {}) {
        if (!axios || !crypto) throw new Error('Axios 或加密资源加载失败，请检查同源脚本。');
        const base = new URL(baseURL, root.location ? root.location.href : 'http://localhost');
        const defaults = {baseURL: base.href, adapter, timeout};
        const request = axios.create(defaults);
        // 元数据使用独立实例，避免公钥和保护清单请求再次触发业务拦截器。
        const metadataRequest = axios.create({...defaults, fetchOptions: {redirect: 'error', cache: 'no-store'}});
        const contexts = new WeakMap();
        let metadata;
        let pending;

        function trace(event) {
            // 诊断面板是可选订阅者，订阅失败不能改变业务请求的发送或返回结果。
            if (onTrace) { try { onTrace(event); } catch (_) { /* 不记录可能包含明文或 Token 的异常。 */ } }
        }

        async function loadCryptoMetadata(force = false) {
            if (!force && metadata) return metadata;
            if (pending) return pending;
            if (force) metadata = null;
            pending = (async () => {
                const [key, urls] = await Promise.all([
                    metadataRequest.get(new URL(KEY_PATH, base).href),
                    metadataRequest.get(new URL(URLS_PATH, base).href)
                ]);
                const publicKey = crypto.resultData(key.data);
                const protectUrls = crypto.resultData(urls.data);
                // 校验密钥可用性，不保存生成的测试密钥。
                crypto.wrapKey(crypto.newKey(), publicKey);
                if (!protectUrls || !Array.isArray(protectUrls.crypto) || protectUrls.crypto.some(pattern =>
                    typeof pattern !== 'string' || !pattern.startsWith('/') || /[\r\n]/.test(pattern) ||
                    /\{[^}]*:/.test(pattern) || pattern.replace(/\{[^{}]+\}/g, '').match(/[{}]/))) {
                    throw new Error('保护清单格式无效，或包含前端尚不支持的正则路径变量。');
                }
                metadata = {publicKey, protectUrls};
                return metadata;
            })();
            try { return await pending; } finally { pending = null; }
        }

        /** 请求拦截器运行在 Axios 默认 JSON 序列化之前，直接接收普通 params/data。 */
        request.interceptors.request.use(async config => {
            const url = new URL(request.getUri(config), base);
            // 跨源和元数据调用不附加 Token 或加密材料；未保护路径也保持普通 Axios 行为。
            if (url.origin !== base.origin || url.pathname === KEY_PATH || url.pathname === URLS_PATH) return config;
            if (config.signal && config.signal.aborted) throw new axios.CanceledError('请求已取消');
            const settings = await loadCryptoMetadata();
            if (config.signal && config.signal.aborted) throw new axios.CanceledError('请求已取消');
            if (!settings.protectUrls.crypto.some(pattern => crypto.matches(pattern, url.pathname))) return config;
            const headers = axios.AxiosHeaders.from(config.headers);
            if (!headers.has('Authorization') && !config.auth) {
                const token = (await getToken() || '').trim().replace(/^Bearer\s+/i, '');
                if (/[\r\n]/.test(token)) throw new Error('Token 不能含换行。');
                if (token) headers.set('Authorization', 'Bearer ' + token);
            }
            if (headers.has('sm4-key') || headers.has('sym-alg')) throw new Error('请传普通业务请求，避免重复添加加密头。');
            if (config.responseType && !['json', 'text'].includes(config.responseType)) throw new Error('受保护接口只支持 JSON 响应，不支持流或文件下载。');
            const method = config.method.toUpperCase();
            let plain = null;
            if (method === 'GET' || method === 'DELETE') {
                if (config.data != null) throw new Error('受保护 GET/DELETE 不支持 Body，请使用 params。');
                if (url.search) plain = crypto.queryString(url.searchParams);
            } else if (['POST', 'PUT', 'PATCH'].includes(method)) {
                if (url.search) throw new Error('当前网关不会解密 POST/PUT/PATCH 的 Query，请使用 JSON data。');
                if (config.data != null) {
                    const contentType = headers.getContentType();
                    if (contentType && String(contentType).split(';')[0].trim().toLowerCase() !== 'application/json') {
                        throw new Error('受保护请求只支持 application/json，不支持 FormData 或文件上传。');
                    }
                    const data = config.data;
                    if (typeof data === 'object' && !Array.isArray(data) && Object.prototype.toString.call(data) !== '[object Object]') {
                        throw new Error('受保护请求只支持普通 JSON data。');
                    }
                    plain = typeof data === 'string' ? data : JSON.stringify(data);
                    try { JSON.parse(plain); } catch (_) { throw new Error('请求 data 不是有效 JSON。'); }
                    headers.setContentType('application/json');
                }
            } else throw new Error('当前加密协议不支持 ' + method + ' 方法。');
            const context = {key: crypto.newKey(), raw: null};
            headers.delete('Content-Length');
            headers.set('sm4-key', crypto.wrapKey(context.key, settings.publicKey));
            headers.set('sym-alg', 'SM4');
            if (plain !== null) {
                const cipher = crypto.encrypt(plain, context.key);
                if (method === 'GET' || method === 'DELETE') url.search = new URLSearchParams({data: cipher}).toString();
                else config.data = {data: cipher};
            }
            config.url = url.href;
            config.baseURL = undefined;
            config.params = undefined;
            config.paramsSerializer = undefined;
            config.headers = headers;
            // 固定使用创建实例时选定的适配器；官方 fetch adapter 可明确禁止加密请求重定向。
            config.adapter = adapter;
            config.fetchOptions = {...config.fetchOptions, redirect: 'error', cache: 'no-store'};
            const transforms = config.transformResponse || [];
            config.transformResponse = [raw => { context.raw = raw; return raw; }, ...[].concat(transforms)];
            contexts.set(config, context);
            const visibleHeaders = headers.toJSON();
            // AxiosHeaders 保留调用方大小写，逐项脱敏避免小写 authorization 绕过展示规则。
            for (const name of Object.keys(visibleHeaders)) if (name.toLowerCase() === 'authorization') visibleHeaders[name] = 'Bearer [已隐藏]';
            trace({stage: 'request', plain, request: {method, url: url.pathname + url.search,
                headers: visibleHeaders, body: config.data == null ? null : JSON.stringify(config.data)}});
            return config;
        });

        /** 响应拦截器在默认 JSON 解析后还原 data，并返回统一 Result 给业务。 */
        request.interceptors.response.use(response => {
            const context = contexts.get(response.config);
            try {
                let result = response.data;
                if (context) {
                    trace({stage: 'response', raw: context.raw});
                    if (response.status !== 204 && response.status !== 205) {
                        if (typeof result === 'string') {
                            try { result = JSON.parse(result); } catch (_) { throw new Error('受保护接口响应不是 JSON。'); }
                        }
                        if (result && result.success === true && result.code === crypto.SUCCESS_CODE && result.data != null) {
                            result.data = crypto.decodeData(crypto.decrypt(result.data, context.key));
                            trace({stage: 'decoded', data: result.data});
                        }
                    }
                }
                if (result && typeof result === 'object' && 'success' in result) crypto.resultData(result);
                return result;
            } finally {
                if (context) context.key = null;
                contexts.delete(response.config);
            }
        }, error => {
            const config = error.config || (error.response && error.response.config);
            const context = config && contexts.get(config);
            if (context) {
                trace({stage: 'response', raw: context.raw});
                context.key = null;
                contexts.delete(config);
            }
            // HTTP、超时、取消保留 AxiosError，不自动重试，也不把失败伪装成空数据。
            return Promise.reject(error);
        });
        request.loadCryptoMetadata = loadCryptoMetadata;
        return request;
    }

    const api = {createRequest};
    if (typeof module === 'object') module.exports = api;
    else root.MaculaRequest = api;
})(globalThis);
