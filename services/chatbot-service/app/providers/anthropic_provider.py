from langchain_anthropic import ChatAnthropic
from langchain_core.language_models import BaseChatModel

from app.providers.base import LLMProvider


class AnthropicLLMProvider(LLMProvider):
    def __init__(self, api_key: str, model: str = "claude-3-5-sonnet-20241022"):
        self._model = ChatAnthropic(api_key=api_key, model=model, temperature=0.1)

    def get_chat_model(self) -> BaseChatModel:
        return self._model
