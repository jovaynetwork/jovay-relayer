FROM dtvmdev1/chain-java17:audit-dev AS stage1

ENV TZ Asia/Shanghai

RUN ln -fs /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo ${TZ} > /etc/timezone \
    && mkdir "/query-service"

RUN yum install -y epel-release && \
    yum install -y haveged openssl gettext jq supervisor tini && \
    yum clean all

ENV LANG=en_US.UTF8
ENV RELAYER_HOME=/query-service
ADD query-service.tar.gz /

ENTRYPOINT ["/usr/bin/tini", "--", "/query-service/bin/entrypoint.sh"]