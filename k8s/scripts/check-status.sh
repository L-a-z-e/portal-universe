#!/bin/bash

echo "📊 Portal Universe Status"
echo "========================="
echo ""

# Pods 상태
echo "🔹 Pods:"
kubectl get pods -n portal-universe

echo ""
echo "🔹 Services:"
kubectl get svc -n portal-universe

echo ""
echo "🔹 Deployments:"
kubectl get deployments -n portal-universe

echo ""
echo "📝 Quick Commands:"
echo "  - Watch pods: kubectl get pods -n portal-universe -w"
echo "  - View logs: kubectl logs -f deployment/<name> -n portal-universe"
echo "  - Describe pod: kubectl describe pod <pod-name> -n portal-universe"
echo "  - Port forward: kubectl port-forward svc/<service> <local-port>:<remote-port> -n portal-universe"