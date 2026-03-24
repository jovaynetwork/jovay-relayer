#!/bin/bash

Help=$(
  cat <<-"HELP"

 init_anchor.sh - Init anchor batch

 Usage:
   init_anchor.sh <params>

 Examples:
  1. start in system service mode：
   init_anchor.sh -a <hex anchor batch header>

 Options:
   -a         hex anchor batch header.
   -h         print help information.

HELP
)

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"
source ${CURR_DIR}/print.sh

while getopts "ha:" opt; do
  case "$opt" in
  "h")
    echo "$Help"
    exit 0
    ;;
  "a")
    ANCHOR_BATCH_HEADER_HEX=$OPTARG
    ;;
  "?")
    echo "invalid arguments. "
    exit 1
    ;;
  *)
    echo "Unknown error while processing options"
    exit 1
    ;;
  esac
done

wait_for_port() {
    local host="$1"
    local port="$2"
    local timeout="${3:-30}"  # 默认超时时间30秒

    log_info "Waiting for $host:$port..."
    for i in $(seq 1 $timeout); do
        nc -z "$host" "$port" >/dev/null 2>&1 && {
            log_info "$host:$port is available"
            return 0
        }
        log_info "Waiting... ($i/$timeout)"
        sleep 1
    done
    log_error "Timeout waiting for $host:$port"
    return 1
}

log_info "⌛️ wait l2 relayer start..."
wait_for_port localhost 7088

if [ -z "${ANCHOR_BATCH_HEADER_HEX}" ]; then
    log_error "set anchor batch header hex into env ANCHOR_BATCH_HEADER_HEX please."
    exit 1
fi

if [ ! -d ${CURR_DIR}/relayer-cli ]; then
    cd ${CURR_DIR}
    tar -zxf ${CURR_DIR}/../relayer-cli-bin.tar.gz
fi

cd ${CURR_DIR}/relayer-cli
log_info "using anchor batch header to init : ${ANCHOR_BATCH_HEADER_HEX}"
java -jar lib/relayer-cli.jar init-anchor-batch --rawAnchorBatchHeaderHex "${ANCHOR_BATCH_HEADER_HEX}"
if [ $? -ne 0 ]; then
  log_error "failed to init anchor batch, please do it manually! "
  exit 1
fi