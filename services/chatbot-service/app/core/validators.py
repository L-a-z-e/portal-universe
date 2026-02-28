"""Input validation utilities for XSS and prompt injection prevention.

Provides reusable Pydantic field validators that mirror Java's @NoXss
and NestJS NoXss decorator patterns.
"""

import logging
import re

logger = logging.getLogger(__name__)

XSS_PATTERNS = [
    re.compile(r"<script\b[^>]*>", re.IGNORECASE),
    re.compile(r"javascript:", re.IGNORECASE),
    re.compile(r"on\w+\s*=", re.IGNORECASE),
    re.compile(r"data:\s*text/html", re.IGNORECASE),
    re.compile(r"vbscript:", re.IGNORECASE),
    re.compile(r"expression\s*\(", re.IGNORECASE),
]

PROMPT_INJECTION_PATTERNS = [
    re.compile(r"이전\s*(지시|명령|규칙).*무시", re.IGNORECASE),
    re.compile(r"ignore\s+(previous|above|all)\s+(instructions?|rules?|prompts?)", re.IGNORECASE),
    re.compile(r"(system\s*prompt|시스템\s*프롬프트)", re.IGNORECASE),
    re.compile(r"(reveal|show|print|출력).*?(instructions?|rules?|prompt)", re.IGNORECASE),
    re.compile(r"you\s+are\s+now\s+", re.IGNORECASE),
    re.compile(r"(새로운|new)\s*(역할|role|persona)", re.IGNORECASE),
    re.compile(r"forget\s+(everything|all)", re.IGNORECASE),
    re.compile(r"do\s+not\s+follow", re.IGNORECASE),
]


def check_no_xss(value: str) -> str:
    """Validate that a string does not contain XSS patterns.

    Raises:
        ValueError: If XSS pattern is detected.
    """
    for pattern in XSS_PATTERNS:
        if pattern.search(value):
            raise ValueError("Input contains potentially dangerous content")
    return value


def check_prompt_injection(value: str) -> bool:
    """Check if a string contains prompt injection patterns.

    Returns True if suspicious patterns are detected (warning only, not blocking).
    """
    for pattern in PROMPT_INJECTION_PATTERNS:
        if pattern.search(value):
            logger.warning(
                "Prompt injection pattern detected: %s",
                pattern.pattern,
            )
            return True
    return False
