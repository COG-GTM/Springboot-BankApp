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

## Database credentials
The chart does **not** ship any credential. Before installing, provide a Secret
(default name `mysql-secret`, override with `secret.name`) in the release
namespace with the keys `MYSQL_ROOT_PASSWORD` and `SPRING_DATASOURCE_PASSWORD`.
Manage it outside of git with External Secrets Operator, Sealed Secrets, Vault
or your cloud KMS, and point the chart at it:

```bash
helm install bankapp bankapp/ --set secret.name=bankapp-db-credentials
```

For a throwaway/local cluster the chart can create the Secret itself from
values passed on the command line (never commit them):

```bash
helm install bankapp bankapp/ \
    --set secret.create=true \
    --set secret.data.MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
    --set secret.data.SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD"
```

## Run the SpringBoot Bankapp using helm
Install Bankapp from helm chart (see the credentials section above first).
```bash
helm install bankapp bankapp/
```

You can install it for multiple environments by changing values in `values.yaml` file
```bash
helm install bankapp-dev bankapp/ --set namespace=dev-namespace --set bankapp_svc.nodePort=30081
```

Happy Helming!


