#!/bin/bash

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"
source ${CURR_DIR}/print.sh

print_title

log_info "stop query-service now..."

PID_RELAYER=$(ps -ewf | grep -e "query-service-.*\.jar" | grep -v grep | awk '{print $2}')
if [ -z "$PID_RELAYER" ]; then
  log_warn "query-service is not running"
  exit 0
fi

kill ${PID_RELAYER}
if [ $? -ne 0 ]; then
  log_error "failed to stop query-service"
  exit 1
fi

log_info "query-service stopped successfully"
