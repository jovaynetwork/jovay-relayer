FROM dtvmdev1/chain-java17:audit-dev AS stage1

ENV TZ Asia/Shanghai

RUN ln -fs /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo ${TZ} > /etc/timezone \
    && mkdir "/l2-relayer"

RUN yum install -y epel-release && \
    yum install -y haveged openssl gettext jq supervisor tini && \
    yum clean all

ENV LANG=en_US.UTF8
ENV RELAYER_HOME=/l2-relayer
ADD l2-relayer.tar.gz /

ENTRYPOINT ["/usr/bin/tini", "--", "/l2-relayer/bin/entrypoint.sh"]