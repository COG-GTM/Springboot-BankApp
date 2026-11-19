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
Install Bankapp from helm chart.
```bash
helm install bankapp bankapp/
```

The app is published only over HTTPS through the Ingress (the Service is `ClusterIP`; there is no NodePort).
Set the host and TLS secret for your environment, either letting cert-manager issue the certificate:
```bash
helm install bankapp bankapp/ --set ingress.host=bank.example.com --set ingress.clusterIssuer=letsencrypt-prod
```
or creating the TLS secret yourself (named by `ingress.tlsSecretName`, default `bankapp-tls-secret`):
```bash
kubectl -n bankapp-namespace create secret tls bankapp-tls-secret --cert=tls.crt --key=tls.key
```

You can install it for multiple environments by changing values in `values.yaml` file
```bash
helm install bankapp-dev bankapp/ --set namespace=dev-namespace --set ingress.host=dev.bankapp.local
```

The session cookie is marked `Secure`, so log in through `https://<ingress host>` (or `kubectl port-forward` to `http://localhost:8080` for local debugging).

Happy Helming!


