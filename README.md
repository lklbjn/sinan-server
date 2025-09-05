# Sinan Server

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5.12-blue.svg)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Sinan Server 是一个基于 Spring Boot 3 开发的现代化书签管理系统后端服务，提供完整的书签收藏、分类、标签管理和分享功能。

## ✨ 功能特性

### 🔖 书签管理

- **书签收藏**：支持网页书签的添加、编辑、删除
- **书签导入**：支持从浏览器导入书签文件
- **使用统计**：记录书签访问次数，智能推荐
- **快速搜索**：支持书签标题、URL、描述的全文搜索

### 🏷️ 分类与标签

- **空间管理**：创建不同的书签空间进行分类
- **标签系统**：为书签添加多个标签，支持标签统计
- **拖拽排序**：支持空间和标签的拖拽排序
- **层级管理**：支持空间和标签的层级结构

### 👥 用户与分享

- **用户认证**：基于 Sa-Token 的安全认证体系
- **GitHub OAuth**：支持 GitHub 第三方登录
- **API 密钥认证**：支持基于用户密钥的 API 访问控制
- **书签分享**：支持书签的接收和分享功能
- **权限控制**：细粒度的用户权限管理

### 🔧 技术特性

- **RESTful API**：标准的 REST 接口设计
- **数据缓存**：Redis 缓存提升性能
- **逻辑删除**：数据安全的软删除机制
- **参数校验**：完整的请求参数验证
- **异常处理**：统一的异常处理机制

## 🏗️ 技术栈

### 后端框架

- **Spring Boot 3.2.4** - 现代化的 Java 企业应用框架
- **Spring Web** - Web 层框架
- **Spring Validation** - 参数校验框架

### 数据存储

- **MySQL 8.0+** - 关系型数据库
- **MyBatis Plus 3.5.12** - 持久层框架
- **Redis** - 缓存数据库

### 安全认证

- **Sa-Token 1.44.0** - 轻量级权限认证框架
- **GitHub OAuth2** - 第三方登录集成

### 工具库

- **Lombok** - 简化 Java 代码
- **Fastjson2** - JSON 处理库
- **OkHttp** - HTTP 客户端

## 🚀 快速开始

### 环境要求

- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**

### 本地开发

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd sinan-server
   ```

2. **配置数据库**
   ```sql
   CREATE DATABASE sinan_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **修改配置**

编辑 `src/main/resources/application-dev.yaml`：

```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/sinan_dev?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      username: your_username
      password: your_password
    data:
      redis:
        host: 127.0.0.1
        port: 6379
        password: your_redis_password

  github:
    oauth2:
      client-id: your_github_client_id
      client-secret: your_github_client_secret
      redirect-uri: http://localhost/github-callback
  sinan:
    server:
      base-url: http://localhost:8080
    upload:
      base-path: /path/to/upload/directory
      icon-path: /path/to/icon/directory
      url-prefix: http://localhost/uploads
    passkey:
      id: localhost
      name: sinan
      origin:
        - http://localhost
        - http://localhost:8080
```

4. **运行项目**
   ```bash
   # 使用 Maven
   mvn spring-boot:run
   
   # 或者使用 IDE 直接运行 SinanApplicationRun.java
   ```

5. **访问服务**

   服务启动后访问：`http://localhost:8080/api`

### Docker 部署

1. **使用 Docker Compose（推荐）** [完整教程](https://sinan.host/docs/guide/deployment.html)
   ```bash
   # 启动完整环境（应用 + MySQL + Redis）
   docker-compose up -d
   
   # 查看日志
   docker-compose logs -f sinan-server
   ```

2. **手动构建**
   ```bash
   # 构建 JAR 包
   mvn clean package -DskipTests
   
   # 构建 Docker 镜像
   docker build -t sinan-server:latest .
   
   # 运行容器
   docker run -d \
     --name sinan-server \
     -p 8080:8080 \
     -v /path/to/config:/resources \
     -e SPRING_PROFILES_ACTIVE=prod \
     sinan-server:latest
   ```

## 📖 API 文档

### API 密钥认证 API

| 方法   | 路径              | 描述    | 认证方式         |
|------|-----------------|-------|--------------|
| POST | `/api/bookmark` | 添加书签  | X-Access-Key |
| GET  | `/api/bookmark` | 获取书签树 | X-Access-Key |

> 💡 **API 密钥认证**: 新增基于用户密钥的 API 访问方式，通过 `X-Access-Key` 请求头进行认证。

### 书签管理 API

| 方法     | 路径                         | 描述     |
|--------|----------------------------|--------|
| POST   | `/api/bookmark`            | 添加书签   |
| PUT    | `/api/bookmark`            | 编辑书签   |
| DELETE | `/api/bookmark/{id}`       | 删除书签   |
| GET    | `/api/bookmark/list`       | 获取书签列表 |
| POST   | `/api/bookmark/import`     | 导入书签文件 |
| PUT    | `/api/bookmark/{id}/usage` | 增加使用次数 |

### 空间管理 API

| 方法     | 路径                | 描述     |
|--------|-------------------|--------|
| POST   | `/api/space`      | 创建空间   |
| PUT    | `/api/space`      | 编辑空间   |
| DELETE | `/api/space/{id}` | 删除空间   |
| GET    | `/api/space/list` | 获取空间列表 |
| PUT    | `/api/space/sort` | 空间排序   |

### 标签管理 API

| 方法     | 路径              | 描述     |
|--------|-----------------|--------|
| POST   | `/api/tag`      | 创建标签   |
| PUT    | `/api/tag`      | 编辑标签   |
| DELETE | `/api/tag/{id}` | 删除标签   |
| GET    | `/api/tag/list` | 获取标签列表 |
| PUT    | `/api/tag/sort` | 标签排序   |

### 用户管理 API

| 方法   | 路径                       | 描述        |
|------|--------------------------|-----------|
| GET  | `/api/user/info`         | 获取用户信息    |
| POST | `/api/user/github-login` | GitHub 登录 |
| POST | `/api/user/logout`       | 用户登出      |
| POST | `/api/user/key`          | 创建用户密钥    |
| GET  | `/api/user/keys`         | 获取用户密钥列表  |

## 📁 项目结构

```
sinan-server/
├── src/main/java/pres/peixinyi/sinan/
│   ├── SinanApplicationRun.java          # 启动类
│   ├── common/                           # 通用工具类
│   │   ├── Result.java                   # 统一响应结果
│   │   └── RedisUtils.java               # Redis 工具类
│   ├── config/                           # 配置类
│   │   ├── MybatisConfigure.java         # MyBatis 配置
│   │   └── SaTokenConfigure.java         # Sa-Token 配置
│   ├── controller/                       # 控制器层
│   │   ├── ApiController.java            # API 密钥认证控制器
│   │   ├── BookmarkController.java       # 书签控制器
│   │   ├── SpaceController.java          # 空间控制器
│   │   ├── TagController.java            # 标签控制器
│   │   ├── UserController.java           # 用户控制器
│   │   └── GitHubOAuth2Controller.java   # GitHub OAuth 控制器
│   ├── dto/                              # 数据传输对象
│   │   ├── request/                      # 请求 DTO
│   │   └── response/                     # 响应 DTO
│   ├── entity/                           # 实体类
│   │   ├── SnBookmark.java               # 书签实体
│   │   ├── SnBookmarkAssTag.java         # 书签标签关联实体
│   │   ├── SnReceivedBookmark.java       # 接收书签实体
│   │   ├── SnSpace.java                  # 空间实体
│   │   ├── SnTag.java                    # 标签实体
│   │   ├── SnUser.java                   # 用户实体
│   │   ├── SnUserCredential.java         # 用户凭证实体
│   │   └── SnUserKey.java                # 用户密钥实体
│   ├── exception/                        # 异常处理
│   ├── mapper/                           # 数据访问层
│   ├── model/                            # 业务模型
│   ├── service/                          # 业务逻辑层
│   └── utils/                            # 工具类
├── src/main/resources/
│   ├── application.yaml                  # 主配置文件
│   ├── application-dev.yaml              # 开发环境配置
│   ├── banner.txt                        # 启动横幅
│   └── mapper/                           # MyBatis XML 映射文件
├── CI-CD-README.md                       # CI/CD 部署说明文档
├── ENVIRONMENT-CONFIG.md                 # 环境配置详细说明
├── docker-compose.yml                    # Docker Compose 配置
├── Dockerfile                            # Docker 镜像构建文件
├── settings.xml                          # Maven 设置文件
└── pom.xml                               # Maven 项目配置
```

## 🔧 配置说明

> 💡 **详细环境配置**: 查看 [ENVIRONMENT-CONFIG.md](ENVIRONMENT-CONFIG.md) 了解完整的环境变量配置和外部数据库部署方式。

### 数据库配置

项目使用 MySQL 作为主数据库，包含以下主要表：

- `sn_user` - 用户表
- `sn_user_key` - 用户密钥表
- `sn_user_credential` - 用户凭证表
- `sn_space` - 空间表
- `sn_bookmark` - 书签表
- `sn_tag` - 标签表
- `sn_bookmark_ass_tag` - 书签标签关联表
- `sn_received_bookmark` - 接收书签表

### 缓存配置

使用 Redis 进行数据缓存，提升系统性能：

- 用户会话缓存
- 热点数据缓存
- 计数器缓存

### 安全配置

基于 Sa-Token 实现：

- JWT Token 认证
- 接口权限控制
- 跨域请求处理

## 🚀 CI/CD 部署

项目集成了完整的 CI/CD 流程，支持自动化构建和部署：

### GitHub Actions 特性

- ✅ 自动构建 Java 应用
- ✅ 运行单元测试
- ✅ 构建 Docker 镜像
- ✅ 推送到阿里云容器镜像服务
- ✅ 生成构建产物（JAR + Docker 镜像）

### 部署方式

1. **阿里云容器镜像服务**：`registry.cn-hangzhou.aliyuncs.com/yixing-tech/sinan-server`
2. **GitHub Artifacts**：JAR 文件和 Docker 镜像离线包

详细的 CI/CD 配置和使用说明请查看：[CI-CD-README.md](CI-CD-README.md)

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范

- 遵循 Java 代码规范
- 添加必要的注释和文档
- 编写单元测试
- 确保所有测试通过

## 📝 更新日志

### v1.1.0(2025-09-05)

- 允许上传书签图标支持url,base64,文件上传
- 实现PassKey 支持

### v1.0.0 (2025-08-20)

- ✨ 初始版本发布
- 🔖 实现书签管理功能
- 🏷️ 实现标签和空间管理
- 👥 实现用户认证和 GitHub OAuth
- 🐳 添加 Docker 支持
- 🚀 集成 CI/CD 流程

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。
[开源许可证](https://sinan.host/docs/guide/open-source-license.html)

## 👨‍💻 作者

**PeiXinyi** - *项目创建者* - [GitHub](https://github.com/peixinyi) - [个人网站](https://peixinyi.com)


## 🙏 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis Plus](https://baomidou.com/)
- [Sa-Token](https://sa-token.cc/)
- [Redis](https://redis.io/)

---

如有问题或建议，欢迎提交 [Issue](../../issues) 或联系作者。
