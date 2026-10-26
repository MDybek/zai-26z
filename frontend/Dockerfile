FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine AS runner

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/default.conf.template
COPY 40-setup-backend.sh /docker-entrypoint.d/40-setup-backend.sh
RUN chmod +x /docker-entrypoint.d/40-setup-backend.sh

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
