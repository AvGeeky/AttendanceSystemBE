FROM redis:7.4.3-alpine

COPY redis.conf.template /usr/local/etc/redis/redis.conf.template
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
