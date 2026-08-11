FROM node:24.18.0-alpine AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:24.18.0-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:9090/api/v1
ARG NEXT_PUBLIC_CSRF_HEADER_NAME=X-XSRF-TOKEN
ARG NEXT_PUBLIC_DEMO_AUTH=false
ARG NEXT_PUBLIC_SHOW_DEMO_ACCOUNTS=true
ENV NEXT_TELEMETRY_DISABLED=1
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV NEXT_PUBLIC_CSRF_HEADER_NAME=${NEXT_PUBLIC_CSRF_HEADER_NAME}
ENV NEXT_PUBLIC_DEMO_AUTH=${NEXT_PUBLIC_DEMO_AUTH}
ENV NEXT_PUBLIC_SHOW_DEMO_ACCOUNTS=${NEXT_PUBLIC_SHOW_DEMO_ACCOUNTS}
COPY --from=dependencies /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:24.18.0-alpine AS runtime
WORKDIR /app
ENV HOSTNAME=0.0.0.0
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
RUN apk add --no-cache wget
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public
USER node
EXPOSE 3000
HEALTHCHECK --interval=20s --timeout=5s --retries=6 CMD wget -qO- http://localhost:3000/login > /dev/null || exit 1
CMD ["node", "server.js"]
