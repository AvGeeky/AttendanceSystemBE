#!/bin/sh

# Inject the REDIS_PASSWORD from env into redis.conf
envsubst < /usr/local/etc/redis/redis.conf.template > /usr/local/etc/redis/redis.conf

exec redis-server /usr/local/etc/redis/redis.conf
