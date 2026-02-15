from pydantic import BaseModel

from app.schemas.enums import DocumentStatus


class DocumentInfo(BaseModel):
    document_id: str
    filename: str
    chunks: int
    status: DocumentStatus


class DocumentList(BaseModel):
    documents: list[DocumentInfo]
    total: int
