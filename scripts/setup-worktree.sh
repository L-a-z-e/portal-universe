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

# Symlink gitignored files/folders
safe_symlink "$MAIN_REPO/.claude" ".claude"
safe_symlink "$MAIN_REPO/certs" "certs"
safe_symlink "$MAIN_REPO/.env" ".env"
safe_symlink "$MAIN_REPO/.env.local" ".env.local"
safe_symlink "$MAIN_REPO/.env.docker" ".env.docker"
safe_symlink "$MAIN_REPO/.mcp.json" ".mcp.json"

echo "✓ Worktree 설정 완료: $WORKTREE_PATH ($BRANCH)"
