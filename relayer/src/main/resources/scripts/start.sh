#!/bin/bash


Help=$(
  cat <<-"HELP"

 start.sh - Start the L2 Relayer

 Usage:
   start.sh <params>

 Examples:
  1. start in system service mode：
   start.sh -s
  2. start in application mode:
   start.sh
  3. start with configuration encrypted:
   start.sh -P your_jasypt_password

 Options:
   -s         run in system service mode.
   -D         run with supervisord.
   -P         your jasypt password.
   -O         The endpoint to send all OTLP traces. If set, start with otlp agent
   -N         Name of this relayer node inside OTLP traces, default `l2-relayer`
   -h         print help information.

HELP
)

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"

while getopts "hsDP:O:N:" opt; do
  case "$opt" in
  "h")
    echo "$Help"
    exit 0
    ;;
  "s")
    IF_SYS_MODE="on"
    ;;
  "D")
    IF_RUN_WITH_SUPERVISOR="on"
    ;;
  "P")
    JASYPT_PASSWD=$OPTARG
    ;;
  "O")
    OTLP_ENDPOINT=$OPTARG
    ;;
  "N")
    OTLP_SERV_NAME=$OPTARG
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

JAR_FILE=$(ls ${CURR_DIR}/../lib/ | grep -e 'relayer.*\.jar')
AGENT_JAR_FILE=$(ls ${CURR_DIR}/../lib/ | grep -e 'opentelemetry-javaagent-.*\.jar')

if [[ -n "${JASYPT_PASSWD}" ]]; then
  JASYPT_FLAG="--jasypt.encryptor.password=${JASYPT_PASSWD}"
fi

if [[ -z "${OTLP_SERV_NAME}" ]]; then
  OTLP_SERV_NAME="l2-relayer"
fi

if [[ -n "${OTLP_ENDPOINT}" ]]; then
  AGENT_FLAG="-javaagent:${CURR_DIR}/../lib/${AGENT_JAR_FILE} -Dotel.traces.sampler=always_on -Dotel.service.name=${OTLP_SERV_NAME} -Dotel.exporter.otlp.endpoint=${OTLP_ENDPOINT} -Dotel.javaagent.log-file=otel-javaagent.log -Dotel.traces.exporter=logging -Dotel.traces.exporter=otlp -Dotel.metrics.exporter=otlp -Dotel.resource.attributes=\"INSTANA_PLUGIN=jvm\"  "
fi

if [ "$IF_SYS_MODE" == "on" ]; then
  if [[ "$OSTYPE" == "darwin"* ]]; then
    log_error "${OSTYPE} not support running in system service mode"
    exit 1
  fi

  touch /usr/lib/systemd/system/test123 >/dev/null && rm -f /usr/lib/systemd/system/test123
  if [ $? -ne 0 ]; then
    log_error "Your account on this OS must have authority to access /usr/lib/systemd/system/"
    exit 1
  fi

  log_info "running in system service mode"

  JAVA_BIN=$(which java)
  if [ -z "$JAVA_BIN" ]; then
    log_error "install jdk before start"
    exit 1
  fi
  if [ ${DEBUG_MODE} == 'on' ]; then
      JAVA_BIN="${JAVA_BIN} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:25005"
  fi
  START_CMD="${JAVA_BIN} ${AGENT_FLAG} -jar -Dlogging.file.path=${CURR_DIR}/../log ${CURR_DIR}/../lib/${JAR_FILE} --spring.config.location=file:${CURR_DIR}/../config/application-prod.yml ${JASYPT_FLAG}"
  WORK_DIR="$(
    cd ${CURR_DIR}/..
    pwd
  )"

  sed -i -e "s#@@START_CMD@@#${START_CMD}#g" ${CURR_DIR}/l2-relayer.service
  sed -i -e "s#@@WORKING_DIR@@#${WORK_DIR}#g" ${CURR_DIR}/l2-relayer.service

  cp -f ${CURR_DIR}/l2-relayer.service /usr/lib/systemd/system/
  if [ $? -ne 0 ]; then
    log_error "failed to cp l2-relayer.service to /usr/lib/systemd/system/"
    exit 1
  fi

  systemctl daemon-reload && systemctl enable l2-relayer.service
  if [ $? -ne 0 ]; then
    log_error "failed to enable l2-relayer.service"
    exit 1
  fi

  systemctl start l2-relayer
  if [ $? -ne 0 ]; then
    log_error "failed to start l2-relayer.service"
    exit 1
  fi

elif [ "$IF_RUN_WITH_SUPERVISOR" == "on" ]; then
  log_info "running with supervisord"

  JAVA_BIN=$(which java)
  if [ -z "$JAVA_BIN" ]; then
    log_error "install jdk before start"
    exit 1
  fi
  if [ "${DEBUG_MODE}" == 'on' ]; then
    JAVA_BIN="${JAVA_BIN} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:25005"
  fi

  if [ ! -d ${CURR_DIR}/relayer-cli ]; then
    cd ${CURR_DIR}
    tar -zxf ${CURR_DIR}/../relayer-cli-bin.tar.gz
    cd -
  fi

  L2_RELAYER_START_CMD="${JAVA_BIN} ${AGENT_FLAG} -jar -Dlogging.file.path=${CURR_DIR}/../log ${CURR_DIR}/../lib/${JAR_FILE} --spring.config.location=file:${CURR_DIR}/../config/application-prod.yml ${JASYPT_FLAG}"
  sed -i "s|@START_CMD|${L2_RELAYER_START_CMD}|" ${CURR_DIR}/l2-relayer.ini
  cp ${CURR_DIR}/l2-relayer.ini /etc/supervisord.d/

  log_info "You need to restart supervisord to start relayer..."
else
  log_info "running in app mode"
  log_info "start l2-relayer now..."

  cd ${CURR_DIR}/..
  if [ ${DEBUG_MODE} == 'on' ]; then
      DEBUG_FLAG=" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:25005"
  fi
  java ${DEBUG_FLAG} ${AGENT_FLAG} -jar -Dlogging.file.path=${CURR_DIR}/../log ${CURR_DIR}/../lib/${JAR_FILE} --spring.config.location=file:${CURR_DIR}/../config/application-prod.yml ${JASYPT_FLAG} >/dev/null 2>&1 &
  if [ $? -ne 0 ]; then
    log_error "failed to start l2-relayer"
    exit 1
  fi
fi

log_info "l2-relayer started successfully"
