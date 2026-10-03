# ${rootArtifactId} Admin

Vue 3/Vite 管理端。业务 API 经生成项目 Gateway，登录 token 由外部 Macula Cloud IAM 提供。

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and
disable
Vetur) + [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).

## Customize configuration

See [Vite Configuration Reference](https://vitejs.dev/config/).

## 本地配置

Admin 根 `.env` 保存各 mode 共享的本地默认值，包括 `/api`、外部 IAM、已批准的 demo OAuth client 和示例账号；`.env.development`、`.env.docker`、`.env.dev` 等文件只覆盖对应 mode 的差异。项目根 `deploy/.env` 的优先级高于 Admin mode 文件，用于统一覆盖本地部署配置；`*_INTERNAL_URL` 只供 Java 应用容器访问。业务、用户和菜单请求统一使用 `/api`，由 Vite/Nginx 代理到生成项目 Gateway，再由 `/admin/**` 路由进入 Admin BFF；只有登录请求由浏览器直接访问外部 Macula Cloud IAM，因此仅 IAM 地址需要允许 Admin 来源的 CORS。容器启动时会先将运行时值编码为 Base64，再生成由浏览器按 UTF-8 解码的 `config.js`，避免引号、反斜杠、换行或非 ASCII 字符破坏脚本语法；修改 `deploy/.env` 后无需重新构建 Admin 镜像。不要提交 `deploy/.env`，也不要使用生产 secret。

- IDE 模式执行 `npm run dev`：根 `.env` 提供 `/api`、外部 IAM 和 demo OAuth 默认值，`.env.development` 开启代理并把 `/api` 转发到本机 `http://127.0.0.1:6000`，因此可直接从 Admin 模块启动。Vite 还会主动加载 `../deploy/.env`，其中的同名值优先级更高。开发模式不使用 Docker 启动时生成的 `config.js`。
- Docker 模式执行 `./deploy/scripts/compose.sh up-apps`：Nginx 把 `/api` 转发到 Compose 中的生成项目 Gateway，并在容器启动时把外部 IAM 与 OAuth 配置写入 `config.js`。

## 安装与开发

```sh
npm ci
```

### 开发服务器

```sh
npm run dev
```

浏览器访问 `http://127.0.0.1:5800`。启动前应先在 IDE 中运行生成项目 Gateway，或确认 `VITE_APP_GATEWAY_PROXY_TARGET` 指向一个可访问的 Gateway。

### 生产构建

```sh
npm run build
```

容器模式构建：

```sh
npm run build:docker
docker build -t ${rootArtifactId}-admin .
```

### 单元测试

```sh
npm run test:unit
```

### Cypress 登录链路测试

```sh
npm run build
npm run test:e2e # or `npm run test:e2e:ci` for headless testing
```

### ESLint

```sh
npm run lint
```
