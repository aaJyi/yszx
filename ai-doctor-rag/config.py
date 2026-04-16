# -*- coding: utf-8 -*-

import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
os.makedirs(DATA_DIR, exist_ok=True)

# ---------- 数据集 ----------
INDEX_CH_DIR = os.path.join(DATA_DIR, "index_ch")
USE_INDEX_CH_QA = True
USE_INDEX_CH_LIVER_CANCER = True
USE_INDEX_CH_LLAMA = True
USE_INDEX_CH_DRUG = True

# ---------- Chroma ----------
CHROMA_PERSIST_DIR = os.path.join(DATA_DIR, "chroma_db")
CHROMA_COLLECTION_CH = "ai_doctor_rag_ch"
# 英文 docs.csv 向量库（与中文集合隔离）
CHROMA_COLLECTION_EN = "ai_doctor_rag_en"
INDEX_DOCS_CSV = os.path.join(DATA_DIR, "index", "docs.csv")
CHROMA_HOST = ""
CHROMA_PORT = 8000

# ---------- 嵌入模型 ----------
BGE_M3_MODEL = "BAAI/bge-m3"
USE_CUDA_FOR_EMBED = os.environ.get("AI_DOCTOR_USE_CUDA", "true").lower() in ("true", "1", "yes")

# ---------- RAG ----------
# chinese_bge：中文 index_ch；english_bge：data/index/docs.csv。可用环境变量覆盖（便于同机双进程）。
RAG_INDEX_SOURCE = os.environ.get("AI_DOCTOR_RAG_INDEX_SOURCE", "chinese_bge").strip().lower()
RAG_TOP_K = 5
RAG_MIN_SIMILARITY = 0.6
RAG_DIRECT_LLM_THRESHOLD = 0.8
MAX_NEW_TOKENS = 15360
TEMPERATURE = 0.7
RAG_HISTORY_MAX_MESSAGES = 10
RAG_HISTORY_MAX_CHARS_PER_MSG = 100

# ---------- LLM ----------
USE_LLM = os.environ.get("AI_DOCTOR_LLM", "ollama")
OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://127.0.0.1:11434")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "deepseek-r1:1.5b")

PROMPT_TEMPLATE_FILE = os.path.join(BASE_DIR, "prompts", "default.txt")
HOST = "0.0.0.0"
PORT = int(os.environ.get("AI_DOCTOR_RAG_PORT", "8765"))
