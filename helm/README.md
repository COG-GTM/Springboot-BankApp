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
Install Bankapp from helm chart. Database passwords are not stored in `values.yaml`; pass
them at install time from your secret manager (or set `secret.create=false` and create the
`mysql-secret` Secret yourself).
```bash
helm install bankapp bankapp/ \
  --set secret.data.MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  --set secret.data.SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD"
```

You can install it for multiple environments by changing values in `values.yaml` file
```bash
helm install bankapp-dev bankapp/ --set namespace=dev-namespace --set bankapp_svc.nodePort=30081 \
  --set secret.data.MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  --set secret.data.SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD"
```

Happy Helming!


