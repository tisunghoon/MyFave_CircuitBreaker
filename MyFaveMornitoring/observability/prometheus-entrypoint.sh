#!/bin/sh
sed "s|\${PROD_HOST}|$PROD_HOST|g" /etc/prometheus/prometheus.template.yml > /tmp/prometheus.yml
exec /bin/prometheus \
  --web.enable-remote-write-receiver \
  --config.file=/tmp/prometheus.yml
