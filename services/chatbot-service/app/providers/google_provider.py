from langchain_core.language_models import BaseChatModel
from langchain_google_genai import ChatGoogleGenerativeAI

from app.providers.base import LLMProvider


class GoogleLLMProvider(LLMProvider):
    def __init__(self, api_key: str, model: str = "gemini-pro"):
        self._model = ChatGoogleGenerativeAI(google_api_key=api_key, model=model, temperature=0.1)

    def get_chat_model(self) -> BaseChatModel:
        return self._model
