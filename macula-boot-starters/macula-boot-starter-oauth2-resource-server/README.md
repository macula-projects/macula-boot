# macula-boot-starter-oauth2-resource-server
1. 资源服务器的安全依赖，支持对JWT和OpaqueToken的验证，依赖Oauth Server
2. 内部REST服务依赖该模块，通过JWT来获取用户信息；
3. 网关或者直接对外的REST服务通过OpaqueToken验证登陆信息
4. 该模块只是验证用户，没有处理权限信息。Token验证后的角色信息是有的

通常情况下，网关使用OpaqueToken，然后获取用户角色，验证是否通过权限校验，生成JWT给后续的微服务识别身份