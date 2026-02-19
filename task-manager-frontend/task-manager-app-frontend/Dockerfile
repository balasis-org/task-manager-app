# ── Stage 1: build the Vite app ──
FROM node:22-alpine AS build
WORKDIR /app

# Build-time env variables (Vite bakes these into the bundle)
ARG VITE_API_BASE=/api
ARG VITE_BLOB_BASE=
ARG VITE_CDN_BASE=

ENV VITE_API_BASE=$VITE_API_BASE
ENV VITE_BLOB_BASE=$VITE_BLOB_BASE
ENV VITE_CDN_BASE=$VITE_CDN_BASE

COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# ── Stage 2: serve with nginx ──
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY ./nginx.prod.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
