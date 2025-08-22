#!/bin/bash


Help=$(
  cat <<-"HELP"

 start.sh - Start the query service

 Usage:
   start.sh <params>

 Examples:
  1. start with supervisord：
   start.sh -D
  2. start in application mode:
   start.sh
  3. start with configuration encrypted:
   start.sh -P your_jasypt_password

 Options:
   -D         run with supervisord.
   -P         your jasypt password.
   -h         print help information.

HELP
)

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"

while getopts "hDP:" opt; do
  case "$opt" in
  "h")
    echo "$Help"
    exit 0
    ;;
  "D")
    IF_RUN_WITH_SUPERVISOR="on"
    ;;
  "P")
    JASYPT_PASSWD=$OPTARG
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

source ${CURR_DIR}/print.sh

print_title

JAR_FILE=$(ls ${CURR_DIR}/../lib/ | grep -e 'query-service.*\.jar')

if [[ -n "${JASYPT_PASSWD}" ]]; then
  JASYPT_FLAG="--jasypt.encryptor.password=${JASYPT_PASSWD}"
fi

if [ "$IF_RUN_WITH_SUPERVISOR" == "on" ]; then
  log_info "running with supervisord"

  JAVA_BIN=$(which java)
  if [ -z "$JAVA_BIN" ]; then
    log_error "install jdk before start"
    exit 1
  fi
  if [ "${DEBUG_MODE}" == 'on' ]; then
    JAVA_BIN="${JAVA_BIN} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:25006"
  fi

  L2_QS_START_CMD="${JAVA_BIN} ${AGENT_FLAG} -jar -Dlogging.file.path=${CURR_DIR}/../log ${CURR_DIR}/../lib/${JAR_FILE} --spring.config.location=file:${CURR_DIR}/../config/application-prod.yml ${JASYPT_FLAG}"
  sed -i "s|@START_CMD|${L2_QS_START_CMD}|" ${CURR_DIR}/query-service.ini
  cp ${CURR_DIR}/query-service.ini /etc/supervisord.d/

  log_info "You need to restart supervisord to take effect"
else
  log_info "running in app mode"
  log_info "start query-service now..."

  cd ${CURR_DIR}/..
  if [ ${DEBUG_MODE} == 'on' ]; then
      DEBUG_FLAG=" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:25006"
  fi
  java ${DEBUG_FLAG} ${AGENT_FLAG} -jar -Dlogging.file.path=${CURR_DIR}/../log ${CURR_DIR}/../lib/${JAR_FILE} --spring.config.location=file:${CURR_DIR}/../config/application-prod.yml ${JASYPT_FLAG} >/dev/null 2>&1 &
  if [ $? -ne 0 ]; then
    log_error "failed to start query-service"
    exit 1
  fi
fi

log_info "query-service started successfully"
