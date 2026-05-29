# Variable Catalog

> Complete reference of all configurable parameters across all infrastructure modules. Use this as a single source of truth when customizing deployments.

## Kubernetes Manifests (`kubernetes/`)

### ConfigMap: `bankapp-config`

| Key | Default Value | Used By | Description |
|-----|---------------|---------|-------------|
| `MYSQL_DATABASE` | `BankDB` | MySQL Deployment, BankApp Deployment | Target MySQL database name |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql-svc.bankapp-namespace.svc.cluster.local:3306/BankDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | BankApp Deployment | Full JDBC connection string |
| `SPRING_DATASOURCE_USERNAME` | `root` | BankApp Deployment | MySQL connection username |

### Secret: `mysql-secret`

| Key | Encoding | Used By | Description |
|-----|----------|---------|-------------|
| `MYSQL_ROOT_PASSWORD` | Base64 | MySQL Deployment | Root password for MySQL server |
| `SPRING_DATASOURCE_PASSWORD` | Base64 | BankApp Deployment | Password for Spring JDBC connection |

### Deployment: `bankapp-deploy`

| Parameter | Value | Description |
|-----------|-------|-------------|
| `replicas` | `2` | Initial pod count |
| `image` | `trainwithshubham/bankapp-eks:v2` | Container image and tag |
| `containerPort` | `8080` | Application listening port |
| `resources.requests.memory` | `512Mi` | Memory request |
| `resources.requests.cpu` | `250m` | CPU request |
| `resources.limits.memory` | `1Gi` | Memory limit |
| `resources.limits.cpu` | `500m` | CPU limit |

### Deployment: `mysql`

| Parameter | Value | Description |
|-----------|-------|-------------|
| `replicas` | `1` | Single-instance database |
| `image` | `mysql:8.0` | MySQL container image |
| `containerPort` | `3306` | MySQL listening port |
| `volumeMount.mountPath` | `/var/lib/mysql` | MySQL data directory |
| `volumeMount.subPath` | `mysql-data` | Sub-directory within PV |

### PersistentVolume: `mysql-pv`

| Parameter | Value | Description |
|-----------|-------|-------------|
| `capacity.storage` | `10Gi` | Total volume capacity |
| `accessModes` | `ReadWriteOnce` | Single-node read-write |
| `persistentVolumeReclaimPolicy` | `Retain` | Keeps data after PVC deletion |
| `storageClassName` | `standard` | Must match cluster storage class |
| `hostPath.path` | `/mnt/data/mysql` | Host filesystem path |

### PersistentVolumeClaim: `mysql-pvc`

| Parameter | Value | Description |
|-----------|-------|-------------|
| `storage` | `10Gi` | Requested storage size |
| `accessModes` | `ReadWriteOnce` | Single-node read-write |
| `storageClassName` | `standard` | Must match PV storage class |

### HorizontalPodAutoscaler: `bankapp-hpa`

| Parameter | Value | Description |
|-----------|-------|-------------|
| `minReplicas` | `1` | Minimum pod count |
| `maxReplicas` | `5` | Maximum pod count |
| `metrics.cpu.averageUtilization` | `40` | Scale-up threshold (%) |

### Ingress: `bankapp-ingress`

| Parameter | Value | Description |
|-----------|-------|-------------|
| `ingressClassName` | `nginx` | Ingress controller class |
| `host` | `megaproject.trainwithshubham.com` | Public hostname |
| `tls.secretName` | `bankapp-tls-secret` | TLS certificate secret |
| `annotations.cluster-issuer` | `letsencrypt-prod` | cert-manager issuer |
| `annotations.ssl-redirect` | `true` | Force HTTPS |
| `annotations.proxy-body-size` | `50m` | Max request body |

### ClusterIssuer: `letsencrypt-prod`

| Parameter | Value | Description |
|-----------|-------|-------------|
| `acme.server` | `https://acme-v02.api.letsencrypt.org/directory` | Let's Encrypt production endpoint |
| `acme.email` | `trainwithshubham@gmail.com` | Certificate notification email |
| `solvers.http01.ingress.class` | `nginx` | Challenge solver class |

---

## Helm Chart (`helm/bankapp/values.yaml`)

### Core Settings

| Key | Default | Description |
|-----|---------|-------------|
| `namespace` | `bankapp-namespace` | Target Kubernetes namespace |
| `configmap.name` | `bankapp-config` | ConfigMap resource name |
| `configmap.data.MYSQL_DATABASE` | `BankDB` | MySQL database name |
| `configmap.data.SPRING_DATASOURCE_USERNAME` | `root` | Database username |

### Database

| Key | Default | Description |
|-----|---------|-------------|
| `db_statefulset.name` | `mysql` | StatefulSet name |
| `db_statefulset.storage` | `5Gi` | PV/PVC/VolumeClaimTemplate storage |
| `image.db` | `mysql:latest` | MySQL container image |

### Application

| Key | Default | Description |
|-----|---------|-------------|
| `app_deployment.name` | `bankapp` | Deployment name |
| `app_deployment.cpu_req` | `80m` | CPU request |
| `app_deployment.cpu_limit` | `800m` | CPU limit |
| `app_deployment.mem_req` | `150Mi` | Memory request |
| `app_deployment.mem_limit` | `700Mi` | Memory limit |
| `image.app` | `trainwithshubham/springboot-bankapp:latest` | App container image |

### Services

| Key | Default | Description |
|-----|---------|-------------|
| `bankapp_svc.port` | `8080` | BankApp service port |
| `bankapp_svc.targetPort` | `8080` | BankApp container port |
| `bankapp_svc.nodePort` | `30080` | BankApp NodePort |
| `mysql_svc.port` | `3306` | MySQL service port |
| `mysql_svc.targetPort` | `3306` | MySQL container port |

### Autoscaling

| Key | Default | Description |
|-----|---------|-------------|
| `hpa.min_replica` | `1` | Minimum pod count |
| `hpa.max_replica` | `5` | Maximum pod count |
| `hpa.cpu_utilizatoion` | `40` | Target CPU utilization (%) |

### Secrets

| Key | Default | Description |
|-----|---------|-------------|
| `secret.name` | `mysql-secret` | Secret resource name |
| `secret.data.MYSQL_ROOT_PASSWORD` | `Test@123` | MySQL root password (auto base64-encoded) |
| `secret.data.SPRING_DATASOURCE_PASSWORD` | `Test@123` | Spring datasource password (auto base64-encoded) |

---

## Docker Compose (`docker-compose.yml`)

### MySQL Service

| Variable | Value | Description |
|----------|-------|-------------|
| `MYSQL_ROOT_PASSWORD` | `Test@123` | MySQL root password |
| `MYSQL_DATABASE` | `BankDB` | Database to create on startup |
| Volume | `bankapp-volume:/var/lib/mysql` | Named volume for data persistence |

### BankApp Service

| Variable | Value | Description |
|----------|-------|-------------|
| `SPRING_DATASOURCE_USERNAME` | `root` | Database username |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/BankDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC URL (uses Docker DNS) |
| `SPRING_DATASOURCE_PASSWORD` | `Test@123` | Database password |
| `DUSER` | *(env var)* | DockerHub username (for image name) |
| `IMAGE` | *(env var)* | Image name (for image tag) |
| Port mapping | `8080:8080` | Host-to-container port |

---

## Jenkins CI Pipeline (`Jenkinsfile`)

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `DOCKER_TAG` | String | `''` | Docker image tag for the build |

### Environment Variables

| Variable | Source | Description |
|----------|--------|-------------|
| `SONAR_HOME` | Jenkins tool `Sonar` | SonarQube Scanner installation path |

### Hard-Coded Values

| Value | Location | Description |
|-------|----------|-------------|
| `bankapp` | Docker build stage | Docker image name |
| `madhupdevops` | Docker build/push stages | DockerHub username |
| `Sonar` | SonarQube stage | SonarQube server configuration name |
| `BankApp-CD` | Post-success | Downstream CD job name |

---

## Jenkins CD Pipeline (`GitOps/Jenkinsfile`)

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `DOCKER_TAG` | String | `''` | Docker image tag from CI job |

### Hard-Coded Values

| Value | Location | Description |
|-------|----------|-------------|
| `trainwithshubham/bankapp-eks` | sed command | Image name in K8s manifest |
| `DevOps` | Git push | Target branch |

---

## EKS Cluster Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| Cluster name | `bankapp` | EKS cluster name |
| Region | `us-west-1` | AWS region |
| K8s version | `1.30` | Kubernetes version |
| Node type | `t2.medium` | EC2 instance type |
| Node count | `2` | Number of worker nodes |
| Node volume | `29 GB` | EBS volume per node |
| SSH key | `eks-nodegroup-key` | EC2 key pair for SSH access |
