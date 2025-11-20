# End-to-End Bank Application Deployment using DevSecOps on AWS EKS

This is a multi-tier banking application written in Java (Spring Boot) that demonstrates a complete DevSecOps pipeline deployment on AWS EKS (Elastic Kubernetes Service).

![Login diagram](images/login.png)
![Transactions diagram](images/transactions.png)

## Table of Contents
- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Deployment Steps](#deployment-steps)
  - [1. Setup Master Machine](#1-setup-master-machine)
  - [2. Create EKS Cluster](#2-create-eks-cluster)
  - [3. Install and Configure Tools](#3-install-and-configure-tools)
  - [4. Configure Jenkins](#4-configure-jenkins)
  - [5. Setup Email Notifications](#5-setup-email-notifications)
  - [6. Configure ArgoCD](#6-configure-argocd)
  - [7. Create Jenkins Jobs](#7-create-jenkins-jobs)
  - [8. Deploy Application](#8-deploy-application)
- [Monitoring Setup](#monitoring-setup)
- [Clean Up](#clean-up)

## Overview

This project implements a comprehensive DevSecOps pipeline for deploying a Spring Boot banking application with automated security scanning, quality checks, and continuous deployment to AWS EKS using GitOps principles.

## Tech Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Source Control** | GitHub | Code repository and version control |
| **Containerization** | Docker | Application containerization |
| **CI/CD** | Jenkins | Continuous Integration pipeline |
| **Security Scanning** | OWASP, Trivy | Dependency and filesystem vulnerability scanning |
| **Code Quality** | SonarQube | Static code analysis and quality gates |
| **GitOps** | ArgoCD | Continuous Deployment and sync |
| **Orchestration** | AWS EKS (Kubernetes) | Container orchestration |
| **Monitoring** | Prometheus & Grafana | Metrics collection and visualization |
| **Package Management** | Helm | Kubernetes package manager |

## Architecture

This deployment follows a GitOps-based DevSecOps architecture:
1. **CI Pipeline**: Jenkins builds, scans, and pushes Docker images
2. **Security Gates**: OWASP dependency checks, Trivy scans, and SonarQube analysis
3. **CD Pipeline**: ArgoCD automatically deploys to EKS based on Git repository changes
4. **Monitoring**: Prometheus collects metrics, Grafana provides dashboards

## Prerequisites

Before starting the deployment, ensure you have:
- AWS account with appropriate permissions
- IAM user with access keys and secret access keys
- SSH key pair for EKS node groups (named `eks-nodegroup-key`)
- Gmail account with 2-step verification enabled (for email notifications)
- Docker Hub account (for storing container images)
- GitHub account with Personal Access Token
- Root or sudo access on the master machine

> [!NOTE]
> This project is configured for the **us-west-1** (North California) region.

## Deployment Steps

### 1. Setup Master Machine

#### 1.1 Create EC2 Instance
Create an EC2 instance on AWS with the following specifications:
- **Instance Type**: t2.medium
- **Storage**: 29 GB
- **Region**: us-west-1 (North California)

#### 1.2 Configure Security Group
Open the following ports in your security group:

![image](https://github.com/user-attachments/assets/4e5ecd37-fe2e-4e4b-a6ba-14c7b62715a3)

#### 1.3 Switch to Root User
```bash
sudo su
```

### 2. Create EKS Cluster
#### 2.1 Install AWS CLI
Configure AWS CLI with your IAM credentials ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/AWSCLI/AWSCLI.sh)):

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install unzip -y
unzip awscliv2.zip
sudo ./aws/install
aws configure
```

When prompted, enter your IAM user's access keys and secret access keys.

#### 2.2 Install kubectl
Install the Kubernetes command-line tool ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/Kubectl/Kubectl.sh)):

```bash
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client
```

#### 2.3 Install eksctl
Install the EKS cluster management tool ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/eksctl%20/eksctl.sh)):

```bash
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version
```

#### 2.4 Create EKS Cluster
Create the EKS control plane:

```bash
eksctl create cluster --name=bankapp \
                      --region=us-west-1 \
                      --version=1.30 \
                      --without-nodegroup
```

#### 2.5 Associate IAM OIDC Provider
Enable IAM roles for service accounts:

```bash
eksctl utils associate-iam-oidc-provider \
  --region us-west-1 \
  --cluster bankapp \
  --approve
```

#### 2.6 Create Node Group
Create worker nodes for the cluster:

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
> Ensure the SSH key pair `eks-nodegroup-key` exists in your AWS account before running this command.
### 3. Install and Configure Tools

#### 3.1 Install Jenkins

Install Jenkins on the master machine:

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

**Change Jenkins Default Port**

Since the BankApp application runs on port 8080, change Jenkins to port 8081:

1. Edit the Jenkins service file:
   ```bash
   sudo nano /usr/lib/systemd/system/jenkins.service
   ```
2. Change the `JENKINS_PORT` environment variable to `8081`

   ![image](https://github.com/user-attachments/assets/6320ae49-82d4-4ae3-9811-bd6f06778483)

3. Reload the daemon and restart Jenkins:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl restart jenkins
   ```

#### 3.2 Install Docker

Install Docker and add the ubuntu user to the docker group:

```bash
sudo apt install docker.io -y
sudo usermod -aG docker ubuntu && newgrp docker
```

#### 3.3 Install and Configure SonarQube

Run SonarQube as a Docker container:

```bash
docker run -itd --name SonarQube-Server -p 9000:9000 sonarqube:lts-community
```

Access SonarQube at `http://<master-ip>:9000` (default credentials: admin/admin)

#### 3.4 Install Trivy

Install Trivy for container and filesystem vulnerability scanning:

```bash
sudo apt-get install wget apt-transport-https gnupg lsb-release -y
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update -y
sudo apt-get install trivy -y
```

#### 3.5 Install and Configure ArgoCD

**Create ArgoCD Namespace**
```bash
kubectl create namespace argocd
```

**Deploy ArgoCD**
```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

**Wait for Pods to be Ready**
```bash
watch kubectl get pods -n argocd
```

**Install ArgoCD CLI**
```bash
curl --silent --location -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/download/v2.4.7/argocd-linux-amd64
chmod +x /usr/local/bin/argocd
```

**Expose ArgoCD Server**

Change the ArgoCD server service from ClusterIP to NodePort:
```bash
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort"}}'
kubectl get svc -n argocd
```

Check the NodePort assigned and expose it in the security group of your EKS worker node.

![image](https://github.com/user-attachments/assets/a2932e03-ebc7-42a6-9132-82638152197f)

**Access ArgoCD UI**

Access ArgoCD at `http://<worker-node-public-ip>:<nodeport>` in your browser. Click "Advanced" and proceed.

![image](https://github.com/user-attachments/assets/29d9cdbd-5b7c-44b3-bb9b-1d091d042ce3)
![image](https://github.com/user-attachments/assets/08f4e047-e21c-4241-ba68-f9b719a4a39a)
![image](https://github.com/user-attachments/assets/1ffa85c3-9055-49b4-aab0-0947b95f0dd2)

**Get Initial Admin Password**
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```

- **Username**: admin
- **Password**: Use the password retrieved from the command above

After logging in, go to **User Info** and update your ArgoCD password.
### 4. Configure Jenkins

#### 4.1 Install Required Plugins

Go to **Manage Jenkins → Plugins → Available plugins** and install:
- OWASP Dependency-Check
- SonarQube Scanner
- Docker
- Pipeline: Stage View

#### 4.2 Configure OWASP Dependency-Check

After installing the OWASP plugin, go to **Manage Jenkins → Tools** and configure OWASP Dependency-Check:

![image](https://github.com/user-attachments/assets/da6a26d3-f742-4ea8-86b7-107b1650a7c2)
![image](https://github.com/user-attachments/assets/3b8c3f20-202e-4864-b3b6-b48d7a604ee8)

#### 4.3 Configure SonarQube Integration

**Generate SonarQube Token**

1. Login to SonarQube server at `http://<master-ip>:9000`
2. Navigate to **Administration → Security → Users → Token**
3. Generate a new token for Jenkins

![image](https://github.com/user-attachments/assets/86ad8284-5da6-4048-91fe-ac20c8e4514a)
![image](https://github.com/user-attachments/assets/6bc671a5-c122-45c0-b1f0-f29999bbf751)
![image](https://github.com/user-attachments/assets/e748643a-e037-4d4c-a9be-944995979c60)

**Add SonarQube Credentials to Jenkins**

Go to **Manage Jenkins → Credentials** and add the SonarQube token as a "Secret text" credential:

![image](https://github.com/user-attachments/assets/0688e105-2170-4c3f-87a3-128c1a05a0b8)

**Configure SonarQube Scanner**

Go to **Manage Jenkins → Tools** and configure SonarQube Scanner installations:

![image](https://github.com/user-attachments/assets/2fdc1e56-f78c-43d2-914a-104ec2c8ea86)

**Configure SonarQube Server**

Go to **Manage Jenkins → System** and search for "SonarQube installations":

![image](https://github.com/user-attachments/assets/ae866185-cb2b-4e83-825b-a125ec97243a)

**Create SonarQube Webhook**

Login to SonarQube, go to **Administration → Webhooks** and create a webhook pointing to Jenkins:
- URL: `http://<jenkins-ip>:8081/sonarqube-webhook/`

![image](https://github.com/user-attachments/assets/16527e72-6691-4fdf-a8d2-83dd27a085cb)
![image](https://github.com/user-attachments/assets/a8b45948-766a-49a4-b779-91ac3ce0443c)

#### 4.4 Add Docker Hub Credentials

Go to **Manage Jenkins → Credentials** and add your Docker Hub username and password:

![image](https://github.com/user-attachments/assets/77402c9c-fc2f-4df7-9a06-09f3f4c38751)

#### 4.5 Add GitHub Credentials

Go to **Manage Jenkins → Credentials** and add your GitHub credentials:

![image](https://github.com/user-attachments/assets/4d0c1a47-621e-4aa2-a0b1-71927fcdaef4)

> [!NOTE]
> Use a GitHub Personal Access Token in the password field.

#### 4.6 Configure Shared Pipeline Library

Go to **Manage Jenkins → System** and search for "Global Trusted Pipeline Libraries":

![image](https://github.com/user-attachments/assets/874b2e03-49b9-4c26-9b0f-bd07ce70c0f1)
![image](https://github.com/user-attachments/assets/1ca83b43-ce85-4970-941d-9a819ce4ecfd)

### 5. Setup Email Notifications

#### 5.1 Open SMTP Port

Allow port 465 (SMTPS) in your Jenkins Master EC2 security group.

#### 5.2 Generate Gmail App Password

1. Open Gmail and go to **Manage your Google Account → Security**
2. Ensure 2-step verification is enabled

   ![image](https://github.com/user-attachments/assets/5ab9dc9d-dcce-4f9d-9908-01095f1253cb)

3. Search for "App passwords" and create an app password for Jenkins

   ![image](https://github.com/user-attachments/assets/701752da-7703-4685-8f06-fe1f65dd1b9c)
   ![image](https://github.com/user-attachments/assets/adc8d8c0-8be4-4319-9042-4115abb5c6fc)

> [!IMPORTANT]
> 2-step verification must be enabled to generate app passwords.

#### 5.3 Add Email Credentials to Jenkins

Go to **Manage Jenkins → Credentials** and add your Gmail username and the app password:

![image](https://github.com/user-attachments/assets/2a42ec62-87c8-43c8-a034-7be0beb8824e)

#### 5.4 Configure Extended E-mail Notification

Go to **Manage Jenkins → System** and search for "Extended E-mail Notification":

![image](https://github.com/user-attachments/assets/bac81e24-bb07-4659-a251-955966feded8)

#### 5.5 Configure E-mail Notification

Scroll down and search for "E-mail Notification" and configure:
- SMTP server: `smtp.gmail.com`
- Use SMTP Authentication: Yes
- Username: Your Gmail address
- Password: The app password generated earlier
- Use SSL: Yes
- SMTP Port: 465

![image](https://github.com/user-attachments/assets/14e254fc-1400-457e-b3f4-046404b66950)
![image](https://github.com/user-attachments/assets/7be70b3a-b0dc-415c-838a-b1c6fd87c182)
![image](https://github.com/user-attachments/assets/cffb6e1d-4838-483e-97e0-6851c204ab21)
### 6. Configure ArgoCD

#### 6.1 Add EKS Cluster to ArgoCD

**Login to ArgoCD from CLI**

```bash
argocd login <worker-node-ip>:<argocd-nodeport> --username admin
```

> [!TIP]
> Replace `<worker-node-ip>:<argocd-nodeport>` with your actual ArgoCD URL (e.g., `52.53.156.187:32738`)

![image](https://github.com/user-attachments/assets/7d05e5ca-1a16-4054-a321-b99270ca0bf9)

**List Available Clusters**

```bash
argocd cluster list
```

![image](https://github.com/user-attachments/assets/76fe7a45-e05c-422d-9652-bdaee02d630f)

**Get Your EKS Cluster Context Name**

```bash
kubectl config get-contexts
```

![image](https://github.com/user-attachments/assets/c9afca1f-b5a3-4685-ae24-cc206a3e3ef1)

**Add EKS Cluster to ArgoCD**

```bash
argocd cluster add <your-cluster-context-name> --name bankapp-eks-cluster
```

> [!TIP]
> Replace `<your-cluster-context-name>` with your actual EKS cluster context (e.g., `Madhup@bankapp.us-west-1.eksctl.io`)

![image](https://github.com/user-attachments/assets/1061fe66-17ec-47b7-9d2e-371f58d3fd90)

**Verify Cluster in ArgoCD UI**

Go to ArgoCD console → **Settings → Clusters** and verify the cluster is added:

![image](https://github.com/user-attachments/assets/6aebb871-4dea-4e09-955a-a4aa43b8f4ef)

#### 6.2 Connect GitHub Repository to ArgoCD

Go to **Settings → Repositories** and click **Connect Repo**:

![image](https://github.com/user-attachments/assets/cc8728e5-546b-4c46-bd4c-538f4cd6a63d)
![image](https://github.com/user-attachments/assets/e665203d-0ebe-4839-af9e-f5866dce5e1b)
![image](https://github.com/user-attachments/assets/b9b869c3-698b-4303-83cc-9ccec66542a3)

> [!NOTE]
> Ensure the connection status shows "Successful"

### 7. Create Jenkins Jobs

#### 7.1 Create BankApp-CI Job

1. Go to Jenkins dashboard and click **New Item**
2. Enter name: `BankApp-CI`
3. Select **Pipeline** and click OK
4. Configure the pipeline to use the `Jenkinsfile` from your repository

![image](https://github.com/user-attachments/assets/17467b79-3110-470a-87a2-2bbfe197551b)
![image](https://github.com/user-attachments/assets/51d79ab0-e1f4-4c4d-a778-0c28119f5da9)

#### 7.2 Create BankApp-CD Job

Create a similar pipeline job named `BankApp-CD` using the `GitOps/Jenkinsfile` from your repository.

#### 7.3 Configure Docker Socket Permissions

To allow Jenkins to build and push Docker images, set appropriate permissions on the Docker socket:

```bash
sudo chmod 666 /var/run/docker.sock
```

> [!WARNING]
> Using `chmod 777` is a security risk. Use `chmod 666` instead, which provides read/write access without execute permissions.

![image](https://github.com/user-attachments/assets/e231c62a-7adb-4335-b67e-480758713dbf)

### 8. Deploy Application

#### 8.1 Create ArgoCD Application

Go to ArgoCD UI → **Applications** → **New App**:

![image](https://github.com/user-attachments/assets/d5b08e06-6256-4f46-afdc-fc43a9e44562)

Configure the application with:
- **Application Name**: bankapp
- **Project**: default
- **Sync Policy**: Automatic (optional)
- **Repository URL**: Your GitHub repository URL
- **Path**: kubernetes
- **Cluster**: bankapp-eks-cluster
- **Namespace**: bankapp-namespace

> [!IMPORTANT]
> Enable the **Auto-Create Namespace** option when creating the ArgoCD application

![image](https://github.com/user-attachments/assets/6a828910-41ba-4f0c-af05-19297321a41b)
![image](https://github.com/user-attachments/assets/a3aa1d22-50ef-4eb1-97fe-9c3ffb504fc3)

#### 8.2 Verify Deployment

Once deployed, you should see the application synced and healthy in ArgoCD:

![image](https://github.com/user-attachments/assets/03f3b69a-d6e0-42ad-992e-11124e7d0898)

#### 8.3 Access the Application

1. Open port 30080 in the security group of your EKS worker node
2. Access the application in your browser:
   ```
   http://<worker-node-public-ip>:30080
   ```

#### 8.4 Email Notification Example

Jenkins will send email notifications for build status:

![image](https://github.com/user-attachments/assets/407f94ed-bf67-441a-bd28-881b6b8739b2)
## Monitoring Setup

Monitor your EKS cluster, Kubernetes components, and workloads using Prometheus and Grafana via Helm.

### Install Helm

Download and install Helm on the master machine:

```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```

### Add Helm Repositories

Add the required Helm chart repositories:

```bash
helm repo add stable https://charts.helm.sh/stable
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

### Deploy Prometheus and Grafana

**Create Prometheus Namespace**

```bash
kubectl create namespace prometheus
kubectl get ns
```

**Install Prometheus Stack**

```bash
helm install stable prometheus-community/kube-prometheus-stack -n prometheus
```

**Verify Installation**

```bash
kubectl get pods -n prometheus
kubectl get svc -n prometheus
```

Wait for all pods to be in the `Running` state.

### Expose Prometheus

Change the Prometheus service from ClusterIP to NodePort:

```bash
kubectl edit svc stable-kube-prometheus-sta-prometheus -n prometheus
```

Change `type: ClusterIP` to `type: NodePort` and save the file.

![image](https://github.com/user-attachments/assets/90f5dc11-23de-457d-bbcb-944da350152e)
![image](https://github.com/user-attachments/assets/ed94f40f-c1f9-4f50-a340-a68594856cc7)

**Verify Prometheus Service**

```bash
kubectl get svc -n prometheus
```

Note the NodePort assigned to Prometheus and expose it in your worker node's security group.

### Expose Grafana

Change the Grafana service from ClusterIP to NodePort:

```bash
kubectl edit svc stable-grafana -n prometheus
```

Change `type: ClusterIP` to `type: NodePort` and save the file.

![image](https://github.com/user-attachments/assets/4a2afc1f-deba-48da-831e-49a63e1a8fb6)

**Verify Grafana Service**

```bash
kubectl get svc -n prometheus
```

Note the NodePort assigned to Grafana and expose it in your worker node's security group.

### Access Grafana

**Get Grafana Admin Password**

```bash
kubectl get secret --namespace prometheus stable-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

**Login to Grafana**

- **URL**: `http://<worker-node-public-ip>:<grafana-nodeport>`
- **Username**: admin
- **Password**: Use the password retrieved from the command above

### View Dashboards

Grafana comes pre-configured with dashboards for Kubernetes monitoring:

![image](https://github.com/user-attachments/assets/d2e7ff2f-059d-48c4-92bb-9711943819c4)
![image](https://github.com/user-attachments/assets/647b2b22-cd83-41c3-855d-7c60ae32195f)
![image](https://github.com/user-attachments/assets/cb98a281-a4f5-46af-98eb-afdb7da6b35a)

## Clean Up

When you're done with the deployment and want to tear down the infrastructure:

### Delete EKS Cluster

```bash
eksctl delete cluster --name=bankapp --region=us-west-1
```

This command will delete the entire EKS cluster, including all node groups and associated resources.

> [!WARNING]
> This action is irreversible and will delete all data and configurations in the cluster.

---

## Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

## Troubleshooting

### Common Issues

**Jenkins Cannot Connect to Docker**
- Ensure Docker socket permissions are set correctly: `sudo chmod 666 /var/run/docker.sock`
- Verify the Jenkins user is in the docker group: `sudo usermod -aG docker jenkins`

**ArgoCD Application Not Syncing**
- Check repository credentials in ArgoCD
- Verify the repository path is correct
- Check ArgoCD application logs: `kubectl logs -n argocd <argocd-application-controller-pod>`

**Pods Not Starting**
- Check pod logs: `kubectl logs <pod-name> -n <namespace>`
- Describe pod for events: `kubectl describe pod <pod-name> -n <namespace>`
- Verify resource availability: `kubectl top nodes`

**SonarQube Quality Gate Failing**
- Review SonarQube dashboard for specific issues
- Check code coverage and code smells
- Adjust quality gate settings if necessary

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the [MIT License](LICENSE).
