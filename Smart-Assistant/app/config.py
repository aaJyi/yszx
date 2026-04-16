import os
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()


def _env(name: str, default: str | None = None) -> str:
    v = os.getenv(name, default)
    if v is None:
        raise RuntimeError(f"Missing env var: {name}")
    return v


@dataclass(frozen=True)
class Settings:
    app_host: str = _env("APP_HOST", "127.0.0.1")
    app_port: int = int(_env("APP_PORT", "8000"))

    mysql_host: str = _env("MYSQL_HOST", "127.0.0.1")
    mysql_port: int = int(_env("MYSQL_PORT", "3307"))
    mysql_user: str = _env("MYSQL_USER", "sa")
    mysql_password: str = _env("MYSQL_PASSWORD", "sa_pass")
    mysql_db: str = _env("MYSQL_DB", "smart_assistant")

    rabbitmq_host: str = _env("RABBITMQ_HOST", "127.0.0.1")
    rabbitmq_port: int = int(_env("RABBITMQ_PORT", "5673"))
    rabbitmq_user: str = _env("RABBITMQ_USER", "sa")
    rabbitmq_password: str = _env("RABBITMQ_PASSWORD", "sa_pass")
    rabbitmq_queue: str = _env("RABBITMQ_QUEUE", "sa_tasks")

    ollama_base_url: str = _env("OLLAMA_BASE_URL", "http://127.0.0.1:11434")
    ollama_model: str = _env("OLLAMA_MODEL", "qwen2.5:7b-instruct")

    search_user_agent: str = _env(
        "SEARCH_USER_AGENT",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123 Safari/537.36",
    )
    search_timeout_secs: int = int(_env("SEARCH_TIMEOUT_SECS", "20"))


settings = Settings()

