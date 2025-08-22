
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
WHITE='\033[1;37m'
NC='\033[0m'
LIGHT_GRAY='\033[0;37m'

function print_blue() {
  printf "${BLUE}%s${NC}\n" "$1"
}

function print_red() {
  printf "${RED}%s${NC}\n" "$1"
}

function print_green() {
  printf "${GREEN}%s${NC}\n" "$1"
}

function print_hint() {
  printf "${WHITE}\033[4m%s${NC}" "$1"
}

function log_info() {
  NOW=$(date "+%Y-%m-%d %H:%M:%S.%s" | cut -b 1-23)
  INFO_PREFIX=$(printf "${GREEN}\033[4m[ INFO ]${NC}")

  INFO=$(printf "_${LIGHT_GRAY}[ %s ]${NC} : %s" "${NOW}" "$1")
  echo "${INFO_PREFIX}${INFO}"
}

function log_warn() {
  NOW=$(date "+%Y-%m-%d %H:%M:%S.%s" | cut -b 1-23)
  WARN_PREFIX=$(printf "${YELLOW}\033[4m[ WARN ]${NC}")

  INFO=$(printf "_${LIGHT_GRAY}[ %s ]${NC} : %s" "${NOW}" "$1")
  echo "${WARN_PREFIX}${INFO}"
}

function log_error() {
  NOW=$(date "+%Y-%m-%d %H:%M:%S.%s" | cut -b 1-23)
  ERROR_PREFIX=$(printf "${RED}\033[4m[ ERROR ]${NC}")

  INFO=$(printf "_${LIGHT_GRAY}[ %s ]${NC} : %s" "${NOW}" "$1")
  echo "${ERROR_PREFIX}${INFO}"
}

function print_title() {
  echo '    ___            __   ______ __            _       '
  echo '   /   |   ____   / /_ / ____// /_   ____ _ (_)____  '
  echo '  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \ '
  echo ' / ___ | / / / // /_ / /___ / / / // /_/ // // / / / '
  echo '/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  '
  echo '                                                     '
  echo
}
