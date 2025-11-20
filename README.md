# End-to-End Bank Application Deployment using DevSecOps on AWS EKS

A comprehensive multi-tier banking application built with Java Spring Boot, demonstrating enterprise-grade DevSecOps practices with automated CI/CD pipelines, security scanning, and cloud-native deployment on AWS EKS.

![Login diagram](images/login.png)
![Transactions diagram](images/transactions.png)

## Table of Contents
- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
  - [1. AWS Infrastructure Setup](#1-aws-infrastructure-setup)
  - [2. EKS Cluster Creation](#2-eks-cluster-creation)
  - [3. Jenkins Installation & Configuration](#3-jenkins-installation--configuration)
  - [4. Security Tools Setup](#4-security-tools-setup)
  - [5. ArgoCD Installation](#5-argocd-installation)
  - [6. Email Notifications](#6-email-notifications)
  - [7. Jenkins Plugins & Integrations](#7-jenkins-plugins--integrations)
  - [8. Pipeline Configuration](#8-pipeline-configuration)
  - [9. Application Deployment](#9-application-deployment)
- [Monitoring with Prometheus & Grafana](#monitoring-with-prometheus--grafana)
- [Cleanup](#cleanup)
- [Troubleshooting](#troubleshooting)

## Overview

This project implements a production-ready banking application with a complete DevSecOps pipeline, featuring:
- **Automated CI/CD**: Jenkins pipelines for continuous integration and deployment
- **Security First**: Integrated OWASP dependency checks, Trivy scanning, and SonarQube analysis
- **GitOps Workflow**: ArgoCD for declarative Kubernetes deployments
- **Cloud Native**: Deployed on AWS EKS with auto-scaling and monitoring
- **Observability**: Prometheus and Grafana for comprehensive monitoring

## Tech Stack

| Category | Technology | Purpose |
|----------|-----------|---------|
| **Source Control** | GitHub | Code repository and version control |
| **Containerization** | Docker | Application packaging and deployment |
| **CI/CD** | Jenkins | Continuous Integration pipeline |
| **Security Scanning** | OWASP, Trivy, SonarQube | Dependency checks, vulnerability scanning, code quality |
| **GitOps** | ArgoCD | Continuous Deployment and GitOps workflow |
| **Orchestration** | AWS EKS (Kubernetes) | Container orchestration and management |
| **Monitoring** | Prometheus & Grafana | Metrics collection and visualization |
| **Package Management** | Helm | Kubernetes application deployment |

## Architecture

The application follows a multi-tier architecture:
- **Frontend/API Layer**: Spring Boot application serving REST APIs
- **Database Layer**: MySQL for persistent data storage
- **Infrastructure Layer**: Kubernetes on AWS EKS with auto-scaling
- **CI/CD Layer**: Jenkins for build automation, ArgoCD for deployment
- **Security Layer**: Multiple scanning tools integrated into the pipeline
- **Monitoring Layer**: Prometheus for metrics, Grafana for dashboards

## Prerequisites

**System Requirements:**
- AWS Account with administrative access
- IAM user with access keys and secret access keys
- Root or sudo access on the master machine
- Basic knowledge of Kubernetes, Docker, and CI/CD concepts

**AWS Resources:**
- 1 EC2 instance (t2.medium) with 29 GB storage for Jenkins Master
- EKS Cluster with 2 worker nodes (t2.medium each)
- Security groups configured for required ports

> [!NOTE]
> This project is configured for the **us-west-1 (North California)** region. Adjust region parameters if deploying elsewhere.

**Required Ports:**
| Port | Service | Purpose |
|------|---------|---------|
| 22 | SSH | Remote access |
| 80 | HTTP | Web traffic |
| 443 | HTTPS | Secure web traffic |
| 465 | SMTPS | Email notifications |
| 8081 | Jenkins | CI/CD server |
| 9000 | SonarQube | Code quality analysis |
| 30000-32767 | NodePort | Kubernetes services |

![Security Group Configuration](https://github.com/user-attachments/assets/4e5ecd37-fe2e-4e4b-a6ba-14c7b62715a3)

## Installation & Setup

### 1. AWS Infrastructure Setup

**Switch to root user:**
```bash
sudo su
```

**Create Master EC2 Instance:**
- Instance type: t2.medium
- Storage: 29 GB
- Region: us-west-1
- Configure security group with ports listed above

### 2. EKS Cluster Creation

**Install AWS CLI** ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/AWSCLI/AWSCLI.sh))
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install unzip -y
unzip awscliv2.zip
sudo ./aws/install
aws configure
```

**Install kubectl** ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/Kubectl/Kubectl.sh))
```bash
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client
```

**Install eksctl** ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/eksctl%20/eksctl.sh))
```bash
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version
```

**Create EKS Cluster:**
```bash
eksctl create cluster --name=bankapp \
                    --region=us-west-1 \
                    --version=1.30 \
                    --without-nodegroup
```

**Associate IAM OIDC Provider:**
```bash
eksctl utils associate-iam-oidc-provider \
  --region us-west-1 \
  --cluster bankapp \
  --approve
```

**Create Node Group:**
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

> [!NOTE]
> Ensure the SSH public key `eks-nodegroup-key` exists in your AWS account before creating the node group.

### 3. Jenkins Installation & Configuration

**Install Jenkins:**
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
```

**Change Jenkins Port (8080 → 8081):**

The BankApp application runs on port 8080, so we need to change Jenkins to port 8081.

1. Edit the Jenkins service file:
   ```bash
   sudo nano /usr/lib/systemd/system/jenkins.service
   ```
2. Change the `JENKINS_PORT` environment variable to `8081`
   
   ![Jenkins Port Configuration](https://github.com/user-attachments/assets/6320ae49-82d4-4ae3-9811-bd6f06778483)

3. Reload the systemd daemon:
   ```bash
   sudo systemctl daemon-reload
   ```

4. Restart Jenkins:
   ```bash
   sudo systemctl restart jenkins
   ```

### 4. Security Tools Setup

**Install Docker:**
```bash
sudo apt install docker.io -y
sudo usermod -aG docker ubuntu && newgrp docker
```

**Install and Configure SonarQube:**
```bash
docker run -itd --name SonarQube-Server -p 9000:9000 sonarqube:lts-community
```

**Install Trivy:**
```bash
sudo apt-get install wget apt-transport-https gnupg lsb-release -y
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update -y
sudo apt-get install trivy -y
```

### 5. ArgoCD Installation

**Create ArgoCD namespace:**
```bash
kubectl create namespace argocd
```

**Apply ArgoCD manifest:**
```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

**Verify all pods are running:**
```bash
watch kubectl get pods -n argocd
```

**Install ArgoCD CLI:**
```bash
curl --silent --location -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/download/v2.4.7/argocd-linux-amd64
chmod +x /usr/local/bin/argocd
```

**Expose ArgoCD Server:**
```bash
# Check current services
kubectl get svc -n argocd

# Change service type from ClusterIP to NodePort
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort"}}'

# Verify the change
kubectl get svc -n argocd
```

**Access ArgoCD UI:**

1. Note the NodePort assigned to argocd-server
   ![ArgoCD Port](https://github.com/user-attachments/assets/a2932e03-ebc7-42a6-9132-82638152197f)

2. Open security group and allow the NodePort

3. Access in browser: `http://<worker-node-public-ip>:<nodeport>`
   ![ArgoCD Login 1](https://github.com/user-attachments/assets/29d9cdbd-5b7c-44b3-bb9b-1d091d042ce3)
   ![ArgoCD Login 2](https://github.com/user-attachments/assets/08f4e047-e21c-4241-ba68-f9b719a4a39a)
   ![ArgoCD Dashboard](https://github.com/user-attachments/assets/1ffa85c3-9055-49b4-aab0-0947b95f0dd2)

4. Get initial admin password:
   ```bash
   kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
   ```

5. Login with username: `admin` and the password from step 4

6. Update your password in **User Info** section

### 6. Email Notifications

**Configure Gmail App Password:**

> [!IMPORTANT]
> Ensure 2-step verification is enabled on your Google account before proceeding.

1. Open Gmail and navigate to **Manage your Google Account → Security**
   ![Gmail Security](https://github.com/user-attachments/assets/5ab9dc9d-dcce-4f9d-9908-01095f1253cb)

2. Search for **App password** and create an app password for Jenkins
   ![App Password 1](https://github.com/user-attachments/assets/701752da-7703-4685-8f06-fe1f65dd1b9c)
   ![App Password 2](https://github.com/user-attachments/assets/adc8d8c0-8be4-4319-9042-4115abb5c6fc)

3. In Jenkins, go to **Manage Jenkins → Credentials** and add your Gmail credentials
   ![Jenkins Email Credentials](https://github.com/user-attachments/assets/2a42ec62-87c8-43c8-a034-7be0beb8824e)

4. Configure **Extended E-mail Notification** in **Manage Jenkins → System**
   ![Extended Email](https://github.com/user-attachments/assets/bac81e24-bb07-4659-a251-955966feded8)

5. Configure **E-mail Notification** settings

   > [!IMPORTANT]
   > Use the app password generated in step 2 in the password field under **E-mail Notification → Advanced**

   ![Email Config 1](https://github.com/user-attachments/assets/14e254fc-1400-457e-b3f4-046404b66950)
   ![Email Config 2](https://github.com/user-attachments/assets/7be70b3a-b0dc-415c-838a-b1c6fd87c182)
   ![Email Config 3](https://github.com/user-attachments/assets/cffb6e1d-4838-483e-97e0-6851c204ab21)

### 7. Jenkins Plugins & Integrations

**Install Required Plugins:**

Navigate to **Manage Jenkins → Plugins → Available plugins** and install:
- OWASP Dependency-Check
- SonarQube Scanner
- Docker
- Pipeline: Stage View

**Configure OWASP Dependency-Check:**

Go to **Manage Jenkins → Tools** and configure OWASP
![OWASP Configuration](https://github.com/user-attachments/assets/da6a26d3-f742-4ea8-86b7-107b1650a7c2)
![OWASP Tools](https://github.com/user-attachments/assets/3b8c3f20-202e-4864-b3b6-b48d7a604ee8)

**Configure SonarQube Integration:**

1. Login to SonarQube and create a token for Jenkins
   - Navigate to **Administration → Security → Users → Token**
   ![SonarQube Token 1](https://github.com/user-attachments/assets/86ad8284-5da6-4048-91fe-ac20c8e4514a)
   ![SonarQube Token 2](https://github.com/user-attachments/assets/6bc671a5-c122-45c0-b1f0-f29999bbf751)
   ![SonarQube Token 3](https://github.com/user-attachments/assets/e748643a-e037-4d4c-a9be-944995979c60)

2. Add SonarQube credentials in Jenkins (**Manage Jenkins → Credentials**)
   ![SonarQube Credentials](https://github.com/user-attachments/assets/0688e105-2170-4c3f-87a3-128c1a05a0b8)

3. Configure SonarQube Scanner in **Manage Jenkins → Tools**
   ![SonarQube Scanner](https://github.com/user-attachments/assets/2fdc1e56-f78c-43d2-914a-104ec2c8ea86)

4. Configure SonarQube server in **Manage Jenkins → System**
   ![SonarQube Server](https://github.com/user-attachments/assets/ae866185-cb2b-4e83-825b-a125ec97243a)

5. Create webhook in SonarQube (**Administration → Webhook**)
   ![SonarQube Webhook 1](https://github.com/user-attachments/assets/16527e72-6691-4fdf-a8d2-83dd27a085cb)
   ![SonarQube Webhook 2](https://github.com/user-attachments/assets/a8b45948-766a-49a4-b779-91ac3ce0443c)

**Configure Docker Credentials:**

Add Docker Hub credentials in **Manage Jenkins → Credentials**
![Docker Credentials](https://github.com/user-attachments/assets/77402c9c-fc2f-4df7-9a06-09f3f4c38751)

**Configure GitHub Credentials:**

Add GitHub Personal Access Token in **Manage Jenkins → Credentials**
![GitHub Credentials](https://github.com/user-attachments/assets/4d0c1a47-621e-4aa2-a0b1-71927fcdaef4)

> [!NOTE]
> Use your GitHub Personal Access Token in the password field when adding GitHub credentials.

**Configure Global Trusted Pipeline Libraries:**

Go to **Manage Jenkins → System** and configure the shared library
![Pipeline Library 1](https://github.com/user-attachments/assets/874b2e03-49b9-4c26-9b0f-bd07ce70c0f1)
![Pipeline Library 2](https://github.com/user-attachments/assets/1ca83b43-ce85-4970-941d-9a819ce4ecfd)

### 8. Pipeline Configuration

**Add EKS Cluster to ArgoCD:**

1. Login to ArgoCD from CLI:
   ```bash
   argocd login <argocd-url>:<nodeport> --username admin
   ```
   > [!TIP]
   > Replace `<argocd-url>:<nodeport>` with your ArgoCD server URL (e.g., `52.53.156.187:32738`)

   ![ArgoCD CLI Login](https://github.com/user-attachments/assets/7d05e5ca-1a16-4054-a321-b99270ca0bf9)

2. List available clusters:
   ```bash
   argocd cluster list
   ```
   ![ArgoCD Cluster List](https://github.com/user-attachments/assets/76fe7a45-e05c-422d-9652-bdaee02d630f)

3. Get your EKS cluster context name:
   ```bash
   kubectl config get-contexts
   ```
   ![Kubectl Contexts](https://github.com/user-attachments/assets/c9afca1f-b5a3-4685-ae24-cc206a3e3ef1)

4. Add your EKS cluster to ArgoCD:
   ```bash
   argocd cluster add <your-cluster-context> --name bankapp-eks-cluster
   ```
   > [!TIP]
   > Replace `<your-cluster-context>` with your actual EKS cluster context name (e.g., `Madhup@bankapp.us-west-1.eksctl.io`)

   ![Add Cluster](https://github.com/user-attachments/assets/1061fe66-17ec-47b7-9d2e-371f58d3fd90)

5. Verify cluster in ArgoCD UI (**Settings → Clusters**)
   ![Verify Cluster](https://github.com/user-attachments/assets/6aebb871-4dea-4e09-955a-a4aa43b8f4ef)

**Connect GitHub Repository to ArgoCD:**

1. Go to **Settings → Repositories** and click **Connect Repo**
   ![Connect Repo 1](https://github.com/user-attachments/assets/cc8728e5-546b-4c46-bd4c-538f4cd6a63d)
   ![Connect Repo 2](https://github.com/user-attachments/assets/e665203d-0ebe-4839-af9e-f5866dce5e1b)
   ![Connect Repo 3](https://github.com/user-attachments/assets/b9b869c3-698b-4303-83cc-9ccec66542a3)

   > [!NOTE]
   > Ensure the connection status shows as successful

**Create Jenkins Jobs:**

1. Create **BankApp-CI** job (Continuous Integration pipeline)
   ![CI Job 1](https://github.com/user-attachments/assets/17467b79-3110-470a-87a2-2bbfe197551b)
   ![CI Job 2](https://github.com/user-attachments/assets/51d79ab0-e1f4-4c4d-a778-0c28119f5da9)

2. Create **BankApp-CD** job (Continuous Deployment pipeline) using the same configuration

**Set Docker Socket Permissions:**

```bash
chmod 777 /var/run/docker.sock
```
![Docker Socket](https://github.com/user-attachments/assets/e231c62a-7adb-4335-b67e-480758713dbf)

### 9. Application Deployment

**Create ArgoCD Application:**

1. In ArgoCD UI, go to **Applications** and click **New App**
   ![New App](https://github.com/user-attachments/assets/d5b08e06-6256-4f46-afdc-fc43a9e44562)

2. Configure application settings

   > [!IMPORTANT]
   > Enable the **Auto-Create Namespace** option when creating the ArgoCD application

   ![App Config 1](https://github.com/user-attachments/assets/6a828910-41ba-4f0c-af05-19297321a41b)
   ![App Config 2](https://github.com/user-attachments/assets/a3aa1d22-50ef-4eb1-97fe-9c3ffb504fc3)

**Access the Application:**

1. Verify deployment is successful
   ![Deployment Success](https://github.com/user-attachments/assets/03f3b69a-d6e0-42ad-992e-11124e7d0898)

2. Open port 30080 in the worker node security group

3. Access the application in your browser:
   ```
   http://<worker-node-public-ip>:30080
   ```

**Email Notification Example:**
![Email Notification](https://github.com/user-attachments/assets/407f94ed-bf67-441a-bd28-881b6b8739b2)

## Monitoring with Prometheus & Grafana
This section covers setting up comprehensive monitoring for your EKS cluster using Prometheus for metrics collection and Grafana for visualization.

**Install Helm:**

```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```

**Add Helm Repositories:**

```bash
# Add stable charts repository
helm repo add stable https://charts.helm.sh/stable

# Add Prometheus community repository
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

# Update repositories
helm repo update
```

**Create Prometheus Namespace:**

```bash
kubectl create namespace prometheus
kubectl get ns
```

**Install Prometheus Stack:**

```bash
helm install stable prometheus-community/kube-prometheus-stack -n prometheus
```

**Verify Installation:**

```bash
# Check all pods are running
kubectl get pods -n prometheus

# Check services
kubectl get svc -n prometheus
```

**Expose Prometheus to External Access:**

1. Edit Prometheus service to change from ClusterIP to NodePort:
   ```bash
   kubectl edit svc stable-kube-prometheus-sta-prometheus -n prometheus
   ```

   > [!IMPORTANT]
   > Change `type: ClusterIP` to `type: NodePort`, save the file, and open the assigned NodePort in the security group

   ![Prometheus Service 1](https://github.com/user-attachments/assets/90f5dc11-23de-457d-bbcb-944da350152e)
   ![Prometheus Service 2](https://github.com/user-attachments/assets/ed94f40f-c1f9-4f50-a340-a68594856cc7)

2. Verify the service change:
   ```bash
   kubectl get svc -n prometheus
   ```

**Expose Grafana to External Access:**

1. Edit Grafana service to change from ClusterIP to NodePort:
   ```bash
   kubectl edit svc stable-grafana -n prometheus
   ```
   ![Grafana Service](https://github.com/user-attachments/assets/4a2afc1f-deba-48da-831e-49a63e1a8fb6)

2. Verify the service change:
   ```bash
   kubectl get svc -n prometheus
   ```

**Access Grafana:**

1. Get Grafana admin password:
   ```bash
   kubectl get secret --namespace prometheus stable-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
   ```

   > [!NOTE]
   > Default username is `admin`

2. Access Grafana in your browser: `http://<worker-node-public-ip>:<grafana-nodeport>`

3. View pre-configured dashboards for Kubernetes monitoring:
   ![Grafana Dashboard 1](https://github.com/user-attachments/assets/d2e7ff2f-059d-48c4-92bb-9711943819c4)
   ![Grafana Dashboard 2](https://github.com/user-attachments/assets/647b2b22-cd83-41c3-855d-7c60ae32195f)
   ![Grafana Dashboard 3](https://github.com/user-attachments/assets/cb98a281-a4f5-46af-98eb-afdb7da6b35a)

## Cleanup

When you're done with the project and want to tear down the infrastructure:

**Delete EKS Cluster:**

```bash
eksctl delete cluster --name=bankapp --region=us-west-1
```

> [!WARNING]
> This will delete all resources associated with the EKS cluster including node groups, load balancers, and persistent volumes. Ensure you have backed up any important data before proceeding.

**Additional Cleanup Steps:**

- Terminate the Jenkins Master EC2 instance
- Remove security groups created for the project
- Delete any EBS volumes that weren't automatically removed
- Remove IAM roles and policies created for the cluster

## Troubleshooting

**Common Issues and Solutions:**

| Issue | Solution |
|-------|----------|
| Jenkins port conflict | Ensure Jenkins is running on port 8081 and the application on 8080 |
| Docker permission denied | Run `sudo usermod -aG docker $USER && newgrp docker` |
| ArgoCD pods not starting | Check if the namespace exists and has sufficient resources |
| SonarQube container fails | Increase vm.max_map_count: `sysctl -w vm.max_map_count=262144` |
| EKS cluster creation fails | Verify AWS credentials and IAM permissions |
| Application not accessible | Check security group rules and NodePort configuration |
| Grafana password not working | Retrieve password again using the kubectl command |

**Useful Commands:**

```bash
# Check Jenkins status
sudo systemctl status jenkins

# View Docker logs
docker logs <container-name>

# Check Kubernetes pod logs
kubectl logs <pod-name> -n <namespace>

# Describe pod for troubleshooting
kubectl describe pod <pod-name> -n <namespace>

# Check ArgoCD application sync status
argocd app get <app-name>

# Force ArgoCD sync
argocd app sync <app-name>
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the [MIT License](LICENSE).

---

**Project maintained by the DevSecOps team**
