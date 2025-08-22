#!/bin/bash

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"
source ${CURR_DIR}/print.sh

log_info "start query service container "
set -e

if [ -z "${MYSQL_HOST}" ]; then
   log_error "no env MYSQL_HOST set"
   exit 1
fi
if [ -z "${MYSQL_PORT}" ]; then
   log_error "no env MYSQL_PORT set"
   exit 1
fi
if [ -z "${MYSQL_USER_PASSWORD}" ]; then
   log_error "no env MYSQL_USER_PASSWORD set"
   exit 1
fi
if [ -z "${MYSQL_USER_NAME}" ]; then
   log_error "no env MYSQL_USER_NAME set"
   exit 1
fi

if [ -n "${JASYPT_PASSWD}" ]; then
    RELAYER_START_FLAG="${RELAYER_START_FLAG} -P ${JASYPT_PASSWD}"
fi

# fix supervisor conf and add java link
sed -i 's|supervisor/conf.d/\*.conf|supervisord.d/\*.ini|g' /etc/supervisor/supervisord.conf
if [ ! -f /usr/bin/java ]; then
  ln -s /opt/java/openjdk/bin/java /usr/bin/java
fi

# start haveged and but not hold it
# haveged -F;
haveged;

bash ${CURR_DIR}/start.sh -D;
if [ $? -ne 0 ]; then
  log_error "failed to start query-service"
  exit 1
fi

exec supervisord