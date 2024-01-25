# 자주사용하는 쿠버명령

## 초기설정

$HOME/.kube/config/late-cluster.yml

export KUBECONFIG=$(pwd)/late-cluster.yml

# 네임스페이스

kubectl get namespace

## 네임스페이스 변경

kubectl config set-context --current --namespace=late-dev

kubectl config view --minify | grep namespace

# POD

kubectl get pod


# Service

kubectl get service

# Ingress

kubectl  get pods -n ingress-nginx

kubectl  get svc -n ingress-nginx

kubectl  get svc -n late-dev


# Labels for Selector
- latepod javalabs
- https://sam.webnori.com/swagger-ui/index.html


