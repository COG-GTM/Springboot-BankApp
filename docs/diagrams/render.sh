#!/usr/bin/env bash
# Re-renders the architecture diagrams from their Graphviz sources. Requires graphviz (`dot`).
set -euo pipefail
cd "$(dirname "$0")/../.."
dot -Gdpi=110 -Tpng docs/diagrams/cicd-flow.dot -o images/flow.png
dot -Gdpi=110 -Tpng docs/diagrams/eks-deployment.dot -o images/eks-deployment.png
