"""
健康档案查询客户端
提问时直接连接本机 MySQL 数据库（localhost:3306）查询用户健康档案，分析得出回答结果。
"""
import json
import logging
from datetime import date, datetime
from typing import Dict, Any, List, Optional

try:
    import pymysql
    from pymysql.cursors import DictCursor
    HAS_PYMYSQL = True
except ImportError:
    HAS_PYMYSQL = False
    logging.warning("pymysql 未安装，请安装: pip install pymysql")

# 配置日志
logger = logging.getLogger(__name__)


class HealthArchiveNotFoundError(Exception):
    """健康档案不存在或未就绪异常"""
    pass


def _get_connection(host: str, port: int, user: str, password: str, database: str):
    """创建本机 MySQL 连接"""
    return pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        cursorclass=DictCursor,
        autocommit=True,
    )


def _to_json_value(v: Any) -> Any:
    """将 date/datetime 转为 ISO 字符串，使返回数据可被 json.dumps 序列化"""
    if v is None:
        return None
    if isinstance(v, (date, datetime)):
        return v.isoformat()
    if isinstance(v, dict):
        return {k: _to_json_value(x) for k, x in v.items()}
    if isinstance(v, list):
        return [_to_json_value(x) for x in v]
    return v


def _row_to_camel(d: Dict[str, Any], key_map: Dict[str, str]) -> Dict[str, Any]:
    """将数据库 snake_case 键转为 API 使用的 camelCase，并保证 date/datetime 可 JSON 序列化"""
    if not d:
        return {}
    out = {}
    for k, v in d.items():
        new_key = key_map.get(k, k)
        out[new_key] = _to_json_value(v)
    return out


class HealthArchiveQueryClient:
    """健康档案查询客户端：直连本机 MySQL（localhost:3306）查询健康档案"""

    # 表名（可与实际库表名一致，若不同可通过环境变量或子类覆盖）
    TABLE_ARCHIVE = "health_archive"
    TABLE_USER_INFO = "user_info"
    TABLE_PROFILE_TAGS = "health_profile_tags"
    TABLE_ALLERGY = "allergy_history"
    TABLE_DISEASE = "disease_history"
    TABLE_FAMILY = "family_history"
    TABLE_AI_ANALYSIS = "health_ai_analysis"

    def __init__(
        self,
        host: str = None,
        port: int = None,
        user: str = None,
        password: str = None,
        database: str = None,
    ):
        """
        初始化健康档案查询客户端（使用本机数据库）

        Args:
            host: 数据库主机，默认从配置读取（localhost）
            port: 数据库端口，默认 3306
            user: 数据库用户
            password: 数据库密码
            database: 数据库名
        """
        if not HAS_PYMYSQL:
            raise ImportError("请安装 pymysql 库: pip install pymysql")

        try:
            from app.core.config import settings
        except ImportError:
            settings = None

        self.host = host if host is not None else (getattr(settings, "db_host", None) or "localhost")
        self.port = port if port is not None else (getattr(settings, "db_port", None) or 3306)
        self.user = user if user is not None else (getattr(settings, "db_user", None) or "root")
        self.password = password if password is not None else (getattr(settings, "db_password", None) or "")
        self.database = database if database is not None else (getattr(settings, "db_name", None) or "medical")

        logger.info(
            f"健康档案查询客户端初始化完成，使用本机数据库: {self.host}:{self.port}/{self.database}"
        )

    def _conn(self):
        return _get_connection(
            self.host, self.port, self.user, self.password, self.database
        )

    def fetch_user_health_archives(self, user_id: int) -> List[Dict[str, Any]]:
        """
        从本机 MySQL 查询指定用户的健康档案列表。
        返回结构与原 API 一致，便于提问时直接用于分析得出回答。

        Args:
            user_id: 用户ID

        Returns:
            健康档案列表，每项包含 healthArchive、userInfo、allergyHistory 等（与 API 格式一致）

        Raises:
            ValueError: user_id 无效
            HealthArchiveNotFoundError: 无健康档案或未就绪
            RuntimeError: 数据库连接/查询失败
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")

        logger.info(f"正在从本机数据库查询用户 {user_id} 的健康档案")

        try:
            conn = self._conn()
        except Exception as e:
            err_str = str(e)
            logger.error(f"连接本机数据库失败: {e}")
            if "1045" in err_str and "using password: NO" in err_str:
                raise RuntimeError(
                    "连接本机数据库失败：MySQL 拒绝了空密码登录。请在项目根目录创建 .env 文件并设置 DB_PASSWORD=你的MySQL密码，或参考 .env.example。"
                ) from e
            raise RuntimeError(f"连接本机数据库失败: {e}") from e

        try:
            with conn.cursor() as cur:
                # 查询该用户所有健康档案（按日期或 ID 倒序，新的在前）
                cur.execute(
                    f"""
                    SELECT archive_id, user_id, user_name, archive_no, archive_name,
                           archive_date, archive_year
                    FROM {self.TABLE_ARCHIVE}
                    WHERE user_id = %s
                    ORDER BY COALESCE(archive_date, '') DESC, archive_id DESC
                    """,
                    (user_id,),
                )
                rows = cur.fetchall()
        finally:
            conn.close()

        if not rows:
            logger.warning(f"用户 {user_id} 在本机数据库中无健康档案")
            raise HealthArchiveNotFoundError(f"用户 {user_id} 的健康档案不存在或未就绪")

        archive_key_map = {
            "archive_id": "archiveId",
            "user_id": "userId",
            "user_name": "userName",
            "archive_no": "archiveNo",
            "archive_name": "archiveName",
            "archive_date": "archiveDate",
            "archive_year": "archiveYear",
        }

        result = self._fetch_archives_with_details(user_id, rows, archive_key_map)
        logger.info(f"成功从本机数据库查询用户 {user_id} 的健康档案，共 {len(result)} 个")
        return result

    def _fetch_archives_with_details(
        self, user_id: int, archive_rows: List[Dict], archive_key_map: Dict[str, str]
    ) -> List[Dict[str, Any]]:
        """对每个档案查从表并组装为 API 同构结构"""
        result = []
        try:
            conn = self._conn()
        except Exception:
            raise
        try:
            with conn.cursor() as cur:
                for row in archive_rows:
                    archive_id = row.get("archive_id")
                    health_archive = _row_to_camel(dict(row), archive_key_map)
                    user_info = self._fetch_user_info_by_cursor(cur, archive_id)
                    profile_tags = self._fetch_profile_tags_by_cursor(cur, archive_id)
                    allergy = self._fetch_allergy_by_cursor(cur, archive_id)
                    disease = self._fetch_disease_by_cursor(cur, archive_id)
                    family = self._fetch_family_by_cursor(cur, archive_id)
                    result.append({
                        "healthArchive": health_archive,
                        "healthProfileTags": profile_tags,
                        "userInfo": user_info,
                        "allergyHistory": allergy,
                        "diseaseHistory": disease,
                        "familyHistory": family,
                    })
        finally:
            conn.close()
        return result

    def _fetch_user_info_by_cursor(self, cur, archive_id: int) -> Dict[str, Any]:
        try:
            cur.execute(
                f"SELECT full_name, gender, birth_date, personal_phone FROM {self.TABLE_USER_INFO} WHERE archive_id = %s LIMIT 1",
                (archive_id,),
            )
            r = cur.fetchone()
        except Exception:
            r = None
        if not r:
            return {}
        return _row_to_camel(
            dict(r),
            {"full_name": "fullName", "birth_date": "birthDate", "personal_phone": "personalPhone"},
        )

    def _fetch_profile_tags_by_cursor(self, cur, archive_id: int) -> Dict[str, Any]:
        try:
            cur.execute(
                f"SELECT is_child_06, is_elderly_65, is_pregnant, blood_type, weight_status FROM {self.TABLE_PROFILE_TAGS} WHERE archive_id = %s LIMIT 1",
                (archive_id,),
            )
            r = cur.fetchone()
        except Exception:
            r = None
        if not r:
            return {}
        return _row_to_camel(
            dict(r),
            {"is_child_06": "isChild06", "is_elderly_65": "isElderly65", "blood_type": "bloodType", "weight_status": "weightStatus"},
        )

    def _fetch_allergy_by_cursor(self, cur, archive_id: int) -> Optional[Dict[str, Any]]:
        try:
            cur.execute(
                f"SELECT allergy_id, archive_id, allergy_type, allergy_name, severity, reaction FROM {self.TABLE_ALLERGY} WHERE archive_id = %s LIMIT 1",
                (archive_id,),
            )
            r = cur.fetchone()
        except Exception:
            r = None
        if not r:
            return None
        return _row_to_camel(
            dict(r),
            {"allergy_id": "allergyId", "archive_id": "archiveId", "allergy_type": "allergyType", "allergy_name": "allergyName"},
        )

    def _fetch_disease_by_cursor(self, cur, archive_id: int) -> Optional[Dict[str, Any]]:
        try:
            cur.execute(
                f"SELECT disease_id, archive_id, disease_name, diagnosis_date, treatment_status FROM {self.TABLE_DISEASE} WHERE archive_id = %s LIMIT 1",
                (archive_id,),
            )
            r = cur.fetchone()
        except Exception:
            r = None
        if not r:
            return None
        return _row_to_camel(
            dict(r),
            {"disease_id": "diseaseId", "archive_id": "archiveId", "disease_name": "diseaseName", "diagnosis_date": "diagnosisDate", "treatment_status": "treatmentStatus"},
        )

    def _fetch_family_by_cursor(self, cur, archive_id: int) -> Optional[Dict[str, Any]]:
        try:
            cur.execute(
                f"SELECT family_id, archive_id, relation, disease_name, age_of_onset FROM {self.TABLE_FAMILY} WHERE archive_id = %s LIMIT 1",
                (archive_id,),
            )
            r = cur.fetchone()
        except Exception:
            r = None
        if not r:
            return None
        return _row_to_camel(
            dict(r),
            {"family_id": "familyId", "archive_id": "archiveId", "disease_name": "diseaseName", "age_of_onset": "ageOfOnset"},
        )

    def fetch_latest_health_archive(self, user_id: int) -> Dict[str, Any]:
        """
        查询指定用户的最新健康档案（从本机数据库）。
        同时查询该档案对应的最新健康分析（health_ai_analysis），一次查询即可供提问和健康建议使用。

        Args:
            user_id: 用户ID

        Returns:
            最新一条健康档案字典，含 healthSummary（最新健康分析，含 analysisResult 供推荐生成使用）
        """
        health_archives = self.fetch_user_health_archives(user_id)
        latest_archive = health_archives[0]
        archive_id = latest_archive.get("healthArchive", {}).get("archiveId")
        if archive_id is not None:
            health_summary = self._fetch_latest_analysis_by_archive(archive_id)
            if health_summary:
                latest_archive["healthSummary"] = health_summary
        logger.info(f"获取用户 {user_id} 的最新健康档案（本机数据库）")
        return latest_archive

    def _fetch_latest_analysis_by_archive(self, archive_id: int) -> Optional[Dict[str, Any]]:
        """根据档案ID查询最新健康分析（含 analysisResult），供健康建议生成使用"""
        try:
            conn = self._conn()
        except Exception:
            return None
        try:
            with conn.cursor() as cur:
                cur.execute(
                    f"""
                    SELECT analysis_id, user_id, archive_id, source_raw_id,
                           overall_health_score, risk_level, analysis_summary, analysis_detail
                    FROM {self.TABLE_AI_ANALYSIS}
                    WHERE archive_id = %s
                    ORDER BY analysis_id DESC
                    LIMIT 1
                    """,
                    (archive_id,),
                )
                row = cur.fetchone()
        finally:
            conn.close()
        if not row:
            return None
        out = {
            "analysisId": row.get("analysis_id"),
            "archiveId": row.get("archive_id"),
            "sourceRawId": row.get("source_raw_id") or 0,
            "overallHealthScore": row.get("overall_health_score"),
            "riskLevel": row.get("risk_level"),
            "analysisSummary": _to_json_value(row.get("analysis_summary")),
        }
        detail = row.get("analysis_detail")
        if detail:
            if isinstance(detail, str):
                try:
                    out["analysisResult"] = json.loads(detail)
                except json.JSONDecodeError:
                    out["analysisResult"] = {"overallAssessment": detail, "dimensionAnalysis": []}
            else:
                out["analysisResult"] = detail
        else:
            out["analysisResult"] = {"overallAssessment": out.get("analysisSummary", ""), "dimensionAnalysis": []}
        return out
