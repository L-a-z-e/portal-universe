from fastapi import Header

from app.core.constants import ADMIN_ROLES
from app.core.error_codes import ChatbotErrorCode
from app.core.exceptions import BusinessException


def get_current_user_id(
    x_user_id: str | None = Header(None),
    x_user_email: str | None = Header(None),
) -> str:
    """Gateway에서 JWT 파싱 후 전달하는 X-User-Id 헤더에서 사용자 ID 추출.

    API Gateway가 JWT 검증을 담당하므로 chatbot-service는 헤더만 신뢰.
    """
    if not x_user_id:
        raise BusinessException.auth_required()
    return x_user_id


def require_admin(
    x_user_id: str | None = Header(None),
    x_user_effective_roles: str | None = Header(None),
    x_user_roles: str | None = Header(None),
) -> str:
    """관리자 권한 확인. Gateway에서 전달하는 역할 헤더 기반 RBAC.

    X-User-Effective-Roles를 우선 확인하고, 없으면 X-User-Roles로 fallback.
    """
    if not x_user_id:
        raise BusinessException.auth_required()
    roles_header = x_user_effective_roles or x_user_roles
    if roles_header:
        roles = [r.strip() for r in roles_header.split(",")]
        if not any(role in ADMIN_ROLES for role in roles):
            raise BusinessException.admin_required()
    return x_user_id
