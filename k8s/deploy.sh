#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
NS=task-tracker

kubectl apply -f "$ROOT/namespace.yaml"
kubectl apply -f "$ROOT/configmap.yaml"
kubectl apply -f "$ROOT/secret.yaml"
kubectl apply -f "$ROOT/pvc.yaml"
kubectl apply -f "$ROOT/service.yaml"
kubectl apply -f "$ROOT/deployment.yaml"

kubectl -n "$NS" rollout status deployment/postgres --timeout=180s
kubectl -n "$NS" wait --for=condition=ready pod -l app=postgres --timeout=180s

kubectl -n "$NS" delete job tracker-migrate --ignore-not-found
kubectl apply -f "$ROOT/job.yaml"
kubectl -n "$NS" wait --for=condition=complete job/tracker-migrate --timeout=180s

kubectl -n "$NS" rollout status deployment/tracker-app --timeout=180s

kubectl -n "$NS" get pods -o wide
kubectl -n "$NS" get svc
