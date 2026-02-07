"""Security 함수 단위 테스트."""

import pytest
from fastapi import HTTPException

from app.core.security import get_current_user_id, require_admin


class TestGetCurrentUserId:
    """get_current_user_id 함수 테스트."""

    def test_get_user_id_with_header(self):
        """유효한 user_id가 전달되면 그대로 반환한다."""
        result = get_current_user_id("user-1")
        assert result == "user-1"

    def test_get_user_id_missing_header(self):
        """user_id가 None이면 401 HTTPException이 발생한다."""
        with pytest.raises(HTTPException) as exc_info:
            get_current_user_id(None)
        assert exc_info.value.status_code == 401
        assert "Authentication required" in exc_info.value.detail


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

    def test_require_admin_no_user_id(self):
        """user_id가 None이면 401 HTTPException이 발생한다."""
        with pytest.raises(HTTPException) as exc_info:
            require_admin(None, "ROLE_SUPER_ADMIN")
        assert exc_info.value.status_code == 401

    def test_require_admin_no_admin_role(self):
        """admin이 아닌 역할이면 403 HTTPException이 발생한다."""
        with pytest.raises(HTTPException) as exc_info:
            require_admin("u1", "ROLE_USER")
        assert exc_info.value.status_code == 403
        assert "Admin access required" in exc_info.value.detail

    def test_require_admin_no_roles_header(self):
        """roles 헤더가 None이면 하위 호환으로 user_id를 반환한다."""
        result = require_admin("u1", None, None)
        assert result == "u1"
