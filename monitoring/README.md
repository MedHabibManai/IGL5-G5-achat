# Monitoring Stack - Prometheus & Grafana

## Overview

This directory contains the configuration for the monitoring stack (Phase 4) which includes:
- **Prometheus**: Metrics collection and storage
- **Grafana**: Metrics visualization and dashboards

## Directory Structure

```
monitoring/
├── README.md                           # This file
├── prometheus.yml                      # Prometheus configuration
└── grafana/
    ├── dashboards/
    │   ├── dashboard-provider.yml      # Dashboard provisioning config
    │   └── jenkins-dashboard.json      # Jenkins metrics dashboard
    └── datasources/
        └── prometheus.yml              # Prometheus datasource config
```

## Quick Start

### 1. Start Monitoring Services

```bash
# Start Prometheus and Grafana
docker-compose --profile phase4 up -d

# Check status
docker ps | grep -E "prometheus|grafana"
```

### 2. Access the Services

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000
  - Username: `admin`
  - Password: `admin` (you'll be prompted to change on first login)

### 3. Verify Setup

**Check Prometheus:**
1. Go to http://localhost:9090
2. Click **Status** → **Targets**
3. Verify targets are being scraped (may show as DOWN if Jenkins Prometheus plugin not installed)

**Check Grafana:**
1. Go to http://localhost:3000
2. Login with admin/admin
3. Go to **Configuration** → **Data Sources**
4. Verify Prometheus datasource is configured
5. Go to **Dashboards** → **Browse**
6. Open "Jenkins CI/CD Metrics" dashboard

## Configuration Details

### Prometheus Configuration

The `prometheus.yml` file configures:
- **Scrape interval**: 15 seconds
- **Targets**:
  - Prometheus itself (localhost:9090)
  - Jenkins (jenkins-cicd:8080/prometheus)
  - SonarQube (sonarqube-server:9000/api/monitoring/metrics)
  - Nexus (nexus-repository:8081/service/metrics/prometheus)

### Grafana Configuration

**Datasource** (`grafana/datasources/prometheus.yml`):
- Automatically provisions Prometheus as the default datasource
- URL: http://prometheus-monitoring:9090

**Dashboard** (`grafana/dashboards/jenkins-dashboard.json`):
- Pre-configured Jenkins metrics dashboard
- Displays:
  - Total executors
  - Build queue size
  - Total nodes
  - Job activity rate
  - Executor usage over time

## Jenkins Prometheus Plugin Setup

To enable Jenkins metrics collection, you need to install the Prometheus plugin:

### Step 1: Install Plugin

1. Go to Jenkins: http://localhost:8080
2. **Manage Jenkins** → **Manage Plugins**
3. Click **Available** tab
4. Search for "Prometheus metrics"
5. Check the box next to "Prometheus metrics plugin"
6. Click **Install without restart**
7. Wait for installation to complete

### Step 2: Configure Plugin (Optional)

1. **Manage Jenkins** → **Configure System**
2. Scroll to **Prometheus** section
3. Default settings are usually fine:
   - Path: `/prometheus`
   - Collecting metrics period: 120 seconds
4. Click **Save**

### Step 3: Verify Metrics Endpoint

```bash
# Check if Jenkins is exposing metrics
curl http://localhost:8080/prometheus

# You should see metrics output like:
# jenkins_executor_count_value 2.0
# jenkins_queue_size_value 0.0
# etc.
```

### Step 4: Verify in Prometheus

1. Go to Prometheus: http://localhost:9090
2. Click **Status** → **Targets**
3. Find the "jenkins" target
4. Status should be **UP** (green)

## Troubleshooting

### Prometheus Can't Scrape Jenkins

**Problem**: Jenkins target shows as DOWN in Prometheus

**Solutions**:
1. Install Prometheus metrics plugin in Jenkins (see above)
2. Verify Jenkins is accessible from Prometheus container:
   ```bash
   docker exec prometheus-monitoring wget -O- http://jenkins-cicd:8080/prometheus
   ```
3. Check Prometheus logs:
   ```bash
   docker logs prometheus-monitoring
   ```

### Grafana Can't Connect to Prometheus

**Problem**: Grafana shows "Bad Gateway" or connection errors

**Solutions**:
1. Verify Prometheus is running:
   ```bash
   docker ps | grep prometheus
   ```
2. Check Prometheus is accessible from Grafana:
   ```bash
   docker exec grafana-dashboard wget -O- http://prometheus-monitoring:9090/-/healthy
   ```
3. Check Grafana logs:
   ```bash
   docker logs grafana-dashboard
   ```

### Dashboard Shows No Data

**Problem**: Jenkins dashboard is empty

**Possible causes**:
1. Jenkins Prometheus plugin not installed
2. No builds have run yet (no metrics to collect)
3. Prometheus not scraping Jenkins successfully

**Solutions**:
1. Install Jenkins Prometheus plugin
2. Run a Jenkins build to generate metrics
3. Check Prometheus targets are UP
4. Verify metrics are available:
   ```bash
   curl http://localhost:8080/prometheus | grep jenkins_
   ```

### Network Timeout When Pulling Images

**Problem**: `net/http: TLS handshake timeout` when starting services

**Solutions**:
1. Check internet connection
2. Retry after a few minutes:
   ```bash
   docker-compose --profile phase4 up -d
   ```
3. Pull images manually first:
   ```bash
   docker pull prom/prometheus:latest
   docker pull grafana/grafana:latest
   docker-compose --profile phase4 up -d
   ```

## Metrics Available

### Jenkins Metrics

Once the Prometheus plugin is installed, Jenkins exposes these metrics:

- `jenkins_executor_count_value` - Total number of executors
- `jenkins_executor_in_use_value` - Number of executors currently in use
- `jenkins_executor_free_value` - Number of free executors
- `jenkins_queue_size_value` - Number of jobs in the build queue
- `jenkins_node_count_value` - Total number of nodes
- `jenkins_node_online_value` - Number of online nodes
- `jenkins_job_count_value` - Total number of jobs
- `jenkins_runs_success_total` - Total successful builds
- `jenkins_runs_failure_total` - Total failed builds
- `jenkins_runs_duration_milliseconds_summary` - Build duration statistics

### Custom Application Metrics (Future)

To add Spring Boot application metrics:

1. Add Micrometer Prometheus dependency to `pom.xml`:
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```

2. Enable Prometheus endpoint in `application.properties`:
   ```properties
   management.endpoints.web.exposure.include=health,info,prometheus
   management.metrics.export.prometheus.enabled=true
   ```

3. Uncomment the `achat-app` job in `prometheus.yml`

## Next Steps

1. ✅ Install Jenkins Prometheus plugin
2. ✅ Verify Prometheus is scraping Jenkins metrics
3. ✅ Access Grafana and view Jenkins dashboard
4. 📊 Create custom dashboards for:
   - Build success/failure rates
   - Build duration trends
   - Queue wait times
   - Resource usage
5. 🔔 Set up alerting rules in Prometheus
6. 📧 Configure alert notifications (email, Slack, etc.)

## Useful Commands

```bash
# Start monitoring stack
docker-compose --profile phase4 up -d

# Stop monitoring stack
docker-compose --profile phase4 down

# View Prometheus logs
docker logs -f prometheus-monitoring

# View Grafana logs
docker logs -f grafana-dashboard

# Restart Prometheus (reload config)
docker restart prometheus-monitoring

# Restart Grafana
docker restart grafana-dashboard

# Check Prometheus configuration
docker exec prometheus-monitoring promtool check config /etc/prometheus/prometheus.yml

# Query Prometheus from command line
curl 'http://localhost:9090/api/v1/query?query=jenkins_executor_count_value'
```

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Jenkins Prometheus Plugin](https://plugins.jenkins.io/prometheus/)
- [Prometheus Query Language (PromQL)](https://prometheus.io/docs/prometheus/latest/querying/basics/)

