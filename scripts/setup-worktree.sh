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

echo "Creating worktree at $WORKTREE_PATH for branch $BRANCH..."
git worktree add "$WORKTREE_PATH" "$BRANCH"

cd "$WORKTREE_PATH"

# Symlink gitignored files/folders
[ -d "$MAIN_REPO/.claude" ] && ln -s "$MAIN_REPO/.claude" .claude
[ -d "$MAIN_REPO/certs" ] && ln -s "$MAIN_REPO/certs" certs
[ -f "$MAIN_REPO/.env" ] && ln -s "$MAIN_REPO/.env" .env
[ -f "$MAIN_REPO/.env.local" ] && ln -s "$MAIN_REPO/.env.local" .env.local
[ -f "$MAIN_REPO/.env.docker" ] && ln -s "$MAIN_REPO/.env.docker" .env.docker
[ -f "$MAIN_REPO/.mcp.json" ] && ln -s "$MAIN_REPO/.mcp.json" .mcp.json

echo "✓ Worktree 설정 완료: $WORKTREE_PATH ($BRANCH)"
echo "  Symlinks:"
[ -d "$MAIN_REPO/.claude" ] && echo "  - .claude → $MAIN_REPO/.claude"
[ -d "$MAIN_REPO/certs" ] && echo "  - certs → $MAIN_REPO/certs"
[ -f "$MAIN_REPO/.env" ] && echo "  - .env → $MAIN_REPO/.env"
[ -f "$MAIN_REPO/.env.local" ] && echo "  - .env.local → $MAIN_REPO/.env.local"
[ -f "$MAIN_REPO/.env.docker" ] && echo "  - .env.docker → $MAIN_REPO/.env.docker"
[ -f "$MAIN_REPO/.mcp.json" ] && echo "  - .mcp.json → $MAIN_REPO/.mcp.json"
