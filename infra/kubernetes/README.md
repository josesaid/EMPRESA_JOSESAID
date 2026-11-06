# Manifiestos de Kubernetes (base)

Base mínima para ui-ms y gateway-ms en el namespace `empresa`.

- `namespace.yaml`: namespace con Pod Security `restricted`.
- `ui-ms.yaml` y `gateway-ms.yaml`: Deployments y Services `ClusterIP`. Ningún Service usa `NodePort` ni `LoadBalancer`.
- `ingress.yaml`: solo la UI sale a la red (ingress-nginx, TLS). Reemplazar `ui.example.com` y la imagen `REGISTRY/...`.
- `network-policy.yaml`: deny-all de entrada y excepciones explícitas. El actuator (9080) solo lo raspa Prometheus.

Pendientes antes de usarlo:
- Imágenes publicadas y secretos (`CRYPTO_*`, `USUARIOS_*`, etc.) como `Secret`, no en el YAML.
- Supuestos: ingress-nginx, Prometheus en el namespace `monitoring` con la etiqueta `app.kubernetes.io/name: prometheus`, y el namespace `ingress-nginx` con su etiqueta estándar.
- Validar con `kubeconform` o `kubectl apply --dry-run=server` contra el cluster.
