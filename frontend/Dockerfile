FROM node:22.14.0-alpine3.21 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:1.27.4-alpine3.21
RUN apk add --no-cache wget \
    && apk upgrade --no-cache \
    && sed -i '/user  nginx;/d' /etc/nginx/nginx.conf \
    && sed -i 's|pid        /var/run/nginx.pid;|pid /tmp/nginx/nginx.pid;|' /etc/nginx/nginx.conf \
    && mkdir -p /var/cache/nginx /var/log/nginx /tmp/nginx \
    && chown -R nginx:nginx /var/cache/nginx /var/log/nginx /tmp/nginx /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx-block-ai-bots.conf /etc/nginx/conf.d/block-ai-bots.conf
COPY --from=build --chown=nginx:nginx /app/dist /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1
