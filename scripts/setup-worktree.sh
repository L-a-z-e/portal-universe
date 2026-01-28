#!/bin/bash
# Git Worktree 생성 및 공유 폴더 symlink 설정
# Usage: ./scripts/setup-worktree.sh <worktree-path> <branch>
#
# Example:
#   ./scripts/setup-worktree.sh ../portal-universe-docs dev

set -e

WORKTREE_PATH=$1
BRANCH=$2
MAIN_REPO=$(cd "$(dirname "$0")/.." && pwd)

if [ -z "$WORKTREE_PATH" ] || [ -z "$BRANCH" ]; then
    echo "Usage: $0 <worktree-path> <branch>"
    exit 1
fi

# 절대 경로로 변환
WORKTREE_ABS_PATH=$(cd "$(dirname "$WORKTREE_PATH")" 2>/dev/null && pwd)/$(basename "$WORKTREE_PATH") 2>/dev/null || WORKTREE_ABS_PATH="$WORKTREE_PATH"

# 메인 repo에서 실행 방지 (순환참조 방지)
if [ "$WORKTREE_ABS_PATH" = "$MAIN_REPO" ]; then
    echo "Error: Worktree path cannot be the main repository"
    exit 1
fi

echo "Creating worktree at $WORKTREE_PATH for branch $BRANCH..."

# 브랜치 존재 여부 확인
if git show-ref --verify --quiet "refs/heads/$BRANCH"; then
    git worktree add "$WORKTREE_PATH" "$BRANCH"
else
    echo "Branch '$BRANCH' not found. Creating from current branch..."
    git worktree add -b "$BRANCH" "$WORKTREE_PATH"
fi

cd "$WORKTREE_PATH"

# Symlink helper function (이미 존재하면 스킵)
safe_symlink() {
    local src=$1
    local dest=$2
    if [ -e "$src" ] && [ ! -e "$dest" ] && [ ! -L "$dest" ]; then
        ln -s "$src" "$dest"
        echo "  - $dest → $src"
    elif [ -L "$dest" ]; then
        echo "  - $dest (already linked, skipped)"
    fi
}

echo "Creating symlinks..."

# Symlink 대상 목록 (gitignored files/folders)
SYMLINK_TARGETS=(
    ".claude"
    "certs"
    ".env"
    ".env.local"
    ".env.docker"
    ".mcp.json"
)

for target in "${SYMLINK_TARGETS[@]}"; do
    safe_symlink "$MAIN_REPO/$target" "$target"
done

# Worktree의 common git dir에 exclude 패턴 추가
# Git worktree는 info/exclude를 common git dir에서 읽음 (worktree-specific dir 아님)
# .gitignore의 trailing slash 패턴(예: certs/)은 디렉토리만 매칭하고
# symlink는 매칭하지 않으므로 info/exclude로 보완
GIT_COMMON_DIR=$(git rev-parse --git-common-dir)
EXCLUDE_FILE="$GIT_COMMON_DIR/info/exclude"
mkdir -p "$GIT_COMMON_DIR/info"

# 기존 exclude 파일에 중복 없이 패턴 추가
add_exclude_pattern() {
    local pattern=$1
    if ! grep -qxF "$pattern" "$EXCLUDE_FILE" 2>/dev/null; then
        echo "$pattern" >> "$EXCLUDE_FILE"
        echo "  - Added '$pattern' to info/exclude"
    fi
}

# 헤더 추가 (최초 1회)
if ! grep -q "setup-worktree.sh" "$EXCLUDE_FILE" 2>/dev/null; then
    echo "" >> "$EXCLUDE_FILE"
    echo "# Added by setup-worktree.sh - symlink 보호 패턴" >> "$EXCLUDE_FILE"
fi

for target in "${SYMLINK_TARGETS[@]}"; do
    add_exclude_pattern "$target"
done

# 검증: symlink가 git에 의해 무시되는지 확인
echo ""
echo "Verifying symlinks are properly ignored..."
LEAKED=""
for target in "${SYMLINK_TARGETS[@]}"; do
    if [ -e "$target" ] || [ -L "$target" ]; then
        STATUS=$(git status --porcelain "$target" 2>/dev/null)
        if [ -n "$STATUS" ]; then
            LEAKED="$LEAKED  ⚠ $target is NOT ignored by git\n"
        fi
    fi
done

if [ -n "$LEAKED" ]; then
    echo -e "\n⚠ WARNING: 다음 symlink가 git에 의해 무시되지 않습니다:"
    echo -e "$LEAKED"
    echo "수동으로 확인해주세요."
    exit 1
else
    echo "  ✓ All symlinks are properly ignored by git"
fi

echo ""
echo "✓ Worktree 설정 완료: $WORKTREE_PATH ($BRANCH)"
