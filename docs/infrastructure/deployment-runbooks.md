# Deployment Runbooks

> Step-by-step operational procedures for deploying, updating, monitoring, and troubleshooting the BankApp infrastructure.

---

## Table of Contents

1. [Runbook 1: Fresh EKS Cluster Setup](#runbook-1-fresh-eks-cluster-setup)
2. [Runbook 2: Deploy with Raw Kubernetes Manifests](#runbook-2-deploy-with-raw-kubernetes-manifests)
3. [Runbook 3: Deploy with Helm](#runbook-3-deploy-with-helm)
4. [Runbook 4: Set Up CI/CD Pipeline (Jenkins + ArgoCD)](#runbook-4-set-up-cicd-pipeline-jenkins--argocd)
5. [Runbook 5: Rolling Update via CI/CD](#runbook-5-rolling-update-via-cicd)
6. [Runbook 6: Local Development with Docker Compose](#runbook-6-local-development-with-docker-compose)
7. [Runbook 7: Set Up Monitoring (Prometheus + Grafana)](#runbook-7-set-up-monitoring-prometheus--grafana)
8. [Runbook 8: TLS Certificate Management](#runbook-8-tls-certificate-management)
9. [Runbook 9: Troubleshooting Guide](#runbook-9-troubleshooting-guide)
10. [Runbook 10: Cluster Teardown](#runbook-10-cluster-teardown)

---

## Runbook 1: Fresh EKS Cluster Setup

**When to use:** First-time infrastructure provisioning on AWS.

### Prerequisites

- AWS account with IAM user (access key + secret key)
- Local machine with internet access

### Steps

#### 1.1 Install CLI Tools

```bash
# AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install unzip -y
unzip awscliv2.zip
sudo ./aws/install
aws configure  # Enter access key, secret key, region (us-west-1), output (json)

# kubectl
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client

# eksctl
curl --silent --location \
  "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" \
  | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version
```

#### 1.2 Create EKS Cluster

```bash
eksctl create cluster --name=bankapp \
    --region=us-west-1 \
    --version=1.30 \
    --without-nodegroup
```

#### 1.3 Associate IAM OIDC Provider

```bash
eksctl utils associate-iam-oidc-provider \
    --region us-west-1 \
    --cluster bankapp \
    --approve
```

#### 1.4 Create Node Group

```bash
eksctl create nodegroup --cluster=bankapp \
    --region=us-west-1 \
    --name=bankapp \
    --node-type=t2.medium \
    --nodes=2 \
    --nodes-min=2 \
    --nodes-max=2 \
    --node-volume-size=29 \
    --ssh-access \
    --ssh-public-key=eks-nodegroup-key
```

> **Note:** Ensure the SSH key `eks-nodegroup-key` exists in your AWS account in the `us-west-1` region.

#### 1.5 Verify Cluster

```bash
kubectl get nodes
kubectl cluster-info
```

### Estimated Time: 20–30 minutes

---

## Runbook 2: Deploy with Raw Kubernetes Manifests

**When to use:** Direct deployment without Helm, or when ArgoCD syncs the `kubernetes/` directory.

### Prerequisites

- EKS cluster running (Runbook 1)
- NGINX Ingress Controller installed
- cert-manager installed
- Metrics Server installed

### Steps

#### 2.1 Install Cluster Prerequisites

```bash
# NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
kubectl get pods -n ingress-nginx  # Wait for Running

# cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.1/cert-manager.yaml
kubectl get pods -n cert-manager  # Wait for Running

# Metrics Server (for HPA)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

#### 2.2 Apply Manifests

```bash
cd kubernetes/

# Apply in dependency order
kubectl apply -f bankapp-namespace.yaml
kubectl apply -f secrets.yaml
kubectl apply -f configmap.yaml
kubectl apply -f persistent-volume.yaml
kubectl apply -f persistent-volume-claim.yaml
kubectl apply -f mysql-deployment.yml
kubectl apply -f mysql-service.yaml

# Wait for MySQL to be ready
kubectl wait --for=condition=ready pod -l app=mysql -n bankapp-namespace --timeout=120s

kubectl apply -f bankapp-deployment.yml
kubectl apply -f bankapp-service.yaml
kubectl apply -f letsencrypt-clusterissuer.yaml
kubectl apply -f bankapp-ingress.yml
kubectl apply -f bankapp-hpa.yml
```

#### 2.3 Verify Deployment

```bash
kubectl get all -n bankapp-namespace
kubectl get ingress -n bankapp-namespace
kubectl get certificate -n bankapp-namespace
kubectl get hpa -n bankapp-namespace
```

#### 2.4 Access Application

- **Via NodePort:** `http://<worker-node-public-ip>:30080`
- **Via Ingress:** `https://megaproject.trainwithshubham.com` (after DNS configuration)

### Estimated Time: 10–15 minutes (after prerequisites)

---

## Runbook 3: Deploy with Helm

**When to use:** Templated deployments with environment-specific values.

### Prerequisites

- EKS cluster running (Runbook 1)
- Helm 3 installed
- NGINX Ingress Controller, Metrics Server, and VPA CRD installed

### Steps

#### 3.1 Install Helm

```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```

#### 3.2 Install Helm Prerequisites

```bash
# NGINX Ingress Controller
helm upgrade --install ingress-nginx ingress-nginx \
    --repo https://kubernetes.github.io/ingress-nginx \
    --namespace ingress-nginx --create-namespace

# Metrics Server
helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/
helm upgrade --install metrics-server metrics-server/metrics-server

# Edit Metrics Server for EKS compatibility
kubectl edit deployments.apps metrics-server
# Add to spec.template.spec.containers[0].args:
#   - --kubelet-insecure-tls
#   - --kubelet-preferred-address-types=InternalIP,Hostname,ExternalIP

# VPA CRD
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.0/vertical-pod-autoscaler/deploy/vpa-v1-crd-gen.yaml
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.0/vertical-pod-autoscaler/deploy/vpa-rbac.yaml
```

#### 3.3 Deploy BankApp

```bash
cd helm/

# Default deployment
helm install bankapp bankapp/

# Or with custom values
helm install bankapp-prod bankapp/ \
    --set namespace=production \
    --set image.app=myregistry/bankapp:v1.0.0 \
    --set bankapp_svc.nodePort=30080 \
    --set secret.data.MYSQL_ROOT_PASSWORD=<strong-password> \
    --set secret.data.SPRING_DATASOURCE_PASSWORD=<strong-password>
```

#### 3.4 Verify

```bash
helm list
kubectl get all -n bankapp-namespace
```

### Estimated Time: 10 minutes

---

## Runbook 4: Set Up CI/CD Pipeline (Jenkins + ArgoCD)

**When to use:** Setting up the complete DevSecOps pipeline from scratch.

### Prerequisites

- EKS cluster running (Runbook 1)
- EC2 instance (t2.medium, 29 GB) for Jenkins
- Domain name pointed to worker node (for ArgoCD access)

### Steps

#### 4.1 Install Jenkins

```bash
sudo apt update -y
sudo apt install fontconfig openjdk-17-jre -y

sudo wget -O /usr/share/keyrings/jenkins-keyring.asc \
    https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc]" \
    https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
    /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update -y
sudo apt-get install jenkins -y

# Change Jenkins port from 8080 to 8081
sudo sed -i 's/JENKINS_PORT=8080/JENKINS_PORT=8081/' /usr/lib/systemd/system/jenkins.service
sudo systemctl daemon-reload
sudo systemctl restart jenkins
```

#### 4.2 Install Docker

```bash
sudo apt install docker.io -y
sudo usermod -aG docker ubuntu && newgrp docker
sudo chmod 777 /var/run/docker.sock
```

#### 4.3 Install Security Tools

```bash
# SonarQube
docker run -itd --name SonarQube-Server -p 9000:9000 sonarqube:lts-community

# Trivy
sudo apt-get install wget apt-transport-https gnupg lsb-release -y
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | \
    sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update -y
sudo apt-get install trivy -y
```

#### 4.4 Configure Jenkins

1. Access Jenkins at `http://<instance-ip>:8081`
2. Install plugins: OWASP, SonarQube Scanner, Docker, Pipeline: Stage View
3. Configure credentials:
   - `docker` — DockerHub username/password
   - `Github-cred` — GitHub username/PAT
   - SonarQube token (from SonarQube → Administration → Security → Users → Tokens)
4. Configure tools:
   - SonarQube Scanner → Name: `Sonar`
   - OWASP Dependency-Check → Name: `OWASP`
5. Configure Global Trusted Pipeline Libraries:
   - Name: `Shared`, Branch: `DevOps`, SCM: Git URL of this repository
6. Configure SonarQube server in Manage Jenkins → System
7. Create SonarQube webhook: `http://<jenkins-ip>:8081/sonarqube-webhook/`

#### 4.5 Install ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort"}}'

# Get initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```

#### 4.6 Configure ArgoCD

```bash
# Install CLI
curl --silent --location -o /usr/local/bin/argocd \
    https://github.com/argoproj/argo-cd/releases/download/v2.4.7/argocd-linux-amd64
chmod +x /usr/local/bin/argocd

# Login
argocd login <worker-ip>:<nodeport> --username admin

# Add EKS cluster
argocd cluster add <cluster-context-name> --name bankapp-eks-cluster

# Create application
argocd app create bankapp \
    --repo <git-repo-url> \
    --path kubernetes \
    --dest-server https://kubernetes.default.svc \
    --dest-namespace bankapp-namespace
```

#### 4.7 Create Jenkins Jobs

1. **BankApp-CI:** Pipeline from SCM → `Jenkinsfile` on `DevOps` branch
2. **BankApp-CD:** Pipeline from SCM → `GitOps/Jenkinsfile` on `DevOps` branch

### Estimated Time: 45–60 minutes

---

## Runbook 5: Rolling Update via CI/CD

**When to use:** Deploying a new version of the application.

### Steps

1. **Push code changes** to the `DevOps` branch
2. **CI Pipeline triggers automatically** (or manually with a `DOCKER_TAG` parameter)
3. CI pipeline:
   - Runs security scans (Trivy, OWASP, SonarQube)
   - Builds Docker image tagged with `DOCKER_TAG`
   - Pushes to DockerHub
   - Triggers CD pipeline
4. **CD Pipeline:**
   - Updates image tag in `kubernetes/bankapp-deployment.yaml`
   - Commits and pushes to `DevOps` branch
5. **ArgoCD** detects the Git change and syncs:
   - Performs rolling update of BankApp pods
   - Zero-downtime deployment (2 replicas)

### Verification

```bash
# Check ArgoCD sync status
argocd app get bankapp

# Check rollout status
kubectl rollout status deployment/bankapp-deploy -n bankapp-namespace

# Verify new image
kubectl get pods -n bankapp-namespace -o jsonpath='{.items[*].spec.containers[*].image}'
```

### Rollback

```bash
# Via ArgoCD
argocd app rollback bankapp

# Via kubectl
kubectl rollout undo deployment/bankapp-deploy -n bankapp-namespace
```

### Estimated Time: 5–10 minutes (automated)

---

## Runbook 6: Local Development with Docker Compose

**When to use:** Running the application locally for development and testing.

### Prerequisites

- Docker and Docker Compose installed
- Application Docker image available

### Steps

```bash
# Set environment variables
export DUSER=<your-dockerhub-username>
export IMAGE=<image-name:tag>

# Start the stack
docker compose up -d

# Check health
docker compose ps

# View application logs
docker compose logs -f mainapp

# Access application
open http://localhost:8080

# Stop the stack
docker compose down

# Stop and remove volumes (full cleanup)
docker compose down -v
```

### Health Checks

| Service | Check | Interval | Retries |
|---------|-------|----------|---------|
| MySQL | `mysqladmin ping -h localhost` | 10s | 3 |
| BankApp | `curl -f http://localhost:8080/actuator/health` | 10s | 5 |

### Estimated Time: 2–3 minutes

---

## Runbook 7: Set Up Monitoring (Prometheus + Grafana)

**When to use:** Adding observability to the EKS cluster.

### Steps

#### 7.1 Install Monitoring Stack

```bash
# Install Helm (if not already)
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh && ./get_helm.sh

# Add repos
helm repo add stable https://charts.helm.sh/stable
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

# Create namespace
kubectl create namespace prometheus

# Install kube-prometheus-stack
helm install stable prometheus-community/kube-prometheus-stack -n prometheus

# Verify
kubectl get pods -n prometheus
```

#### 7.2 Expose Dashboards

```bash
# Expose Prometheus
kubectl edit svc stable-kube-prometheus-sta-prometheus -n prometheus
# Change type: ClusterIP → type: NodePort

# Expose Grafana
kubectl edit svc stable-grafana -n prometheus
# Change type: ClusterIP → type: NodePort

# Get Grafana password
kubectl get secret --namespace prometheus stable-grafana \
    -o jsonpath="{.data.admin-password}" | base64 --decode; echo
# Username: admin
```

#### 7.3 Access Dashboards

- **Prometheus:** `http://<worker-ip>:<prometheus-nodeport>`
- **Grafana:** `http://<worker-ip>:<grafana-nodeport>`

> Open the assigned NodePorts in the EKS worker node security group.

### Estimated Time: 10–15 minutes

---

## Runbook 8: TLS Certificate Management

**When to use:** Setting up or troubleshooting HTTPS/TLS for the application.

### Via cert-manager (Kubernetes)

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.1/cert-manager.yaml

# Apply ClusterIssuer
kubectl apply -f kubernetes/letsencrypt-clusterissuer.yaml

# Apply Ingress (includes TLS config)
kubectl apply -f kubernetes/bankapp-ingress.yml

# Verify certificate
kubectl get certificate -n bankapp-namespace
kubectl describe certificate bankapp-tls-secret -n bankapp-namespace
```

### Via Certbot (EC2 / Nginx)

```bash
# Install
sudo apt install python3-certbot-nginx -y

# Generate certificate
sudo certbot --nginx -d bank.joakim.online

# Verify
curl -vI https://bank.joakim.online

# Test renewal
sudo certbot renew --dry-run

# Auto-renewal (crontab)
echo "0 0 1 * * certbot renew --quiet" | sudo tee -a /etc/crontab
```

---

## Runbook 9: Troubleshooting Guide

### Application Not Starting

```bash
# Check pod status and events
kubectl describe pod -l app=bankapp-deploy -n bankapp-namespace

# Check logs
kubectl logs -l app=bankapp-deploy -n bankapp-namespace --tail=100

# Check if MySQL is reachable
kubectl exec -it deploy/bankapp-deploy -n bankapp-namespace -- \
    nc -z mysql-svc.bankapp-namespace.svc.cluster.local 3306
```

### MySQL Connection Issues

```bash
# Check MySQL pod
kubectl describe pod -l app=mysql -n bankapp-namespace
kubectl logs -l app=mysql -n bankapp-namespace --tail=50

# Verify Secret values
kubectl get secret mysql-secret -n bankapp-namespace -o yaml

# Verify ConfigMap values
kubectl get configmap bankapp-config -n bankapp-namespace -o yaml
```

### Ingress / TLS Issues

```bash
# Check Ingress Controller
kubectl get pods -n ingress-nginx
kubectl logs -l app.kubernetes.io/name=ingress-nginx -n ingress-nginx --tail=50

# Check certificate status
kubectl get certificate -n bankapp-namespace
kubectl describe clusterissuer letsencrypt-prod

# Check Ingress events
kubectl describe ingress bankapp-ingress -n bankapp-namespace
```

### HPA Not Scaling

```bash
# Verify Metrics Server
kubectl get pods -n kube-system | grep metrics-server
kubectl top nodes
kubectl top pods -n bankapp-namespace

# Check HPA status
kubectl describe hpa bankapp-hpa -n bankapp-namespace
```

### ArgoCD Sync Issues

```bash
# Check app status
argocd app get bankapp

# Force sync
argocd app sync bankapp --force

# Check ArgoCD logs
kubectl logs -l app.kubernetes.io/name=argocd-application-controller -n argocd --tail=50
```

---

## Runbook 10: Cluster Teardown

**When to use:** Decommissioning the entire infrastructure.

### Steps

```bash
# 1. Delete ArgoCD applications
argocd app delete bankapp --cascade

# 2. Delete monitoring stack
helm uninstall stable -n prometheus
kubectl delete namespace prometheus

# 3. Delete application resources
kubectl delete -f kubernetes/
# OR
helm uninstall bankapp

# 4. Delete Ingress Controller
kubectl delete -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

# 5. Delete cert-manager
kubectl delete -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.1/cert-manager.yaml

# 6. Delete ArgoCD
kubectl delete namespace argocd

# 7. Delete EKS cluster
eksctl delete cluster --name=bankapp --region=us-west-1
```

> **Warning:** This is irreversible. Ensure all important data is backed up before proceeding.

### Estimated Time: 15–20 minutes

---

## Security Checklist

Before any production deployment, verify:

- [ ] MySQL passwords changed from defaults in `secrets.yaml` or `values.yaml`
- [ ] OWASP Dependency-Check passes with no critical vulnerabilities
- [ ] SonarQube Quality Gate passes
- [ ] Trivy filesystem scan shows no critical CVEs
- [ ] TLS certificates are valid and auto-renewal is configured
- [ ] Kubernetes RBAC is configured (not using default service accounts)
- [ ] Docker images use specific version tags (not `latest`)
- [ ] Resource limits are set for all containers
- [ ] Network policies restrict pod-to-pod communication
- [ ] AWS security groups follow least-privilege principle
