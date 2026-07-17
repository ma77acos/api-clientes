#!/bin/bash

set -e

SERVER="root@ma77acos.site"
REMOTE_TMP="/tmp"

echo "======================================"
echo "🚀 BUILD + UPLOAD"
echo "======================================"

echo ""
echo "🧪 Corriendo unit tests (si fallan, el deploy se aborta)..."
if ! ./gradlew test; then
  echo ""
  echo "❌ Tests fallaron. Deploy abortado — no se genera JAR ni se sube a producción."
  exit 1
fi
echo "✅ Tests OK"

echo ""
echo "🧹 Ejecutando Gradle (jarProd)..."
./gradlew clean jarProd

echo ""
echo "🔍 Buscando jar generado..."

JAR_FILE=$(find build/libs -maxdepth 1 -name "*.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
  echo "❌ No se encontró ningún .jar en build/libs"
  exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
APP_NAME="${JAR_NAME%.jar}"

echo "📦 Jar encontrado: $JAR_NAME"
echo "🧩 App detectada: $APP_NAME"

echo ""
echo "📤 Subiendo al VPS..."

scp "$JAR_FILE" "${SERVER}:${REMOTE_TMP}/"

echo ""
echo "✅ Upload completado"

echo ""
echo "👉 Ejecutar en VPS:"
echo ""
echo "deploy $APP_NAME"
echo ""

echo ""
echo "🚀 Ejecutando deploy remoto..."

ssh "$SERVER" "/opt/deploy/deploy.sh $APP_NAME"

echo ""
echo "✅ DEPLOY COMPLETO"