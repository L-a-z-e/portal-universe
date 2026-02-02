from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.core.config import settings


def create_text_splitter() -> RecursiveCharacterTextSplitter:
    """문서 청킹용 텍스트 분할기 생성."""
    return RecursiveCharacterTextSplitter(
        chunk_size=settings.rag_chunk_size,
        chunk_overlap=settings.rag_chunk_overlap,
        separators=["\n\n", "\n", ". ", " ", ""],
        length_function=len,
    )
