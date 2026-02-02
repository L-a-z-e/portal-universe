from fastapi import Header, HTTPException, status


def get_current_user_id(
    x_user_id: str | None = Header(None),
    x_user_email: str | None = Header(None),
) -> str:
    """Gateway에서 JWT 파싱 후 전달하는 X-User-Id 헤더에서 사용자 ID 추출.

    API Gateway가 JWT 검증을 담당하므로 chatbot-service는 헤더만 신뢰.
    """
    if not x_user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required",
        )
    return x_user_id


def require_admin(
    x_user_id: str | None = Header(None),
    x_user_roles: str | None = Header(None),
) -> str:
    """관리자 권한 확인. Gateway에서 전달하는 X-User-Roles 헤더 기반 RBAC.

    X-User-Roles가 없으면 일반 인증만 확인하여 하위 호환.
    """
    if not x_user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required",
        )
    if x_user_roles:
        roles = [r.strip().lower() for r in x_user_roles.split(",")]
        if "admin" not in roles and "role_admin" not in roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Admin access required",
            )
    return x_user_id
