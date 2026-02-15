"""Application constants.

비즈니스 로직 상수 및 enum 값을 정의합니다.
환경 변수로 제어해야 하는 값은 config.py의 Settings에 정의합니다.
"""

# Admin roles for RBAC
ADMIN_ROLES = {"ROLE_SUPER_ADMIN", "ROLE_BLOG_ADMIN", "ROLE_SHOPPING_ADMIN"}
