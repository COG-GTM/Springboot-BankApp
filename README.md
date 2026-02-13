# End-to-End Bank Application Deployment using DevSecOps on AWS EKS

A multi-tier banking web application written in Java (Spring Boot).

![Login diagram](images/login.png)
![Transactions diagram](images/transactions.png)

---

## Tech Stack

| Category           | Tool                              |
|--------------------|-----------------------------------|
| Source Control      | GitHub                           |
| Containerization    | Docker                           |
| CI                  | Jenkins                          |
| Dependency Check    | OWASP                            |
| Code Quality        | SonarQube                        |
| Filesystem Scan     | Trivy                            |
| CD                  | ArgoCD                           |
| Orchestration       | AWS EKS (Kubernetes)             |
| Monitoring          | Helm (Grafana & Prometheus)      |

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [EKS Cluster Setup](#eks-cluster-setup)
3. [Jenkins Installation](#jenkins-installation)
4. [Docker Installation](#docker-installation)
5. [SonarQube Setup](#sonarqube-setup)
6. [Trivy Installation](#trivy-installation)
7. [ArgoCD Installation & Configuration](#argocd-installation--configuration)
8. [Email Notifications](#email-notifications)
9. [Jenkins Plugin & Tool Configuration](#jenkins-plugin--tool-configuration)
10. [Application Deployment](#application-deployment)
11. [Monitoring with Prometheus & Grafana](#monitoring-with-prometheus--grafana)
12. [Clean Up](#clean-up)

---

## Prerequisites

- Root user access:
  ```bash
  sudo su
  ```

> [!Note]
> This project will be implemented on the North California region (`us-west-1`).

- **AWS EC2 instance:** 1 Master machine (`t2.medium`) with 29 GB of storage.
- **Security group ports:** Open the ports shown below.

![image](https://github.com/user-attachments/assets/4e5ecd37-fe2e-4e4b-a6ba-14c7b62715a3)

---

## EKS Cluster Setup

### Install AWS CLI

- IAM user with **access keys and secret access keys**
- [AWSCLI setup reference](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/AWSCLI/AWSCLI.sh)

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install unzip
unzip awscliv2.zip
sudo ./aws/install
aws configure
```

### Install kubectl

[kubectl setup reference](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/Kubectl/Kubectl.sh)

```bash
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client
```

### Install eksctl

[eksctl setup reference](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/eksctl%20/eksctl.sh)

```bash
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version
```

### Create the EKS Cluster

```bash
eksctl create cluster --name=bankapp \
                    --region=us-west-1 \
                    --version=1.30 \
                    --without-nodegroup
```

### Associate IAM OIDC Provider

```bash
eksctl utils associate-iam-oidc-provider \
  --region us-west-1 \
  --cluster bankapp \
  --approve
```

### Create Node Group

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

> [!Note]
> Make sure the SSH public key `eks-nodegroup-key` is available in your AWS account.

---

## Jenkins Installation

```bash
sudo apt update -y
sudo apt install fontconfig openjdk-21-jre -y

sudo wget -O /usr/share/keyrings/jenkins-keyring.asc \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key

echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc]" \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt-get update -y
sudo apt-get install jenkins -y
```

### Change Jenkins Default Port

The bank application runs on port 8080, so Jenkins must be moved to port 8081:

1. Open `/usr/lib/systemd/system/jenkins.service` and change the `JENKINS_PORT` environment variable:

   ![image](https://github.com/user-attachments/assets/6320ae49-82d4-4ae3-9811-bd6f06778483)

2. Reload the daemon and restart Jenkins:

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl restart jenkins
   ```

---

## Docker Installation

```bash
sudo apt install docker.io -y
sudo usermod -aG docker ubuntu && newgrp docker
```

---

## SonarQube Setup

```bash
docker run -itd --name SonarQube-Server -p 9000:9000 sonarqube:lts-community
```

---

## Trivy Installation

```bash
sudo apt-get install wget apt-transport-https gnupg lsb-release -y
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update -y
sudo apt-get install trivy -y
```

---

## ArgoCD Installation & Configuration

### Create Namespace and Install ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

Wait for all pods to be running:

```bash
watch kubectl get pods -n argocd
```

### Install ArgoCD CLI

```bash
curl --silent --location -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/download/v2.4.7/argocd-linux-amd64
chmod +x /usr/local/bin/argocd
```

### Expose ArgoCD Server

1. Check services:

   ```bash
   kubectl get svc -n argocd
   ```

2. Change from ClusterIP to NodePort:

   ```bash
   kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort"}}'
   ```

3. Confirm the change:

   ```bash
   kubectl get svc -n argocd
   ```

4. Note the assigned NodePort and open it in the worker node's security group.

   ![image](https://github.com/user-attachments/assets/a2932e03-ebc7-42a6-9132-82638152197f)

5. Access ArgoCD in your browser:

   ```
   <public-ip-worker>:<port>
   ```

   Click **Advanced** and proceed.

   ![image](https://github.com/user-attachments/assets/29d9cdbd-5b7c-44b3-bb9b-1d091d042ce3)
   ![image](https://github.com/user-attachments/assets/08f4e047-e21c-4241-ba68-f9b719a4a39a)
   ![image](https://github.com/user-attachments/assets/1ffa85c3-9055-49b4-aab0-0947b95f0dd2)

### Log In to ArgoCD

- Fetch the initial admin password:

  ```bash
  kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
  ```

- **Username:** `admin`
- Go to **User Info** and update your password.

---

## Email Notifications

### Gmail App Password Setup

1. Allow port **465** (SMTPS) in the Jenkins Master EC2 security group.
2. Open Gmail and go to **Manage your Google Account > Security**.

> [!Important]
> 2-step verification must be enabled.

![image](https://github.com/user-attachments/assets/5ab9dc9d-dcce-4f9d-9908-01095f1253cb)

3. Search for **App password** and create one for Jenkins.

   ![image](https://github.com/user-attachments/assets/701752da-7703-4685-8f06-fe1f65dd1b9c)
   ![image](https://github.com/user-attachments/assets/adc8d8c0-8be4-4319-9042-4115abb5c6fc)

### Configure Email in Jenkins

1. Go to **Manage Jenkins > Credentials** and add the email username/password.

   ![image](https://github.com/user-attachments/assets/2a42ec62-87c8-43c8-a034-7be0beb8824e)

2. Go to **Manage Jenkins > System**, find **Extended E-mail Notification**, and configure it.

   ![image](https://github.com/user-attachments/assets/bac81e24-bb07-4659-a251-955966feded8)

3. Scroll to **E-mail Notification** and complete the setup.

> [!Important]
> Enter your recently copied Gmail password in the **E-mail Notification > Advanced** password field.

![image](https://github.com/user-attachments/assets/14e254fc-1400-457e-b3f4-046404b66950)
![image](https://github.com/user-attachments/assets/7be70b3a-b0dc-415c-838a-b1c6fd87c182)
![image](https://github.com/user-attachments/assets/cffb6e1d-4838-483e-97e0-6851c204ab21)

---

## Jenkins Plugin & Tool Configuration

### Install Required Plugins

Go to **Manage Jenkins > Plugins > Available plugins** and install:

- OWASP
- SonarQube Scanner
- Docker
- Pipeline: Stage View

### Configure OWASP

Go to **Manage Jenkins > Plugins > Available plugins**.

![image](https://github.com/user-attachments/assets/da6a26d3-f742-4ea8-86b7-107b1650a7c2)

After installation, go to **Manage Jenkins > Tools** to configure.

![image](https://github.com/user-attachments/assets/3b8c3f20-202e-4864-b3b6-b48d7a604ee8)

### Configure SonarQube

1. Log in to SonarQube, navigate to **Administration > Security > Users > Token** and generate a token.

   ![image](https://github.com/user-attachments/assets/86ad8284-5da6-4048-91fe-ac20c8e4514a)
   ![image](https://github.com/user-attachments/assets/6bc671a5-c122-45c0-b1f0-f29999bbf751)
   ![image](https://github.com/user-attachments/assets/e748643a-e037-4d4c-a9be-944995979c60)

2. In Jenkins, go to **Manage Jenkins > Credentials** and add the SonarQube token.

   ![image](https://github.com/user-attachments/assets/0688e105-2170-4c3f-87a3-128c1a05a0b8)

3. Go to **Manage Jenkins > Tools**, find **SonarQube Scanner installations**, and configure.

   ![image](https://github.com/user-attachments/assets/2fdc1e56-f78c-43d2-914a-104ec2c8ea86)

4. Go to **Manage Jenkins > System**, find **SonarQube installations**, and configure.

   ![image](https://github.com/user-attachments/assets/ae866185-cb2b-4e83-825b-a125ec97243a)

### Add Docker Hub Credentials

Go to **Manage Jenkins > Credentials** and add your Docker Hub credentials.

![image](https://github.com/user-attachments/assets/77402c9c-fc2f-4df7-9a06-09f3f4c38751)

### Add GitHub Credentials

Go to **Manage Jenkins > Credentials** and add GitHub credentials.

![image](https://github.com/user-attachments/assets/4d0c1a47-621e-4aa2-a0b1-71927fcdaef4)

> [!Note]
> Use a Personal Access Token in the password field.

### Configure Global Trusted Pipeline Libraries

Go to **Manage Jenkins > System** and search for **Global Trusted Pipeline Libraries**.

![image](https://github.com/user-attachments/assets/874b2e03-49b9-4c26-9b0f-bd07ce70c0f1)
![image](https://github.com/user-attachments/assets/1ca83b43-ce85-4970-941d-9a819ce4ecfd)

### Configure SonarQube Webhook

Log in to SonarQube, go to **Administration > Webhook**, and create a webhook.

![image](https://github.com/user-attachments/assets/16527e72-6691-4fdf-a8d2-83dd27a085cb)
![image](https://github.com/user-attachments/assets/a8b45948-766a-49a4-b779-91ac3ce0443c)

---

## Application Deployment

### Add EKS Cluster to ArgoCD

On the Master machine, use the ArgoCD CLI to register the cluster.

1. Log in to ArgoCD:

   ```bash
   argocd login 52.53.156.187:32738 --username admin
   ```

   > [!Tip]
   > Replace `52.53.156.187:32738` with your ArgoCD URL.

   ![image](https://github.com/user-attachments/assets/7d05e5ca-1a16-4054-a321-b99270ca0bf9)

2. List available clusters:

   ```bash
   argocd cluster list
   ```

   ![image](https://github.com/user-attachments/assets/76fe7a45-e05c-422d-9652-bdaee02d630f)

3. Get your cluster name:

   ```bash
   kubectl config get-contexts
   ```

   ![image](https://github.com/user-attachments/assets/c9afca1f-b5a3-4685-ae24-cc206a3e3ef1)

4. Add your cluster to ArgoCD:

   ```bash
   argocd cluster add Madhup@bankapp.us-west-1.eksctl.io --name bankapp-eks-cluster
   ```

   > [!Tip]
   > Replace `Madhup@bankapp.us-west-1.eksctl.io` with your EKS cluster name.

   ![image](https://github.com/user-attachments/assets/1061fe66-17ec-47b7-9d2e-371f58d3fd90)

5. Verify in ArgoCD console under **Settings > Clusters**.

   ![image](https://github.com/user-attachments/assets/6aebb871-4dea-4e09-955a-a4aa43b8f4ef)

### Connect Repository in ArgoCD

Go to **Settings > Repositories** and click **Connect Repo**.

![image](https://github.com/user-attachments/assets/cc8728e5-546b-4c46-bd4c-538f4cd6a63d)
![image](https://github.com/user-attachments/assets/e665203d-0ebe-4839-af9e-f5866dce5e1b)
![image](https://github.com/user-attachments/assets/b9b869c3-698b-4303-83cc-9ccec66542a3)

> [!Note]
> Connection should be successful.

### Create Jenkins Jobs

1. Create **BankApp-CI** job:

   ![image](https://github.com/user-attachments/assets/17467b79-3110-470a-87a2-2bbfe197551b)
   ![image](https://github.com/user-attachments/assets/51d79ab0-e1f4-4c4d-a778-0c28119f5da9)

2. Create **BankApp-CD** job (same configuration as CI).

3. Grant Docker socket permissions:

   ```bash
   chmod 777 /var/run/docker.sock
   ```

   ![image](https://github.com/user-attachments/assets/e231c62a-7adb-4335-b67e-480758713dbf)

### Deploy via ArgoCD

1. Go to **Applications** and click **New App**.

   ![image](https://github.com/user-attachments/assets/d5b08e06-6256-4f46-afdc-fc43a9e44562)

> [!Important]
> Make sure to enable the **Auto-Create Namespace** option.

![image](https://github.com/user-attachments/assets/6a828910-41ba-4f0c-af05-19297321a41b)
![image](https://github.com/user-attachments/assets/a3aa1d22-50ef-4eb1-97fe-9c3ffb504fc3)

2. Your application is now deployed on AWS EKS!

   ![image](https://github.com/user-attachments/assets/03f3b69a-d6e0-42ad-992e-11124e7d0898)

3. Open port **30080** on the worker node and access the app:

   ```
   <worker-public-ip>:30080
   ```

4. Email notification confirmation:

   ![image](https://github.com/user-attachments/assets/407f94ed-bf67-441a-bd28-881b6b8739b2)

---

## Monitoring with Prometheus & Grafana

All monitoring commands should be run on the **Master machine**.

### Install Helm

```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```

### Add Helm Repositories

```bash
helm repo add stable https://charts.helm.sh/stable
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
```

### Install Prometheus

```bash
kubectl create namespace prometheus
kubectl get ns
helm install stable prometheus-community/kube-prometheus-stack -n prometheus
```

### Verify Installation

```bash
kubectl get pods -n prometheus
kubectl get svc -n prometheus
```

### Expose Prometheus and Grafana

> [!Important]
> Change services from ClusterIP to NodePort. After changing, save the file and open the assigned NodePort in the security group.

**Prometheus:**

```bash
kubectl edit svc stable-kube-prometheus-sta-prometheus -n prometheus
```

![image](https://github.com/user-attachments/assets/90f5dc11-23de-457d-bbcb-944da350152e)
![image](https://github.com/user-attachments/assets/ed94f40f-c1f9-4f50-a340-a68594856cc7)

Verify:

```bash
kubectl get svc -n prometheus
```

**Grafana:**

```bash
kubectl edit svc stable-grafana -n prometheus
```

![image](https://github.com/user-attachments/assets/4a2afc1f-deba-48da-831e-49a63e1a8fb6)

Verify:

```bash
kubectl get svc -n prometheus
```

### Access Grafana

Get the admin password:

```bash
kubectl get secret --namespace prometheus stable-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

> [!Note]
> **Username:** `admin`

### Grafana Dashboards

![image](https://github.com/user-attachments/assets/d2e7ff2f-059d-48c4-92bb-9711943819c4)
![image](https://github.com/user-attachments/assets/647b2b22-cd83-41c3-855d-7c60ae32195f)
![image](https://github.com/user-attachments/assets/cb98a281-a4f5-46af-98eb-afdb7da6b35a)

---

## Clean Up

Delete the EKS cluster:

```bash
eksctl delete cluster --name=bankapp --region=us-west-1
```
