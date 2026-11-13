/* Copyright (c) 2026 Macula. Licensed under the Apache License, Version 2.0. */
(function () {
    'use strict';
    const client = globalThis.MaculaCrypto;
    const el = id => document.getElementById(id);
    const API = '/consumer/api/v1/consumer';
    const expectedPaths = [API + '/echo/*', API + '/user'];
    const outputIds = ['plain', 'request', 'response', 'decoded'];
    let ready = false;
    let busy = false;

    /** 统一使用纯文本呈现用户输入和远端内容。 */
    function show(id, value) {
        el(id).textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
    }

    function status(id, text, state = '') {
        show(id, text);
        el(id).className = state;
    }

    let request;

    /** 将请求库初始化放在可重试加载流程中，资源缺失时也能显示错误。 */
    function initRequest() {
        if (!client || !globalThis.MaculaRequest) throw new Error('请求库或加密资源加载失败，请检查同源脚本。');
        request = globalThis.MaculaRequest.createRequest({
            getToken: () => el('token').value,
            onTrace(event) {
                if (event.stage === 'request') { show('plain', event.plain); show('request', event.request); }
                if (event.stage === 'response') show('response', event.raw);
                if (event.stage === 'decoded') show('decoded', event.data);
            }
        });
    }

    function controls() {
        document.querySelectorAll('[data-send]').forEach(button => { button.disabled = busy || !ready; });
        document.querySelectorAll('input, textarea').forEach(input => { input.disabled = busy; });
        el('reload').disabled = busy;
        el('clear').disabled = busy;
    }

    async function load() {
        busy = true;
        ready = false;
        controls();
        status('connection', '正在获取内置公钥与保护清单…');
        show('public-key', '等待加载');
        show('protect-urls', '等待加载');
        try {
            if (!request) initRequest();
            const {publicKey, protectUrls} = await request.loadCryptoMetadata(true);
            show('public-key', publicKey);
            show('protect-urls', protectUrls);
            if (!expectedPaths.every(path => protectUrls.crypto.some(pattern => client.matches(pattern, path.replace('*', 'demo'))))) {
                throw new Error('保护清单未覆盖示例的两条 URL，请使用 README 中的 local 配置。');
            }
            ready = true;
            status('connection', '已就绪 · 公钥与两条保护 URL 已加载', 'ok');
        } catch (error) {
            status('connection', error.message || '加载失败，请检查网关与静态资源。', 'error');
        } finally { busy = false; controls(); }
    }

    /** 页面只做业务字段校验，拦截器不依赖 UserResult。 */
    function userInput() {
        const text = el('user-json').value;
        if (text.length > 16000) throw new Error('用户 JSON 长度不能超过 16000 个字符。');
        let user;
        try { user = JSON.parse(text); } catch (_) { throw new Error('用户 JSON 格式错误。'); }
        if (!user || Array.isArray(user) || typeof user !== 'object') throw new Error('用户 JSON 必须是对象。');
        if (typeof user.userName !== 'string' || !user.userName.trim()) throw new Error('请填写字符串 userName。');
        if (user.age != null && (!Number.isInteger(user.age) || user.age < 0)) throw new Error('age 必须是非负整数。');
        return user;
    }

    async function send(kind) {
        if (busy || !ready) return;
        outputIds.forEach(id => show(id, '—'));
        let timer;
        try {
            const input = kind === 'get' ? {name: el('name').value, str: el('str').value} : userInput();
            if (kind === 'get' && !input.str.length) throw new Error('str 不能为空。');
            const url = kind === 'get' ? API + '/echo/' + client.validateName(input.name) : API + '/user';
            busy = true;
            controls();
            status('status', '正在发送 ' + kind.toUpperCase() + '，等待网关与下游服务…');
            const controller = new AbortController();
            timer = setTimeout(() => controller.abort(), 20000);
            // 业务只使用 Axios params/data，序列化、加密和解密均由 request.js 处理。
            const result = kind === 'get'
                ? await request.get(url, {params: {str: input.str}, signal: controller.signal})
                : await request.post(url, input, {signal: controller.signal});
            const data = client.resultData(result);
            if (kind === 'get') {
                if (typeof data !== 'string' || !data.includes(input.str) || !data.endsWith(',' + input.name)) {
                    throw new Error('响应已解密，但 echo 结果与本次输入不一致，请检查下游或降级响应。');
                }
            } else if (!data || data.userName !== input.userName || (input.age != null && data.age !== input.age)) {
                throw new Error('响应已解密，但 userName/age 与本次输入不一致，请检查下游。');
            }
            show('decoded', data);
            status('status', '往返成功 · 拦截器已自动加解密，业务结果已核对', 'ok');
        } catch (error) {
            status('status', ['ERR_CANCELED', 'ECONNABORTED', 'ETIMEDOUT'].includes(error.code) ? '请求超过 20 秒或已取消，请检查服务状态；不会自动重试。' : (error.response && error.response.data && error.response.data.msg) || error.message || '请求失败。', 'error');
        } finally { clearTimeout(timer); busy = false; controls(); }
    }

    el('get-form').addEventListener('submit', event => { event.preventDefault(); send('get'); });
    el('post-form').addEventListener('submit', event => { event.preventDefault(); send('post'); });
    el('reload').addEventListener('click', load);
    el('clear').addEventListener('click', () => {
        outputIds.forEach(id => show(id, '—'));
        status('status', '结果已清空。');
    });
    window.addEventListener('pagehide', () => { el('token').value = ''; });
    load();
})();
