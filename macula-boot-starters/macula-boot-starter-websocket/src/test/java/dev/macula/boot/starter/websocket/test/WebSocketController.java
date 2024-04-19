/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.macula.boot.starter.websocket.test;

import dev.macula.boot.starter.websocket.test.vo.Greeting;
import dev.macula.boot.starter.websocket.test.vo.HelloMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * <p>
 * <b>WebSocketController</b> 测试Controller
 * </p>
 *
 * @author Rain
 * @since 2024/4/15
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 客户端通过/app/test/hello发送websocket消息，
     * 默认转发到/topic/test/hello给客户端订阅,
     * SendTo 重定向到 /topic/greetings，广播消息
     */
    @MessageMapping("/test/hello")
    @SendTo("/topic/test/greetings")
    public Greeting greeting(HelloMessage message) throws Exception {
        Thread.sleep(1000);
        return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }

    /**
     * HTTP请求后通过template发送广播消息
     */
    @PostMapping("/test/hello2/{groupId}")
    public void group(HelloMessage message, @PathVariable("groupId") String groupId) throws Exception {
        Thread.sleep(1000);
        simpMessagingTemplate.convertAndSend("/topic/test/group/" + groupId,
                new Greeting("Hello Group, " + HtmlUtils.htmlEscape(message.getName()) + "!"));
    }

    /**
     * 客户端通过/app/test/me发送websocket消息，
     * SendToUser重定向到/user/{username}/queue/test/me，UserDestinationMessageHandler再次处理发送到/queue/test/me-user{sessionId}
     * 只有发送方自己能收到这个消息
     * 发送方订阅 /user/queue/test/me，UserDestinationMessageHandler处理成订阅/queue/test/me-user{sessionId}
     * 保证只有自己收到
     */
    @MessageMapping("/test/me")
    @SendToUser("/queue/test/me")  // 谁请求的发给谁，不是广播
    public Greeting me(HelloMessage message) throws Exception {
        Thread.sleep(1000);
        return new Greeting("Hello MyUser, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }

    /**
     * 发送消息给指定用户，用户端需要订阅 /user/queue/test/chat，UserDestinationMessageHandler处理成订阅/queue/test/chat-user{sessionId}
     */
    @MessageMapping("/test/chat/{userId}")
    public void chat(@DestinationVariable("userId") String userId, HelloMessage message) throws Exception {
        // 这个消息会发送到 /user/{userId}/queue/chat
        // UserDestinationMessageHandler再次处理发送到/queue/chat-user{sessionId}
        // 注意用户订阅前要处理已经登录状态
        simpMessagingTemplate.convertAndSendToUser(userId,"/queue/test/chat",
                new Greeting("Hello Chat, " + HtmlUtils.htmlEscape(message.getName()) + "!"));
    }
}
