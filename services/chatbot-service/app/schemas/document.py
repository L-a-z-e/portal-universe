from pydantic import BaseModel


class DocumentInfo(BaseModel):
    document_id: str
    filename: str
    chunks: int
    status: str  # "indexed" | "processing" | "error"


class DocumentList(BaseModel):
    documents: list[DocumentInfo]
    total: int
