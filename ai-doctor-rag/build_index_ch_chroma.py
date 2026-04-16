# -*- coding: utf-8 -*-

import os
import re
import uuid
import json

import config


def _strip_html(text: str) -> str:
    if not text or not isinstance(text, str):
        return ""
    return re.sub(r"<[^>]+>", "", text).strip()


def _load_qa_json(path: str) -> list:
    with open(path, "r", encoding="utf-8") as f:
        raw = f.read().strip()
    if raw.startswith("["):
        items = json.loads(raw)
    else:
        items = [json.loads(line) for line in raw.split("\n") if line.strip()]
    out = []
    for it in items:
        q = (it.get("instruction") or it.get("input") or "").strip()
        a = (it.get("output") or "").strip()
        if not q and not a:
            continue
        if not q:
            q = a[:100] + "…" if len(a) > 100 else a
        out.append((q, a))
    return out


def _load_drug_json(path: str) -> list:
    with open(path, "r", encoding="utf-8") as f:
        items = json.load(f)
    if not isinstance(items, list):
        items = [items]
    out = []
    for row in items:
        name = str(row.get("药品", "")).strip()
        ind = _strip_html(str(row.get("药品适应症", "")))
        diseases = str(row.get("医学专家标注的适应病症", "")).strip()
        text = f"药品：{name}。适应症：{ind}。适用疾病：{diseases}。"
        meta = {"source": "drug", "药品": name, "适应症": ind, "适用疾病": diseases}
        out.append((text, meta))
    return out


def load_index_ch_datasets():
    base = getattr(config, "INDEX_CH_DIR", os.path.join(config.DATA_DIR, "index_ch"))
    docs = []

    if getattr(config, "USE_INDEX_CH_QA", True):
        p = os.path.join(base, "qa.json")
        if os.path.isfile(p):
            for q, a in _load_qa_json(p):
                text = f"{q} {a}"
                docs.append((text, {"source": "qa", "input": q, "output": a}))

    if getattr(config, "USE_INDEX_CH_LIVER_CANCER", True):
        p = os.path.join(base, "liver_cancer.json")
        if os.path.isfile(p):
            for q, a in _load_qa_json(p):
                text = f"{q} {a}"
                docs.append((text, {"source": "liver_cancer", "input": q, "output": a}))

    if getattr(config, "USE_INDEX_CH_LLAMA", True):
        p = os.path.join(base, "llama_data.json")
        if os.path.isfile(p):
            for q, a in _load_qa_json(p):
                text = f"{q} {a}"
                docs.append((text, {"source": "llama_data", "input": q, "output": a}))

    if getattr(config, "USE_INDEX_CH_DRUG", True):
        p = os.path.join(base, "drug.json")
        if os.path.isfile(p):
            for text, meta in _load_drug_json(p):
                docs.append(
                    (
                        text,
                        {
                            "source": "drug",
                            "input": meta.get("药品", "") + "可以治疗什么疾病？",
                            "output": "适应症:"
                            + meta.get("适应症")
                            + "适用疾病"
                            + meta.get("适用疾病"),
                        },
                    )
                )

    return docs


def main():
    import chromadb

    model_name = getattr(config, "BGE_M3_MODEL", "BAAI/bge-m3")
    collection_name = getattr(config, "CHROMA_COLLECTION_CH", "ai_doctor_rag_ch")

    print("1. 加载数据集")
    docs = load_index_ch_datasets()
    if not docs:
        raise SystemExit("未加载到任何文档")
    texts = [d[0] for d in docs]
    payloads = [d[1] for d in docs]
    print("   条数:", len(texts))

    print("2. 加载 BGE-m3 嵌入模型…")
    try:
        from FlagEmbedding import BGEM3FlagModel
        embed_model = BGEM3FlagModel(model_name, use_fp16=True)
    except Exception as e:
        print("   FlagEmbedding 未安装或加载失败:", e)
        print("   请安装: pip install FlagEmbedding")
        raise
    print("3. 生成向量…")
    embeds = embed_model.encode(texts)["dense_vecs"]

    print("4. 连接 Chroma 并写入集合", collection_name)
    path = getattr(config, "CHROMA_PERSIST_DIR", None) or os.path.join(config.DATA_DIR, "chroma_db")
    host = getattr(config, "CHROMA_HOST", "") or None
    if host:
        from chromadb.config import Settings
        client = chromadb.HttpClient(
            host=host,
            port=getattr(config, "CHROMA_PORT", 8000),
            settings=Settings(anonymized_telemetry=False),
        )
    else:
        os.makedirs(path, exist_ok=True)
        client = chromadb.PersistentClient(path=path)
    try:
        client.delete_collection(collection_name)
    except Exception:
        pass
    coll = client.get_or_create_collection(
        name=collection_name,
        metadata={"hnsw:space": "cosine", "description": "RAG medical Q&A (index_ch, BGE-m3)"},
    )
    n = len(texts)
    batch_size = 5000
    for start in range(0, n, batch_size):
        end = min(start + batch_size, n)
        ids = [str(uuid.uuid4()) for _ in range(start, end)]
        embs = embeds[start:end].tolist()
        doc_texts = [texts[i][:2000] for i in range(start, end)]
        metas = [
            {
                "source": str(payloads[i].get("source", "")),
                "input": payloads[i].get("input", ""),
                "output": payloads[i].get("output", ""),
            }
            for i in range(start, end)
        ]
        coll.add(ids=ids, embeddings=embs, documents=doc_texts, metadatas=metas)
    print("   已写入 Chroma 条数:", n)

    print("\n在 config.py 中设置 RAG_INDEX_SOURCE = 'chinese_bge' 后启动服务即可使用本中文向量库（BGE-m3 + Chroma，与之前中文库分隔）。")


if __name__ == "__main__":
    main()
