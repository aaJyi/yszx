"""
应用配置
"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置"""
    app_name: str = "医疗咨询对话系统"
    app_version: str = "0.1.0"
    debug: bool = False
    
    # API 配置
    api_prefix: str = "/api/v1"
    
    # 本机 MySQL 健康档案数据库配置（提问时直接查询本机数据库）
    db_host: str = "localhost"
    db_port: int = 3306
    db_user: str = "root"
    db_password: str = "sjt"
    db_name: str = "Medical Vision Intelligent Travel"
    
    # 原始健康数据/健康档案处理/重建 的 HTTP API 地址（Android 后端，默认 8080）
    # 可通过 .env 设置 HEALTH_API_BASE_URL=http://localhost:8081
    health_api_base_url: str = "http://localhost:8080"

    # OpenAI 兼容 LLM（默认本地 Ollama：http://127.0.0.1:11434/v1）
    llm_api_key: str = "ollama"
    llm_base_url: str = "http://127.0.0.1:11434/v1"
    # 16GB 内存笔记本推荐：qwen2.5vl:3b（约 3.2GB，图文多模态，见 https://ollama.com/library/qwen2.5vl ）
    llm_model: str = "qwen2.5vl:3b"
    # 纯文本任务（如健康建议）：勿用 VL 模型跑超长无图 prompt；可通过 LLM_MODEL_TEXT 覆盖
    llm_model_text: str = "deepseek-r1:1.5b"
    # 多图/长提示词推理较慢，避免默认短超时断开
    llm_http_timeout_seconds: float = 600.0
    # 向 Ollama 透传 options.num_gpu；部分版本会因此对 /v1/chat/completions 直接 500，故默认关闭
    llm_ollama_runtime_options: bool = False
    # Ollama options.num_gpu：较大值表示尽量把权重层放到 GPU（仍依赖本机 CUDA 与驱动）
    llm_ollama_num_gpu: int = 99
    # 入模前将图片最长边限制到此值（像素），减轻本地 Ollama 显存/内存与 vision 编码压力；0=不缩放
    llm_image_max_long_edge: int = 1280
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
