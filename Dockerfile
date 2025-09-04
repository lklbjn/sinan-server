# 用于 GitHub Actions 的简化 Dockerfile
FROM openjdk:17-jdk-slim

LABEL maintainer="peixinyi"
LABEL description="Sinan Server Application"

# 设置容器环境变量
ARG SOFTWARE_VERSION=unknown
ARG SOFTWARE_VERSION_DATE=unknown
ENV SOFTWARE_VERSION=${SOFTWARE_VERSION}
ENV SOFTWARE_VERSION_DATE=${SOFTWARE_VERSION_DATE}

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户（安全最佳实践）
RUN groupadd -r sinan && useradd -r -g sinan sinan

# 设置工作目录
WORKDIR /app

# 从构建的 jar 目录复制JAR文件
COPY jar/sinan.jar app.jar

# 设置文件权限
RUN chown -R sinan:sinan /app

# 设置配置文件路径和 JVM 参数
ENV CONFIG_PATH=/resources/application.yaml
ENV JVM_ARGS="${JVM_ARGS} -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom"

# 设置时区
RUN echo 'Asia/Shanghai' > /etc/timezone && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建并设置日志目录
RUN mkdir -p /app/logs && \
    chown -R sinan:sinan /app/logs

# 暴露端口
EXPOSE 8080

# 挂载配置文件和日志目录
VOLUME ["/resources", "/app/logs", "/upload"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 切换到非 root 用户
USER sinan

# 执行 JAR 包
ENTRYPOINT ["sh", "-c", "java $JVM_ARGS -jar app.jar --spring.config.location=$CONFIG_PATH"]
