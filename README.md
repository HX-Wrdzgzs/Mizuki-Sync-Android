# MizukiSync (舞萌同步)

![Android](https://img.shields.io/badge/Android-Kotlin-3DDC84?style=flat-square&logo=android)
![Backend](https://img.shields.io/badge/Backend-FastAPI-009688?style=flat-square&logo=fastapi)
![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)

MizukiSync 是一款专为舞萌 DX (maimai DX) 玩家设计的 Android 第三方客户端。通过对接 [落雪查分器 (LXNS) API](https://lxns.net/)，为玩家提供美观、流畅且安全的成绩查询与曲库管理体验。

本项目采用**前后端分离架构**，将核心的数据清洗、版本去重以及涉密的开发者 API 密钥全部下沉至私有服务端，确保 Android 客户端极致轻量且绝对安全。

## ✨ 核心特性

- **📱 现代化交互 UI**
  - 六宫格清晰布局，精心设计的圆角卡片与列表。
  - 玩家名片全景展示（Rating、段位、阶级、底板、头像、称号跑马灯）。
- **🎵 极速曲库与智能筛选**
  - 支持 **定数范围 (精确到小数)**、**游戏版本**、**乐曲分类** 的三重联合筛选。
  - 彻底告别硬编码：服务端定时拉取官方数据，客户端动态生成筛选字典，新版本自动适配，无惧“未知版本”或幽灵子版本号。
- **📊 成绩同步与盲盒**
  - 一键同步最新玩家状态，主页展示最新游玩成绩（盲盒抽签），直观显示单曲达成率与评级。
- **🛡️ 极致的安全架构**
  - 客户端完全脱敏：不包含任何第三方开发者 API Key，防止逆向抓包。
  - 服务端数据清洗：后台守护进程（Daemon）每日自动拉取落雪数据，剔除冗余前缀（如 `maimai DX `）、清理隐形字符，为客户端提供开箱即用的干净 JSON 接口。

## 🏗️ 架构说明

本项目分为两个主要部分：

1. **Android 客户端 (`/app`)**
   - 语言：Kotlin
   - 核心组件：`RecyclerView`, `CardView`, `Coroutines` (协程异步处理)
   - 网络与图片：`OkHttp`, `Glide`
   - 特点：仅负责 UI 渲染、本地 `SharedPreferences` 缓存管理及对端请求，无任何重度数据处理逻辑。

2. **Python 服务端 (`/backend`)**
   - 框架：FastAPI, Uvicorn
   - 核心文件：
     - `main.py`: 提供 OAuth 认证、公共数据分发、单曲详情中转及玩家成绩抽取接口。
     - `lxns_update.py`: 独立守护进程，每 24 小时从落雪 API 全量同步曲目、别名、字典，并执行数据去重与清洗。

## 🚀 部署与运行

### 服务端部署 (Backend)

建议使用 Linux 服务器进行部署：

1. 安装依赖：
   ```bash
   pip install fastapi uvicorn httpx requests
