# -*- coding: utf-8 -*-
"""
从 data/index/docs.csv（列 input, output）构建英文医学问答向量库，写入独立 Chroma 集合。
流程与 build_index_ch_chroma.py 一致：BGE-m3 嵌入 + Chroma 持久化。

用法：
  cd ai-doctor-rag
  python build_index_en_chroma.py

启动英文服务前请设置：
  AI_DOCTOR_RAG_INDEX_SOURCE=english_bge
  AI_DOCTOR_RAG_PORT=8766
"""

import csv
import os
import uuid

import config


def load_docs_csv(path: str):
    if not os.path.isfile(path):
        raise FileNotFoundError(f"docs.csv 不存在: {path}")
    docs = []
    with open(path, "r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            q = (row.get("input") or "").strip()
            a = (row.get("output") or "").strip()
            if not q and not a:
                continue
            text = f"{q} {a}"
            docs.append((text, {"source": "docs_csv", "input": q, "output": a}))
    return docs


def main():
    import chromadb

    csv_path = getattr(config, "INDEX_DOCS_CSV", os.path.join(config.DATA_DIR, "index", "docs.csv"))
    model_name = getattr(config, "BGE_M3_MODEL", "BAAI/bge-m3")
    collection_name = getattr(config, "CHROMA_COLLECTION_EN", "ai_doctor_rag_en")

    print("1. 加载英文数据集", csv_path)
    docs = load_docs_csv(csv_path)
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
        metadata={"hnsw:space": "cosine", "description": "RAG medical Q&A (docs.csv EN, BGE-m3)"},
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

    print(
        "\n启动英文 RAG 时设置环境变量后运行 python app.py：\n"
        "  AI_DOCTOR_RAG_INDEX_SOURCE=english_bge\n"
        "  AI_DOCTOR_RAG_PORT=8766\n"
        "集合名:", collection_name
    )


if __name__ == "__main__":
    main()
