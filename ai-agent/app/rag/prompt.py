"""
提示词构建模块
"""


def build_prompt(question: str, docs: list) -> str:
    """
    构建面向医疗场景的 RAG 提示词
    
    Args:
        question: 用户问题
        docs: 检索到的文档列表，格式 [{"content": "...", "source": "...", "score": ...}]
        
    Returns:
        构建好的提示词字符串
    """
    # ========== 1. 系统角色说明 ==========
    system_role = """你是医疗辅助 AI，专门帮助用户理解医疗健康相关信息。"""
    
    # ========== 2. 重要声明 ==========
    disclaimer = """【重要声明】
- 本 AI 不提供医疗诊断
- 本 AI 不替代专业医生的诊断和治疗建议
- 本 AI 仅提供基于医学资料的信息参考
- 如有健康问题，请及时咨询专业医生"""
    
    # ========== 3. 检索到的医学资料 ==========
    if docs:
        context_section = "【检索到的医学资料】\n\n"
        context_parts = []
        for i, doc in enumerate(docs, 1):
            title = doc.get('title', 'unknown')
            content = doc.get('content', '')
            metadata = doc.get('metadata', {})
            source = metadata.get('source', 'unknown')
            context_parts.append(f"资料 {i}（标题：{title}，来源：{source}）：\n{content}")
        context_section += "\n\n".join(context_parts)
    else:
        context_section = "【检索到的医学资料】\n\n未检索到相关医学资料。"
    
    # ========== 4. 用户问题 ==========
    question_section = f"""【用户问题】
{question}"""
    
    # ========== 5. 回答要求 ==========
#     instructions = """【回答要求】
# 1. 请主要基于上述提供的医学资料回答用户的问题，但也可以根据其他知识回答用户的问题
# 2. 如果提供的资料不足以回答用户的问题，请明确说明：根据提供的资料，无法确定相关信息，建议咨询专业医生
# 3. 回答要客观、准确，避免过度解读
# 4. 如果资料中有多个相关信息，请综合说明
# 5. 回答末尾请提醒用户：以上信息仅供参考，如有不适请及时就医"""
    instructions = """只是站位用的要求，不必理会"""
    
    # ========== 构建完整 Prompt ==========
    prompt = f"""{system_role}

{disclaimer}

{context_section}

{question_section}

{instructions}

请开始回答："""
    
    return prompt
