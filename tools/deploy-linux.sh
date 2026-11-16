#!/usr/bin/env bash
set -euo pipefail

# 用法：deploy-linux.sh <包含 jar/dist/dist-portal 的发布目录>
# 依赖：/opt/laminar-ops/current 软链、systemctl 服务 laminar-ops、后端 8080 health。
RELEASE_SRC="${1:?usage: deploy-linux.sh <release-dir>}"
APP_ROOT="/opt/laminar-ops"
STAMP="$(date +%Y%m%d_%H%M%S)"
RELEASE_DIR="$APP_ROOT/releases/$STAMP"
PREVIOUS="$(readlink -f "$APP_ROOT/current" 2>/dev/null || true)"

mkdir -p "$APP_ROOT/releases" "$RELEASE_DIR"
cp -a "$RELEASE_SRC"/. "$RELEASE_DIR"/

ln -sfn "$RELEASE_DIR" "$APP_ROOT/current"
systemctl restart laminar-ops

for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
    echo "部署成功：$RELEASE_DIR"
    exit 0
  fi
  sleep 2
done

echo "健康检查失败，开始回滚" >&2
if [ -n "$PREVIOUS" ] && [ -d "$PREVIOUS" ]; then
  ln -sfn "$PREVIOUS" "$APP_ROOT/current"
  systemctl restart laminar-ops
  sleep 5
  if curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
    echo "已回滚到 $PREVIOUS" >&2
  else
    echo "回滚后健康检查仍失败，请人工介入" >&2
  fi
fi
exit 1
