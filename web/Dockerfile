# syntax=docker/dockerfile:1
ARG NODE_IMAGE=node@sha256:06d7adf3e0ef61a13f1d28682726366f97a15aa1888c934d3ecec86c33918b53
ARG NGINX_IMAGE=nginx@sha256:47928bf67eb62a1863cb08d9ecb52d3e704304ba614eaee17884204677702951
FROM ${NODE_IMAGE} AS build
WORKDIR /workspace
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN corepack enable && corepack prepare "$(node -p "require('./package.json').packageManager")" --activate
RUN --mount=type=cache,target=/pnpm/store \
    pnpm install --frozen-lockfile --store-dir /pnpm/store
COPY . .
RUN pnpm build && pnpm typecheck

FROM ${NGINX_IMAGE} AS runtime
COPY docker/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=6 \
    CMD wget -q -T 2 -O- http://127.0.0.1/healthz | grep -qx ready || exit 1
