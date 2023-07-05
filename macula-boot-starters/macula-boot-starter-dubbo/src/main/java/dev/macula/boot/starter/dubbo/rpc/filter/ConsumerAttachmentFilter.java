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

package dev.macula.boot.starter.dubbo.rpc.filter;

import dev.macula.boot.constants.SecurityConstants;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.apache.dubbo.spring.security.utils.SecurityNames.SECURITY_CONTEXT_HOLDER_CLASS_NAME;

/**
 * <p>
 * <b>ConsumerAuthFilter</b> 传送消费方的通用属性给提供方参考，包括用户名等
 * </p>
 *
 * @author Rain
 * @version $Id: ConsumerAttachmentFilter.java 5937 2015-11-05 07:06:29Z wzp $
 * @since 2015年9月23日
 */
@Activate(group = CommonConstants.CONSUMER, onClass = {SECURITY_CONTEXT_HOLDER_CLASS_NAME})
public class ConsumerAttachmentFilter implements Filter {

	public final static String USER_NAME = "ConsumerUserName";

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		// 消费方的用户名
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String name = SecurityConstants.BACKGROUND_USER;
		if (authentication != null) {
			name = authentication.getName();
		}

		RpcContext.getContext().setAttachment(USER_NAME, name);
		return invoker.invoke(invocation);
	}
}
