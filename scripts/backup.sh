#!/bin/bash

set -e

BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

echo "💾 Creating backup in $BACKUP_DIR..."

# Backup database
if docker-compose ps postgres | grep -q "Up"; then
    echo "🗄️ Backing up PostgreSQL database..."
    docker-compose exec -T postgres pg_dump -U postgres libraryextra > $BACKUP_DIR/database.sql
    echo "✅ Database backup completed"
else
    echo "⚠️ PostgreSQL container is not running"
fi

# Backup configuration files
echo "📁 Backing up configuration files..."
cp .env.local $BACKUP_DIR/ 2>/dev/null || echo "No .env.local found"
cp application.yml $BACKUP_DIR/ 2>/dev/null || echo "No application.yml found"

echo "✅ Backup completed in $BACKUP_DIR"
echo "📊 Backup contents:"
ls -la $BACKUP_DIR