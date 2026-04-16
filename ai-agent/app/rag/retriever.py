"""
文档检索器（真实实现 - FAISS 向量检索）
"""
import os
import re
import logging
import numpy as np
from pathlib import Path
from typing import List, Dict, Optional

# 配置日志
logger = logging.getLogger(__name__)

try:
    from sentence_transformers import SentenceTransformer
    import faiss
    HAS_FAISS = True
except ImportError:
    HAS_FAISS = False
    logger.warning("sentence-transformers 或 faiss 未安装，请安装: pip install sentence-transformers faiss-cpu")


class Retriever:
    """文档检索器（基于 FAISS 向量检索）"""
    
    def __init__(
        self, 
        docs_dir: str = "data/medical_docs", 
        chunk_size: int = 400, 
        chunk_overlap: int = 50,
        model_name: str = "paraphrase-multilingual-MiniLM-L12-v2"
    ):
        """
        初始化检索器
        
        Args:
            docs_dir: 文档目录路径
            chunk_size: chunk 大小（字符数）
            chunk_overlap: chunk 重叠大小（字符数）
            model_name: embedding 模型名称（支持中文的模型）
        """
        if not HAS_FAISS:
            raise ImportError("请安装必要的依赖: pip install sentence-transformers faiss-cpu")
        
        self.docs_dir = Path(docs_dir)
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.chunks: List[Dict] = []
        self.embeddings: Optional[np.ndarray] = None
        self.faiss_index: Optional[faiss.Index] = None
        
        # 初始化 embedding 模型
        logger.info(f"正在加载 embedding 模型: {model_name}")
        self.embedding_model = SentenceTransformer(model_name)
        logger.info("Embedding 模型加载完成")
        
        # 启动时加载所有文档
        self._load_documents()
        
        # 构建 FAISS 索引
        self._build_faiss_index()
    
    def _load_documents(self):
        """加载所有文档并切分为 chunks"""
        if not self.docs_dir.exists():
            # 如果目录不存在，创建空列表并返回
            logger.info(f"文档目录不存在: {self.docs_dir}，跳过文档加载")
            self.chunks = []
            return
        
        # 统计文件数量
        txt_files = list(self.docs_dir.rglob("*.txt"))
        md_files = list(self.docs_dir.rglob("*.md"))
        total_files = len(txt_files) + len(md_files)
        
        logger.info(f"开始加载文档，找到 {len(txt_files)} 个 .txt 文件，{len(md_files)} 个 .md 文件，共 {total_files} 个文件")
        
        # 记录加载前的 chunk 数量
        chunks_before = len(self.chunks)
        
        # 遍历目录中的所有 .txt 和 .md 文件
        for file_path in txt_files:
            self._load_file(file_path)
        
        for file_path in md_files:
            self._load_file(file_path)
        
        # 记录加载后的 chunk 数量
        chunks_after = len(self.chunks)
        chunks_added = chunks_after - chunks_before
        
        logger.info(f"文档加载完成，共加载 {total_files} 个文件，新增 {chunks_added} 个 chunks，当前共有 {chunks_after} 个 chunks")
    
    def _build_faiss_index(self):
        """构建 FAISS 向量索引"""
        if not self.chunks:
            logger.warning("没有 chunks，跳过 FAISS 索引构建")
            return
        
        logger.info(f"开始为 {len(self.chunks)} 个 chunks 生成 embedding 并构建 FAISS 索引")
        
        # 提取所有 chunk 的文本内容
        chunk_texts = [chunk["content"] for chunk in self.chunks]
        
        # 批量生成 embedding
        logger.info("正在生成 embeddings...")
        self.embeddings = self.embedding_model.encode(
            chunk_texts,
            show_progress_bar=True,
            convert_to_numpy=True
        )
        
        # 获取 embedding 维度
        embedding_dim = self.embeddings.shape[1]
        logger.info(f"Embedding 维度: {embedding_dim}, 共生成 {len(self.embeddings)} 个 embeddings")
        
        # 构建 FAISS IndexFlatL2（L2 距离）
        self.faiss_index = faiss.IndexFlatL2(embedding_dim)
        
        # 将 embeddings 添加到索引（需要转换为 float32）
        embeddings_f32 = self.embeddings.astype('float32')
        self.faiss_index.add(embeddings_f32)
        
        logger.info(f"FAISS 索引构建完成，索引大小: {self.faiss_index.ntotal}")
    
    def _load_file(self, file_path: Path):
        """
        加载单个文件并切分为 chunks
        
        Args:
            file_path: 文件路径
        """
        try:
            # 读取文件内容
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 清理内容（移除多余空白）
            content = re.sub(r'\s+', ' ', content).strip()
            
            if not content:
                return
            
            # 切分为 chunks
            chunks = self._split_text(content)
            logger.info(f"文件 {file_path.name} 切分为 {len(chunks)} 个 chunks")
            
            # 为每个 chunk 添加 metadata
            source_file = str(file_path.relative_to(self.docs_dir))
            # 提取文件名（去掉扩展名）作为 title
            file_stem = file_path.stem
            for chunk_id, chunk_text in enumerate(chunks):
                self.chunks.append({
                    "content": chunk_text,
                    "title": file_stem,  # 使用文件名作为 title
                    "source": source_file,
                    "source_file": source_file,
                    "chunk_id": chunk_id,
                    "metadata": {
                        "source": source_file,
                        "chunk_id": chunk_id
                    }
                })
        except Exception as e:
            # 如果文件读取失败，记录错误但继续处理其他文件
            logger.warning(f"无法加载文件 {file_path}: {e}")
    
    def _split_text(self, text: str) -> List[str]:
        """
        将文本切分为 chunks
        
        Args:
            text: 原始文本
            
        Returns:
            chunk 列表
        """
        if len(text) <= self.chunk_size:
            return [text]
        
        chunks = []
        start = 0
        text_length = len(text)
        
        while start < text_length:
            # 计算当前 chunk 的结束位置
            end = start + self.chunk_size
            
            if end >= text_length:
                # 最后一段
                chunks.append(text[start:])
                break
            
            # 尝试在句号、问号、感叹号等标点符号处切分
            # 如果没有找到合适的切分点，则在空格处切分
            best_split = end
            
            # 向前查找句号、问号、感叹号
            for i in range(end, max(start + self.chunk_size - 100, start), -1):
                if text[i] in '。！？\n':
                    best_split = i + 1
                    break
            
            # 如果没找到标点符号，尝试在空格处切分
            if best_split == end:
                for i in range(end, max(start + self.chunk_size - 100, start), -1):
                    if text[i] in ' \t':
                        best_split = i + 1
                        break
            
            chunks.append(text[start:best_split].strip())
            
            # 计算下一个 chunk 的起始位置（考虑重叠）
            start = best_split - self.chunk_overlap
            if start < 0:
                start = 0
        
        return chunks
    
    def search(self, query: str, top_k: int = 3) -> List[Dict]:
        """
        使用 FAISS 向量检索相关文档
        
        Args:
            query: 查询文本
            top_k: 返回 Top K 个结果（默认 3）
            
        Returns:
            Top K 相关文档 chunk 列表，统一格式：
            [
                {
                    "title": "...",
                    "content": "...",
                    "metadata": {
                        "source": "...",
                        "chunk_id": ...
                    }
                },
                ...
            ]
        """
        logger.info(f"开始向量检索，查询: {query[:50]}..." if len(query) > 50 else f"开始向量检索，查询: {query}")
        
        if not self.chunks or self.faiss_index is None:
            # 如果没有加载文档或索引未构建，返回空列表
            logger.warning("没有可用的 chunks 或 FAISS 索引未构建，返回空结果")
            return []
        
        # 对 query 生成 embedding
        logger.info("正在生成查询 embedding...")
        query_embedding = self.embedding_model.encode([query], convert_to_numpy=True)
        query_embedding_f32 = query_embedding.astype('float32')
        
        # 使用 FAISS 检索 TopK
        logger.info(f"使用 FAISS 检索 Top {top_k} 个结果...")
        distances, indices = self.faiss_index.search(query_embedding_f32, top_k)
        
        # 构建返回结果
        result = []
        for i, (distance, idx) in enumerate(zip(distances[0], indices[0])):
            if idx < len(self.chunks):
                chunk = self.chunks[idx]
                result.append({
                    "title": chunk.get("title", chunk.get("source_file", "unknown")),
                    "content": chunk["content"],
                    "metadata": {
                        "source": chunk.get("source", chunk.get("source_file", "unknown")),
                        "chunk_id": chunk.get("chunk_id", 0)
                    }
                })
                logger.debug(f"检索结果 {i+1}: distance={distance:.4f}, source={chunk.get('source')}")
        
        logger.info(f"向量检索完成，返回 {len(result)} 个结果")
        
        return result
    
    def debug_dump(self):
        """
        调试方法：打印当前已加载的文档和 chunk 信息
        仅用于开发调试
        """
        print("=" * 60)
        print("【Retriever 调试信息】")
        print("=" * 60)
        
        # 统计文档数量（按 source_file 去重）
        unique_sources = set()
        for chunk in self.chunks:
            source = chunk.get("source", "unknown")
            unique_sources.add(source)
        
        print(f"\n已加载文档数量: {len(unique_sources)}")
        print(f"总 chunks 数量: {len(self.chunks)}")
        
        # 打印文档列表
        if unique_sources:
            print(f"\n文档列表:")
            for i, source in enumerate(sorted(unique_sources), 1):
                # 统计每个文档的 chunk 数量
                chunk_count = sum(1 for chunk in self.chunks if chunk.get("source") == source)
                print(f"  {i}. {source} ({chunk_count} chunks)")
        
        # 打印前 2 个 chunk 示例
        print(f"\n前 2 个 chunk 示例:")
        print("-" * 60)
        for i, chunk in enumerate(self.chunks[:2], 1):
            print(f"\nChunk {i}:")
            print(f"  Title: {chunk.get('title', 'N/A')}")
            print(f"  Source: {chunk.get('source', 'N/A')}")
            print(f"  Chunk ID: {chunk.get('chunk_id', 'N/A')}")
            content = chunk.get('content', '')
            content_preview = content[:100] + "..." if len(content) > 100 else content
            print(f"  Content: {content_preview}")
            print(f"  Content Length: {len(content)} 字符")
        
        print("=" * 60)