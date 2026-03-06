#!/bin/bash

set -e

echo "🚀 Starting ExtraLibrary Development Environment..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Load environment variables if .env.local exists
if [ -f .env.local ]; then
    echo "✅ Loading environment variables from .env.local"
    export $(cat .env.local | grep -v '^#' | grep -v '^$' | xargs)
fi

# Clean up any existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down --remove-orphans

# Build and start services
echo "🐳 Building and starting services..."
docker-compose up --build -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 45

# Check service health
echo "🔍 Checking service health..."
docker-compose ps

# Test database connection
echo "🗄️ Testing database connection..."
if docker-compose exec -T postgres pg_isready -U postgres -d libraryextra; then
    echo "✅ Database is ready"
else
    echo "❌ Database connection failed"
fi

# Test application health
echo "🏥 Testing application health..."
sleep 15
if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "✅ Application is healthy"
else
    echo "⚠️ Application might still be starting up..."
fi

echo ""
echo "🌐 Services available at:"
echo "  📚 API:       http://localhost:8081"
echo "  🗄️ PgAdmin:   http://localhost:5050"
echo "  💾 PostgreSQL: localhost:5432"
echo ""
echo "📋 Credentials:"
echo "  PgAdmin:   admin@libraryextra.com / admin123"
echo "  PostgreSQL: postgres / postgres"
echo "  API Admin: admin@extralibrary.com / admin123"
echo ""
echo "✅ Development environment is ready!"
echo "📄 View logs: docker-compose logs -f"
echo "🛑 Stop: docker-compose down"