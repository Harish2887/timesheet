# Docker Setup for Timesheet Application

This document provides instructions for running the Timesheet Application using Docker and managing it with Portainer.

## Prerequisites

- Docker Desktop or Docker Engine installed
- Docker Compose installed
- At least 2GB of free RAM
- At least 5GB of free disk space

## Quick Start

### 1. Environment Setup

Copy the environment file and customize it:
```bash
cp environment.env .env
```

Edit `.env` file and update the following values:
- `SMTP_USERNAME` and `SMTP_PASSWORD` for email functionality
- `MYSQL_ROOT_PASSWORD` and `MYSQL_PASSWORD` for database security
- `JWT_SECRET` and `APP_JWT_SECRET` for JWT token security

### 2. Run the Application

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop services and remove volumes (WARNING: This will delete all data)
docker-compose down -v
```

### 3. Access the Application

- **Timesheet Application**: http://localhost:8080
- **MySQL Database**: localhost:3306 (from host machine)

### 4. Access the Application

Once the containers are running:
- **Timesheet Application**: http://localhost:8080 (or http://YOUR_SERVER_IP:8080 for remote access)
- **MySQL Database**: localhost:3306

**Note**: For Raspberry Pi deployment, see `RASPBERRY-PI-DEPLOY.md` for detailed instructions.

## Docker Commands Reference

### Application Management

```bash
# Build the application image
docker-compose build

# Start services in background
docker-compose up -d

# Start services with logs
docker-compose up

# View running containers
docker-compose ps

# View logs for all services
docker-compose logs

# View logs for specific service
docker-compose logs timesheet-app
docker-compose logs mysql

# Restart a specific service
docker-compose restart timesheet-app

# Stop all services
docker-compose down

# Stop and remove everything including volumes
docker-compose down -v --remove-orphans
```

### Database Management

```bash
# Connect to MySQL database
docker exec -it timesheet-mysql mysql -u timesheet_user -p timesheet_db

# Backup database
docker exec timesheet-mysql mysqldump -u root -p timesheet_db > backup.sql

# Restore database
docker exec -i timesheet-mysql mysql -u root -p timesheet_db < backup.sql

# View database logs
docker-compose logs mysql
```

### Application Debugging

```bash
# View application logs
docker-compose logs -f timesheet-app

# Execute shell in application container
docker exec -it timesheet-app /bin/sh

# View application health
docker inspect timesheet-app --format='{{.State.Health.Status}}'
```

## File Structure

```
timesheet-app/
├── Dockerfile                      # Multi-stage build for the application
├── docker-compose.yml             # Main application stack
├── .dockerignore                   # Files to ignore during Docker build
├── environment.env                 # Environment variables template
├── DOCKER-README.md               # This file
├── RASPBERRY-PI-DEPLOY.md         # Raspberry Pi deployment guide
├── start-docker.sh                # Startup script
├── backend/                        # Spring Boot backend
└── frontend/                       # React frontend
```

## Services Overview

### timesheet-app
- **Image**: Built from local Dockerfile
- **Port**: 8080
- **Description**: Spring Boot application with embedded React frontend
- **Volumes**: 
  - `app_uploads:/app/uploads` - File uploads storage
  - `app_logs:/app/logs` - Application logs

### mysql
- **Image**: mysql:8.0
- **Port**: 3306
- **Description**: MySQL database server
- **Volumes**: 
  - `mysql_data:/var/lib/mysql` - Database files



## Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `MYSQL_ROOT_PASSWORD` | MySQL root password | timesheet123 |
| `MYSQL_DATABASE` | Database name | timesheet_db |
| `MYSQL_USER` | Database user | timesheet_user |
| `MYSQL_PASSWORD` | Database password | timesheet123 |
| `APP_PORT` | Application port | 8080 |
| `JWT_SECRET` | JWT signing secret | (default provided) |
| `SMTP_HOST` | Email SMTP host | smtp.gmail.com |
| `SMTP_USERNAME` | Email username | your-email@gmail.com |
| `SMTP_PASSWORD` | Email password/app password | your-app-password |

## Volumes

- **mysql_data**: Persists MySQL database files
- **app_uploads**: Stores uploaded invoice files
- **app_logs**: Stores application log files


## Networks

- **timesheet-network**: Internal network for application communication


## Health Checks

Both the application and database include health checks:
- **MySQL**: Uses `mysqladmin ping`
- **Application**: Checks `/actuator/health` endpoint

## Troubleshooting

### Common Issues

1. **Port already in use**
   ```bash
   # Check what's using the port
   lsof -i :8080
   # Change APP_PORT in .env file
   ```

2. **Database connection failed**
   ```bash
   # Check if MySQL is healthy
   docker-compose ps
   # View MySQL logs
   docker-compose logs mysql
   ```

3. **Application won't start**
   ```bash
   # Check application logs
   docker-compose logs timesheet-app
   # Verify environment variables
   docker exec timesheet-app env | grep SPRING
   ```

4. **Out of disk space**
   ```bash
   # Clean up unused Docker resources
   docker system prune -a
   # Remove unused volumes
   docker volume prune
   ```

### Logs Location

- Application logs: `docker-compose logs timesheet-app`
- Database logs: `docker-compose logs mysql`
- All logs: `docker-compose logs`

### Performance Tuning

For production deployments, consider:
- Increasing memory limits in docker-compose.yml
- Using external database
- Setting up proper logging rotation
- Implementing backup strategies

## Production Deployment

For production use:

1. **Security**:
   - Change all default passwords
   - Use strong JWT secrets
   - Configure proper SMTP settings
   - Use HTTPS with reverse proxy

2. **Persistence**:
   - Use named volumes or bind mounts for data
   - Implement database backup strategy
   - Configure log rotation

3. **Monitoring**:
   - Set up health monitoring
   - Configure alerts
   - Use Portainer for container management

4. **Scaling**:
   - Use external database for multiple app instances
   - Implement load balancing
   - Consider using Docker Swarm or Kubernetes

## Support

For issues related to:
- **Docker setup**: Check this README and Docker logs
- **Application functionality**: Check application logs
- **Database issues**: Check MySQL logs and connection settings
