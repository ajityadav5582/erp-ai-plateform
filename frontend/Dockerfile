# syntax=docker/dockerfile:1

# ------------------------------ Dependencies ------------------------------
FROM node:20-alpine AS deps
WORKDIR /app
RUN corepack enable
COPY package.json package-lock.json* ./
RUN npm ci

# ------------------------------ Builder -----------------------------------
FROM node:20-alpine AS builder
WORKDIR /app
RUN corepack enable
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Build-time environment variables (public only).
ARG NEXT_PUBLIC_APP_URL=http://localhost:3000
ENV NEXT_PUBLIC_APP_URL=$NEXT_PUBLIC_APP_URL
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# ------------------------------ Runner ------------------------------------
FROM node:20-alpine AS runner
WORKDIR /app
RUN corepack enable
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# `output: "standalone"` produces a minimal server bundle.
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

# Run as a non-root user.
RUN addgroup --system --gid 1001 nodejs \
  && adduser --system --uid 1001 nextjs \
  && chown -R nextjs:nodejs /app
USER nextjs

EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# `server.js` is emitted by the standalone output.
CMD ["node", "server.js"]
