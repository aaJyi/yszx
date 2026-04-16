"""
健康档案处理接口客户端
用于推送体检报告数据到健康档案处理 API
"""
import logging
import json
import re
from datetime import date
from typing import Dict, Any, List, Optional

try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False
    logging.warning("requests 库未安装，请安装: pip install requests")

# 配置日志
logger = logging.getLogger(__name__)

try:
    from app.core.config import settings
    BASE_URL = getattr(settings, "health_api_base_url", "http://localhost:8080")
except ImportError:
    BASE_URL = "http://localhost:8080"

# 与库表 health_genetic_history.disease_name varchar(200) 对齐；防止超长写入失败
GENETIC_DISEASE_NAME_MAX_LEN = 200


def _truncate_str_for_db(value: Any, max_len: int, ellipsis: str = "…") -> str:
    """按字符数截断，避免超过 MySQL VARCHAR 长度。"""
    if value is None:
        return ""
    s = str(value).strip()
    if max_len <= 0 or len(s) <= max_len:
        return s
    if max_len <= len(ellipsis):
        return s[:max_len]
    return s[: max_len - len(ellipsis)] + ellipsis


def fetch_health_archive_detail(archive_id: int) -> Optional[Dict[str, Any]]:
    """GET /health-archive-process/detail/{archiveId}，返回 data 映射。"""
    if not HAS_REQUESTS:
        raise ImportError("请安装 requests 库: pip install requests")
    url = f"{BASE_URL}/health-archive-process/detail/{archive_id}"
    try:
        response = requests.get(url, timeout=30)
    except requests.exceptions.RequestException as e:
        logger.warning("获取健康档案详情请求失败 archiveId=%s: %s", archive_id, e)
        return None
    if response.status_code != 200:
        logger.warning(
            "获取健康档案详情 HTTP 异常 archiveId=%s status=%s",
            archive_id,
            response.status_code,
        )
        return None
    try:
        json_data = response.json()
    except ValueError:
        return None
    if json_data.get("code") != 200:
        logger.warning(
            "获取健康档案详情业务失败 archiveId=%s: %s",
            archive_id,
            json_data.get("message"),
        )
        return None
    data = json_data.get("data")
    return data if isinstance(data, dict) else None


def _llm_organize_json_to_archive_detail(archive_json: Dict[str, Any]) -> Dict[str, Any]:
    """
    将智能体「建档」输出的 JSON（basicInfo / diagnoses等）转为与 getHealthArchiveDetail 相近的结构，供 _build_generic_archive_payload 使用。
    """
    archive_json = archive_json or {}
    basic = archive_json.get("basicInfo")
    if not isinstance(basic, dict):
        basic = {}

    name = (
        basic.get("name")
        or basic.get("userName")
        or basic.get("fullName")
        or "用户"
    )
    name = str(name).strip() or "用户"

    health_archive: Dict[str, Any] = {
        "archiveName": basic.get("archiveName") or "AI智能分析健康档案",
        "archiveNo": basic.get("archiveNo") or f"AI-{date.today().strftime('%Y%m%d')}",
        "userName": name,
    }
    if basic.get("archiveDate"):
        health_archive["archiveDate"] = basic["archiveDate"]
    if basic.get("archiveYear") is not None:
        health_archive["archiveYear"] = basic["archiveYear"]

    user_info: Dict[str, Any] = {
        "fullName": name,
        "gender": basic.get("gender") or "未说明的性别",
        "birthDate": basic.get("birthDate") or "1990-01-01",
        "personalPhone": basic.get("phone") or basic.get("personalPhone") or "暂无",
    }
    for k in (
        "idNumber",
        "idType",
        "ethnicity",
        "residenceAddress",
        "educationLevel",
        "occupation",
        "maritalStatus",
        "paymentMethod",
        "residenceType",
        "workSchool",
        "nativePlace",
        "birthPlace",
    ):
        v = basic.get(k)
        if v not in (None, "", []):
            user_info[k] = v

    disease_history: List[Dict[str, Any]] = []
    diagnoses_block = archive_json.get("diagnoses")
    diagnosis_items: List[Any] = []
    if isinstance(diagnoses_block, list):
        diagnosis_items = diagnoses_block
    elif isinstance(diagnoses_block, dict):
        for dkey, dval in diagnoses_block.items():
            if isinstance(dval, dict):
                item = dict(dval)
                if not item.get("name") and not item.get("diagnosisName"):
                    item["name"] = str(dkey)
                diagnosis_items.append(item)
            elif dval is not None and str(dval).strip():
                diagnosis_items.append({"name": str(dkey), "diagnosisName": str(dkey)})

    for d in diagnosis_items:
        if not isinstance(d, dict):
            continue
        nm = d.get("name") or d.get("diagnosisName")
        if not nm:
            continue
        row: Dict[str, Any] = {"diseaseName": str(nm)}
        od = d.get("onsetDate") or d.get("date") or d.get("diagnosisDate")
        row["onsetDate"] = od if od else date.today().isoformat()
        disease_history.append(row)

    for s in archive_json.get("symptoms") or []:
        if isinstance(s, str) and s.strip():
            disease_history.append(
                {
                    "diseaseName": f"症状：{s.strip()}",
                    "onsetDate": date.today().isoformat(),
                }
            )

    out: Dict[str, Any] = {
        "healthArchive": health_archive,
        "userInfo": user_info,
    }
    if disease_history:
        out["diseaseHistory"] = disease_history

    visits = archive_json.get("visits") or []
    if visits:
        try:
            note = json.dumps(visits, ensure_ascii=False)
            if len(note) > 3800:
                note = note[:3800] + "…"
            out.setdefault("exposureHistory", {})
            if isinstance(out["exposureHistory"], dict):
                out["exposureHistory"]["exposureDetails"] = f"就诊/随访记录(JSON)：{note}"
        except Exception:
            pass

    other = archive_json.get("otherImportantInfo")
    if other not in (None, "", []):
        try:
            txt = other if isinstance(other, str) else json.dumps(other, ensure_ascii=False)
            if len(txt) > 2000:
                txt = txt[:2000] + "…"
            out.setdefault("exposureHistory", {})
            if isinstance(out["exposureHistory"], dict):
                prev = str(out["exposureHistory"].get("exposureDetails") or "").strip()
                prefix = f"{prev}\n" if prev else ""
                merged = f"{prefix}【其他重要信息】{txt}"
                if len(merged) > 4000:
                    merged = merged[:4000] + "…"
                out["exposureHistory"]["exposureDetails"] = merged
                out["exposureHistory"].setdefault("exposureType", "无")
            else:
                out["exposureHistory"] = {
                    "exposureType": "无",
                    "exposureDetails": f"【其他重要信息】{txt}"[:4000],
                }
        except Exception:
            pass

    return out


def _merge_llm_archive_detail_into_existing(
    base: Optional[Dict[str, Any]], overlay: Dict[str, Any]
) -> Dict[str, Any]:
    """将 LLM 解析块合并进已有档案详情（保留 archiveId 等主键字段）。"""
    out: Dict[str, Any] = dict(base) if base else {}
    for section, val in (overlay or {}).items():
        if val is None:
            continue
        if section in ("diseaseHistory", "vaccinationHistory", "familyHistory"):
            if isinstance(val, list) and val:
                out[section] = val
            continue
        if isinstance(val, dict) and val:
            existing = out.get(section)
            if isinstance(existing, dict):
                merged = dict(existing)
                for sk, sv in val.items():
                    if sv not in (None, "", []):
                        merged[sk] = sv
                out[section] = merged
            else:
                out[section] = val
        elif isinstance(val, list) and val:
            out[section] = val
    return out


def push_medical_report(user_id: int, report_json: dict) -> Dict[str, Any]:
    """
    将智能体生成的「建档 JSON」写入后端健康档案库。

    - 若用户已有档案：拉取详情后与 LLM 字段合并，走 update/{archiveId}
    - 否则：组装 HealthArchiveRequest 形状，走 process创建

    注意：后端统一返回 HTTP 200，须检查 body 内 code==200，否则此前会误报成功但未落库。
    """
    if not HAS_REQUESTS:
        raise ImportError("请安装 requests 库: pip install requests")

    llm_detail = _llm_organize_json_to_archive_detail(report_json or {})
    existing_id = get_latest_archive_id(user_id)

    if existing_id:
        base_detail = fetch_health_archive_detail(existing_id)
        merged = _merge_llm_archive_detail_into_existing(base_detail, llm_detail)
        logger.info(
            "用户 %s 已有档案 archiveId=%s，使用 update 回填 AI 结构化字段",
            user_id,
            existing_id,
        )
        return update_health_archive_by_ai(user_id, existing_id, merged, None)

    payload = _build_generic_archive_payload(
        archive_detail=llm_detail, analysis_result=None
    )
    url = f"{BASE_URL}/health-archive-process/process?userId={user_id}"
    headers = {
        "Content-Type": "application/json",
        "userId": str(user_id),
    }

    try:
        response = requests.post(url, json=payload, headers=headers, timeout=60)
    except requests.exceptions.RequestException as e:
        raise RuntimeError(f"健康档案处理 API 请求失败: {e}") from e

    if response.status_code != 200:
        body_preview = (response.text or "")[:800]
        logger.error(
            "健康档案处理 API HTTP 异常，status=%s, url=%s, response=%s",
            response.status_code,
            url,
            body_preview,
        )
        raise RuntimeError(
            f"健康档案处理 API 返回异常，status={response.status_code}，response={body_preview}"
        )

    try:
        data = response.json()
    except ValueError:
        body_preview = (response.text or "")[:800]
        logger.error("健康档案处理 API 返回非 JSON，response=%s", body_preview)
        raise RuntimeError(f"健康档案处理 API 返回非 JSON，response={body_preview}")

    if data.get("code") != 200:
        msg = data.get("message", "")
        logger.error(
            "健康档案处理业务失败 code=%s message=%s data=%s",
            data.get("code"),
            msg,
            str(data)[:500],
        )
        raise RuntimeError(f"健康档案处理失败 code={data.get('code')}: {msg}")

    logger.info(
        "健康档案已落库（新建）userId=%s archiveId=%s",
        user_id,
        (data.get("data") or {}).get("archiveId"),
    )
    return data


def submit_health_analysis(user_id: int, analysis_payload: dict) -> Dict[str, Any]:
    """
    提交健康分析结果到健康分析结果提交接口
    
    Args:
        user_id: 用户ID（路径参数）
        analysis_payload: 分析结果负载字典，应包含以下字段：
            - archiveId: Integer（必填）- 档案ID
            - sourceRawId: Long（必填）- 原始数据ID
            - modelName: String（必填）- 模型名称
            - modelVersion: String（必填）- 模型版本
            - analysisResult: Object（必填）- AI分析结果对象
            - recommendations: List[Object]（可选）- 健康建议列表
    
    Returns:
        API 响应的 JSON 数据（response.json()）
    
    Raises:
        ImportError: 当 requests 库未安装时
        RuntimeError: 当网络异常、HTTP 状态码异常或 JSON 解析失败时
    """
    if not HAS_REQUESTS:
        raise ImportError("请安装 requests 库: pip install requests")
    
    # 构建请求 URL（userId 在路径参数中，不在 JSON 请求体中）
    url = f"{BASE_URL}/health-ai-analysis/submit/user/{user_id}"
    
    # 设置请求头
    headers = {
        "Content-Type": "application/json"
    }
    
    try:
        # 发送 POST 请求（健康分析保存+扩展数据+建议生成可能较慢，超时设为 120 秒）
        response = requests.post(
            url,
            json=analysis_payload,
            headers=headers,
            timeout=120
        )
    except requests.exceptions.RequestException:
        raise RuntimeError("健康分析结果提交 API 请求失败")
    
    # 检查 HTTP 状态码
    if response.status_code != 200:
        raise RuntimeError("健康分析结果提交 API 返回异常")
    
    # 解析 JSON 响应
    try:
        return response.json()
    except ValueError:
        raise RuntimeError("健康分析结果提交 API 返回非 JSON")


def fetch_user_health_data(user_id: int) -> Dict[str, Any]:
    """
    获取用户的完整健康数据（包括健康档案）
    
    使用 /health-ai-analysis/data/user/{userId} 接口获取用户的完整数据。
    
    Args:
        user_id: 用户ID
    
    Returns:
        用户的完整健康数据字典，包含：
        - rawHealthDataList: 原始健康数据列表
        - healthArchives: 健康档案列表
        如果不存在则返回 None
    
    Raises:
        ImportError: 当 requests 库未安装时
        RuntimeError: 当网络异常、HTTP 状态码异常或 JSON 解析失败时
    """
    if not HAS_REQUESTS:
        raise ImportError("请安装 requests 库: pip install requests")
    
    # 构建请求 URL
    url = f"{BASE_URL}/health-ai-analysis/data/user/{user_id}"
    
    try:
        # 发送 GET 请求
        response = requests.get(url, timeout=5)
    except requests.exceptions.RequestException as e:
        error_msg = f"用户健康数据查询 API 请求失败: {str(e)}"
        logger.error(error_msg)
        raise RuntimeError(error_msg) from e
    
    # 检查 HTTP 状态码
    if response.status_code == 404:
        logger.info(f"用户 {user_id} 没有健康数据")
        return None
    
    if response.status_code != 200:
        error_msg = f"用户健康数据查询 API 返回异常，状态码: {response.status_code}"
        logger.error(error_msg)
        raise RuntimeError(error_msg)
    
    # 解析 JSON 响应
    try:
        json_data = response.json()
        
        # 检查响应中的 code 字段
        if json_data.get("code") != 200:
            code = json_data.get("code", "unknown")
            message = json_data.get("message", "未知错误")
            if code == 404:
                logger.info(f"用户 {user_id} 没有健康数据")
                return None
            error_msg = f"API 返回错误，code: {code}, message: {message}"
            logger.error(error_msg)
            raise RuntimeError(error_msg)
        
        # 提取 data 字段
        data = json_data.get("data")
        if data is None:
            logger.info(f"用户 {user_id} 没有健康数据")
            return None
        
        logger.info(f"成功获取用户 {user_id} 的健康数据")
        return data
        
    except ValueError as e:
        error_msg = f"用户健康数据查询 API 返回非 JSON: {str(e)}"
        logger.error(error_msg)
        raise RuntimeError(error_msg) from e


def get_latest_archive_id(user_id: int) -> Optional[int]:
    """
    获取用户最新的健康档案ID
    
    Args:
        user_id: 用户ID
    
    Returns:
        最新的健康档案ID，如果不存在则返回 None
    """
    try:
        data = fetch_user_health_data(user_id)
        if not data:
            return None
        
        health_archives = data.get("healthArchives", [])
        if not health_archives:
            logger.info(f"用户 {user_id} 没有健康档案")
            return None
        
        # 获取第一个（最新的）健康档案的 archiveId
        latest_archive = health_archives[0]
        archive_info = latest_archive.get("healthArchive", {})
        archive_id = archive_info.get("archiveId")
        
        if archive_id:
            logger.info(f"获取到用户 {user_id} 的最新健康档案ID: {archive_id}")
            return archive_id
        else:
            logger.warning(f"用户 {user_id} 的健康档案中没有 archiveId")
            return None
            
    except Exception as e:
        logger.warning(f"获取用户 {user_id} 的健康档案ID失败: {str(e)}")
        return None


def fetch_latest_health_analysis(user_id: int) -> Dict[str, Any]:
    """
    获取用户最新的健康分析结果
    
    Args:
        user_id: 用户ID
    
    Returns:
        最新的健康分析结果字典，如果不存在则返回 None
    
    Raises:
        ImportError: 当 requests 库未安装时
        RuntimeError: 当网络异常、HTTP 状态码异常或 JSON 解析失败时
    """
    if not HAS_REQUESTS:
        raise ImportError("请安装 requests 库: pip install requests")
    
    # 构建请求 URL（假设接口为 GET /health-ai-analysis/user/{userId}/latest）
    url = f"{BASE_URL}/health-ai-analysis/user/{user_id}/latest"
    
    try:
        # 发送 GET 请求
        response = requests.get(url, timeout=5)
    except requests.exceptions.RequestException as e:
        error_msg = f"健康分析结果查询 API 请求失败: {str(e)}"
        logger.error(error_msg)
        raise RuntimeError(error_msg) from e
    
    # 检查 HTTP 状态码
    if response.status_code == 404:
        # 用户没有健康分析结果
        logger.info(f"用户 {user_id} 没有健康分析结果")
        return None
    
    if response.status_code != 200:
        error_msg = f"健康分析结果查询 API 返回异常，状态码: {response.status_code}"
        logger.error(error_msg)
        raise RuntimeError(error_msg)
    
    # 解析 JSON 响应
    try:
        json_data = response.json()
        
        # 检查响应中的 code 字段
        if json_data.get("code") != 200:
            code = json_data.get("code", "unknown")
            message = json_data.get("message", "未知错误")
            if code == 404:
                logger.info(f"用户 {user_id} 没有健康分析结果")
                return None
            error_msg = f"API 返回错误，code: {code}, message: {message}"
            logger.error(error_msg)
            raise RuntimeError(error_msg)
        
        # 提取 data 字段
        data = json_data.get("data")
        if data is None:
            logger.info(f"用户 {user_id} 没有健康分析结果")
            return None
        
        logger.info(f"成功获取用户 {user_id} 的最新健康分析结果")
        return data
        
    except ValueError as e:
        error_msg = f"健康分析结果查询 API 返回非 JSON: {str(e)}"
        logger.error(error_msg)
        raise RuntimeError(error_msg) from e


def submit_health_recommendation(user_id: int, recommendation_payload: dict) -> Dict[str, Any]:
    """
    提交健康建议结果到健康分析结果提交接口
    
    根据接口文档，使用 /health-ai-analysis/submit/user/{userId} 接口提交建议。
    该接口会同时保存分析结果和建议到数据库。
    
    Args:
        user_id: 用户ID（路径参数）
        recommendation_payload: 健康建议负载字典，应包含以下字段：
            - archiveId: Integer（必填）- 档案ID
            - sourceRawId: Long（必填）- 原始数据ID
            - modelName: String（必填）- 模型名称
            - modelVersion: String（必填）- 模型版本
            - analysisResult: Object（必填）- AI分析结果对象
            - recommendations: List[Object]（可选）- 健康建议列表
    
    Returns:
        API 响应的 JSON 数据（response.json()）
    
    Raises:
        ImportError: 当 requests 库未安装时
        RuntimeError: 当网络异常、HTTP 状态码异常或 JSON 解析失败时
    """
    if not HAS_REQUESTS:
        raise ImportError("请安装 requests 库: pip install requests")
    
    # 构建请求 URL（使用接口文档中的接口路径）
    url = f"{BASE_URL}/health-ai-analysis/submit/user/{user_id}"
    
    # 设置请求头
    headers = {
        "Content-Type": "application/json"
    }
    
    try:
        # 发送 POST 请求（健康建议提交可能较慢，超时设为 120 秒）
        response = requests.post(
            url,
            json=recommendation_payload,
            headers=headers,
            timeout=120
        )
    except requests.exceptions.RequestException as e:
        error_msg = f"健康建议提交 API 请求失败: {str(e)}"
        logger.error(error_msg)
        raise RuntimeError(error_msg) from e
    
    # 检查 HTTP 状态码
    if response.status_code != 200:
        error_msg = f"健康建议提交 API 返回异常，状态码: {response.status_code}, 响应: {response.text[:200]}"
        logger.error(error_msg)
        raise RuntimeError(error_msg)
    
    # 解析 JSON 响应
    try:
        json_data = response.json()
        
        # 检查响应中的 code 字段
        if json_data.get("code") != 200:
            code = json_data.get("code", "unknown")
            message = json_data.get("message", "未知错误")
            error_msg = f"API 返回错误，code: {code}, message: {message}"
            logger.error(error_msg)
            raise RuntimeError(error_msg)
        
        logger.info(f"成功提交用户 {user_id} 的健康建议")
        return json_data
        
    except ValueError as e:
        error_msg = f"健康建议提交 API 返回非 JSON: {str(e)}"
        logger.error(error_msg)
        raise RuntimeError(error_msg) from e


def convert_recommendation_result_to_api_format(recommendation_result: Dict[str, Any]) -> List[Dict[str, Any]]:
    """
    将 recommendationResult 格式转换为接口文档要求的 recommendations 数组格式
    
    Args:
        recommendation_result: recommendationResult 对象，包含：
            - recommendationMeta: Object
            - overallAdvice: Object
            - recommendationItems: List[Object]
            - notice: Object
    
    Returns:
        转换后的 recommendations 数组，格式符合接口文档要求
    """
    recommendations = []
    
    recommendation_items = recommendation_result.get("recommendationItems", [])
    overall_advice = recommendation_result.get("overallAdvice", {})
    if isinstance(overall_advice, str) and overall_advice.strip():
        overall_advice = {
            "summary": overall_advice.strip()[:8000],
            "priorityLevel": "高" if any(x in overall_advice for x in ("严重", "重度", "紧急", "自杀")) else "中",
        }
    elif not isinstance(overall_advice, dict):
        overall_advice = {}
    if not isinstance(recommendation_items, list):
        recommendation_items = []
    
    # category 到 recommendationType 的映射
    category_to_type = {
        "生活方式": "LIFESTYLE",
        "饮食": "DIET",
        "运动": "EXERCISE",
        "医疗": "MEDICAL",
        "复查建议": "FOLLOW_UP",
        "风险提示": "SERVICE",
        "心理健康": "LIFESTYLE",
        "service": "SERVICE",
        "Service": "SERVICE",
    }
    
    # 转换每个 recommendationItem
    for item in recommendation_items:
        if isinstance(item, str) and item.strip():
            recommendations.append({
                "recommendationType": "LIFESTYLE",
                "title": "健康建议",
                "content": item.strip()[:8000],
                "priority": overall_advice.get("priorityLevel", "中"),
                "dimensionCode": None,
            })
            continue
        if not isinstance(item, dict):
            continue
        category = item.get("category", "")
        if isinstance(category, str) and "|" in category:
            category = category.split("|")[0].strip()
        if isinstance(category, str):
            category = category.strip().strip("'\"")
        recommendation_type = category_to_type.get(category, "LIFESTYLE")
        
        # 字段映射：
        # category -> recommendationType (已映射)
        # title -> title
        # content -> content
        # 优先使用 item 中的 priority，否则使用 overallAdvice.priorityLevel
        priority = item.get("priority") or overall_advice.get("priorityLevel", "中")
        
        recommendation = {
            "recommendationType": recommendation_type,
            "title": str(item.get("title") or "")[:500],
            "content": str(item.get("content") or "")[:8000],
            "priority": priority,
            "dimensionCode": item.get("dimensionCode")  # 如果存在则使用，否则为 None
        }
        
        recommendations.append(recommendation)
    
    # 如果没有 recommendationItems，但 overallAdvice 有内容，创建一个总体建议
    summary_fallback = overall_advice.get("summary") or overall_advice.get("overallAdvice")
    if isinstance(summary_fallback, str) and summary_fallback.strip():
        summary_fallback = summary_fallback.strip()
    else:
        summary_fallback = ""
    if not recommendations and summary_fallback:
        recommendations.append({
            "recommendationType": "LIFESTYLE",
            "title": "总体健康建议",
            "content": summary_fallback[:8000],
            "priority": overall_advice.get("priorityLevel", "中"),
            "dimensionCode": None
        })
    
    return recommendations


def _to_profile_list(value: Any) -> Optional[List[str]]:
    """将 profile tag 字段兼容转换为字符串列表。"""
    if value is None:
        return None
    if isinstance(value, list):
        return [str(v) for v in value if v is not None]
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return None
        if text.startswith("[") and text.endswith("]"):
            try:
                parsed = json.loads(text)
                if isinstance(parsed, list):
                    return [str(v) for v in parsed if v is not None]
            except Exception:
                pass
        return [text]
    return [str(value)]


def _pick(existing: Dict[str, Any], key: str, default_value: Any) -> Any:
    """优先使用已有值，缺失时使用默认值。"""
    value = existing.get(key) if isinstance(existing, dict) else None
    return default_value if value is None else value


def _normalize_enum(value: Any, allowed: List[str], alias: Optional[Dict[str, str]] = None, default_value: Any = None) -> Any:
    """将输入值归一化为数据库允许的 ENUM 值，不可识别则返回默认值。"""
    if value is None:
        return default_value
    text = str(value).strip()
    if not text:
        return default_value
    if text in allowed:
        return text
    if alias and text in alias:
        mapped = alias[text]
        if mapped in allowed:
            return mapped
    return default_value


def _json_text(value: Any, default_obj: Any) -> str:
    """将值转为可写入 MySQL JSON 列的 JSON 文本。"""
    target = default_obj if value is None else value
    try:
        if isinstance(target, str):
            text = target.strip()
            # 若已是 JSON 文本则直接使用
            if text and ((text.startswith("{") and text.endswith("}")) or (text.startswith("[") and text.endswith("]"))):
                json.loads(text)
                return text
            # 普通字符串转为 JSON 字符串
            return json.dumps(text, ensure_ascii=False)
        return json.dumps(target, ensure_ascii=False)
    except Exception:
        return json.dumps(default_obj, ensure_ascii=False)


def _build_generic_archive_payload(archive_detail: Dict[str, Any], analysis_result: Optional[Dict[str, Any]]) -> Dict[str, Any]:
    """
    构建“全量主从表”更新 payload：
    - 优先使用用户上传后已有数据
    - 次优先使用 AI 分析的 extendedData
    - 兜底使用大众化默认值
    """
    archive_detail = archive_detail or {}
    health_archive = archive_detail.get("healthArchive") or {}
    profile_tags = archive_detail.get("healthProfileTags") or {}
    user_info = archive_detail.get("userInfo") or {}
    user_emergency_contacts = archive_detail.get("userEmergencyContacts") or {}
    user_certificates = archive_detail.get("userCertificates") or {}
    allergy_history = archive_detail.get("allergyHistory") or {}
    exposure_history = archive_detail.get("exposureHistory") or {}
    disease_history = archive_detail.get("diseaseHistory") or []
    vaccination_history = archive_detail.get("vaccinationHistory") or []
    family_history = archive_detail.get("familyHistory") or []
    genetic_history = archive_detail.get("geneticHistory") or {}
    disability_history = archive_detail.get("disabilityHistory") or {}

    # 兼容 detail 返回中的 list 别名
    if not disease_history:
        disease_history = archive_detail.get("diseaseHistoryList") or []
    if not vaccination_history:
        vaccination_history = archive_detail.get("vaccinationHistoryList") or []
    if not family_history:
        family_history = archive_detail.get("familyHistoryList") or []

    extended_data = (analysis_result or {}).get("extendedData", {}) if isinstance(analysis_result, dict) else {}
    lifestyle = extended_data.get("lifestyleStatus", {}) if isinstance(extended_data, dict) else {}
    social = extended_data.get("socialRelationshipAssessment", {}) if isinstance(extended_data, dict) else {}

    gender_allowed = ["男", "女", "未说明的性别", "未知的性别"]
    residence_allowed = ["户籍", "非户籍"]
    education_allowed = ["研究生", "大学本科", "大学专科和专科学校", "中等专业学校", "技工学校", "高中", "初中", "小学", "文盲或半文盲", "不详"]
    occupation_allowed = [
        "党的机关、国家机关、群众团体和社会组织、企事业单位负责人",
        "专业技术人员",
        "办事人员和有关人员",
        "社会生产服务和生活服务人员",
        "农、林、牧、渔业生产及辅助人员",
        "生产制造及有关人员",
        "军队人员",
        "不便分类的其他从业人员",
        "无职业",
        "学生"
    ]
    marital_allowed = ["未婚", "已婚", "丧偶", "离婚", "未说明的婚姻状况"]
    payment_allowed = ["城镇职工基本医疗保险", "城乡居民基本医疗保险", "医疗救助", "商业医疗保险", "公费", "自费", "其他"]
    genetic_allowed = ["无", "有"]

    gender_alias = {"未知": "未知的性别", "未说明": "未说明的性别", "1": "男", "2": "女"}
    residence_alias = {"常住": "户籍", "户籍常住": "户籍", "流动": "非户籍", "非本地": "非户籍"}
    education_alias = {
        "硕士": "研究生",
        "博士": "研究生",
        "硕士及以上": "研究生",
        "大专": "大学专科和专科学校",
        "中专": "中等专业学校",
        "小学或初中": "初中",
        "本科": "大学本科"
    }
    occupation_alias = {
        "其他": "不便分类的其他从业人员",
        "离职": "无职业"
    }
    marital_alias = {"未知": "未说明的婚姻状况"}
    payment_alias = {
        "医保": "城乡居民基本医疗保险",
        "医疗保险": "城乡居民基本医疗保险",
        "居民医保": "城乡居民基本医疗保险",
        "职工医保": "城镇职工基本医疗保险",
        "未说明": "自费"
    }
    genetic_alias = {"否": "无", "没有": "无", "无遗传病": "无", "是": "有", "存在": "有"}

    # 主表（尽量保留原始档案元信息）
    health_archive_payload = {
        "archiveNo": _pick(health_archive, "archiveNo", f"AI-{date.today().strftime('%Y%m%d')}"),
        "archiveName": _pick(health_archive, "archiveName", "AI智能分析健康档案"),
        "userName": _pick(health_archive, "userName", _pick(user_info, "fullName", "用户")),
        "archiveDate": _pick(health_archive, "archiveDate", date.today().isoformat()),
        "archiveYear": _pick(health_archive, "archiveYear", date.today().year),
        "archiveManagerName": _pick(health_archive, "archiveManagerName", "社区健康管理中心"),
        "archiveManagerPhone": _pick(health_archive, "archiveManagerPhone", "400-000-0000"),
    }

    profile_payload = {
        "isChild06": _pick(profile_tags, "isChild06", False),
        "isElderly65": _pick(profile_tags, "isElderly65", False),
        "isPregnant": _pick(profile_tags, "isPregnant", False),
        "pregnancyRisk": _pick(profile_tags, "pregnancyRisk", "低风险"),
        "weightStatus": _pick(profile_tags, "weightStatus", lifestyle.get("weightStatus") if isinstance(lifestyle, dict) else "正常"),
        "bloodType": _pick(profile_tags, "bloodType", "不详"),
        "chronicDisease": _to_profile_list(profile_tags.get("chronicDisease")) or ["暂无明确慢病"],
        "statutoryInfo": _to_profile_list(profile_tags.get("statutoryInfo")) or ["无"],
    }

    user_info_payload = {
        "fullName": _pick(user_info, "fullName", health_archive_payload["userName"]),
        "gender": _normalize_enum(
            _pick(user_info, "gender", "未说明的性别"),
            gender_allowed,
            gender_alias,
            "未说明的性别"
        ),
        "birthDate": _pick(user_info, "birthDate", "1990-01-01"),
        "idType": _pick(user_info, "idType", "身份证"),
        "idNumber": _pick(user_info, "idNumber", "000000000000000000"),
        "workSchool": _pick(user_info, "workSchool", "暂无"),
        "nativePlace": _pick(user_info, "nativePlace", "暂无"),
        "birthPlace": _pick(user_info, "birthPlace", "暂无"),
        "ethnicity": _pick(user_info, "ethnicity", "汉族"),
        "familyDoctorSigned": _pick(user_info, "familyDoctorSigned", False),
        "familyDoctorName": _pick(user_info, "familyDoctorName", "未签约"),
        "familyDoctorPhone": _pick(user_info, "familyDoctorPhone", "暂无"),
        "personalPhone": _pick(user_info, "personalPhone", "暂无"),
        "residenceType": _normalize_enum(
            _pick(user_info, "residenceType", "户籍"),
            residence_allowed,
            residence_alias,
            "户籍"
        ),
        "residenceAddress": _pick(user_info, "residenceAddress", "暂无"),
        "educationLevel": _normalize_enum(
            _pick(user_info, "educationLevel", social.get("educationLevel") if isinstance(social, dict) else "不详"),
            education_allowed,
            education_alias,
            "不详"
        ),
        "occupation": _normalize_enum(
            _pick(user_info, "occupation", social.get("occupationType") if isinstance(social, dict) else "无职业"),
            occupation_allowed,
            occupation_alias,
            "无职业"
        ),
        "maritalStatus": _normalize_enum(
            _pick(user_info, "maritalStatus", social.get("maritalStatus") if isinstance(social, dict) else "未说明的婚姻状况"),
            marital_allowed,
            marital_alias,
            "未说明的婚姻状况"
        ),
        "paymentMethod": _normalize_enum(
            _pick(user_info, "paymentMethod", social.get("medicalPaymentMethod") if isinstance(social, dict) else "自费"),
            payment_allowed,
            payment_alias,
            "自费"
        ),
    }

    emergency_payload = {
        "contactName": _pick(user_emergency_contacts, "contactName", "家属"),
        "relationship": _pick(user_emergency_contacts, "relationship", "家人"),
        "phoneNumber": _pick(user_emergency_contacts, "phoneNumber", "暂无"),
    }

    certificate_payload = {
        "certificateType": _pick(user_certificates, "certificateType", "医保电子凭证"),
        "certificateIssuer": _pick(user_certificates, "certificateIssuer", "医保局"),
        "issueDate": _pick(user_certificates, "issueDate", date.today().isoformat()),
        "expiryDate": user_certificates.get("expiryDate"),
    }

    allergy_payload = {
        "allergyType": _pick(allergy_history, "allergyType", "无"),
        # DB 列是 JSON 类型，必须传合法 JSON 文本
        "drugAllergyDetails": _json_text(_pick(allergy_history, "drugAllergyDetails", None), []),
        "foodAllergyDetails": _pick(allergy_history, "foodAllergyDetails", "无"),
        "otherAllergyDetails": _pick(allergy_history, "otherAllergyDetails", "无"),
    }

    exposure_payload = {
        "exposureType": _pick(exposure_history, "exposureType", "无"),
        "exposureDetails": _pick(exposure_history, "exposureDetails", "无特殊暴露史"),
    }

    disease_payload = disease_history if disease_history else [{
        "diseaseName": "暂无明确既往疾病",
        "onsetDate": date.today().isoformat()
    }]

    vaccination_payload = vaccination_history if vaccination_history else [{
        "vaccineName": "按国家免疫规划接种",
        "vaccinationDate": date.today().isoformat()
    }]

    family_payload = family_history if family_history else [{
        "relativeType": "其他",
        "diseases": _json_text(None, ["无明确家族遗传病史"])
    }]
    # family_history.diseases 也是 JSON 列，统一规范化
    normalized_family_payload = []
    for item in family_payload:
        row = item if isinstance(item, dict) else {}
        normalized_family_payload.append({
            "relativeType": _normalize_enum(
                row.get("relativeType"),
                ["父亲", "母亲", "兄弟姐妹", "子女", "祖父", "祖母", "外祖父", "外祖母", "其他"],
                {"直系亲属": "其他", "家人": "其他"},
                "其他"
            ),
            "diseases": _json_text(row.get("diseases"), ["无明确家族遗传病史"])
        })

    raw_genetic_name = _pick(genetic_history, "diseaseName", "无")
    raw_g_str = str(raw_genetic_name).strip() if raw_genetic_name is not None else ""
    genetic_disease = _truncate_str_for_db(raw_g_str, GENETIC_DISEASE_NAME_MAX_LEN)
    if len(raw_g_str) > GENETIC_DISEASE_NAME_MAX_LEN:
        logger.warning(
            "遗传史 diseaseName 已由 %s 字符截断至 %s",
            len(raw_g_str),
            GENETIC_DISEASE_NAME_MAX_LEN,
        )
    genetic_payload = {
        "hasGeneticDisease": _normalize_enum(
            _pick(genetic_history, "hasGeneticDisease", "无"),
            genetic_allowed,
            genetic_alias,
            "无"
        ),
        "diseaseName": genetic_disease or "无",
    }

    disability_payload = {
        "disabilityTypes": _pick(disability_history, "disabilityTypes", "无"),
    }

    return {
        "healthArchive": health_archive_payload,
        "healthProfileTags": profile_payload,
        "userInfo": user_info_payload,
        "userEmergencyContacts": emergency_payload,
        "userCertificates": certificate_payload,
        "allergyHistory": allergy_payload,
        "exposureHistory": exposure_payload,
        "diseaseHistory": disease_payload,
        "vaccinationHistory": vaccination_payload,
        "familyHistory": normalized_family_payload,
        "geneticHistory": genetic_payload,
        "disabilityHistory": disability_payload,
    }


def update_health_archive_by_ai(
    user_id: int,
    archive_id: int,
    archive_detail: Dict[str, Any],
    analysis_result: Optional[Dict[str, Any]] = None
) -> Dict[str, Any]:
    """
    将智能体分析后的结构化信息回填到健康档案主表/从表（按 archiveId）。
    """
    if not HAS_REQUESTS:
        raise ImportError("请安装 requests 库: pip install requests")

    url = f"{BASE_URL}/health-archive-process/update/{archive_id}"
    headers = {
        "Content-Type": "application/json",
        "userId": str(user_id)
    }

    payload = _build_generic_archive_payload(archive_detail=archive_detail, analysis_result=analysis_result)

    # 若遇到 ENUM 截断错误，自动删除触发字段后重试，避免整单回填失败
    for attempt in range(3):
        try:
            response = requests.post(url, json=payload, headers=headers, timeout=30)
        except requests.exceptions.RequestException as e:
            error_msg = f"健康档案回填 API 请求失败: {str(e)}"
            logger.error(error_msg)
            raise RuntimeError(error_msg) from e

        if response.status_code != 200:
            error_msg = f"健康档案回填 API 返回异常，状态码: {response.status_code}, 响应: {response.text[:200]}"
            logger.error(error_msg)
            raise RuntimeError(error_msg)

        try:
            json_data = response.json()
        except ValueError as e:
            error_msg = f"健康档案回填 API 返回非 JSON: {str(e)}"
            logger.error(error_msg)
            raise RuntimeError(error_msg) from e

        if json_data.get("code") == 200:
            logger.info(f"成功回填用户 {user_id} 的健康档案，archiveId={archive_id}")
            return json_data

        code = json_data.get("code", "unknown")
        message = json_data.get("message", "未知错误")
        # 匹配 Data truncated for column 'xxx'
        match = re.search(r"Data truncated for column '([^']+)'", str(message))
        if code == 400 and match:
            col = match.group(1)
            col_map = {
                "residence_type": ("userInfo", "residenceType"),
                "education_level": ("userInfo", "educationLevel"),
                "marital_status": ("userInfo", "maritalStatus"),
                "payment_method": ("userInfo", "paymentMethod"),
                "gender": ("userInfo", "gender"),
                "id_type": ("userInfo", "idType"),
            }
            target = col_map.get(col)
            if target:
                section, field = target
                if isinstance(payload.get(section), dict) and field in payload[section]:
                    payload[section][field] = None
                    logger.warning(f"回填字段 {section}.{field} 与库枚举不兼容，已置空后重试（第{attempt + 1}次）")
                    continue

        error_msg = f"健康档案回填失败，code: {code}, message: {message}"
        logger.error(error_msg)
        raise RuntimeError(error_msg)

    raise RuntimeError("健康档案回填失败：多次重试后仍存在字段枚举不兼容")