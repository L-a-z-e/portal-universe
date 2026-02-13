"""Input validation utilities for XSS prevention.

Provides reusable Pydantic field validators that mirror Java's @NoXss
and NestJS NoXss decorator patterns.
"""

import re

XSS_PATTERNS = [
    re.compile(r"<script\b[^>]*>", re.IGNORECASE),
    re.compile(r"javascript:", re.IGNORECASE),
    re.compile(r"on\w+\s*=", re.IGNORECASE),
    re.compile(r"data:\s*text/html", re.IGNORECASE),
    re.compile(r"vbscript:", re.IGNORECASE),
    re.compile(r"expression\s*\(", re.IGNORECASE),
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
