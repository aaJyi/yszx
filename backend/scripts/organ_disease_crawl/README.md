# 器官疾病知识：辅助脚本

## 说明

- **正式数据**：推荐以仓库内 `src/main/resources/sql/seed_organ_disease_detail.sql` 为准（中文教学示意，便于审核）。
- **本目录脚本**：使用 **Wikipedia MediaWiki API** 拉取英文条目摘要（`extracts`），便于人工整理后写入数据库，**非**对整站 HTML 做高频抓取。

## 环境

```bash
cd D:\happyLife\backend\scripts\organ_disease_crawl
pip install -r requirements.txt
```

## 一、按器官各拉取「3 个常见关联疾病」摘要（推荐）

映射文件：`organ_top3_titles.json`（18 个器官 × 3 条英文标题，可按需修改）。

**在 `venv` 中执行：**

```bash
cd D:\happyLife\backend\scripts\organ_disease_crawl
python fetch_organs_top3.py
```

默认输出：`extracts_by_organ.json`（按 `organ_key` 分组，每条含 `wikipedia_title`、`extract`、`missing`）。

自定义输出与间隔：

```bash
python fetch_organs_top3.py --out my_extracts.json --delay 1.5
```

## 二、仅手动指定若干英文标题

```bash
python fetch_wikipedia_extracts.py --titles "Stroke,Liver_cirrhosis,Pneumonia" --out extracts.json --delay 1.5
```

将生成的文本人工翻译、裁剪后，可拼接为 `INSERT` 写入 `organ_disease_detail` 表。

## 三、将 `extracts_by_organ.json` 译为中文并写入 MySQL

依赖：`pip install -r requirements.txt`（含 `pymysql`、`deep-translator`）。

**环境变量（与 `application.properties` 中库名一致）：**

| 变量 | 说明 |
|------|------|
| `MYSQL_HOST` | 默认 `localhost` |
| `MYSQL_PORT` | 默认 `3306` |
| `MYSQL_USER` | 默认 `root` |
| `MYSQL_PASSWORD` | **必填**（或本机无密码则留空） |
| `MYSQL_DATABASE` | 库名，默认 `mvit`（也可用 `MYSQL_DB`） |

**执行（PowerShell 示例）：**

```powershell
cd D:\happyLife\backend\scripts\organ_disease_crawl
$env:MYSQL_PASSWORD = "你的密码"
$env:MYSQL_DATABASE = "mvit"
python import_extracts_to_mysql.py --json extracts_by_organ.json
```

- 会先删除同一批器官中 `data_source = wikipedia_zh` 的旧记录，再插入新译文，**不影响** `seed` 种子数据。
- 需能访问 Google 翻译服务（机翻）；仅测试库连接可用 `--skip-translate`（英文直存，不推荐生产）。

**只预览不写库：**

```bash
python import_extracts_to_mysql.py --json extracts_by_organ.json --dry-run
```

导入后，管理端病机图谱与接口 **`GET /admin/organ-diseases?organKey=brain`** 会直接读到新数据（若同器官既有 `seed` 又有 `wikipedia_zh`，列表会合并展示，按 `sort_order` 排序）。

## 合规提示

- 控制频率（`--delay`），使用明确 `User-Agent`。
- 机翻仅供参考；临床产品请以医疗机构与权威指南为准；本模块默认仅为管理端教学示意。
