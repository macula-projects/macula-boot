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

package dev.macula.boot.starter.websocket.stomp;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.user.*;
import org.springframework.util.Assert;
import org.springframework.web.socket.messaging.*;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * <b>RedisSimpUserRegistry</b>基于Redis保存订阅用户记录
 * </p>
 *
 * @author Rain
 * @since 2024/4/23
 */
@RequiredArgsConstructor
public class RedisSimpUserRegistry implements SimpUserRegistry, SmartApplicationListener {

    @Setter
    private int order = Ordered.LOWEST_PRECEDENCE;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return AbstractSubProtocolEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        AbstractSubProtocolEvent subProtocolEvent = (AbstractSubProtocolEvent) event;
        Message<?> message = subProtocolEvent.getMessage();
        MessageHeaders headers = message.getHeaders();

        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        Assert.state(sessionId != null, "No session id");

        if (event instanceof SessionConnectedEvent) {
            Principal user = subProtocolEvent.getUser();
            if (user == null) {
                return;
            }
            String name = user.getName();
            if (user instanceof DestinationUserNameProvider) {
                name = ((DestinationUserNameProvider) user).getDestinationUserName();
            }
            addUserSessions(name, user, sessionId);

        } else if (event instanceof SessionDisconnectEvent) {
            removeUserSessions(sessionId);

        } else if (event instanceof SessionSubscribeEvent) {
            String id = SimpMessageHeaderAccessor.getSubscriptionId(headers);
            String destination = SimpMessageHeaderAccessor.getDestination(headers);
            if (id != null && destination != null) {
                addSessionSubscriptions(sessionId, id, destination);
            }

        } else if (event instanceof SessionUnsubscribeEvent) {
            String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
            if (subscriptionId != null) {
                removeSessionSubscriptions(sessionId, subscriptionId);
            }
        }
    }

    @Override
    public SimpUser getUser(@NonNull String userName) {
        // 从user获取principal
        String key = CacheConstants.WEBSOCKET_USERS_KEY + userName;
        Principal user = (Principal) redisTemplate.boundValueOps(key).get();
        if (user != null) {
            LocalSimpUser simpUser = new LocalSimpUser(userName, user);

            // 从user获取sessionIds
            key = CacheConstants.WEBSOCKET_USER_SESSIONS_KEY + userName;
            Set<Object> sessionIds = redisTemplate.boundSetOps(key).members();
            if (sessionIds != null && !sessionIds.isEmpty()) {
                for (Object sessionId : sessionIds) {
                    LocalSimpSession simpSession = new LocalSimpSession(sessionId.toString(), simpUser);
                    // 获取sessionId对应的订阅
                    key = CacheConstants.WEBSOCKET_SESSION_SUBSCRIPTIONS_KEY + sessionId;
                    List<Object> subs_destination = redisTemplate.boundHashOps(key).values();
                    if (subs_destination != null && !subs_destination.isEmpty()) {
                        for (Object sub_destination : subs_destination) {
                            String[] sub_dest = sub_destination.toString().split(",");
                            simpSession.addSubscription(sub_dest[0], sub_dest[1]);
                        }
                    }
                    simpUser.addSession(simpSession);
                }
            }
            return simpUser;
        }
        return null;
    }

    @Override
    public Set<SimpUser> getUsers() {
        return Collections.emptySet();
    }

    @Override
    public int getUserCount() {
        return 0;
    }

    @Override
    public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
        return Collections.emptySet();
    }

    private void addUserSessions(String name, Principal user, String sessionId) {
        // 建立user session的关联关系
        String key = CacheConstants.WEBSOCKET_USER_SESSIONS_KEY + name;
        redisTemplate.boundSetOps(key).add(sessionId);

        // 保存userPrincipal
        key = CacheConstants.WEBSOCKET_USERS_KEY + name;
        redisTemplate.boundValueOps(key).set(user);

        // 建立SESSION User关系
        key = CacheConstants.WEBSOCKET_SESSION_USER_KEY + sessionId;
        redisTemplate.boundValueOps(key).set(name);
    }

    private void removeUserSessions(String sessionId) {
        // 提取用户名
        String key = CacheConstants.WEBSOCKET_SESSION_USER_KEY + sessionId;
        String name = (String) redisTemplate.boundValueOps(key).get();
        if (StrUtil.isNotEmpty(name)) {
            // 删除sessionId
            redisTemplate.delete(key);

            // 从user sessions中删除sessionId
            key = CacheConstants.WEBSOCKET_USER_SESSIONS_KEY + name;
            redisTemplate.boundSetOps(key).remove(sessionId);

            // 如果user sessions中为空，则删除user
            Long userSessionsSize = redisTemplate.boundSetOps(key).size();
            if (userSessionsSize == null || userSessionsSize <= 0) {
                redisTemplate.delete(key);
                key = CacheConstants.WEBSOCKET_USERS_KEY + name;
                redisTemplate.delete(key);
            }
        }
        // 删除该sessionId所有订阅
        key = CacheConstants.WEBSOCKET_SESSION_SUBSCRIPTIONS_KEY + sessionId;
        redisTemplate.delete(key);
    }

    private void addSessionSubscriptions(String sessionId, String subscriptionId, String destination) {
        String key = CacheConstants.WEBSOCKET_SESSION_SUBSCRIPTIONS_KEY + sessionId;
        redisTemplate.boundHashOps(key).put(subscriptionId, subscriptionId + "," + destination);
    }

    private void removeSessionSubscriptions(String sessionId, String subscriptionId) {
        String key = CacheConstants.WEBSOCKET_SESSION_SUBSCRIPTIONS_KEY + sessionId;
        redisTemplate.boundHashOps(key).delete(subscriptionId);
        Long subIdSize = redisTemplate.boundSetOps(key).size();
        if (subIdSize == null || subIdSize <= 0) {
            redisTemplate.delete(key);
        }
    }


    private static class LocalSimpUser implements SimpUser {

        private final String name;

        private final Principal user;

        private final Map<String, SimpSession> userSessions = new ConcurrentHashMap<>(1);

        public LocalSimpUser(String userName, Principal user) {
            Assert.notNull(userName, "User name must not be null");
            this.name = userName;
            this.user = user;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Nullable
        @Override
        public Principal getPrincipal() {
            return this.user;
        }

        @Override
        public boolean hasSessions() {
            return !this.userSessions.isEmpty();
        }

        @Override
        @Nullable
        public SimpSession getSession(@Nullable String sessionId) {
            return (sessionId != null ? this.userSessions.get(sessionId) : null);
        }

        @Override
        public Set<SimpSession> getSessions() {
            return new HashSet<>(this.userSessions.values());
        }

        void addSession(SimpSession session) {
            this.userSessions.put(session.getId(), session);
        }

        void removeSession(String sessionId) {
            this.userSessions.remove(sessionId);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return (this == other ||
                    (other instanceof SimpUser && getName().equals(((SimpUser) other).getName())));
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public String toString() {
            return "name=" + getName() + ", sessions=" + this.userSessions;
        }
    }


    private static class LocalSimpSession implements SimpSession {

        private final String id;

        private final LocalSimpUser user;

        private final Map<String, SimpSubscription> subscriptions = new ConcurrentHashMap<>(4);

        public LocalSimpSession(String id, LocalSimpUser user) {
            Assert.notNull(id, "Id must not be null");
            Assert.notNull(user, "User must not be null");
            this.id = id;
            this.user = user;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public LocalSimpUser getUser() {
            return this.user;
        }

        @Override
        public Set<SimpSubscription> getSubscriptions() {
            return new HashSet<>(this.subscriptions.values());
        }

        void addSubscription(String id, String destination) {
            this.subscriptions.put(id, new LocalSimpSubscription(id, destination, this));
        }

        void removeSubscription(String id) {
            this.subscriptions.remove(id);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return (this == other ||
                    (other instanceof SimpSubscription && getId().equals(((SimpSubscription) other).getId())));
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }

        @Override
        public String toString() {
            return "id=" + getId() + ", subscriptions=" + this.subscriptions;
        }
    }


    private static class LocalSimpSubscription implements SimpSubscription {

        private final String id;

        private final LocalSimpSession session;

        private final String destination;

        public LocalSimpSubscription(String id, String destination, LocalSimpSession session) {
            Assert.notNull(id, "Id must not be null");
            Assert.hasText(destination, "Destination must not be empty");
            Assert.notNull(session, "Session must not be null");
            this.id = id;
            this.destination = destination;
            this.session = session;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public LocalSimpSession getSession() {
            return this.session;
        }

        @Override
        public String getDestination() {
            return this.destination;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SimpSubscription)) {
                return false;
            }
            SimpSubscription otherSubscription = (SimpSubscription) other;
            return (getId().equals(otherSubscription.getId()) &&
                    getSession().getId().equals(otherSubscription.getSession().getId()));
        }

        @Override
        public int hashCode() {
            return getId().hashCode() * 31 + getSession().getId().hashCode();
        }

        @Override
        public String toString() {
            return "destination=" + this.destination;
        }
    }

}
