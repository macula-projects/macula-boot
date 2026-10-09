## 概述

该模块基于[Spring Boot Websocket](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.websockets)搭建，支持基于[STOMP协议](https://docs.spring.io/spring-framework/reference/web/websocket.html)的Websocket通讯，通过Redis Pub/Sub实现Websocket跨实例通讯。支持微服务调用统一登录上下文获取。



## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-websocket</artifactId>
    <version>${macula.version}</version>
</dependency>
```



## 使用配置

### websocket服务模块配置

配置前缀为 `macula.websocket`。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 启用 WebSocket 自动配置 |
| `permit-test` | `true` | 放行内置测试消息路径；生产环境应设为 `false` |
| `endpoint` | `[/websocket]` | STOMP 握手端点列表 |
| `broker-destination-prefixes` | `[/topic, /queue]` | 消息代理处理的目标前缀 |
| `app-destination-prefixes` | `[/app]` | 应用消息处理前缀 |
| `user-destination-prefix` | `/user` | 用户定向消息前缀 |
| `heartbeat` | `[10000, 10000]` | 服务端发送 / 期望客户端发送的心跳间隔，单位毫秒 |

```yaml
macula:
  websocket:
    enabled: true
    permit-test: false
    endpoint: [/websocket]
    broker-destination-prefixes: [/topic, /queue]
    app-destination-prefixes: [/app]
    user-destination-prefix: /user
    heartbeat: [10000, 10000]
```

### 网关需要配置路由

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: macula-example-consumer-ws
              uri: lb:ws://macula-example-alibaba-consumer
              predicates:
                - Path=/websocket/**
              filters:
                - StripPrefix=1
macula:
  gateway:
    security:
      only-auth-urls: [/websocket/**]
```



## Spring Websocket核心流程

客户端完成 WebSocket/STOMP 连接后，发送到 `/app` 的消息由应用处理，发往 `/topic`、`/queue` 的消息由代理路由给订阅者；`/user` 用于用户定向消息。

具体见[详细介绍](https://www.tony-bro.com/posts/3568303861/index.html)

## 核心功能

{{% alert title="提示" color="primary" %}}

框架本身已经实现WebSocketMessageBrokerConfigurer接口，如果需要再次定制实现该接口，请不要添加@EnableWebSocketMessageBroker。

{{% /alert %}}



本模块基于websocket之上使用STOMP协议实现广播、点对点消息发送功能，具体示例如下：

### WebSocket服务模块定义

#### HTML5示例

##### index.html

```html
<!DOCTYPE html>
<html>
<head>
    <title>Hello WebSocket</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
          integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
    <link href="/css/main.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.1.1.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
    <script src="/js/app.js"></script>
</head>
<body>
<noscript><h2 style="color: #ff0000">Seems your browser doesn't support Javascript! Websocket relies on Javascript being
    enabled. Please enable
    Javascript and reload this page!</h2></noscript>
<div id="main-content" class="container">
    <div class="row">
        <div class="col-md-12">
            <div class="form-group">
                <label for="token">Access Token:</label>
                <input type="text" id="token" class="form-control" placeholder="Access Token?" />
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-4">
            <form class="form-inline">
                <div class="form-group">
                    <label for="connect">Connect:</label>
                    <button id="connect" class="btn btn-default" type="submit">Connect</button>
                    <button id="disconnect" class="btn btn-default" type="submit" disabled="disabled">Disconnect
                    </button>
                </div>
            </form>
        </div>
        <div class="col-md-8">
            <form class="form-inline">
                <div class="form-group">
                    <input type="text" id="name" class="form-control" placeholder="Your Name?">
                    <input type="text" id="sendTo" class="form-control" placeholder="Send To?">
                </div>
                <button id="send" class="btn btn-default" type="submit">Greeting</button>
                <button id="send2" class="btn btn-default" type="submit">Group</button>
                <button id="send3" class="btn btn-default" type="submit">OnlyMe</button>
                <button id="send4" class="btn btn-default" type="submit">Chat</button>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <table id="conversation" class="table table-striped">
                <thead>
                <tr>
                    <th>Greetings</th>
                </tr>
                </thead>
                <tbody id="greetings">
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
```

##### app.js

```javascript
const stompClient = new StompJs.Client({
    onConnect: function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);

        // DEMO
        stompClient.subscribe('/topic/greetings', (greeting) => {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅群组消息
        stompClient.subscribe('/topic/group/' + groupId, function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅私人消息
        stompClient.subscribe('/user/queue/me', function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅他人发给我的消息
        stompClient.subscribe('/user/queue/chat', function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });
    },

    // If disconnected, it will retry after 200ms
    reconnectDelay: 10000,

    debug: function (str) {
        console.log(str);
    },
    onWebSocketError: function (error) {
        console.error('Error with websocket', error);
        disconnect();
    },
    onDisconnect: function () {
        disconnect();
    },
    onStompError: function (frame) {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
    }
});


function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    } else {
        $("#conversation").hide();
    }
    $("#greetings").html("");
}

function connect() {
    let token = $("#token").val()
    if (token !== undefined && token.length > 0) {
        stompClient.brokerURL = 'ws://localhost:8000/websocket/websocket?access_token=' + token
        stompClient.activate();
    } else {
        alert("Please input access token!")
    }
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    stompClient.publish({
        destination: "/app/hello",
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function sendName2() {
    $.post("http://localhost:8000/consumer/hello2/" + groupId, {
        name: $("#name").val()
    });
}

function sendName3() {
    stompClient.publish({
        destination: "/app/me",
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function sendName4() {
    stompClient.publish({
        destination: "/app/chat/" + $("#sendTo").val(),
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function showGreeting(message) {
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    groupId = 123;
    $("form").on('submit', (e) => e.preventDefault());
    $("#connect").click(() => connect());
    $("#disconnect").click(() => disconnect());
    $("#send").click(() => sendName());
    $("#send2").click(() => sendName2());
    $("#send3").click(() => sendName3());
    $("#send4").click(() => sendName4());
});
```

#### 微信小程序示例

微信小程序默认没有浏览器的Websocket对象，需要针对性的封装，代码如下：

- utils/websocket-wx.js

```javascript
(function () {
  wx.webSocketBackup = WebSocket;
  WebSocket = function (uri) {
      this.uri = uri;
      this.socketTask = wx.connectSocket({
          url: uri,
          fail: function(e) {
            console.log("failed:" )
            console.log(e)
          },
          success: function() {
            console.log("success!")
          }
      });
      this.readyState = this.socketTask.readyState;

      this.eventMap = {};
      var that = this;
      this.socketTask.onOpen(function () {
          if (that.eventMap.hasOwnProperty('open')) {
              that.eventMap['open']();
          } else {
              that.onopen();
              that.readyState = that.socketTask.readyState;
          }
      });
      this.socketTask.onMessage(function (res) {
          if (that.eventMap.hasOwnProperty('message')) {
              that.eventMap['message'](res);
          } else {
              that.readyState = that.socketTask.readyState;
              that.onmessage(res);
          }
      });
      this.socketTask.onClose(function () {
          if (that.eventMap.hasOwnProperty('close')) {
              that.eventMap['close']();
          } else {
              that.onclose();
              that.readyState = that.socketTask.readyState;
          }
      });
      this.socketTask.onError(function (res) {
          if (that.eventMap.hasOwnProperty('error')) {
              that.eventMap['error'](res);
          } else {
              that.onerror(res);
              that.readyState = that.socketTask.readyState;
          }
      });
  };

  var event = {};

  WebSocket.prototype = {
      readyState: 3,

      addEventListener: function (event, callback) {
          this.eventMap[event] = callback;
      },
      onopen: function () {

      },
      onmessage: function (res) {
          console.log('default 实现:: ' + res)
      },
      onclose: function () {

      },
      onerror: function (res) {

      },
      send: function (data) {
          this.socketTask.send({
              data: data
          });
      },
      close: function() {
          this.socketTask.close();
      }
  };

  if (typeof exports !== "undefined" && exports !== null) {
      exports.WebSocket = WebSocket;
  }

}).call(this);
```

- 下载https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.js，放到utils目录

- pages/index/index.js

```javascript
// pages/index/index.js
import {Client} from '../../utils/stomp.umd' // 假设已经引入了STOMP库
import {WebSocket} from '../../utils/websocket-wx'

let app = getApp();

Page({
    data: {
        connected: false,
        token: '',
        namex: '',
        sendTo: '',
        greetings: [],
        groupId: 123, //  硬编码的群组ID，实际应用中应该动态获取
    },

    onLoad() {
        this.stompClient = new Client({
            onConnect: (frame) => {
                this.setData({ connected: true });
        
                console.log('Connected: ' + frame);
        
                const subscriptions = [
                    '/topic/greetings',
                    `/topic/group/${this.data.groupId}`,
                    '/user/queue/me',
                    '/user/queue/chat'
                ];
        
                subscriptions.forEach(subscription => {
                    this.stompClient.subscribe(subscription, (greeting) => {
                        const message = JSON.parse(greeting.body).content;
                        this.showGreeting(message);
                    });
                });
            },
            onWebSocketError: (e) => {
                console.error('Error with websocket', error); 
                this.disconnect({force: true});
            },
            onWebSocketClose: (e) => {
                console.info("Websocket close ", e)
                this.setData({ connected: false, greetings: [] });
            },
            onDisconnect: () => {
                console.debug('Disconnect websocket')
            },
            onStompError: (frame) => { 
              console.error('Broker reported error: ' + frame.headers['message']); 
            },
            reconnectDelay: 5000,
            debug: function (str) { 
                console.log(str); 
            },
        });
    },

    onUnload: function() {
        this.disconnect(); 
    },

    connect() {
        this.stompClient.webSocketFactory= function () {
            return new WebSocket("wss://localhost:8000/websocket/websocket?access_token=NjYzMjY4NWUtMjg1Zi00YzY0LWI0OWMtOTc2YjRmZDVhYWIxIyNhZG1pbiMjZTRkYTRhMzItNTkyYi00NmYwLWFlMWQtNzg0MzEwZTg4NDIz");
        };
        if (!this.stompClient.connected) {
            this.stompClient.activate(); 
        }
    },

    disconnect(options = {}) {
        console.log("===disconnect ")
        this.stompClient.deactivate(options);
    },

    publishMessage(destination) {
        this.stompClient.publish({
            destination: destination,
            headers: { 'grayversion': "feat-123" },
            body: JSON.stringify({ 'name': this.data.namex })
        });
    },

    bindAccessTokenInput(e) {
        this.setData({ token: e.detail.value });
    },

    bindNamexInput(e) {
        this.setData({ namex: e.detail.value });
    },

    bindSendToInput(e) {
        this.setData({ sendTo: e.detail.value });
    },

    sendGreeting() {
        this.publishMessage('/app/hello');
    },

    sendGroupMessage() {
        wx.request({
            url: `http://localhost:8000/consumer/hello2/${this.data.groupId}`,
            method: 'POST',
            data: { name: this.data.namex },
            success: res => console.log(res)
        });
    },

    sendPersonalMessage() {
        this.publishMessage('/app/me');
    },

    sendMessageToChat() {
        this.publishMessage(`/app/chat/${this.data.sendTo}`);
    },

    showGreeting(message) {
        this.setData({
            greetings: [...this.data.greetings, message]
        });
    }
});
```

- pages/index/index.wxml

```xml
<!--index.wxml-->
<scroll-view class="scrollarea" scroll-y type="list">
<!-- pages/index/index.wxml -->
<view class="container">
    <view class="form-group">
        <label for="token">Access Token:</label>
        <input type="text" id="token" placeholder="Access Token?" bindinput="bindAccessTokenInput"/>
    </view>
    
    <view class="form-group">
        <button bindtap="connect" disabled="{{connected}}">Connect</button>
        <button bindtap="disconnect" disabled="{{!connected}}">Disconnect</button>
    </view>

    <view class="form-group">
        <input type="text" id="namex" placeholder="Your Name?" bindinput="bindNamexInput"/>
        <input type="text" id="sendTo" placeholder="Send To?" bindinput="bindSendToInput"/>
        <button bindtap="sendGreeting">Greeting</button>
        <button bindtap="sendGroupMessage">Group</button>
        <button bindtap="sendPersonalMessage">OnlyMe</button>
        <button bindtap="sendMessageToChat">Chat</button>
    </view>

    <view class="conversation" wx:if="{{connected}}">
        Greetings
        <view wx:for="{{greetings}}" wx:key="index">
            {{item}}
        </view>
    </view>
</view>
</scroll-view>

```

- pages/index/index.wxss

```css
/**index.wxss**/
page {
  background-color: #f5f5f5;
}

.container {
  max-width: 1280rpx; /* 使用rpx适配不同屏幕尺寸 */
  padding: 40rpx 60rpx;
  margin: 0 auto 40rpx;
  background-color: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 10rpx;
}

```



#### 服务端

##### WebSocketController

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 客户端通过/app/hello发送websocket消息，
     * 默认转发到/topic/hello给客户端订阅,
     * SendTo 重定向到 /topic/greetings，广播消息
     */
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Greeting greeting(HelloMessage message) throws Exception {
        Thread.sleep(1000);
        return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }

    /**
     * HTTP请求后通过template发送广播消息
     */
    @PostMapping("/hello2/{groupId}")
    public void group(HelloMessage message, @PathVariable("groupId") String groupId) throws Exception {
        Thread.sleep(1000);
        simpMessagingTemplate.convertAndSend("/topic/group/" + groupId,
                new Greeting("Hello Group, " + HtmlUtils.htmlEscape(message.getName()) + "!"));
    }

    /**
     * 客户端通过/app/me发送websocket消息，
     * SendToUser重定向到/user/{username}/queue/me，UserDestinationMessageHandler再次处理发送到/queue/me-user{sessionId}
     * 只有发送方自己能收到这个消息
     * 发送方订阅 /user/queue/me，UserDestinationMessageHandler处理成订阅/queue/me-user{sessionId}
     * 保证只有自己收到
     */
    @MessageMapping("/me")
    @SendToUser("/queue/me")  // 谁请求的发给谁，不是广播
    public Greeting me(HelloMessage message) throws Exception {
        Thread.sleep(1000);
        return new Greeting("Hello MyUser, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }

    /**
     * 发送消息给指定用户，用户端需要订阅 /user/queue/chat，UserDestinationMessageHandler处理成订阅/queue/chat-user{sessionId}
     */
    @MessageMapping("/chat/{sendTo}")
    public void chat(@DestinationVariable("sendTo") String sendTo, HelloMessage message) throws Exception {
        // 这个消息会发送到 /user/{sendTo}/queue/chat
        // UserDestinationMessageHandler再次处理发送到/queue/chat-user{sessionId}
        // 注意用户订阅前要处理已经登录状态
        simpMessagingTemplate.convertAndSendToUser(sendTo,"/queue/chat",
                new Greeting("Hello " + HtmlUtils.htmlEscape(sendTo) + "! From "+ HtmlUtils.htmlEscape(message.getName())));
    }
}
```

具体可以参考macula-example/macula-example-consumer项目

### 安全配置

注册 `MessageSecurityMetaSourceCustomizer` Bean 自定义消息目标权限：

```java
@Bean
MessageSecurityMetaSourceCustomizer messageSecurityCustomizer() {
    return messages -> messages
        .simpDestMatchers("/app/admin/**").hasRole("ADMIN")
        .simpSubscribeDestMatchers("/topic/admin/**").hasRole("ADMIN");
}
```

接口参数已改为 Spring Security 7 的 `MessageMatcherDelegatingAuthorizationManager.Builder`。自定义规则在内置测试放行之后、`anyMessage().authenticated()` 兜底之前执行。

`permit-test=true` 时放行无目标消息、`/app/test/**` 发送及 `/user/queue/test/**`、`/topic/test/**` 订阅。当前实现以空的 `csrfChannelInterceptor` 替代默认拦截器，不执行 STOMP CONNECT CSRF 校验；生产部署需结合 HTTP 握手认证、来源限制和消息授权评估安全边界。



## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-redis</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-messaging</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- Spring-boot: https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt
- Spring-framework: https://github.com/spring-projects/spring-framework/blob/main/LICENSE.txt
- Spring-security：https://github.com/spring-projects/spring-security/blob/main/LICENSE.txt
- Hutool：https://github.com/dromara/hutool/blob/v5-master/LICENSE
