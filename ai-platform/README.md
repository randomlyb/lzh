# AI 知识平台 (AI Knowledge Platform)

基于微服务架构的 AI 知识管理平台，集成任务调度、RAG 检索增强生成、智能分析等功能。

## 🏗️ 项目架构

```
ai-platform/
├── ai-auth-service/          # 认证服务 (端口 8082)
├── ai-service/               # AI 服务 (端口 8083)
├── ai-frontend/              # 前端应用 (端口 5173)
├── ai-common/                # 公共模块
├── ai-pojo/                  # 数据对象
├── scheduler-parent/         # 调度服务父模块
│   ├── scheduler-service/    # 调度服务 (端口 8084)
│   └── task-scheduler-common/# 调度公共模块
└── deploy/                   # 部署文件
    ├── sql/                  # 数据库初始化脚本
    ├── services/             # 服务 JAR 包和 Dockerfile
    ├── frontend/             # 前端构建产物
    └── docker-compose.yml    # Docker 编排文件
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 5.7+
- Redis 6+
- RabbitMQ 3.8+
- Elasticsearch 8.x
- ZooKeeper 3.8+
- Nacos 2.2+

### 本地开发

#### 1. 初始化数据库

```bash
# 按顺序执行
mysql -u root -p < deploy/sql/nacos-schema.sql
mysql -u root -p < deploy/sql/init.sql
mysql -u root -p < deploy/sql/ai-schema.sql
mysql -u root -p < deploy/sql/auth-schema.sql
mysql -u root -p < deploy/sql/task-scheduler-schema.sql
```

#### 2. 启动基础设施

```bash
cd deploy
docker-compose up -d rabbitmq redis elasticsearch zookeeper nacos
```

#### 3. 启动后端服务

```bash
# 认证服务
cd ai-auth-service && mvn spring-boot:run

# AI 服务
cd ai-service && mvn spring-boot:run

# 调度服务
cd scheduler-parent/scheduler-service && mvn spring-boot:run
```

#### 4. 启动前端

```bash
cd ai-frontend
npm install
npm run dev
```

访问 http://localhost:5173

### Docker 部署

```bash
cd deploy
docker-compose up -d
```

## 📊 核心功能

### 1. 用户认证
- JWT Token 认证
- 角色权限管理
- 用户信息管理

### 2. 知识库管理
- 文档上传与解析
- 文档分块与向量化
- 知识库 CRUD 操作
- 文档标签管理

### 3. RAG 智能检索
- Elasticsearch 全文检索
- 向量相似度检索
- LLM 智能问答
- 对话历史管理

### 4. 任务调度
- Cron 表达式定时任务
- 固定频率任务
- 任务执行日志
- 失败重试机制
- 多执行器路由策略

### 5. AI 智能分析
- 任务执行统计分析
- 任务执行趋势分析
- 失败任务诊断
- 智能排查建议

## 🔧 技术栈

### 后端
- Spring Boot 3.2
- Spring Cloud Alibaba
- Nacos (服务注册与配置中心)
- MyBatis
- RabbitMQ (消息队列)
- Redis (缓存)
- Elasticsearch (检索引擎)
- Quartz (任务调度)
- ZooKeeper (分布式协调)

### 前端
- Vue 3
- Element Plus
- Vite
- ECharts
- MarkdownIt

### AI/LLM
- DeepSeek API
- RAG (检索增强生成)
- Agent 工具调用

## 📁 部署说明

### 部署目录结构

```
deploy/
├── sql/                      # 数据库脚本
│   ├── nacos-schema.sql      # Nacos 数据库
│   ├── init.sql              # 主业务数据库
│   ├── ai-schema.sql         # AI 服务扩展表
│   ├── auth-schema.sql       # 认证服务扩展表
│   └── task-scheduler-schema.sql  # 调度服务扩展表
├── services/                 # 后端服务
│   ├── ai-auth-service/      # 认证服务
│   ├── ai-service/           # AI 服务
│   └── scheduler-service/    # 调度服务
├── frontend/                 # 前端静态文件
├── docker-compose.yml        # Docker 编排
└── docker-compose-dev.yml    # 开发环境编排
```

### 生产环境部署

1. **打包服务**
```bash
mvn clean package -DskipTests
```

2. **初始化数据库**
```bash
mysql -u root -p < deploy/sql/nacos-schema.sql
mysql -u root -p < deploy/sql/init.sql
```

3. **启动服务**
```bash
cd deploy
docker-compose up -d
```

### 服务端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| ai-auth-service | 8082 | 认证服务 |
| ai-service | 8083 | AI 服务 |
| scheduler-service | 8084 | 调度服务 |
| ai-frontend | 5173 | 前端应用 |
| Nacos | 8848 | 注册中心 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| RabbitMQ | 5672/15672 | 消息队列 |
| Elasticsearch | 9200 | 检索引擎 |
| ZooKeeper | 2181 | 分布式协调 |

## 🤖 AI 智能分析功能

### Agent 工具链

- `get_job_statistics` - 获取任务执行统计
- `get_job_execution_trend` - 获取任务执行趋势
- `get_failed_jobs` - 获取失败任务详情
- `get_job_duration_statistics` - 获取任务耗时统计
- `get_job_logs` - 获取任务执行日志

### 分析工作流

```
用户提问
  ↓
调用 get_job_statistics → 获取整体数据
  ↓
调用 get_job_execution_trend → 识别异常时间点
  ↓
调用 get_failed_jobs → 定位失败最多的任务
  ↓
调用 get_job_logs → 获取具体异常日志
  ↓
综合所有数据，生成分析报告
```

### 分析报告格式

```
【数据概览】
- 总执行次数、成功次数、失败次数、成功率
- 近 7 日任务执行情况表格

【异常分析】
- 整体任务成功率分析
- 失败峰值时间点识别
- 零执行现象说明
- 异常持续性分析

【可能原因】
- 基于统计数据的排查方向
- 所有推测均标注"需要结合日志验证"

【建议排查路径】
- 第一优先级：定位失败任务
- 第二优先级：查看任务日志
- 第三优先级：检查任务执行链路
```

## 📝 开发规范

- 使用 JDK 17
- 遵循阿里巴巴 Java 开发规范
- 提交前执行 `mvn clean package`
- 使用 `.gitignore` 排除编译产物

## 📄 License

MIT License