#!/bin/bash
# Git Hooks 설치 스크립트
# Usage: ./scripts/install-hooks.sh
#
# Git worktree의 경우 common git dir에 hook을 설치합니다.

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
HOOKS_SRC="$SCRIPT_DIR/hooks"

# Git 디렉토리 찾기 (worktree의 경우 common dir 사용)
GIT_DIR=$(git rev-parse --git-dir 2>/dev/null)
GIT_COMMON_DIR=$(git rev-parse --git-common-dir 2>/dev/null)

# Hooks 디렉토리 결정
if [ -n "$GIT_COMMON_DIR" ] && [ "$GIT_DIR" != "$GIT_COMMON_DIR" ]; then
    # Worktree: common dir에 설치
    HOOKS_DEST="$GIT_COMMON_DIR/hooks"
    echo "Worktree 감지: common git dir에 hooks 설치"
else
    # Main repo
    HOOKS_DEST="$GIT_DIR/hooks"
fi

mkdir -p "$HOOKS_DEST"

# Hook 설치 함수
# Usage: install_hook <git-hook-name> <source-script-name>
install_hook() {
    local git_hook=$1     # e.g. "pre-commit"
    local src_name=$2     # e.g. "pre-commit-symlink-guard"
    local src_file="$HOOKS_SRC/$src_name"
    local dest_file="$HOOKS_DEST/$git_hook"

    if [ ! -f "$src_file" ]; then
        echo "  ⚠ Hook 소스 없음: $src_file"
        return 1
    fi

    # 기존 hook이 있으면 병합
    if [ -f "$dest_file" ] && [ ! -L "$dest_file" ]; then
        if ! grep -q "symlink-guard" "$dest_file" 2>/dev/null; then
            echo "  - 기존 $git_hook hook에 symlink-guard 추가"
            echo "" >> "$dest_file"
            echo "# Added by install-hooks.sh - symlink guard" >> "$dest_file"
            echo 'REPO_ROOT=$(git rev-parse --show-toplevel)' >> "$dest_file"
            echo "\"\$REPO_ROOT/scripts/hooks/$src_name\" || exit 1" >> "$dest_file"
            return 0
        else
            echo "  - $git_hook hook에 symlink-guard 이미 존재 (skipped)"
            return 0
        fi
    fi

    # 새 hook 생성
    # Worktree에서는 show-toplevel이 worktree 경로를 반환하므로
    # git-common-dir 기반 main repo 경로도 fallback으로 사용
    cat > "$dest_file" << HOOK_SCRIPT
#!/bin/bash
# Git $git_hook Hook
# Installed by: scripts/install-hooks.sh

REPO_ROOT=\$(git rev-parse --show-toplevel)
MAIN_REPO=\$(cd "\$(git rev-parse --git-common-dir)/.." && pwd)

GUARD_SCRIPT=""
if [ -f "\$REPO_ROOT/scripts/hooks/$src_name" ]; then
    GUARD_SCRIPT="\$REPO_ROOT/scripts/hooks/$src_name"
elif [ -f "\$MAIN_REPO/scripts/hooks/$src_name" ]; then
    GUARD_SCRIPT="\$MAIN_REPO/scripts/hooks/$src_name"
fi

if [ -n "\$GUARD_SCRIPT" ]; then
    "\$GUARD_SCRIPT" || exit 1
fi

exit 0
HOOK_SCRIPT

    chmod +x "$dest_file"
    echo "  ✓ $git_hook hook 설치됨: $dest_file"
}

echo "Installing git hooks..."
install_hook "pre-commit" "pre-commit-symlink-guard"

echo ""
echo "✓ Git hooks 설치 완료"
