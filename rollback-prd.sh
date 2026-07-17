#!/bin/bash

set -e

SERVER="root@ma77acos.site"

echo "======================================"
echo "⏪ REMOTE ROLLBACK"
echo "======================================"

echo ""
echo "🔍 Detectando app..."

JAR_FILE=$(find build/libs -maxdepth 1 -name "*.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
  echo "❌ No se encontró ningún .jar en build/libs"
  exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
APP_NAME="${JAR_NAME%.jar}"

echo "🧩 App detectada: $APP_NAME"

echo ""
echo "🚀 Ejecutando rollback remoto..."

ssh "$SERVER" "/opt/deploy/rollback.sh $APP_NAME"

echo ""
echo "✅ ROLLBACK COMPLETADO"
