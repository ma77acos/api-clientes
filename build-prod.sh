#!/bin/bash
# Build JAR de producción (mismo artefacto que deploy-prd.sh)
set -e

echo "🚀 Building JAR para PRODUCCIÓN..."

./gradlew clean jarProd

if [ -f "build/libs/turnos-control.jar" ]; then
  echo "✅ Build exitoso!"
  echo "📦 Archivo: build/libs/turnos-control.jar"
  echo ""
  echo "Para ejecutar localmente con perfil prod:"
  echo "  java -jar -Dspring.profiles.active=prod build/libs/turnos-control.jar"
else
  echo "❌ Error en el build"
  exit 1
fi
