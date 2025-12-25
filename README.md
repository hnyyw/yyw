# NetDisk（类百度网盘）开发骨架

## 目录
- `netdisk-docs/openapi.yaml`：接口合同（可导入 Swagger/Apifox）
- `netdisk-server/`：后端（Spring Boot 3 + JWT + S3/MinIO Multipart）
- `netdisk-web/`：前端（Vue3 + TS，后续生成）
- `docker-compose.yml`：开发环境（MySQL/Redis/MinIO）

## 一键启动依赖（MySQL/Redis/MinIO）
在工作区根目录执行：

```bash
docker compose up -d
```

MinIO 控制台：`http://localhost:9001`（账号/密码：`minioadmin` / `minioadmin`）

## 启动后端
进入后端目录执行：

```bash
mvn -f netdisk-server/pom.xml spring-boot:run
```

Swagger UI：`http://localhost:8080/swagger-ui/index.html`

## 快速自测顺序（最短链路）
1. `POST /api/v1/auth/register`
2. `POST /api/v1/auth/login` 拿 `access_token`
3. 新建文件夹、列表目录
4. 上传：`/uploads/precheck` → `/uploads/init` → 前端 PUT 分片到 presigned url → `/uploads/complete`
5. 下载：`/files/{entryId}/download-link`

