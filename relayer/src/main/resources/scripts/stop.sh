#!/bin/bash

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"
source ${CURR_DIR}/print.sh

print_title

log_info "stop l2-relayer now..."

PID_RELAYER=$(ps -ewf | grep -e "relayer-.*\.jar" | grep -v grep | awk '{print $2}')
if [ -z "$PID_RELAYER" ]; then
  log_warn "l2-relayer is not running"
  exit 0
fi

kill ${PID_RELAYER}
if [ $? -ne 0 ]; then
  log_error "failed to stop l2-relayer"
  exit 1
fi

log_info "l2-relayer stopped successfully"
