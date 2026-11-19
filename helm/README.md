# HELM 

## Installing Helm
```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```
**NOTE** This Helm chart assumes that you already have installed Ingress Cotroller, Metrics Server and VPA CRD


- Ingress Controller
    ```bash
        helm upgrade --install ingress-nginx ingress-nginx \
            --repo https://kubernetes.github.io/ingress-nginx \
            --namespace ingress-nginx --create-namespace
    ```
- Metrics Server for HPA
    ```bash
        helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/
        helm upgrade --install metrics-server metrics-server/metrics-server
    ```
    ```bash
        kubectl edit deployments.apps metrics-server
        ## add these two enteries at: spec.template.spec.containers[0].args
        # - --kubelet-insecure-tls
        # - --kubelet-preferred-address-types=InternalIP,Hostname,ExternalIP
    ```
- VPA Custom Resource definition (CRD).
    ```bash
        kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.0/vertical-pod-autoscaler/deploy/vpa-v1-crd-gen.yaml

        kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.0/vertical-pod-autoscaler/deploy/vpa-rbac.yaml
    ```

## Run the SpringBoot Bankapp using helm
The chart ships **no database passwords**: `helm template`/`helm install` fail until
you provide them. The app connects as the least-privilege MySQL user `bankapp`
(rights on the bank schema only), MySQL `root` only accepts logins from inside the
MySQL pod, and a NetworkPolicy lets only the bankapp pods reach port 3306.

Install Bankapp from helm chart with two different, random passwords:
```bash
helm install bankapp bankapp/ \
  --set-string secret.data.MYSQL_ROOT_PASSWORD="$(openssl rand -base64 24)" \
  --set-string secret.data.SPRING_DATASOURCE_PASSWORD="$(openssl rand -base64 24)"
```

Or reference a Secret managed outside the chart (External Secrets, Sealed Secrets, ...)
that has the keys `MYSQL_ROOT_PASSWORD` and `SPRING_DATASOURCE_PASSWORD`:
```bash
helm install bankapp bankapp/ --set secret.existingSecret=bankapp-db-credentials
```

You can install it for multiple environments by changing values in `values.yaml` file
```bash
helm install bankapp-dev bankapp/ --set namespace=dev-namespace --set bankapp_svc.nodePort=30081 \
  --set secret.existingSecret=bankapp-db-credentials
```

> **Upgrading an existing release:** the MySQL image only creates the `bankapp` user
> when the data volume is empty. On an existing volume, create it first
> (`CREATE USER 'bankapp'@'%' IDENTIFIED BY '<app password>'; GRANT ALL ON BankDB.* TO 'bankapp'@'%';`),
> rotate the old root password, and drop `'root'@'%'`.

Happy Helming!


