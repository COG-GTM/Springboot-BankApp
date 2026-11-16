# Architecture diagrams

| Source | Rendered image | Used in |
| --- | --- | --- |
| [cicd-flow.dot](cicd-flow.dot) | [images/flow.png](../../images/flow.png) | `cicd.md`, `README.md`, `README-K8S.md` |
| [eks-deployment.dot](eks-deployment.dot) | [images/eks-deployment.png](../../images/eks-deployment.png) | `cicd.md`, `README.md`, `README-K8S.md` |

The sources are [Graphviz](https://graphviz.org/) DOT files. After editing one, re-render both PNGs from the repo root:

```bash
sudo apt-get install -y graphviz   # once
./docs/diagrams/render.sh
```

Keep the diagrams in step with `Jenkinsfile`, `GitOps/Jenkinsfile`, `kubernetes/` and the setup steps in `README.md`.
