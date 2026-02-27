#!/bin/bash

echo "🚀 Building para PRODUCCIÓN..."

# Limpiar
./gradlew clean

# Build con perfil prod
SPRING_PROFILES_ACTIVE=prod ./gradlew bootWar

# Verificar
if [ -f "build/libs/api.war" ]; then
    echo "✅ Build exitoso!"
    echo "📦 Archivo: build/libs/api.war"
    echo ""
    echo "Para ejecutar:"
    echo "  java -jar -Dspring.profiles.active=prod build/libs/api.war"
else
    echo "❌ Error en el build"
    exit 1
fi