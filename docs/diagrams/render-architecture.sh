#!/usr/bin/env bash
# Renders docs/diagrams/architecture.dot to images/architecture.{png,svg}.
# Needs Graphviz (e.g. `sudo apt-get install -y graphviz fonts-dejavu`).
set -euo pipefail
cd "$(dirname "$0")/../.."
dot -Tpng -Gdpi=110 docs/diagrams/architecture.dot -o images/architecture.png
dot -Tsvg docs/diagrams/architecture.dot -o images/architecture.svg
