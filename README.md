# End-to-End Bank Application Deployment using DevSecOps on AWS EKS

This is a multi-tier banking application written in Java (Spring Boot) that demonstrates a complete DevSecOps pipeline deployment on AWS EKS.

![Login diagram](images/login.png)
![Transactions diagram](images/transactions.png)

## Table of Contents
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Infrastructure Setup](#infrastructure-setup)
  - [Create EKS Cluster](#create-eks-cluster)
  - [Install Jenkins](#install-jenkins)
  - [Install Docker](#install-docker)
  - [Install and Configure SonarQube](#install-and-configure-sonarqube)
  - [Install Trivy](#install-trivy)
  - [Install and Configure ArgoCD](#install-and-configure-argocd)
- [Email Notification Setup](#email-notification-setup)
- [Jenkins Configuration](#jenkins-configuration)
- [ArgoCD Configuration](#argocd-configuration)
- [Monitoring Setup](#monitoring-setup)
- [Clean Up](#clean-up)

## Tech Stack

This project leverages the following technologies:

- **GitHub** - Source code management
- **Docker** - Containerization
- **Jenkins** - Continuous Integration (CI)
- **OWASP Dependency Check** - Security vulnerability scanning
- **SonarQube** - Code quality analysis
- **Trivy** - Filesystem and container image scanning
- **ArgoCD** - Continuous Deployment (CD) with GitOps
- **AWS EKS** - Managed Kubernetes service
- **Helm** - Kubernetes package manager
- **Prometheus & Grafana** - Monitoring and visualization
  
## Prerequisites

Before starting the deployment, ensure you have:

- Root user access on your system
  ```bash
  sudo su
  ```

> [!Note]
> This project will be implemented in the North California region (us-west-1).

- **AWS EC2 Instance**: Create 1 Master machine on AWS (t2.medium) with 29 GB of storage
- **Security Group Configuration**: Open the required ports in your security group
  ![image](https://github.com/user-attachments/assets/4e5ecd37-fe2e-4e4b-a6ba-14c7b62715a3)

## Infrastructure Setup

### Create EKS Cluster
**Required AWS Setup:**
- IAM user with access keys and secret access keys
- AWS CLI configured ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/AWSCLI/AWSCLI.sh))

**Install AWS CLI:**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install unzip
unzip awscliv2.zip
sudo ./aws/install
aws configure
```

**Install kubectl** ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/Kubectl/Kubectl.sh)):
```bash
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client
```

**Install eksctl** ([Setup Guide](https://github.com/DevMadhup/DevOps-Tools-Installations/blob/main/eksctl%20/eksctl.sh)):
```bash
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version
```

#### Create EKS Cluster
```bash
eksctl create cluster --name=bankapp \
                    --region=us-west-1 \
                    --version=1.30 \
                    --without-nodegroup
```

#### Associate IAM OIDC Provider
```bash
eksctl utils associate-iam-oidc-provider \
  --region us-west-1 \
  --cluster bankapp \
  --approve
```

#### Create Node Group
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
> Make sure the ssh-public-key "eks-nodegroup-key" is available in your AWS account

### Install Jenkins
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

#### Configure Jenkins Port

After installing Jenkins, change the default port from 8080 to 8081 (the bankapp application will run on 8080).

1. Open `/usr/lib/systemd/system/jenkins.service` file and change the `JENKINS_PORT` environment variable
   ![image](https://github.com/user-attachments/assets/6320ae49-82d4-4ae3-9811-bd6f06778483)

2. Reload daemon:
   ```bash
   sudo systemctl daemon-reload 
   ```

3. Restart Jenkins:
   ```bash
   sudo systemctl restart jenkins
   ```

### Install Docker

```bash
sudo apt install docker.io -y
sudo usermod -aG docker ubuntu && newgrp docker
```

### Install and Configure SonarQube

```bash
docker run -itd --name SonarQube-Server -p 9000:9000 sonarqube:lts-community
```

### Install Trivy

```bash
sudo apt-get install wget apt-transport-https gnupg lsb-release -y
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update -y
sudo apt-get install trivy -y
```

### Install and Configure ArgoCD
#### Create ArgoCD Namespace
```bash
kubectl create namespace argocd
```

#### Apply ArgoCD Manifest
```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

#### Verify All Pods are Running
```bash
watch kubectl get pods -n argocd
```

#### Install ArgoCD CLI
```bash
curl --silent --location -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/download/v2.4.7/argocd-linux-amd64
chmod +x /usr/local/bin/argocd
```

#### Check ArgoCD Services
```bash
kubectl get svc -n argocd
```

#### Change ArgoCD Server Service from ClusterIP to NodePort
```bash
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort"}}'
```

#### Confirm Service is Patched
```bash
kubectl get svc -n argocd
```

#### Expose ArgoCD Server Port
Check the port where ArgoCD server is running and expose it in the security groups of your Kubernetes worker node.

![image](https://github.com/user-attachments/assets/a2932e03-ebc7-42a6-9132-82638152197f)

#### Access ArgoCD UI
Access ArgoCD in your browser at:
```
<public-ip-worker>:<port>
```

Click on "Advanced" and proceed.

![image](https://github.com/user-attachments/assets/29d9cdbd-5b7c-44b3-bb9b-1d091d042ce3)
![image](https://github.com/user-attachments/assets/08f4e047-e21c-4241-ba68-f9b719a4a39a)
![image](https://github.com/user-attachments/assets/1ffa85c3-9055-49b4-aab0-0947b95f0dd2)

#### Fetch Initial ArgoCD Password
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```

**Default Username:** admin

After logging in, go to **User Info** and update your ArgoCD password.

## Email Notification Setup

### Configure SMTP Port
Go to your Jenkins Master EC2 instance and allow port 465 for SMTPS in the security group.

### Generate Gmail Application Password
To authenticate Jenkins with Gmail, you need to generate an application password:

1. Open Gmail and go to **Manage your Google Account → Security**
   > [!Important]
   > Make sure 2-step verification is enabled

   ![image](https://github.com/user-attachments/assets/5ab9dc9d-dcce-4f9d-9908-01095f1253cb)

2. Search for **App password** and create an app password for Jenkins
   ![image](https://github.com/user-attachments/assets/701752da-7703-4685-8f06-fe1f65dd1b9c)
   ![image](https://github.com/user-attachments/assets/adc8d8c0-8be4-4319-9042-4115abb5c6fc)

3. Once the app password is created, go back to Jenkins: **Manage Jenkins → Credentials** to add username and password for email notification
   ![image](https://github.com/user-attachments/assets/2a42ec62-87c8-43c8-a034-7be0beb8824e)

4. Go to **Manage Jenkins → System** and search for **Extended E-mail Notification**
   ![image](https://github.com/user-attachments/assets/bac81e24-bb07-4659-a251-955966feded8)

5. Scroll down and search for **E-mail Notification** and set up email notification
   > [!Important]
   > Enter your Gmail app password in the password field under **E-mail Notification → Advanced**

   ![image](https://github.com/user-attachments/assets/14e254fc-1400-457e-b3f4-046404b66950)
   ![image](https://github.com/user-attachments/assets/7be70b3a-b0dc-415c-838a-b1c6fd87c182)
   ![image](https://github.com/user-attachments/assets/cffb6e1d-4838-483e-97e0-6851c204ab21)

## Jenkins Configuration

### Install Required Plugins

Go to Jenkins and click on **Manage Jenkins → Plugins → Available plugins** and install the following plugins:
- OWASP Dependency Check
- SonarQube Scanner
- Docker
- Pipeline: Stage View

### Configure OWASP

Navigate to **Manage Jenkins → Tools** and configure OWASP Dependency Check:

![image](https://github.com/user-attachments/assets/da6a26d3-f742-4ea8-86b7-107b1650a7c2)
![image](https://github.com/user-attachments/assets/3b8c3f20-202e-4864-b3b6-b48d7a604ee8)

### Configure SonarQube Integration

1. Login to SonarQube server and create credentials for Jenkins integration
   - Navigate to **Administration → Security → Users → Token**
   ![image](https://github.com/user-attachments/assets/86ad8284-5da6-4048-91fe-ac20c8e4514a)
   ![image](https://github.com/user-attachments/assets/6bc671a5-c122-45c0-b1f0-f29999bbf751)
   ![image](https://github.com/user-attachments/assets/e748643a-e037-4d4c-a9be-944995979c60)

2. Go to **Manage Jenkins → Credentials** and add SonarQube credentials:
   ![image](https://github.com/user-attachments/assets/0688e105-2170-4c3f-87a3-128c1a05a0b8)

3. Go to **Manage Jenkins → Tools** and search for SonarQube Scanner installations:
   ![image](https://github.com/user-attachments/assets/2fdc1e56-f78c-43d2-914a-104ec2c8ea86)

### Add Docker Credentials

Go to **Manage Jenkins → Credentials** and add Docker credentials to push the updated Docker image to Docker Hub:

![image](https://github.com/user-attachments/assets/77402c9c-fc2f-4df7-9a06-09f3f4c38751)

### Add GitHub Credentials

Add GitHub credentials to push updated code from the pipeline:

![image](https://github.com/user-attachments/assets/4d0c1a47-621e-4aa2-a0b1-71927fcdaef4)

> [!Note]
> When adding GitHub credentials, use a Personal Access Token in the password field

### Configure SonarQube Server

Go to **Manage Jenkins → System** and search for SonarQube installations:

![image](https://github.com/user-attachments/assets/ae866185-cb2b-4e83-825b-a125ec97243a)

### Configure Global Trusted Pipeline Libraries

Go to **Manage Jenkins → System** and search for Global Trusted Pipeline Libraries:

![image](https://github.com/user-attachments/assets/874b2e03-49b9-4c26-9b0f-bd07ce70c0f1)
![image](https://github.com/user-attachments/assets/1ca83b43-ce85-4970-941d-9a819ce4ecfd)

### Configure SonarQube Webhook

Login to SonarQube server, go to **Administration → Webhook** and click on create:

![image](https://github.com/user-attachments/assets/16527e72-6691-4fdf-a8d2-83dd27a085cb)
![image](https://github.com/user-attachments/assets/a8b45948-766a-49a4-b779-91ac3ce0443c)

## ArgoCD Configuration

### Add EKS Cluster to ArgoCD

Go to the Master Machine and add your EKS cluster to ArgoCD for application deployment using CLI:
#### Login to ArgoCD from CLI
```bash
argocd login 52.53.156.187:32738 --username admin
```

> [!Tip]
> Replace `52.53.156.187:32738` with your ArgoCD URL

![image](https://github.com/user-attachments/assets/7d05e5ca-1a16-4054-a321-b99270ca0bf9)

#### Check Available Clusters in ArgoCD
```bash
argocd cluster list
```

![image](https://github.com/user-attachments/assets/76fe7a45-e05c-422d-9652-bdaee02d630f)

#### Get Your Cluster Name
```bash
kubectl config get-contexts
```

![image](https://github.com/user-attachments/assets/c9afca1f-b5a3-4685-ae24-cc206a3e3ef1)

#### Add Your Cluster to ArgoCD
```bash
argocd cluster add Madhup@bankapp.us-west-1.eksctl.io --name bankapp-eks-cluster
```

> [!Tip]
> Replace `Madhup@bankapp.us-west-1.eksctl.io` with your EKS Cluster Name

![image](https://github.com/user-attachments/assets/1061fe66-17ec-47b7-9d2e-371f58d3fd90)

#### Verify Cluster in ArgoCD Console
Once your cluster is added to ArgoCD, go to ArgoCD console: **Settings → Clusters** and verify it.

![image](https://github.com/user-attachments/assets/6aebb871-4dea-4e09-955a-a4aa43b8f4ef)

### Connect Repository to ArgoCD

Go to **Settings → Repositories** and click on **Connect repo**:

![image](https://github.com/user-attachments/assets/cc8728e5-546b-4c46-bd4c-538f4cd6a63d)
![image](https://github.com/user-attachments/assets/e665203d-0ebe-4839-af9e-f5866dce5e1b)
![image](https://github.com/user-attachments/assets/b9b869c3-698b-4303-83cc-9ccec66542a3)

> [!Note]
> Connection should be successful

### Create Jenkins Jobs

#### Create BankApp-CI Job
![image](https://github.com/user-attachments/assets/17467b79-3110-470a-87a2-2bbfe197551b)
![image](https://github.com/user-attachments/assets/51d79ab0-e1f4-4c4d-a778-0c28119f5da9)

#### Create BankApp-CD Job
Create the BankApp-CD job using the same process as the CI job.

### Configure Docker Permissions

Provide permission to Docker socket so that Docker build and push commands do not fail:

```bash
chmod 777 /var/run/docker.sock
```

![image](https://github.com/user-attachments/assets/e231c62a-7adb-4335-b67e-480758713dbf)

### Deploy Application in ArgoCD

Go to **Applications** and click on **New App**:

![image](https://github.com/user-attachments/assets/d5b08e06-6256-4f46-afdc-fc43a9e44562)

> [!Important]
> Make sure to click on the **Auto-Create Namespace** option while creating the ArgoCD application

![image](https://github.com/user-attachments/assets/6a828910-41ba-4f0c-af05-19297321a41b)
![image](https://github.com/user-attachments/assets/a3aa1d22-50ef-4eb1-97fe-9c3ffb504fc3)

### Access the Deployed Application

Congratulations! Your application is deployed on AWS EKS Cluster.

![image](https://github.com/user-attachments/assets/03f3b69a-d6e0-42ad-992e-11124e7d0898)

Open port 30080 on the worker node and access it in your browser:
```
<worker-public-ip>:30080
```

### Email Notification Example
![image](https://github.com/user-attachments/assets/407f94ed-bf67-441a-bd28-881b6b8739b2)

## Monitoring Setup

### Monitor EKS Cluster with Prometheus and Grafana using Helm
#### Install Helm Chart

```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```

#### Add Helm Stable Charts
```bash
helm repo add stable https://charts.helm.sh/stable
```

#### Add Prometheus Helm Repository
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
```

#### Create Prometheus Namespace
```bash
kubectl create namespace prometheus
kubectl get ns
```

#### Install Prometheus using Helm
```bash
helm install stable prometheus-community/kube-prometheus-stack -n prometheus
```

#### Verify Prometheus Installation
```bash
kubectl get pods -n prometheus
```

#### Check Prometheus Services
```bash
kubectl get svc -n prometheus
```

#### Expose Prometheus to External Access

Change the service type from ClusterIP to NodePort:

```bash
kubectl edit svc stable-kube-prometheus-sta-prometheus -n prometheus
```

> [!Important]
> Change it from ClusterIP to NodePort, save the file, and open the assigned NodePort in the security group

![image](https://github.com/user-attachments/assets/90f5dc11-23de-457d-bbcb-944da350152e)
![image](https://github.com/user-attachments/assets/ed94f40f-c1f9-4f50-a340-a68594856cc7)

#### Verify Prometheus Service
```bash
kubectl get svc -n prometheus
```

#### Expose Grafana to External Access

Change the Grafana service type to NodePort:

```bash
kubectl edit svc stable-grafana -n prometheus
```

![image](https://github.com/user-attachments/assets/4a2afc1f-deba-48da-831e-49a63e1a8fb6)

#### Check Grafana Service
```bash
kubectl get svc -n prometheus
```

#### Get Grafana Password
```bash
kubectl get secret --namespace prometheus stable-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

> [!Note]
> Default Username: admin

#### View Grafana Dashboard

Access Grafana in your browser and explore the pre-configured dashboards:

![image](https://github.com/user-attachments/assets/d2e7ff2f-059d-48c4-92bb-9711943819c4)
![image](https://github.com/user-attachments/assets/647b2b22-cd83-41c3-855d-7c60ae32195f)
![image](https://github.com/user-attachments/assets/cb98a281-a4f5-46af-98eb-afdb7da6b35a)

## Clean Up

### Delete EKS Cluster

When you're done with the project, clean up resources to avoid unnecessary AWS charges:

```bash
eksctl delete cluster --name=bankapp --region=us-west-1
```

## Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the [MIT License](LICENSE).
