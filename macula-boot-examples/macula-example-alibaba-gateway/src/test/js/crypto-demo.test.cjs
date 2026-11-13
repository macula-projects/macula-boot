/* Copyright (c) 2026 Macula. Licensed under the Apache License, Version 2.0. */
const {test} = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('../../main/resources/static/crypto-demo/crypto-client.js');
const {createRequest} = require('../../main/resources/static/crypto-demo/request.js');
const axios = require('../../main/resources/static/crypto-demo/vendor/axios.min.js');
const sm2 = require('../../main/resources/static/crypto-demo/vendor/sm2.js');
const vector = require('../resources/crypto-demo/interop.json');
const pair = sm2.generateKeyPairHex();
const API = '/consumer/api/v1/consumer';
const paths = [API + '/echo/*', API + '/user'];
const success = data => JSON.stringify({success:true,code:'00000',data});

/** 使用真实 Axios 拦截器和序列化，仅将网络 adapter 替换为测试桩。 */
function harness({patterns=paths,respond,token='demo-token',onTrace}={}) {
    const wires=[], traces=[];
    let metadataCalls=0, tokenCalls=0;
    const adapter=async config=>{
        const url=new URL(axios.getUri(config),'http://localhost');
        let data,status=200;
        if(url.pathname==='/gateway/protect/key'){metadataCalls++;data=success(pair.publicKey);}
        else if(url.pathname==='/gateway/protect/urls'){metadataCalls++;data=success({crypto:patterns,sign:[]});}
        else{
            const wire={url,headers:config.headers,method:config.method.toUpperCase(),body:config.data,config};
            wires.push(wire);
            if(!config.headers.has('sm4-key'))data=success('unchanged');
            else{
                wire.key=sm2.doDecrypt(config.headers.get('sm4-key').slice(2),pair.privateKey,1);
                if(wire.method==='GET'||wire.method==='DELETE')wire.plain=url.search?crypto.decrypt(url.searchParams.get('data'),wire.key):null;
                else wire.plain=wire.body?crypto.decrypt(JSON.parse(wire.body).data,wire.key):null;
                if(respond)({data,status=200}=respond(wire));
                else{
                    const plain=wire.method==='GET'?'Hello '+wire.plain.slice(4)+','+decodeURIComponent(url.pathname.split('/').pop()):wire.plain;
                    data=success(crypto.encrypt(plain,wire.key));
                }
            }
        }
        const response={config,status,statusText:'test',headers:{'Content-Type':'application/json'},data};
        if(config.validateStatus&&!config.validateStatus(status))throw new axios.AxiosError('HTTP '+status,axios.AxiosError.ERR_BAD_REQUEST,config,null,response);
        return response;
    };
    const options={baseURL:'http://localhost',adapter,getToken:()=>{tokenCalls++;return token;},onTrace:event=>{traces.push(event);if(onTrace)onTrace(event);}};
    return {request:createRequest(options),adapter,wires,traces,get metadataCalls(){return metadataCalls;},get tokenCalls(){return tokenCalls;}};
}

test('实际密码库与安全随机源保持互通',()=>{
    const key=crypto.newKey();
    assert.match(key,/^[0-9a-f]{32}$/);assert.notEqual(key,crypto.newKey());
    assert.equal(crypto.decrypt(crypto.encrypt(vector.plain,key),key),vector.plain);
    assert.equal(sm2.doDecrypt(crypto.wrapKey(key,pair.publicKey).slice(2),pair.privateKey,1),key);
});

test('Axios request.get 使用 params，拦截器自动返回已解密 Result',async()=>{
    const h=harness();
    const result=await h.request.get(API+'/echo/demo',{params:{str:vector.get.str}});
    assert.equal(result.data,'Hello '+vector.get.str+',demo');
    assert.equal(h.wires[0].url.pathname,API+'/echo/demo');assert.equal(h.wires[0].plain,'str='+vector.get.str);
    assert.equal(h.wires[0].headers.get('sym-alg'),'SM4');assert.equal(h.wires[0].config.fetchOptions.redirect,'error');
    assert.doesNotMatch(JSON.stringify(h.traces),/demo-token/);assert.doesNotMatch(JSON.stringify(h.traces),new RegExp(h.wires[0].key));
    assert.equal(typeof h.request.interceptors.request.use,'function');
});

test('Axios request.post 直接传对象，由默认 transformRequest 序列化密文包装',async()=>{
    const h=harness(),payload={arbitrary:'A&B+"中文',items:[1,2],enabled:true};
    const result=await h.request.post(API+'/user',payload);
    assert.deepEqual(result.data,payload);assert.deepEqual(JSON.parse(h.wires[0].plain),payload);
    assert.equal(h.wires[0].headers.getContentType(),'application/json');
    assert.equal(h.wires[0].headers.get('authorization'),'Bearer demo-token');
    assert.equal(h.wires[0].body.includes('arbitrary'),false);
    assert.deepEqual(payload,{arbitrary:'A&B+"中文',items:[1,2],enabled:true});
});

test('其他业务路径也可复用，同一实例不认识示例接口和字段',async()=>{
    const h=harness({patterns:['/orders/**']}),payload={orderId:42,items:['A','B']};
    assert.deepEqual((await h.request.post('/orders/42',payload)).data,payload);
    assert.equal(h.wires[0].url.pathname,'/orders/42');
});

test('未保护、跨源和元数据请求保持普通 Axios 行为，不附加认证或密钥',async()=>{
    const h=harness();
    await h.request.get('/public',{params:{str:'hello'},headers:{'X-Test':'same'}});
    assert.equal(h.wires[0].url.searchParams.get('str'),'hello');assert.equal(h.wires[0].headers.get('X-Test'),'same');
    assert.equal(h.wires[0].headers.has('authorization'),false);assert.equal(h.tokenCalls,0);
    const before=h.metadataCalls;
    await h.request.get('https://example.test/api');assert.equal(h.metadataCalls,before);
    assert.equal(h.wires[1].headers.has('sm4-key'),false);assert.equal(h.traces.length,0);
    await h.request.get('/gateway/protect/key');
    await h.request.post('/public',{ordinary:true});
    assert.equal(h.wires[2].body,'{"ordinary":true}');assert.equal(h.tokenCalls,0);
});

test('清单支持 Ant 通配符与简单路径变量，保持路径边界',()=>{
    for(const [pattern,path] of [['/a/**','/a'],['/a/**','/a/b/c'],['/**/x','/x'],['/a/*','/a/123'],['/a/{id}','/a/123'],['/a/??','/a/12']])assert.equal(crypto.matches(pattern,path),true,pattern+' '+path);
    for(const [pattern,path] of [['/a/*','/a/b/c'],['/a/*','/a/'],['/a/*','/ab/1'],['/a/??','/a/123'],['/a/{id}','/a/123/'],['/a','/a/']])assert.equal(crypto.matches(pattern,path),false,pattern+' '+path);
});

test('并发元数据加载合并，各请求密钥独立且响应不串线',async()=>{
    const h=harness();
    assert.deepEqual((await Promise.all(['one','two'].map(str=>h.request.get(API+'/echo/demo',{params:{str}})))).map(result=>result.data),['Hello one,demo','Hello two,demo']);
    assert.equal(h.metadataCalls,2);assert.notEqual(h.wires[0].key,h.wires[1].key);
    await h.request.loadCryptoMetadata(true);assert.equal(h.metadataCalls,4);
    assert.equal(Object.keys(h.wires[0].config).some(key=>key.toLowerCase().includes('sm4key')),false);
});

test('显式 Authorization 优先，signal 取消不发业务请求，调试回调不干扰业务',async()=>{
    const h=harness({onTrace:()=>{throw new Error('diagnostic failure');}});
    assert.deepEqual((await h.request.post(API+'/user',{x:1},{headers:{authorization:'Bearer explicit'}})).data,{x:1});
    assert.equal(h.wires[0].headers.get('authorization'),'Bearer explicit');assert.equal(h.tokenCalls,0);
    assert.doesNotMatch(JSON.stringify(h.traces),/explicit/);
    const controller=new AbortController();controller.abort();
    await assert.rejects(h.request.get(API+'/echo/demo',{params:{str:'x'},signal:controller.signal}),error=>axios.isCancel(error));
    assert.equal(h.wires.length,1);
});

test('不支持的 Query、data 和响应类型在发送业务前拒绝',async()=>{
    const h=harness();
    for(const str of ['a&b','a+b','a"b','a\nb'])await assert.rejects(h.request.get(API+'/echo/demo',{params:{str}}),/Query 协议/);
    await assert.rejects(h.request.get(API+'/echo/demo',{params:{data:'already'}}),/预包装/);
    await assert.rejects(h.request.get(API+'/echo/demo',{params:{access_token:'x'}}),/Query 凭据/);
    await assert.rejects(h.request.post(API+'/user','text',{headers:{'Content-Type':'text/plain'}}),/application\/json/);
    await assert.rejects(h.request.post(API+'/user','bad',{headers:{'Content-Type':'application/json'}}),/有效 JSON/);
    await assert.rejects(h.request.post(API+'/user?x=1',{}),/不会解密/);
    await assert.rejects(h.request.head(API+'/echo/demo'),/不支持 HEAD/);
    await assert.rejects(h.request.get(API+'/echo/demo',{headers:{'sm4-key':'already'}}),/重复/);
    await assert.rejects(h.request.get(API+'/echo/demo',{responseType:'blob'}),/JSON 响应/);
    await assert.rejects(h.request.get(API+'/echo/demo',{data:'plain'}),/不支持 Body/);
    assert.equal(h.wires.length,0);
});

test('HTTP 与业务错误统一 reject，不对未加密错误解密或重试',async()=>{
    const failed=JSON.stringify({success:false,code:'A0301',msg:'未认证'});
    const h=harness({respond:()=>({data:failed,status:401})});
    await assert.rejects(h.request.post(API+'/user',{}),error=>error.isAxiosError&&error.response.status===401);
    assert.equal(h.wires.length,1);assert.equal(h.traces[1].raw,failed);
    const business=harness({respond:()=>({data:failed})});
    await assert.rejects(business.request.post(API+'/user',{}),/未认证/);
    const empty=harness({respond:()=>({data:success(null)})});assert.equal((await empty.request.post(API+'/user',{})).data,null);
    const plain=harness({respond:()=>({data:success('plaintext')})});await assert.rejects(plain.request.get(API+'/echo/demo',{params:{str:'x'}}),/Base64/);
});

test('元数据失败后可重新加载，不能降级明文；拒绝不支持的清单模式',async()=>{
    let broken=true,calls=0;const h=harness();
    const request=createRequest({adapter:config=>{calls++;return broken?Promise.resolve({config,status:200,headers:{},data:'bad'}):h.adapter(config);}});
    await assert.rejects(request.get(API+'/echo/demo',{params:{str:'x'}}),/成功的 Result/);assert.equal(h.wires.length,0);
    broken=false;assert.equal((await request.get(API+'/echo/demo',{params:{str:'x'}})).data,'Hello x,demo');assert.equal(calls,5);
    const bad=harness({patterns:['/a/{id:[0-9]+}']});await assert.rejects(bad.request.loadCryptoMetadata(),/保护清单/);
});

test('实际 demo.js 使用 request.get/post（DOM 桩）',async()=>{
    const fs=require('node:fs'),vm=require('node:vm'),elements=new Map();
    for(const id of ['reload','clear','connection','public-key','protect-urls','token','get-form','post-form','name','str','user-json','status','plain','request','response','decoded'])elements.set(id,{value:'',textContent:'',className:'',disabled:false,listeners:{},addEventListener(event,fn){this.listeners[event]=fn;}});
    elements.get('name').value=vector.get.name;elements.get('str').value=vector.get.str;elements.get('user-json').value=JSON.stringify(vector.user);
    const buttons=[{disabled:true},{disabled:true}],h=harness();
    const context={document:{getElementById:id=>elements.get(id),querySelectorAll:selector=>selector==='[data-send]'?buttons:[elements.get('name'),elements.get('str'),elements.get('user-json'),elements.get('token')]},
        window:{addEventListener(){}},MaculaCrypto:crypto,MaculaRequest:{createRequest:options=>createRequest({...options,adapter:h.adapter})},AbortController,setTimeout,clearTimeout};
    vm.runInNewContext(fs.readFileSync(require.resolve('../../main/resources/static/crypto-demo/demo.js'),'utf8'),context);
    const wait=async(condition)=>{for(let i=0;i<100;i++){if(condition())return;await new Promise(r=>setTimeout(r,5));}assert.fail('页面脚本未完成');};
    await wait(()=>elements.get('connection').className==='ok');assert.equal(buttons[0].disabled,false);
    elements.get('get-form').listeners.submit({preventDefault(){}});await wait(()=>elements.get('status').className==='ok');
    assert.equal(elements.get('decoded').textContent,'Hello '+vector.get.str+',demo');
    elements.get('post-form').listeners.submit({preventDefault(){}});assert.match(elements.get('status').textContent,/需要 OAuth Token/);assert.equal(h.wires.length,1);
    elements.get('token').value='demo-token';elements.get('post-form').listeners.submit({preventDefault(){}});await wait(()=>elements.get('status').className==='ok');
    assert.deepEqual(JSON.parse(elements.get('decoded').textContent),vector.user);assert.doesNotMatch(elements.get('request').textContent,/demo-token/);
});
