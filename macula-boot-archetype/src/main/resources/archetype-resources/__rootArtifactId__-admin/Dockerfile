# syntax=docker/dockerfile:1.7
#set( $symbol_dollar = '$' )
FROM node:22-alpine AS builder

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

ARG VITE_MODE=docker
ARG VITE_APP_API_BASEURL=/api
ENV VITE_APP_API_BASEURL=${symbol_dollar}{VITE_APP_API_BASEURL}
RUN npm run build -- --mode "${symbol_dollar}{VITE_MODE}"

FROM nginxinc/nginx-unprivileged:1.27.5-alpine

COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx/templates/config.js.template /opt/admin/config.js.template
COPY nginx/entrypoint.d/25-admin-runtime-config.sh /docker-entrypoint.d/25-admin-runtime-config.sh
COPY --chown=101:101 --from=builder /workspace/dist /usr/share/nginx/html

USER root
RUN chmod 755 /docker-entrypoint.d/25-admin-runtime-config.sh \
    && chown 101:101 /usr/share/nginx/html
USER 101:101
EXPOSE 8080
