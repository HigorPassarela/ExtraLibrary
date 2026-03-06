#!/bin/bash

set -e

echo "🚀 Preparing ExtraLibrary for deployment..."

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ Not in a git repository. Please initialize git first."
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "⚠️ You have uncommitted changes. Please commit them first."
    git status --short
    exit 1
fi

# Run tests
echo "🧪 Running tests..."
if mvn test -q; then
    echo "✅ All tests passed"
else
    echo "❌ Tests failed. Please fix them before deploying."
    exit 1
fi

# Build application
echo "🔨 Building application..."
if mvn clean package -DskipTests -q; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi

# Test Docker build
echo "🐳 Testing Docker build..."
if docker build -t extralibrary:test . > /dev/null 2>&1; then
    echo "✅ Docker build successful"
    docker rmi extralibrary:test > /dev/null 2>&1
else
    echo "❌ Docker build failed"
    exit 1
fi

# Get current version/commit
COMMIT_HASH=$(git rev-parse --short HEAD)
BRANCH=$(git branch --show-current)

echo "📝 Deployment Summary:"
echo "  Branch: $BRANCH"
echo "  Commit: $COMMIT_HASH"
echo "  Time: $(date)"

# Push to repository
echo "📤 Pushing to repository..."
git push origin $BRANCH

echo ""
echo "✅ Ready for Railway deployment!"
echo ""
echo "🚂 Next steps:"
echo "1. Go to https://railway.app"
echo "2. Connect your GitHub repository"
echo "3. Add PostgreSQL database"
echo "4. Set environment variables:"
echo "   - JWT_SECRET=your-secret-key"
echo "   - ADMIN_PASSWORD=secure-password"
echo "   - LIBRARIAN_PASSWORD=secure-password"
echo "5. Deploy!"
echo ""
echo "📚 Documentation: https://docs.railway.app"