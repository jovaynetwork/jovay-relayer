FROM centos:centos8.4.2105

RUN sed -i s/mirror.centos.org/vault.centos.org/g  /etc/yum.repos.d/CentOS-* && \
    sed -i s/^#baseurl=http/baseurl=http/g         /etc/yum.repos.d/CentOS-* && \
    sed -i s/^mirrorlist=http/#mirrorlist=http/g   /etc/yum.repos.d/CentOS-* && \
    yum clean all && \
    yum makecache

RUN yum install -y java-17-openjdk-devel wget

RUN cd / && \
    wget --no-check-certificate https://archive.apache.org/dist/maven/maven-3/3.8.9/binaries/apache-maven-3.8.9-bin.tar.gz && \
    tar -zxvf apache-maven-3.8.9-bin.tar.gz

ENV PATH=/apache-maven-3.8.9/bin:$PATH
ENV JAVA_HOME=/usr/
