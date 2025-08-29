# Raspberry Pi Deployment Guide

This guide explains how to deploy your Timesheet Application on a Raspberry Pi and access it from other devices on your network.

## Prerequisites

### On Your Raspberry Pi:
- Raspberry Pi OS (32-bit or 64-bit)
- Docker and Docker Compose installed
- At least 2GB RAM (4GB recommended)
- At least 8GB free storage
- Network connectivity

### Installing Docker on Raspberry Pi:
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add your user to docker group
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose -y

# Restart to apply group changes
sudo reboot
```

## Deployment Steps

### 1. Copy Project to Raspberry Pi

Transfer your project folder to the Raspberry Pi using one of these methods:

#### Option A: Using SCP
```bash
# From your local machine
scp -r /Users/harkum/timesheet-app pi@YOUR_PI_IP:/home/pi/
```

#### Option B: Using Git (if you have it in a repository)
```bash
# On Raspberry Pi
git clone YOUR_REPOSITORY_URL
cd timesheet-app
```

#### Option C: Using USB/Network drive
Copy the entire `timesheet-app` folder to your Pi via USB drive or network share.

### 2. Configure Environment

```bash
# Navigate to project directory on Pi
cd /home/pi/timesheet-app

# Copy environment template
cp environment.env .env

# Edit environment file
nano .env
```

**Important:** Update these values in `.env`:
```bash
# Replace YOUR_PI_IP with your actual Pi IP address
APP_FRONTEND_URL=http://192.168.1.100:8080  # Example IP

# Update email settings for password reset functionality
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Change default passwords for security
MYSQL_ROOT_PASSWORD=your-strong-password
MYSQL_PASSWORD=your-db-password
JWT_SECRET=your-long-random-secret-key
```

### 3. Find Your Pi's IP Address

```bash
# On Raspberry Pi, find IP address
hostname -I
# or
ip addr show | grep inet

# Note: Use the 192.168.x.x or 10.x.x.x address
```

### 4. Deploy the Application

```bash
# Make startup script executable
chmod +x start-docker.sh

# Start the application
./start-docker.sh

# Or use docker-compose directly
docker-compose up -d
```

### 5. Access Your Application

Once deployed, you can access your timesheet application from any device on your network:

- **From any computer/phone on same network:** `http://YOUR_PI_IP:8080`
- **Example:** `http://192.168.1.100:8080`

## Network Configuration

### For Local Network Access:
The default configuration binds to `0.0.0.0:8080`, making it accessible from any device on your local network.

### For Internet Access (Advanced):
To access from outside your network, you'll need to:

1. **Configure Port Forwarding** on your router:
   - Forward external port (e.g., 8080) to your Pi's IP:8080
   - Ensure your router's firewall allows this

2. **Use Dynamic DNS** (if you don't have a static IP):
   - Services like DuckDNS, No-IP, or DynDNS
   - Update your environment: `APP_FRONTEND_URL=http://yourdomain.duckdns.org:8080`

3. **Consider HTTPS** for security:
   - Use a reverse proxy like Nginx or Traefik
   - Obtain SSL certificates (Let's Encrypt)

## Monitoring and Management

### Using Your Existing Portainer:
Since you already have Portainer running on your Pi, you can manage this application through it:

1. Access your Portainer: `http://YOUR_PI_IP:9000`
2. Go to "Stacks" and add a new stack
3. Copy the content of `docker-compose.yml`
4. Set environment variables in Portainer
5. Deploy the stack

### Using Command Line:
```bash
# View logs
docker-compose logs -f

# Check status
docker-compose ps

# Restart services
docker-compose restart

# Stop application
docker-compose down

# Update application (after code changes)
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## Performance Optimization for Raspberry Pi

### For Raspberry Pi 3/4:
```bash
# Add to your .env file for better performance
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
```

### For Limited Resources:
```bash
# Reduce MySQL memory usage
# Add to docker-compose.yml under mysql service:
command: --innodb-buffer-pool-size=128M --innodb-flush-log-at-trx-commit=2
```

## Troubleshooting

### Common Issues:

1. **Cannot access from other devices:**
   ```bash
   # Check if application is binding to all interfaces
   netstat -tlnp | grep 8080
   
   # Should show: 0.0.0.0:8080, not 127.0.0.1:8080
   ```

2. **Out of memory errors:**
   ```bash
   # Check system resources
   free -h
   df -h
   
   # Consider adding swap space
   sudo dphys-swapfile swapoff
   sudo nano /etc/dphys-swapfile  # Set CONF_SWAPSIZE=1024
   sudo dphys-swapfile setup
   sudo dphys-swapfile swapon
   ```

3. **Database connection issues:**
   ```bash
   # Check MySQL container logs
   docker-compose logs mysql
   
   # Restart database
   docker-compose restart mysql
   ```

4. **Application not starting:**
   ```bash
   # Check application logs
   docker-compose logs timesheet-app
   
   # Check if ports are already in use
   sudo netstat -tlnp | grep :8080
   ```

## Security Considerations

### For Production Use:
1. **Change all default passwords** in `.env`
2. **Use strong JWT secrets**
3. **Configure firewall rules:**
   ```bash
   sudo ufw enable
   sudo ufw allow 8080/tcp
   sudo ufw allow ssh
   ```
4. **Regular updates:**
   ```bash
   sudo apt update && sudo apt upgrade -y
   docker-compose pull
   docker-compose up -d
   ```

## Backup Strategy

### Database Backup:
```bash
# Create backup
docker exec timesheet-mysql mysqldump -u root -p timesheet_db > backup_$(date +%Y%m%d).sql

# Restore backup
docker exec -i timesheet-mysql mysql -u root -p timesheet_db < backup_20231201.sql
```

### Full Application Backup:
```bash
# Backup volumes
docker run --rm -v timesheet-app_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_backup.tar.gz -C /data .
docker run --rm -v timesheet-app_app_uploads:/data -v $(pwd):/backup alpine tar czf /backup/uploads_backup.tar.gz -C /data .
```

## Support

If you encounter issues:
1. Check the application logs: `docker-compose logs -f`
2. Verify network connectivity: `ping YOUR_PI_IP`
3. Ensure all environment variables are set correctly
4. Check available system resources: `free -h` and `df -h`

Your timesheet application should now be accessible from any device on your network at `http://YOUR_PI_IP:8080`!
