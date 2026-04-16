"""
PDF 转图片工具模块
用于将 PDF 文件的每一页转换为 PNG 图片
"""
import io
import logging
from typing import List

try:
    import fitz  # PyMuPDF
    HAS_PYMUPDF = True
except ImportError:
    HAS_PYMUPDF = False
    logging.warning("PyMuPDF 库未安装，请安装: pip install PyMuPDF")

# 配置日志
logger = logging.getLogger(__name__)


def pdf_bytes_to_images(pdf_bytes: bytes) -> List[bytes]:
    """
    将 PDF 文件的每一页转换为 PNG 图片
    
    Args:
        pdf_bytes: PDF 文件的二进制数据（bytes）
    
    Returns:
        每一页图片的 PNG bytes 列表，按页顺序排列
        示例: [page1_png_bytes, page2_png_bytes, ...]
    
    Raises:
        ImportError: 当 PyMuPDF 库未安装时
        ValueError: 当 PDF 数据无效或无法解析时
        RuntimeError: 当转换过程中发生错误时
    """
    if not HAS_PYMUPDF:
        raise ImportError("请安装 PyMuPDF 库: pip install PyMuPDF")
    
    if not pdf_bytes:
        raise ValueError("PDF 数据不能为空")
    
    logger.debug(f"开始转换 PDF，数据大小: {len(pdf_bytes)} 字节")
    
    try:
        # 将 bytes 转换为文件对象
        pdf_stream = io.BytesIO(pdf_bytes)
        
        # 打开 PDF 文档
        pdf_document = fitz.open(stream=pdf_stream, filetype="pdf")
        
        # 获取总页数
        page_count = len(pdf_document)
        logger.debug(f"PDF 共有 {page_count} 页")
        
        if page_count == 0:
            pdf_document.close()
            raise ValueError("PDF 文件没有页面")
        
        # 存储所有页面的图片 bytes
        image_bytes_list = []
        
        # 遍历每一页
        for page_num in range(page_count):
            try:
                # 获取页面
                page = pdf_document[page_num]
                
                # 将页面转换为像素图（Pixmap）
                # matrix 参数控制缩放比例，默认 1.0 表示原始大小
                # 可以调整 matrix=fitz.Matrix(2, 2) 来增加分辨率（2倍）
                pix = page.get_pixmap(matrix=fitz.Matrix(1, 1))
                
                # 将像素图转换为 PNG 格式的 bytes
                png_bytes = pix.tobytes("png")
                
                image_bytes_list.append(png_bytes)
                logger.debug(f"第 {page_num + 1} 页转换完成，图片大小: {len(png_bytes)} 字节")
                
                # 释放内存
                pix = None
                
            except Exception as e:
                error_msg = f"转换第 {page_num + 1} 页时出错: {str(e)}"
                logger.error(error_msg)
                pdf_document.close()
                raise RuntimeError(error_msg) from e
        
        # 关闭 PDF 文档
        pdf_document.close()
        
        logger.info(f"PDF 转换完成，共 {len(image_bytes_list)} 页")
        return image_bytes_list
        
    except fitz.FileDataError as e:
        error_msg = f"PDF 文件数据无效或损坏: {str(e)}"
        logger.error(error_msg)
        raise ValueError(error_msg) from e
        
    except Exception as e:
        error_msg = f"PDF 转图片时发生未知错误: {str(e)}"
        logger.error(error_msg, exc_info=True)
        raise RuntimeError(error_msg) from e
