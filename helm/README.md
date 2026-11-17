# Helm Module — BankApp Chart

> Helm chart for deploying the Spring Boot BankApp with MySQL StatefulSet, autoscaling (HPA + VPA), Ingress, and persistent storage on Kubernetes.

## Overview

The `bankapp` Helm chart packages all Kubernetes resources needed to run the banking application. Compared to the raw manifests in `kubernetes/`, this chart uses Go templates and a centralized `values.yaml` for environment-specific customization. MySQL is deployed as a **StatefulSet** (rather than a Deployment) for stable network identity and persistent storage.

## Architecture

![Helm Chart Dependency Graph](../docs/infrastructure/diagrams/helm-chart-deps.png)

## Chart Metadata

| Field | Value |
|-------|-------|
| API Version | v2 |
| Name | `bankapp` |
| Type | application |
| Chart Version | 0.1.0 |
| App Version | 1.16.0 |

## Template Inventory

| File | Kind | Description |
|------|------|-------------|
| `namespace.yml` | Namespace | Creates the target namespace |
| `secrets.yml` | Secret | MySQL and Spring datasource passwords (auto base64-encoded) |
| `configMap.yml` | ConfigMap | Database connection URL, username, database name |
| `persistentVolume.yml` | PersistentVolume | hostPath volume for MySQL data |
| `persistentVolumeClaim.yml` | PersistentVolumeClaim | Storage claim for the PV |
| `mysqlStatefulSet.yml` | StatefulSet | MySQL with liveness/readiness probes and volume claim template |
| `mysqlService.yml` | Service (Headless) | ClusterIP: None for StatefulSet DNS |
| `deployment.yml` | Deployment | BankApp with init container waiting for MySQL, resource limits, and health probes |
| `service.yml` | Service (NodePort) | Exposes BankApp on configurable NodePort |
| `ingress.yml` | Ingress | NGINX Ingress for domain-based routing |
| `hpa.yaml` | HorizontalPodAutoscaler | CPU-based horizontal scaling |
| `vpa.yaml` | VerticalPodAutoscaler | Automatic resource right-sizing |
| `NOTES.txt` | *(post-install)* | Usage instructions displayed after `helm install` |

## Variable Catalog (`values.yaml`)

### Namespace

| Key | Default | Description |
|-----|---------|-------------|
| `namespace` | `bankapp-namespace` | Target Kubernetes namespace |

### ConfigMap

| Key | Default | Description |
|-----|---------|-------------|
| `configmap.name` | `bankapp-config` | ConfigMap resource name |
| `configmap.data.MYSQL_DATABASE` | `BankDB` | MySQL database name |
| `configmap.data.SPRING_DATASOURCE_USERNAME` | `root` | MySQL username |

### Database (StatefulSet)

| Key | Default | Description |
|-----|---------|-------------|
| `db_statefulset.name` | `mysql` | StatefulSet and pod name |
| `db_statefulset.storage` | `5Gi` | PV/PVC storage size |

### Application Deployment

| Key | Default | Description |
|-----|---------|-------------|
| `app_deployment.name` | `bankapp` | Deployment name and label selector |
| `app_deployment.cpu_req` | `80m` | CPU request |
| `app_deployment.cpu_limit` | `800m` | CPU limit |
| `app_deployment.mem_req` | `150Mi` | Memory request |
| `app_deployment.mem_limit` | `700Mi` | Memory limit |

### Container Images

| Key | Default | Description |
|-----|---------|-------------|
| `image.app` | `trainwithshubham/springboot-bankapp:latest` | BankApp container image |
| `image.db` | `mysql:latest` | MySQL container image |

### BankApp Service

| Key | Default | Description |
|-----|---------|-------------|
| `bankapp_svc.port` | `8080` | Service port |
| `bankapp_svc.targetPort` | `8080` | Container target port |
| `bankapp_svc.nodePort` | `30080` | NodePort for external access |

### MySQL Service

| Key | Default | Description |
|-----|---------|-------------|
| `mysql_svc.port` | `3306` | Service port |
| `mysql_svc.targetPort` | `3306` | Container target port |

### HPA (Horizontal Pod Autoscaler)

| Key | Default | Description |
|-----|---------|-------------|
| `hpa.min_replica` | `1` | Minimum pod count |
| `hpa.max_replica` | `5` | Maximum pod count |
| `hpa.cpu_utilizatoion` | `40` | Target average CPU utilization (%) |

### Secrets

| Key | Default | Description |
|-----|---------|-------------|
| `secret.name` | `mysql-secret` | Secret resource name |
| `secret.data.MYSQL_ROOT_PASSWORD` | `Test@123` | MySQL root password (auto base64-encoded by template) |
| `secret.data.SPRING_DATASOURCE_PASSWORD` | `Test@123` | Spring datasource password (auto base64-encoded by template) |

## Prerequisites

- Helm 3.x installed
- Kubernetes cluster with `kubectl` configured
- **NGINX Ingress Controller** installed
- **Metrics Server** installed (for HPA)
- **VPA CRD** installed (for VPA)

```bash
# Install prerequisites
helm upgrade --install ingress-nginx ingress-nginx \
    --repo https://kubernetes.github.io/ingress-nginx \
    --namespace ingress-nginx --create-namespace

helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/
helm upgrade --install metrics-server metrics-server/metrics-server

kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.0/vertical-pod-autoscaler/deploy/vpa-v1-crd-gen.yaml
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.0/vertical-pod-autoscaler/deploy/vpa-rbac.yaml
```

## Usage

### Install

```bash
helm install bankapp bankapp/
```

### Multi-Environment

```bash
# Dev environment with different namespace and NodePort
helm install bankapp-dev bankapp/ \
  --set namespace=dev-namespace \
  --set bankapp_svc.nodePort=30081

# Staging with custom image
helm install bankapp-staging bankapp/ \
  --set namespace=staging-namespace \
  --set image.app=myregistry/bankapp:v2.0.0
```

### Upgrade

```bash
helm upgrade bankapp bankapp/ --set image.app=myregistry/bankapp:v2.1.0
```

### Uninstall

```bash
helm uninstall bankapp
```

## Verification

```bash
# Check Helm release
helm list

# Check all deployed resources
kubectl get all -n bankapp-namespace

# Access the application
# Via NodePort: http://<node-ip>:30080
# Via Ingress:  http://bankapp.local
```
