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
import org.springframework.messaging.handler.annotation.MessageMapping;
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

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Greeting greeting(HelloMessage message) throws Exception {
        Thread.sleep(1000); // simulated delay
        return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }

    @PostMapping("/game/begin/{groupId}")
    public void group(HelloMessage message, @PathVariable("groupId") String groupId) throws Exception {
        Thread.sleep(1000); // simulated delay
        simpMessagingTemplate.convertAndSend("/topic/group/" + groupId,
                new Greeting("Hello Group, " + HtmlUtils.htmlEscape(message.getName()) + "!"));
    }


    @MessageMapping("/sendToUser")
    @SendToUser("/")
    public Greeting sendToUser(HelloMessage message) throws Exception {
        Thread.sleep(1000); // simulated delay
        log.info("userId:{},msg:{}", message.getUserId(), message.getName());
        return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }

}
