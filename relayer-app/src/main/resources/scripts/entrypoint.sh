#!/bin/bash

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"
source ${CURR_DIR}/print.sh

log_info "start l2 relayer container "
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
if [ -z "${REDIS_URL}" ]; then
   log_error "no env REDIS_URL set"
   exit 1
fi
if [ -z "${REDIS_PORT}" ]; then
   log_error "no env REDIS_PORT set"
   exit 1
fi
if [ -z "${REDIS_USER_PASSWORD}" ]; then
   log_error "no env REDIS_USER_PASSWORD set"
   exit 1
fi
if [ -z "${L1_BLOB_POOL_TX_SIGN_SERVICE_TYPE}" ]; then
   log_error "no env L1_BLOB_POOL_TX_SIGN_SERVICE_TYPE set"
   exit 1
fi
if [ -z "${L1_LEGACY_POOL_TX_SIGN_SERVICE_TYPE}" ]; then
   log_error "no env L1_LEGACY_POOL_TX_SIGN_SERVICE_TYPE set"
   exit 1
fi
if [ -z "${L1_RPC_URL}" ]; then
   log_error "no env L1_RPC_URL set"
   exit 1
fi
if [ -z "${L1_ROLLUP_CONTRACT}" ]; then
   log_error "no env L1_ROLLUP_CONTRACT set"
   exit 1
fi
if [ -z "${L1_MAILBOX_CONTRACT}" ]; then
   log_error "no env L1_MAILBOX_CONTRACT set"
   exit 1
fi
if [ -z "${L2_TX_SIGN_SERVICE_TYPE}" ]; then
   log_error "no env L2_TX_SIGN_SERVICE_TYPE set"
   exit 1
fi
if [ -z "${L2_RPC_URL}" ]; then
   log_error "no env L2_RPC_URL set"
   exit 1
fi
if [ -z "${TRACER_IP}" ]; then
   log_error "no env TRACER_IP set"
   exit 1
fi
if [ -z "${TRACER_PORT}" ]; then
   log_error "no env TRACER_PORT set"
   exit 1
fi
if [ -n "${PROVER_CONTROLLER_IP}" ]; then
    export PROVER_CONTROLLER_ENDPOINTS="${PROVER_CONTROLLER_IP}:${PROVER_CONTROLLER_PORT}"
    log_info "single PC endpoint: ${PROVER_CONTROLLER_ENDPOINTS}"
fi
if [ -z "${PROVER_CONTROLLER_ENDPOINTS}" ]; then
    log_error "none PC info set"
    exit 1
fi

log_info "PROVER_CONTROLLER_ENDPOINTS set ${PROVER_CONTROLLER_ENDPOINTS}"
if [ -n "${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT}" ]; then
    RELAYER_START_FLAG="${RELAYER_START_FLAG} -O ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT}"
fi

# fix supervisor conf and add java link
sed -i 's|supervisor/conf.d/\*.conf|supervisord.d/\*.ini|g' /etc/supervisor/supervisord.conf
if [ ! -f /usr/bin/java ]; then
  ln -s /opt/java/openjdk/bin/java /usr/bin/java
fi

# start haveged and but not hold it
# haveged -F;
haveged;

bash ${CURR_DIR}/start.sh -D -N ${OTEL_SERVICE_NAME:-l2-relayer} ${RELAYER_START_FLAG};
if [ $? -ne 0 ]; then
  log_error "failed to start l2-relayer"
  exit 1
fi

exec supervisord