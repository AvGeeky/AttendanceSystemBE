FROM redis:7.4.3-alpine

ARG REDIS_PASSWORD
ENV REDIS_PASSWORD=${REDIS_PASSWORD}

# Properly inject Redis config with newlines
RUN printf "%s\n" \
  "bind 0.0.0.0" \
  "protected-mode yes" \
  "save 60 100" \
  "appendonly yes" \
  "dir /data" \
  "requirepass ${REDIS_PASSWORD}" \
  > /usr/local/etc/redis/redis.conf

CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
