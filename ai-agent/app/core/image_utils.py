"""
图片工具模块
用于处理图片相关的工具函数
"""
import base64
import logging
from io import BytesIO

# 配置日志
logger = logging.getLogger(__name__)


def image_bytes_to_data_url(image_bytes: bytes) -> str:
    """
    将图片的 bytes 转换为 data URL 格式
    
    Args:
        image_bytes: 图片的二进制数据（bytes）
    
    Returns:
        data URL 格式的字符串，格式: data:image/png;base64,{base64_string}
        示例: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
    
    Raises:
        ValueError: 当 image_bytes 为空或无效时
    """
    if not image_bytes:
        raise ValueError("图片数据不能为空")
    
    if not isinstance(image_bytes, bytes):
        raise ValueError(f"图片数据必须是 bytes 类型，当前类型: {type(image_bytes)}")
    
    try:
        # 将 bytes 进行 base64 编码
        base64_string = base64.b64encode(image_bytes).decode("utf-8")
        
        # 构建 data URL
        data_url = f"data:image/png;base64,{base64_string}"
        
        logger.debug(f"图片转换完成，原始大小: {len(image_bytes)} 字节，Base64 长度: {len(base64_string)} 字符")
        
        return data_url
        
    except Exception as e:
        error_msg = f"图片转换为 data URL 失败: {str(e)}"
        logger.error(error_msg)
        raise ValueError(error_msg) from e


def image_bytes_to_llm_data_url(image_bytes: bytes, max_long_edge: int = 1280) -> str:
    """
    将图片转为供视觉 LLM 使用的 data URL；可选按最长边缩小并转 JPEG，降低 Ollama 多图场景下 OOM/500 风险。

    max_long_edge <= 0 时不缩放，行为与 image_bytes_to_data_url 一致（仍标为 png data url）。
    """
    if not image_bytes:
        raise ValueError("图片数据不能为空")
    if max_long_edge <= 0:
        return image_bytes_to_data_url(image_bytes)

    try:
        from PIL import Image

        img = Image.open(BytesIO(image_bytes))
        if img.mode != "RGB":
            img = img.convert("RGB")
        w, h = img.size
        long_edge = max(w, h)
        if long_edge > max_long_edge:
            scale = max_long_edge / float(long_edge)
            nw = max(1, int(round(w * scale)))
            nh = max(1, int(round(h * scale)))
            try:
                resample = Image.Resampling.LANCZOS
            except AttributeError:
                resample = Image.LANCZOS  # type: ignore[attr-defined]
            img = img.resize((nw, nh), resample)
            logger.debug("图片已缩放至 %sx%s（最长边上限 %s）", nw, nh, max_long_edge)
        buf = BytesIO()
        img.save(buf, format="JPEG", quality=88, optimize=True)
        out = buf.getvalue()
        b64 = base64.b64encode(out).decode("utf-8")
        logger.debug(
            "LLM 入模图片 JPEG 字节=%s base64 字符=%s",
            len(out),
            len(b64),
        )
        return f"data:image/jpeg;base64,{b64}"
    except Exception as e:
        logger.warning("图片缩放/转 JPEG 失败，改用原图入模: %s", e)
        return image_bytes_to_data_url(image_bytes)
