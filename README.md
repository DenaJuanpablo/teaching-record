面对面教学智能记录系统 (Teaching Record)

项目简介
本系统旨在将教学音视频（课堂答疑、论文指导、答辩等）自动转写、分析，生成结构化纪要（摘要、关键词、问题点、待办清单等），形成可检索、可复用的教学档案。项目包含前端和后端两部分，采用前后端分离架构。

技术栈
前端
Vue 3 + Vite
Element Plus (UI 组件库)
Vue Router (路由)
Axios (HTTP 请求)

后端
Spring Boot 3.3.2
Spring Data JPA
MySQL 数据库
集成讯飞语音识别 API
集成 SiliconFlow 大模型 API

核心功能
音视频文件上传（支持 mp4/mov/wav/mp3，最大 500MB）
记录列表分页、筛选（关键词、状态、场景类型）
详情页视频播放，异步处理记录（转写 + AI 分析）
转写结果展示（时间轴分段）
分析结果展示（摘要、关键词、结构化大纲）
删除记录（列表页和详情页）

快速启动

环境要求
Node.js 16+（推荐 18+）
Java JDK 17+
MySQL 8.0+
Maven 3.6+（或用 IDEA 内置）
Git

1. 克隆项目
git clone https://github.com/DenaJuanpablo/teaching-record.git
cd teaching-record


2. 前端启动
cd teachflow-client
npm install          # 安装依赖
npm run dev          # 启动开发服务器

访问 http://localhost:5173 即可看到前端页面。

3. 后端启动
3.1 创建数据库
在 MySQL 中创建数据库：
CREATE DATABASE teaching_record CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

3.2 修改配置文件
后端配置文件位于 `teachflow-server/src/main/resources/`。复制 `application-example.yml` 为 `application.yml`，并填入真实的配置（数据库密码、API密钥等）。

然后用文本编辑器打开 `application.yml`，替换 `${...}` 占位符为实际值（如数据库密码、讯飞 AppId 等）。

3.3 运行后端
使用 IDEA 打开 `teachflow-server` 目录，运行 `Main.java`；或在命令行中：
cd teachflow-server
mvn spring-boot:run

后端默认运行在 http://localhost:8080。

4. 访问系统
上传页：http://localhost:5173/upload
列表页：http://localhost:5173/records
详情页：http://localhost:5173/records/{id}

配置说明
前端代理配置：`vite.config.js` 中已配置代理，将 `/api` 和 `/uploads` 转发到后端，无需额外修改。
后端敏感信息（数据库密码、讯飞密钥等），已通过 `.gitignore` 忽略 `application.yml`，并提供 `application-example.yml` 作为模板。

项目结构

teaching-record/
├── teachflow-client/          # 前端项目
│   ├── src/                    # 源代码
│   ├── public/                 # 静态资源
│   ├── package.json            # 依赖配置
│   └── vite.config.js          # Vite 配置
├── teachflow-server/           # 后端项目
│   ├── src/                    # 源代码
│   ├── pom.xml                 # Maven 配置
│   └── application.yml         # 本地配置（已忽略）
└── README.md                   # 本文档






