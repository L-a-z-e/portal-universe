"""Security 함수 단위 테스트."""

import pytest

from app.core.error_codes import ChatbotErrorCode
from app.core.exceptions import BusinessException
from app.core.security import get_current_user_id, require_admin


class TestGetCurrentUserId:
    """get_current_user_id 함수 테스트."""

    def test_get_user_id_with_header(self):
        """유효한 user_id가 전달되면 그대로 반환한다."""
        result = get_current_user_id("user-1")
        assert result == "user-1"

    def test_get_user_id_missing_header(self):
        """user_id가 None이면 CH020 AUTHENTICATION_REQUIRED 예외."""
        with pytest.raises(BusinessException) as exc_info:
            get_current_user_id(None)
        assert exc_info.value.code == ChatbotErrorCode.AUTHENTICATION_REQUIRED.code
        assert exc_info.value.status_code == 401


class TestRequireAdmin:
    """require_admin 함수 테스트."""

    def test_require_admin_with_super_admin_role(self):
        """ROLE_SUPER_ADMIN 역할이 있으면 user_id를 반환한다."""
        result = require_admin("u1", "ROLE_SUPER_ADMIN")
        assert result == "u1"

    def test_require_admin_with_blog_admin_role(self):
        """ROLE_BLOG_ADMIN 역할이 있으면 user_id를 반환한다."""
        result = require_admin("u1", "ROLE_BLOG_ADMIN")
        assert result == "u1"

    def test_require_admin_with_shopping_admin_role(self):
        """ROLE_SHOPPING_ADMIN 역할이 있으면 user_id를 반환한다."""
        result = require_admin("u1", "ROLE_SHOPPING_ADMIN")
        assert result == "u1"

    def test_require_admin_with_multiple_roles(self):
        """쉼표 구분 다중 역할 중 관리자 역할이 있으면 통과."""
        result = require_admin("u1", "ROLE_USER,ROLE_SUPER_ADMIN")
        assert result == "u1"

    def test_require_admin_no_user_id(self):
        """user_id가 None이면 CH020 AUTHENTICATION_REQUIRED 예외."""
        with pytest.raises(BusinessException) as exc_info:
            require_admin(None, "ROLE_SUPER_ADMIN")
        assert exc_info.value.code == ChatbotErrorCode.AUTHENTICATION_REQUIRED.code
        assert exc_info.value.status_code == 401

    def test_require_admin_no_admin_role(self):
        """admin이 아닌 역할이면 CH021 ADMIN_REQUIRED 예외."""
        with pytest.raises(BusinessException) as exc_info:
            require_admin("u1", "ROLE_USER")
        assert exc_info.value.code == ChatbotErrorCode.ADMIN_REQUIRED.code
        assert exc_info.value.status_code == 403

    def test_require_admin_no_roles_header(self):
        """roles 헤더가 모두 None이면 CH021 ADMIN_REQUIRED 예외 (fail-secure)."""
        with pytest.raises(BusinessException) as exc_info:
            require_admin("u1", None, None)
        assert exc_info.value.code == ChatbotErrorCode.ADMIN_REQUIRED.code
        assert exc_info.value.status_code == 403

    def test_require_admin_effective_roles_priority(self):
        """X-User-Effective-Roles가 X-User-Roles보다 우선한다."""
        result = require_admin("u1", "ROLE_SUPER_ADMIN", "ROLE_USER")
        assert result == "u1"

    def test_require_admin_fallback_to_x_user_roles(self):
        """effective-roles가 None이면 x-user-roles로 fallback."""
        result = require_admin("u1", None, "ROLE_SUPER_ADMIN")
        assert result == "u1"
