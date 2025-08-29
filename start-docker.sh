#!/bin/bash

# Timesheet Application Docker Startup Script

set -e

echo "🚀 Starting Timesheet Application with Docker..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose is not installed. Please install docker-compose and try again."
    exit 1
fi

# Create .env file from template if it doesn't exist
if [ ! -f .env ]; then
    if [ -f environment.env ]; then
        echo "📋 Creating .env file from environment.env template..."
        cp environment.env .env
        echo "⚠️  Please edit .env file with your actual configuration before proceeding."
        echo "   Important: Update SMTP credentials, database passwords, and JWT secrets."
        read -p "Press Enter after updating .env file, or Ctrl+C to cancel..."
    else
        echo "❌ No environment.env template found. Please create environment configuration."
        exit 1
    fi
fi

# Function to start services
start_services() {
    echo "🔧 Building and starting services..."
    docker-compose up -d --build
    
    echo "⏳ Waiting for services to be healthy..."
    sleep 10
    
    # Check service status
    echo "📊 Service Status:"
    docker-compose ps
    
    echo ""
    echo "🎉 Timesheet Application is starting up!"
    echo "   Application: http://0.0.0.0:${APP_PORT:-8080}"
    echo "   Database: 0.0.0.0:${MYSQL_PORT:-3306}"
    echo "   Remote access: http://YOUR_PI_IP:${APP_PORT:-8080}"
    echo ""
    echo "📝 To view logs: docker-compose logs -f"
    echo "🛑 To stop: docker-compose down"
}

# Function to show deployment info
show_deployment_info() {
    echo "📋 Raspberry Pi Deployment Information:"
    echo "   1. Copy this project folder to your Raspberry Pi"
    echo "   2. Update environment.env with your Pi's IP address"
    echo "   3. Run: ./start-docker.sh on your Pi"
    echo "   4. Access via: http://YOUR_PI_IP:8080"
}

# Parse command line arguments
case "${1:-start}" in
    "start")
        start_services
        ;;
    "deploy-info")
        show_deployment_info
        ;;
    "all")
        start_services
        ;;
    "stop")
        echo "🛑 Stopping all services..."
        docker-compose down
        ;;
    "logs")
        docker-compose logs -f
        ;;
    "status")
        echo "📊 Application Services:"
        docker-compose ps
        ;;
    "clean")
        echo "🧹 Cleaning up Docker resources..."
        docker-compose down -v
        docker system prune -f
        ;;
    *)
        echo "Usage: $0 {start|deploy-info|all|stop|logs|status|clean}"
        echo ""
        echo "Commands:"
        echo "  start       - Start timesheet application (default)"
        echo "  deploy-info - Show Raspberry Pi deployment information"
        echo "  all         - Start application services"
        echo "  stop        - Stop all services"
        echo "  logs        - View application logs"
        echo "  status      - Show status of all services"
        echo "  clean       - Stop services and clean up (WARNING: removes data)"
        exit 1
        ;;
esac
