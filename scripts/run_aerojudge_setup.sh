#!/bin/bash
#
# AeroJudge setup launcher for prepared base images.
# This script fetches the current judge_setup.sh and runs it.

set -Eeuo pipefail

RAW_BASE_URL="${AEROJUDGE_RAW_BASE_URL:-https://raw.githubusercontent.com/AeroJudge/aerojudge-device-app/main}"
SETUP_SCRIPT="/home/judge/judge_setup.sh"

cd /home/judge

echo "Fetching AeroJudge setup script from:"
echo "  $RAW_BASE_URL/scripts/judge_setup.sh"

curl -fsSL "$RAW_BASE_URL/scripts/judge_setup.sh" -o "$SETUP_SCRIPT"
chmod +x "$SETUP_SCRIPT"

echo
echo "Starting AeroJudge setup..."
exec "$SETUP_SCRIPT"
