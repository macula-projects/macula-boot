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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.apache.dubbo.spring.security.utils.SecurityNames.SECURITY_CONTEXT_HOLDER_CLASS_NAME;

/**
 * <p>
 * <b>ProviderAttachmentFilter</b> 提供方获取消费方通过RpcContext传过来的参数并做相应处理
 * </p>
 *
 * @author Rain
 * @version $Id: ProviderAttachmentFilter.java 5937 2015-11-05 07:06:29Z wzp $
 * @since 2015年9月23日
 */
@Activate(group = CommonConstants.PROVIDER, onClass = {SECURITY_CONTEXT_HOLDER_CLASS_NAME})
public class ProviderAttachmentFilter implements Filter {

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		// 消费方的用户名
		String userName = RpcContext.getContext().getAttachment(ConsumerAttachmentFilter.USER_NAME);

		// 提供给后台更新lastUpdatedBy和createBy
		if (StringUtils.isNotEmpty(userName)) {
			SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(userName, null));
		}
		return invoker.invoke(invocation);
	}

}
