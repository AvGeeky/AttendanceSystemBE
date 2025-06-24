FROM redis:7.4.3-alpine
#FROM redis:7.4.3
ARG REDIS_PASSWORD
ENV REDIS_PASSWORD=${REDIS_PASSWORD}

# Ensure the config directory exists first
RUN mkdir -p /usr/local/etc/redis && \
    echo "bind 0.0.0.0" >> /usr/local/etc/redis/redis.conf && \
    echo "protected-mode yes" >> /usr/local/etc/redis/redis.conf && \
    echo "save 60 50" >> /usr/local/etc/redis/redis.conf && \
    echo "appendonly yes" >> /usr/local/etc/redis/redis.conf && \
    echo "dir /data" >> /usr/local/etc/redis/redis.conf && \
    echo "requirepass ${REDIS_PASSWORD}" >> /usr/local/etc/redis/redis.conf

CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
