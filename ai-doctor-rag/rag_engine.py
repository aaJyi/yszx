# -*- coding: utf-8 -*-


import os
import numpy as np
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity

import config

_embed_model = None
_llm_model = None
_tokenizer = None
_embeddings = None
_df_docs = None
_chroma_client = None
_chroma_collection = None
_vector_db_type = None
_chroma_collection_ch = None
_chroma_collection_en = None
_bge_m3_model = None


def _get_embed_device():
    if not getattr(config, "USE_CUDA_FOR_EMBED", True):
        return "cpu"
    try:
        import torch
        if getattr(torch, "cuda", None) and torch.cuda.is_available():
            return "cuda"
    except Exception:
        pass
    return "cpu"


def _get_embed_model():
    global _embed_model
    if _embed_model is None:
        from sentence_transformers import SentenceTransformer
        device = _get_embed_device()
        if device == "cuda":
            print("[RAG] 嵌入模型使用 GPU (CUDA)")
        _embed_model = SentenceTransformer(config.EMBED_MODEL, device=device)
    return _embed_model


def _get_llm():
    global _llm_model, _tokenizer
    if _llm_model is None:
        from transformers import AutoModelForCausalLM, AutoTokenizer
        import torch
        model_name = (
            config.LLM_MODEL_HUATUO
            if config.USE_LLM == "huatuo"
            else config.LLM_MODEL_SMALL
        )
        _llm_model = AutoModelForCausalLM.from_pretrained(
            model_name, torch_dtype="auto", device_map="auto"
        )
        _tokenizer = AutoTokenizer.from_pretrained(model_name)
    return _llm_model, _tokenizer


def _load_prompt_template() -> str:
    """从 config.PROMPT_TEMPLATE_FILE 读取提示词模板，缺失时返回内置默认。"""
    if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge":
        path = os.path.join(config.BASE_DIR, "prompts", "default_en.txt")
    else:
        path = getattr(config, "PROMPT_TEMPLATE_FILE", None)
    if path and os.path.isfile(path):
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    return """Based on the following references and your medical knowledge, provide a detailed response.

References:
{references}

{history_block}Question: {query}

By considering:
1. The key medical concepts in the question.
2. How the reference cases relate to this question.
3. What medical principles should be applied.
4. Any potential complications or considerations.

Give the final response:
"""


def _format_history_block(history: list, use_medical_labels: bool = True) -> str:
    """将对话历史格式化为提示词中的 history_block；限制条数与每条长度，避免上下文过长导致后续只返回结尾。"""
    if not history or not isinstance(history, (list, tuple)):
        return ""
    max_msgs = getattr(config, "RAG_HISTORY_MAX_MESSAGES", 10)
    max_chars = getattr(config, "RAG_HISTORY_MAX_CHARS_PER_MSG", 400)
    recent = history[-max_msgs:] if len(history) > max_msgs else history
    lines = []
    if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge":
        user_label, bot_label = "User", "Assistant"
    else:
        user_label, bot_label = ("患者", "医生") if use_medical_labels else ("用户", "助手")
    for m in recent:
        role = m.get("role", "user")
        content = (m.get("content") or "").strip()
        if not content:
            continue
        if len(content) > max_chars:
            content = content[:max_chars] + "…"
        label = user_label if role == "user" else bot_label
        lines.append(f"{label}：{content}")
    if not lines:
        return ""
    header = "Conversation history:\n" if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge" else "【对话记录】\n"
    return header + "\n".join(lines) + "\n\n"


def _build_prompt(references: str, query: str, history: list = None) -> str:
    """根据模板、参考病例、当前问题及可选对话历史构建完整 prompt。"""
    template = _load_prompt_template()
    history_block = _format_history_block(history or [])
    return template.format(references=references, history_block=history_block, query=query)


def _generate_with_deepseek_direct(query: str, history: list = None) -> str:
    """直接 LLM 生成（无 RAG）：仅将对话上下文交给 DeepSeek。"""
    api_key = getattr(config, "DEEPSEEK_API_KEY", "") or os.environ.get("DEEPSEEK_API_KEY")
    if not api_key:
        return "[未配置 DEEPSEEK_API_KEY]"
    prompt = _build_direct_prompt(query=query, history=history)
    from openai import OpenAI
    client = OpenAI(api_key=api_key, base_url=getattr(config, "DEEPSEEK_API_BASE", "https://api.deepseek.com").rstrip("/"))
    resp = client.chat.completions.create(
        model=getattr(config, "DEEPSEEK_MODEL", "deepseek-chat"),
        messages=[{"role": "user", "content": prompt}],
        max_tokens=getattr(config, "MAX_NEW_TOKENS", 512),
        temperature=getattr(config, "TEMPERATURE", 0.7),
    )
    return (resp.choices[0].message.content or "").strip()


def _generate_with_deepseek(query: str, contexts: list, history: list = None) -> str:
    """使用云 DeepSeek API（OpenAI 兼容）基于检索上下文生成回答。"""
    api_key = getattr(config, "DEEPSEEK_API_KEY", "") or os.environ.get("DEEPSEEK_API_KEY")
    if not api_key:
        return "[未配置 DEEPSEEK_API_KEY，请在 config.py 或环境变量中设置]"
    references = "\n".join(
        [
            f"Reference {i+1}:\nQuestion: {c['question']}\nAnswer: {c['answer']}"
            for i, c in enumerate(contexts)
        ]
    )
    prompt = _build_prompt(references=references, query=query, history=history)
    from openai import OpenAI
    client = OpenAI(
        api_key=api_key,
        base_url=getattr(config, "DEEPSEEK_API_BASE", "https://api.deepseek.com").rstrip("/"),
    )
    resp = client.chat.completions.create(
        model=getattr(config, "DEEPSEEK_MODEL", "deepseek-chat"),
        messages=[{"role": "user", "content": prompt}],
        max_tokens=getattr(config, "MAX_NEW_TOKENS", 512),
        temperature=getattr(config, "TEMPERATURE", 0.7),
    )
    response = (resp.choices[0].message.content or "").strip()
    if "Give the final response:" in response:
        response = response.split("Give the final response:")[-1].strip()
    return response


def _generate_with_ollama(query: str, contexts: list, history: list = None) -> str:
    """使用本地 Ollama（如 DeepSeek）基于检索上下文生成回答，提示词来自 prompts/default.txt。"""
    import urllib.request
    import json
    references = "\n".join(
        [
            f"Reference {i+1}:\nQuestion: {c['question']}\nAnswer: {c['answer']}"
            for i, c in enumerate(contexts)
        ]
    )
    prompt = _build_prompt(references=references, query=query, history=history)
    url = f"{config.OLLAMA_BASE_URL.rstrip('/')}/api/generate"
    payload = {
        "model": config.OLLAMA_MODEL,
        "prompt": prompt,
        "stream": False,
        "options": {
            "num_predict": getattr(config, "MAX_NEW_TOKENS", 512),
            "temperature": getattr(config, "TEMPERATURE", 0.7),
        },
    }
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        out = json.loads(resp.read().decode("utf-8"))
    response = out.get("response", "").strip()
    if "Give the final response:" in response:
        response = response.split("Give the final response:")[-1].strip()
    return response


def _generate_with_ollama_direct(query: str, history: list = None) -> str:
    """直接 LLM 生成（无 RAG）：仅将对话上下文交给 Ollama，非流式。"""
    import urllib.request
    import json
    prompt = _build_direct_prompt(query=query, history=history)
    url = f"{config.OLLAMA_BASE_URL.rstrip('/')}/api/generate"
    payload = {
        "model": config.OLLAMA_MODEL,
        "prompt": prompt,
        "stream": False,
        "options": {
            "num_predict": getattr(config, "MAX_NEW_TOKENS", 512),
            "temperature": getattr(config, "TEMPERATURE", 0.7),
        },
    }
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        out = json.loads(resp.read().decode("utf-8"))
    return out.get("response", "").strip()


def _build_direct_prompt(query: str, history: list = None) -> str:
    """当检索相似度低于阈值时，仅用对话上下文构建 prompt（不含参考病例）。
    使用 用户/助手 标签，避免 患者/医生 框架误导非医疗问题的回答。"""
    history_block = _format_history_block(history or [], use_medical_labels=False)
    if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge":
        template_path = os.path.join(config.BASE_DIR, "prompts", "direct_en.txt")
    else:
        template_path = os.path.join(config.BASE_DIR, "prompts", "direct.txt")
    if os.path.isfile(template_path):
        with open(template_path, "r", encoding="utf-8") as f:
            template = f.read()
    else:
        if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge":
            template = "Conversation history:\n{history_block}\nCurrent question:\n{query}\n\nAnswer based on the conversation:"
        else:
            template = "【对话记录】\n{history_block}\n【当前问题】\n{query}\n\n请根据对话上下文回答："
    empty_hist = (
        "Conversation history:\n(none)\n\n"
        if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge"
        else "【对话记录】\n（无）\n\n"
    )
    return template.format(history_block=history_block or empty_hist, query=query)


def _generate_with_ollama_stream(query: str, contexts: list, history: list = None):
    """使用 Ollama 流式生成（含 RAG 参考病例）。"""
    import json
    import requests
    references = "\n".join(
        [f"Reference {i+1}:\nQuestion: {c['question']}\nAnswer: {c['answer']}"
         for i, c in enumerate(contexts)]
    )
    prompt = _build_prompt(references=references, query=query, history=history)
    url = f"{config.OLLAMA_BASE_URL.rstrip('/')}/api/generate"
    payload = {
        "model": config.OLLAMA_MODEL,
        "prompt": prompt,
        "stream": True,
        "options": {
            "num_predict": getattr(config, "MAX_NEW_TOKENS", 512),
            "temperature": getattr(config, "TEMPERATURE", 0.7),
        },
    }
    with requests.post(url, json=payload, stream=True, timeout=120) as resp:
        resp.raise_for_status()
        for line in resp.iter_lines(decode_unicode=True):
            if not line or not line.strip():
                continue
            try:
                obj = json.loads(line)
                txt = obj.get("response", "")
                if txt:
                    yield txt
                if obj.get("done"):
                    return
            except json.JSONDecodeError:
                pass


def _generate_with_ollama_stream_direct(query: str, history: list = None):
    """直接 LLM 生成（无 RAG 参考）：仅将对话上下文交给 Ollama。"""
    import json
    import requests
    prompt = _build_direct_prompt(query=query, history=history)
    url = f"{config.OLLAMA_BASE_URL.rstrip('/')}/api/generate"
    payload = {
        "model": config.OLLAMA_MODEL,
        "prompt": prompt,
        "stream": True,
        "options": {
            "num_predict": getattr(config, "MAX_NEW_TOKENS", 512),
            "temperature": getattr(config, "TEMPERATURE", 0.7),
        },
    }
    with requests.post(url, json=payload, stream=True, timeout=120) as resp:
        resp.raise_for_status()
        for line in resp.iter_lines(decode_unicode=True):
            if not line or not line.strip():
                continue
            try:
                obj = json.loads(line)
                txt = obj.get("response", "")
                if txt:
                    yield txt
                if obj.get("done"):
                    return
            except json.JSONDecodeError:
                pass


def _get_chroma_client():
    """获取 Chroma 客户端（本地持久化或远程，供多机共享）。"""
    global _chroma_client, _chroma_collection
    if _chroma_client is None:
        import chromadb
        host = getattr(config, "CHROMA_HOST", "") or None
        if host:
            from chromadb.config import Settings
            _chroma_client = chromadb.HttpClient(
                host=host,
                port=getattr(config, "CHROMA_PORT", 8000),
                settings=Settings(anonymized_telemetry=False),
            )
        else:
            _chroma_client = chromadb.PersistentClient(
                path=getattr(config, "CHROMA_PERSIST_DIR", config.DATA_DIR),
            )
        _chroma_collection = _chroma_client.get_or_create_collection(
            name=getattr(config, "CHROMA_COLLECTION_NAME", "ai_doctor_rag"),
            metadata={"hnsw:space": "cosine", "description": "RAG medical Q&A"},
        )
    return _chroma_client, _chroma_collection


def _get_chroma_collection_ch():
    """获取 Chroma 中文 BGE-m3 集合（与默认中文集合分隔）。"""
    global _chroma_client, _chroma_collection_ch
    if _chroma_collection_ch is None:
        _get_chroma_client()  # 复用同一 Chroma 连接
        coll_name = getattr(config, "CHROMA_COLLECTION_CH", "ai_doctor_rag_ch")
        _chroma_collection_ch = _chroma_client.get_or_create_collection(
            name=coll_name,
            metadata={"hnsw:space": "cosine", "description": "RAG medical Q&A (index_ch, BGE-m3)"},
        )
    return _chroma_collection_ch


def reload_chinese_bge_index():
    """外部脚本重建 Chroma 持久化文件后调用，丢弃内存中的集句柄以便重新加载磁盘数据。"""
    global _chroma_collection_ch
    _chroma_collection_ch = None
    return _get_chroma_collection_ch()


def _get_chroma_collection_en():
    """Chroma 英文 BGE-m3 集合（docs.csv，与中文集合分隔）。"""
    global _chroma_client, _chroma_collection_en
    if _chroma_collection_en is None:
        _get_chroma_client()
        coll_name = getattr(config, "CHROMA_COLLECTION_EN", "ai_doctor_rag_en")
        _chroma_collection_en = _chroma_client.get_or_create_collection(
            name=coll_name,
            metadata={"hnsw:space": "cosine", "description": "RAG medical Q&A (docs.csv EN, BGE-m3)"},
        )
    return _chroma_collection_en


def reload_english_bge_index():
    global _chroma_collection_en
    _chroma_collection_en = None
    return _get_chroma_collection_en()


def _rag_retrieval_fallback_message() -> str:
    if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge":
        return (
            "No sufficiently relevant match was found in the knowledge base. "
            "Try rephrasing in clear English, or ask about a specific symptom or condition."
        )
    return (
        "未找到与您问题足够相关的医学参考。"
        "当前知识库主要为英文内容，中文问题可能匹配不佳。"
        "建议：① 尝试用英文描述症状（如：poor sleep, insomnia）；② 或换一种问法。"
    )


def _get_bge_m3_model():
    """获取 BGE-m3 嵌入模型（中文 index_ch 检索用）。"""
    global _bge_m3_model
    if _bge_m3_model is None:
        from FlagEmbedding import BGEM3FlagModel
        _bge_m3_model = BGEM3FlagModel(
            getattr(config, "BGE_M3_MODEL", "BAAI/bge-m3"),
            use_fp16=True,
        )
    return _bge_m3_model


def load_index():
    """加载索引：chinese_bge 时连 Chroma 中文 BGE 集合；否则 VECTOR_DB=chroma 连 Chroma，=file 时加载 npy+csv。"""
    global _embeddings, _df_docs, _vector_db_type
    idx_source = getattr(config, "RAG_INDEX_SOURCE", None) or (
        "english" if getattr(config, "USE_ENGLISH_INDEX", False) else "chinese"
    )
    if idx_source == "chinese_bge":
        _vector_db_type = "chinese_bge"
        _get_chroma_collection_ch()
        return None, None
    if idx_source == "english_bge":
        _vector_db_type = "english_bge"
        _get_chroma_collection_en()
        return None, None
    _vector_db_type = getattr(config, "VECTOR_DB", "file") or "file"
    if _vector_db_type == "chroma":
        _get_chroma_client()
        return None, None
    if not os.path.isfile(getattr(config, "EMBEDDINGS_NPY", "")) or not os.path.isfile(getattr(config, "DOCS_CSV", "")):
        raise FileNotFoundError("未找到索引文件。请先运行: python build_index_ch_chroma.py")
    _embeddings = np.load(config.EMBEDDINGS_NPY)
    _df_docs = pd.read_csv(config.DOCS_CSV)
    return _embeddings, _df_docs


def retrieve(query: str, k: int = None) -> list:
    """检索与 query 最相关的 k 条问答（Chroma 中文 BGE / Chroma / 本地向量）。"""
    k = k or config.RAG_TOP_K
    if _vector_db_type is None:
        load_index()
    if _vector_db_type == "chinese_bge":
        coll = _get_chroma_collection_ch()
        model = _get_bge_m3_model()
        q_vec = model.encode([query])["dense_vecs"][0].tolist()
        res = coll.query(query_embeddings=[q_vec], n_results=k, include=["documents", "metadatas", "distances"])
        out = []
        if res and res.get("ids") and res["ids"][0]:
            ids = res["ids"][0]
            docs = res.get("documents", [[]])[0] or []
            metas = res.get("metadatas", [[]])[0] or []
            dists = res.get("distances", [[]])[0] or []
            for i in range(len(ids)):
                meta = metas[i] if i < len(metas) else {}
                d = dists[i] if i < len(dists) else 0
                sim = 1.0 - float(d) if d is not None else 0.5
                out.append({
                    "question": meta.get("input", ""),
                    "answer": meta.get("output", docs[i] if i < len(docs) else ""),
                    "similarity": sim,
                })
        return out
    if _vector_db_type == "english_bge":
        coll = _get_chroma_collection_en()
        model = _get_bge_m3_model()
        q_vec = model.encode([query])["dense_vecs"][0].tolist()
        res = coll.query(query_embeddings=[q_vec], n_results=k, include=["documents", "metadatas", "distances"])
        out = []
        if res and res.get("ids") and res["ids"][0]:
            ids = res["ids"][0]
            docs = res.get("documents", [[]])[0] or []
            metas = res.get("metadatas", [[]])[0] or []
            dists = res.get("distances", [[]])[0] or []
            for i in range(len(ids)):
                meta = metas[i] if i < len(metas) else {}
                d = dists[i] if i < len(dists) else 0
                sim = 1.0 - float(d) if d is not None else 0.5
                out.append({
                    "question": meta.get("input", ""),
                    "answer": meta.get("output", docs[i] if i < len(docs) else ""),
                    "similarity": sim,
                })
        return out
    if _vector_db_type == "chroma":
        _, coll = _get_chroma_client()
        embed_model = _get_embed_model()
        q_emb = embed_model.encode([query]).tolist()
        res = coll.query(query_embeddings=q_emb, n_results=k, include=["documents", "metadatas", "distances"])
        out = []
        if res and res.get("ids") and res["ids"][0]:
            ids = res["ids"][0]
            docs = res.get("documents", [[]])[0] or []
            metas = res.get("metadatas", [[]])[0] or []
            dists = res.get("distances", [[]])[0] or []
            for i, doc_id in enumerate(ids):
                meta = metas[i] if i < len(metas) else {}
                d = dists[i] if i < len(dists) else 0
                sim = 1.0 - float(d) if d is not None else 0.5
                out.append({
                    "question": meta.get("input", ""),
                    "answer": meta.get("output", docs[i] if i < len(docs) else ""),
                    "similarity": sim,
                })
        return out
    if _embeddings is None or _df_docs is None:
        load_index()
    embed_model = _get_embed_model()
    q_emb = embed_model.encode([query])
    sim = cosine_similarity(q_emb, _embeddings)[0]
    top_k_idx = np.argsort(sim)[-k:][::-1]
    out = []
    for i in top_k_idx:
        row = _df_docs.iloc[i]
        out.append({
            "question": row.get("input", row.get("question", "")),
            "answer": row.get("output", row.get("answer", "")),
            "similarity": float(sim[i]),
        })
    return out


def generate_with_llm_direct(query: str, history: list = None) -> str:
    """直接 LLM 生成（无 RAG）：仅将对话上下文交给 HuatuoGPT/small。"""
    model, tokenizer = _get_llm()
    prompt = _build_direct_prompt(query=query, history=history)
    inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
    outputs = model.generate(
        **inputs,
        max_new_tokens=config.MAX_NEW_TOKENS,
        temperature=config.TEMPERATURE,
        do_sample=True,
        pad_token_id=tokenizer.eos_token_id,
    )
    response = tokenizer.decode(outputs[0], skip_special_tokens=True)
    return response.strip()


def generate_with_llm(query: str, contexts: list, history: list = None) -> str:
    """使用 LLM 基于检索上下文生成回答（HuatuoGPT 风格 prompt）。"""
    model, tokenizer = _get_llm()
    context_prompt = "\n".join(
        [
            f"Reference {i+1}:\nQuestion: {c['question']}\nAnswer: {c['answer']}"
            for i, c in enumerate(contexts)
        ]
    )
    history_block = _format_history_block(history or [])
    if history_block:
        history_block = "Conversation history:\n" + history_block + "\n"
    prompt = f"""Based on the following references and your medical knowledge, provide a detailed response:

References:
{context_prompt}

{history_block}Question: {query}

By considering:
1. The key medical concepts in the question.
2. How the reference cases relate to this question.
3. What medical principles should be applied.
4. Any potential complications or considerations.

Give the final response:
"""
    messages = [{"role": "user", "content": prompt}]
    if hasattr(tokenizer, "apply_chat_template"):
        text = tokenizer.apply_chat_template(
            messages, tokenize=False, add_generation_prompt=True
        )
    else:
        text = prompt
    inputs = tokenizer(text, return_tensors="pt").to(model.device)
    outputs = model.generate(
        **inputs,
        max_new_tokens=config.MAX_NEW_TOKENS,
        temperature=config.TEMPERATURE,
        do_sample=True,
        pad_token_id=tokenizer.eos_token_id,
    )
    response = tokenizer.decode(outputs[0], skip_special_tokens=True)
    if "Give the final response:" in response:
        response = response.split("Give the final response:")[-1]
    return response.strip()


def chat_stream(query: str, k: int = None, use_llm: str = None, history: list = None):
    """
    流式对话：先检索，再按 use_llm 流式生成或一次性返回。
    history: 上下文对话记录 [{"role":"user"|"assistant","content":"..."}, ...]
    yield: 先 dict 元数据 {"type":"meta", "references": [...], "best_similarity": float, "used_llm": bool}
           再 str 文本块，最后 {"type":"done"}
    """
    k = k or config.RAG_TOP_K
    use_llm = use_llm if use_llm is not None else config.USE_LLM
    history = history or []
    search_query = _build_search_query(query, history)
    contexts = retrieve(search_query, k=k)
    _log_chat_input(query, history, contexts)
    best_similarity = contexts[0]["similarity"] if contexts else None
    used_llm = use_llm in ("huatuo", "small", "ollama", "deepseek")

    yield {"type": "meta", "references": contexts, "best_similarity": best_similarity, "used_llm": used_llm}

    # 若检索相似度低于阈值，跳过 RAG，直接将对话上下文交给 LLM
    direct_threshold = getattr(config, "RAG_DIRECT_LLM_THRESHOLD", 0.7)
    use_direct_llm = (best_similarity is None or best_similarity < direct_threshold) and used_llm

    answer = ""
    if use_llm == "ollama":
        try:
            if use_direct_llm:
                for chunk in _generate_with_ollama_stream_direct(query, history=history):
                    answer += chunk
                    yield chunk
            else:
                for chunk in _generate_with_ollama_stream(query, contexts, history=history):
                    answer += chunk
                    yield chunk
        except Exception as e:
            yield f"[Ollama 流式生成异常: {e}]"
    elif use_llm == "deepseek":
        try:
            if use_direct_llm:
                answer = _generate_with_deepseek_direct(query, history=history)
            else:
                answer = _generate_with_deepseek(query, contexts, history=history)
            yield answer
        except Exception as e:
            yield f"[DeepSeek API 异常: {e}]"
    elif use_llm in ("huatuo", "small"):
        try:
            if use_direct_llm:
                answer = generate_with_llm_direct(query, history=history)
            else:
                answer = generate_with_llm(query, contexts, history=history)
            yield answer
        except Exception as e:
            yield f"[LLM 生成异常: {e}]"
    else:
        best_sim = contexts[0]["similarity"] if contexts else 0.0
        if contexts and best_sim >= config.RAG_MIN_SIMILARITY:
            suffix = (
                "\n\n[The above is retrieved reference material only; not medical advice. See a clinician.]"
                if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge"
                else "\n\n[以上为检索到的参考回答，仅供参考，不构成诊断建议。]"
            )
            answer = contexts[0]["answer"][:800] + suffix
        else:
            answer = _rag_retrieval_fallback_message()
        yield answer

    yield {"type": "done"}


def _log_chat_input(query: str, history: list, contexts: list):
    """每次向大模型提问时输出：当前提问、上下文、检索到的内容。"""
    sep = "=" * 60
    print("\n" + sep)
    print("[RAG 提问] 当前提问:", query)
    if history:
        print("[RAG 提问] 上下文:")
        for i, m in enumerate(history):
            role = m.get("role", "")
            content = (m.get("content") or "")[:500]
            if len((m.get("content") or "")) > 500:
                content += "..."
            print(f"  {i+1}. {role}: {content}")
    else:
        print("[RAG 提问] 上下文: （无）")
    if contexts:
        print("[RAG 提问] 检索到的内容 (共 %d 条):" % len(contexts))
        for i, c in enumerate(contexts):
            sim = c.get("similarity")
            q = (c.get("question") or "")[:200]
            a = (c.get("answer") or "")[:300]
            if len(c.get("answer") or "") > 300:
                a += "..."
            print("  --- 条 %d (相似度 %.4f) ---" % (i + 1, sim))
            print("  Q: %s" % q)
            print("  A: %s" % a)
    else:
        print("[RAG 提问] 检索到的内容: （无）")
    print(sep + "\n")


def _build_search_query(query: str, history: list) -> str:
    """根据当前问题与对话历史构建检索用查询。优先保留首轮主诉，避免追问时检索跑偏。"""
    if not history:
        return query.strip()
    user_msgs = [m.get("content", "").strip() for m in history if m.get("role") == "user"]
    if not user_msgs:
        return query.strip()
    # 首轮主诉最重要，必须纳入检索；再拼接近期补充与当前追问
    first_complaint = user_msgs[0]
    rest = user_msgs[1:]
    if not rest:
        return (first_complaint + " " + query.strip()).strip()
    # 取最近 2 轮用户提问 + 当前问题，与首轮主诉拼接
    recent = rest[-2:] if len(rest) > 2 else rest
    parts = [first_complaint] + recent + [query.strip()]
    return " ".join(p for p in parts if p).strip()


def chat(query: str, k: int = None, use_llm: str = None, history: list = None) -> dict:
    """
    单轮对话：先检索，再视配置决定是否用 LLM 生成。
    history: 上下文对话记录 [{"role":"user"|"assistant","content":"..."}, ...]
    """
    k = k or config.RAG_TOP_K
    use_llm = use_llm if use_llm is not None else config.USE_LLM
    history = history or []
    search_query = _build_search_query(query, history)
    contexts = retrieve(search_query, k=k)
    _log_chat_input(query, history, contexts)
    best_similarity = contexts[0]["similarity"] if contexts else None
    direct_threshold = getattr(config, "RAG_DIRECT_LLM_THRESHOLD", 0.7)
    use_direct_llm = best_similarity is None or best_similarity < direct_threshold
    answer = ""
    if use_llm == "deepseek":
        try:
            if use_direct_llm:
                answer = _generate_with_deepseek_direct(query, history=history)
            else:
                answer = _generate_with_deepseek(query, contexts, history=history)
        except Exception as e:
            answer = f"[DeepSeek API 异常: {e}]"
    elif use_llm == "ollama":
        try:
            if use_direct_llm:
                answer = _generate_with_ollama_direct(query, history=history)
            else:
                answer = _generate_with_ollama(query, contexts, history=history)
        except Exception as e:
            answer = f"[Ollama 生成异常: {e}]"
    elif use_llm in ("huatuo", "small"):
        try:
            if use_direct_llm:
                answer = generate_with_llm_direct(query, history=history)
            else:
                answer = generate_with_llm(query, contexts, history=history)
        except Exception as e:
            answer = f"[LLM 生成异常: {e}]"
    if not answer:
        # 仅检索模式：仅当最佳匹配相似度达到阈值时才返回，否则提示不相关
        best_sim = contexts[0]["similarity"] if contexts else 0.0
        if contexts and best_sim >= config.RAG_MIN_SIMILARITY:
            suffix = (
                "\n\n[The above is retrieved reference material only; not medical advice. See a clinician.]"
                if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge"
                else "\n\n[以上为检索到的参考回答，仅供参考，不构成诊断建议。]"
            )
            answer = contexts[0]["answer"][:800] + suffix
        else:
            answer = _rag_retrieval_fallback_message()
    best_similarity = contexts[0]["similarity"] if contexts else None
    return {
        "answer": answer,
        "references": contexts,
        "used_llm": use_llm in ("huatuo", "small", "ollama", "deepseek"),
        "best_similarity": best_similarity,
    }
