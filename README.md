# MizukiSync（舞萌同步）

![Android](https://img.shields.io/badge/Android-Kotlin-3DDC84?style=flat-square&logo=android)
![Backend](https://img.shields.io/badge/Backend-FastAPI-009688?style=flat-square&logo=fastapi)

MizukiSync 是一款面向舞萌 DX（maimai DX）玩家的 Android 第三方客户端。项目通过对接落雪查分器（LXNS）相关数据能力，为玩家提供成绩查询、曲库检索、筛选与同步体验。

本项目采用前后端分离架构：Android 客户端负责界面展示、本地缓存与用户交互；后端负责数据清洗、版本去重、接口转发与涉密配置托管，避免在客户端内暴露开发者 API Key。

## 核心特性

### 现代化 Android 客户端

- 六宫格主页布局，使用圆角卡片与列表组织功能入口。
- 玩家名片展示 Rating、段位、阶级、底板、头像与称号等信息。
- 通过本地缓存降低重复请求成本，提升移动端体验。

### 曲库与筛选

- 支持按定数范围、游戏版本、乐曲分类等条件筛选曲目。
- 后端定时拉取并清洗曲库数据，减少客户端硬编码。
- 对曲名、版本前缀、隐形字符等数据问题进行规范化处理。

### 成绩同步

- 支持同步玩家最新状态。
- 主页展示最近游玩记录、单曲达成率与评级。
- 后端负责统一处理第三方 API 调用与数据格式转换。

### 安全架构

- 客户端不内置第三方开发者密钥。
- 服务端统一保存 LXNS 相关配置与私密凭证。
- 敏感接口建议部署在可信服务端，并配合访问控制、日志审计和速率限制。

## 架构说明

```text
Android Client
  ├─ Kotlin UI
  ├─ OkHttp / Glide
  └─ SharedPreferences cache
        ↓
FastAPI Backend
  ├─ OAuth / API relay
  ├─ LXNS data cleanup
  ├─ song database cache
  └─ scheduled updater
        ↓
LXNS / external data sources
```

## 项目结构

```text
app/                 Android 客户端
backend/             Python / FastAPI 服务端
  main.py            API 服务入口
  lxns_update.py     曲库与字典同步脚本
```

## 部署与运行

### 服务端部署

建议使用 Linux 服务器部署后端。

安装依赖：

```bash
pip install fastapi uvicorn httpx requests
```

配置 `main.py` 与 `lxns_update.py` 中的服务端参数：

- `LX_CLIENT_ID`
- `LX_CLIENT_SECRET`
- `DEV_API_KEY`
- `SONG_DB_PATH`

启动曲库清洗守护进程：

```bash
nohup python3 lxns_update.py > update.log 2>&1 &
```

启动 FastAPI 服务：

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

### Android 客户端编译

1. 使用 Android Studio 打开项目。
2. 确认 Gradle 与 Kotlin 插件配置正常。
3. 全局搜索默认后端地址，例如 `https://api.mizuki.top`，替换为你自己的后端域名或 IP。
4. Sync Project。
5. Build APK。

## 注意事项

- 请勿将真实 API Key、Client Secret 或数据库凭证提交到仓库。
- 建议将生产配置改为环境变量或独立配置文件，并将其加入 `.gitignore`。
- 曲库与成绩数据来源于第三方服务，接口格式和可用性可能随上游变化。
- 游戏资产版权归原游戏开发商及相关权利方所有，本项目仅用于学习交流和个人数据统计。

## 致谢

感谢落雪查分器（LXNS）提供稳定的数据能力。

## License

当前仓库尚未提供独立 `LICENSE` 文件。如需正式开源分发，建议补充明确许可证文本。
