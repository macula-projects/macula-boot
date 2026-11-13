/* Copyright (c) 2026 Macula. Licensed under the Apache License, Version 2.0. */
(function (root) {
    'use strict';
    const sm2 = typeof module === 'object' ? require('./vendor/sm2.js') : root.sm2;
    const sm4 = typeof module === 'object' ? require('./vendor/sm4.js') : root.sm4;
    const SUCCESS_CODE = '00000';

    /** 以浏览器安全随机源生成单次请求密钥，不持久化。 */
    function newKey() {
        if (!root.crypto || !root.crypto.getRandomValues) throw new Error('浏览器不支持安全随机数，请使用 localhost 或 HTTPS。');
        return Array.from(root.crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, '0')).join('');
    }

    /** SM4/ECB/PKCS7 与 Hutool 的 UTF-8/Base64 传输格式互通。 */
    function encrypt(plain, key) {
        const bytes = sm4.encrypt(plain, key, {padding: 'pkcs#7', output: 'array'});
        return btoa(bytes.map(byte => String.fromCharCode(byte)).join(''));
    }

    /** 响应只还原 data；echo 是原始字符串，user 是 JSON 字符串。 */
    function decrypt(cipher, key) {
        if (typeof cipher !== 'string' || !cipher || cipher.length % 4 !== 0 || !/^[A-Za-z0-9+/]*={0,2}$/.test(cipher)) {
            throw new Error('响应 data 不是有效的 Base64 密文，请检查保护配置和服务响应。');
        }
        const bytes = Array.from(atob(cipher), c => c.charCodeAt(0));
        if (!bytes.length || bytes.length % 16 !== 0) throw new Error('响应密文长度不符合 SM4 要求。');
        return sm4.decrypt(bytes, key, {padding: 'pkcs#7'});
    }

    /** sm-crypto 省略的 04 点前缀须补回，Hutool 默认使用 C1C3C2。 */
    function wrapKey(key, publicKey) {
        if (!/^04[0-9a-f]{128}$/i.test(publicKey) || !sm2.verifyPublicKey(publicKey)) throw new Error('网关公钥格式无效，请重新加载。');
        return '04' + sm2.doEncrypt(key, publicKey, 1);
    }

    /** 防止编码后的路径段改变路由结构。 */
    function validateName(name) {
        if (!name || name.trim() !== name || /[\\/;?#%\u0000-\u001f]/.test(name) || name === '.' || name === '..') {
            throw new Error('name 必须是非空的单个路径段，不能含斜杠、百分号、分号或 URL 分隔符。');
        }
        return encodeURIComponent(name);
    }

    /** HTTP 200 仍可能是业务失败，必须校验统一 Result。 */
    function resultData(result) {
        if (!result || result.success !== true || result.code !== SUCCESS_CODE) {
            throw new Error(result && (result.msg || result.cause) || '服务未返回成功的 Result。');
        }
        return result.data;
    }

    /** 按 Ant 路径段匹配服务端清单，支持 *、**、? 和简单 {变量}。 */
    function matches(pattern, path) {
        const parts = pattern.split('/').filter(Boolean);
        const segments = path.split('/').filter(Boolean);
        const memo = new Map();
        function visit(i, j) {
            const key = i + ':' + j;
            if (memo.has(key)) return memo.get(key);
            let result;
            if (i === parts.length) result = j === segments.length;
            else if (parts[i] === '**') result = visit(i + 1, j) || (j < segments.length && visit(i, j + 1));
            else {
                const tokens = parts[i].match(/\{[^{}]+\}|[^{}]/g) || [];
                const source = tokens.map(token => {
                    if (token.startsWith('{')) return '[^/]+';
                    if (token === '*') return '[^/]*';
                    if (token === '?') return '[^/]';
                    return token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                }).join('');
                result = j < segments.length && new RegExp('^' + source + '$').test(segments[j]) && visit(i + 1, j + 1);
            }
            memo.set(key, result);
            return result;
        }
        return visit(0, 0) && (pattern.endsWith('/**') || pattern.endsWith('/') === path.endsWith('/'));
    }

    /** 整体 Query 编码是现有协议的限制；不把业务字段名写入拦截器。 */
    function queryString(params) {
        return Array.from(params, ([name, value]) => {
            if (!name || /[=&+"\u0000-\u001f]/.test(name) || /[&+"\u0000-\u001f]/.test(value)) {
                throw new Error('当前网关 Query 协议不支持参数中的 &、+、双引号、控制字符或非法参数名。');
            }
            if (name === 'data' || name === 'access_token') throw new Error('加密请求不支持预包装 data 或 Query 凭据，请通过 Authorization 认证。');
            return name + '=' + value;
        }).join('&');
    }

    /** 通用响应解码：对象/数组还原为 JSON，其余保留字符串，避免将数字字符串改成数字。 */
    function decodeData(plain) {
        if (/^\s*[\[{]/.test(plain)) {
            try { return JSON.parse(plain); } catch (_) { /* 普通业务文本可能以括号开头。 */ }
        }
        return plain;
    }

    const client = {SUCCESS_CODE, newKey, encrypt, decrypt, wrapKey, validateName, resultData, matches, queryString, decodeData};
    if (typeof module === 'object') module.exports = client;
    else root.MaculaCrypto = client;
})(globalThis);
