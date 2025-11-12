# 🏪 Achat Application - Full Stack

Complete e-commerce management system with **Spring Boot backend**, **React frontend**, and **MySQL database**.

## 📋 Architecture

```
Frontend (React)  →  Backend (Spring Boot)  →  Database (MySQL)
   Port 3000             Port 8089                Port 3306
```

### Technology Stack

**Frontend:**
- React.js 18
- Axios for API calls
- Modern CSS3
- Responsive design

**Backend:**
- Spring Boot 2.5.3
- Spring Data JPA
- MySQL Connector
- Spring Boot Actuator
- Swagger/OpenAPI

**Database:**
- MySQL 8.0
- Persistent storage

---

## 🚀 Quick Start

### Option 1: Run Everything with Docker Compose (Recommended)

```bash
# Build and start all services
docker-compose -f docker-compose-fullstack.yml up --build

# Access the application:
# Frontend: http://localhost:3000
# Backend API: http://localhost:8089/SpringMVC
# Swagger UI: http://localhost:8089/SpringMVC/swagger-ui/index.html
```

### Option 2: Run Services Separately

#### 1. Start Database

```bash
docker run -d \
  --name achat-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=achatdb \
  -p 3306:3306 \
  mysql:8.0
```

#### 2. Start Backend

```bash
# Run locally with Maven
mvn spring-boot:run

# OR build and run Docker
docker build -t achat-backend .
docker run -p 8089:8089 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/achatdb \
  achat-backend
```

#### 3. Start Frontend

```bash
cd frontend

# Development mode
npm install
npm start
# Opens http://localhost:3000

# OR build for production
npm run build
# Serve with: npx serve -s build -l 3000
```

---

## 📱 Features

### Product Management (Gestion des Produits)
- ✅ List all products
- ✅ Add new product
- ✅ Edit product
- ✅ Delete product
- ✅ Real-time updates

### Stock Management (Gestion des Stocks)
- ✅ View all stocks
- ✅ Add/Edit/Delete stocks
- ✅ Assign products to stocks

### Additional Modules (To Implement)
- 📦 Suppliers (Fournisseurs)
- 🧾 Invoices (Factures)
- 👤 Operators (Opérateurs)
- 💳 Payments (Règlements)

---

## 🔧 Configuration

### Backend Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/achatdb
spring.datasource.username=root
spring.datasource.password=root

# Server
server.port=8089
server.servlet.context-path=/SpringMVC

# CORS (enable for frontend)
# Add @CrossOrigin("*") on controllers
```

### Frontend Configuration

Edit `frontend/.env`:

```env
# Local development
REACT_APP_API_URL=http://localhost:8089/SpringMVC

# AWS deployment
# REACT_APP_API_URL=http://34.232.40.171:8089/SpringMVC

# Kubernetes
# REACT_APP_API_URL=http://localhost/SpringMVC
```

---

## 🎨 Adding More Features

### Add a New Entity (e.g., Fournisseur)

1. **Create Service** (`frontend/src/services/fournisseurService.js`):

```javascript
import apiClient from './apiClient';

const FOURNISSEUR_API = '/fournisseur';

export default {
  getAll: async () => {
    const response = await apiClient.get(`${FOURNISSEUR_API}/retrieve-all-fournisseurs`);
    return response.data;
  },
  // ... other methods
};
```

2. **Create Component** (`frontend/src/components/Fournisseur/FournisseurList.jsx`):

```javascript
import React, { useState, useEffect } from 'react';
import fournisseurService from '../../services/fournisseurService';

const FournisseurList = () => {
  const [fournisseurs, setFournisseurs] = useState([]);
  // ... similar to ProduitList
};
```

3. **Add to App.js**:

```javascript
import FournisseurList from './components/Fournisseur/FournisseurList';
```

---

## 🌐 Deployment Options

### Deploy to AWS

**Backend already deployed:**
```
http://34.232.40.171:8089/SpringMVC
```

**Deploy Frontend:**

1. Build production frontend:
```bash
cd frontend
npm run build
```

2. Upload to AWS S3 + CloudFront OR deploy with EC2:
```bash
# Build Docker image
docker build -t achat-frontend ./frontend

# Push to Docker Hub
docker tag achat-frontend habibmanai/achat-frontend:latest
docker push habibmanai/achat-frontend:latest

# Deploy to EC2 (same as backend)
```

### Deploy to Kubernetes

**Already have K8s manifests in `k8s/` directory!**

Add frontend deployment:

```yaml
# k8s/frontend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: achat-frontend
  namespace: achat-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: achat-frontend
  template:
    metadata:
      labels:
        app: achat-frontend
    spec:
      containers:
      - name: frontend
        image: habibmanai/achat-frontend:latest
        ports:
        - containerPort: 80
        env:
        - name: REACT_APP_API_URL
          value: "http://localhost/SpringMVC"
---
apiVersion: v1
kind: Service
metadata:
  name: achat-frontend
  namespace: achat-app
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 80
  selector:
    app: achat-frontend
```

Apply:
```bash
kubectl apply -f k8s/frontend-deployment.yaml
```

---

## 📊 API Endpoints (Backend)

### Products
- `GET /produit/retrieve-all-produits` - Get all products
- `GET /produit/retrieve-produit/{id}` - Get product by ID
- `POST /produit/add-produit` - Add new product
- `PUT /produit/modify-produit` - Update product
- `DELETE /produit/remove-produit/{id}` - Delete product

### Stocks
- `GET /stock/retrieve-all-stocks` - Get all stocks
- `GET /stock/retrieve-stock/{id}` - Get stock by ID
- `POST /stock/add-stock` - Add new stock
- `PUT /stock/modify-stock` - Update stock
- `DELETE /stock/remove-stock/{id}` - Delete stock

**Full API Documentation:**
```
http://localhost:8089/SpringMVC/swagger-ui/index.html
```

---

## 🐛 Troubleshooting

### CORS Errors

If you see CORS errors in browser console:

**Solution:** Backend already has `@CrossOrigin("*")` on controllers, but verify:

```java
@RestController
@CrossOrigin("*")  // ← Make sure this is present
@RequestMapping("/produit")
public class ProduitRestController {
  // ...
}
```

### Cannot Connect to Backend

1. Check backend is running: `http://localhost:8089/SpringMVC/actuator/health`
2. Check REACT_APP_API_URL in `.env` file
3. Restart frontend: `npm start`

### Database Connection Issues

```bash
# Check MySQL is running
docker ps | grep mysql

# Check database exists
docker exec -it achat-mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

---

## 📦 Project Structure

```
IGL5-G5-achat/
├── src/                        # Spring Boot backend
│   ├── main/
│   │   ├── java/
│   │   │   └── tn/esprit/rh/achat/
│   │   │       ├── controllers/
│   │   │       ├── entities/
│   │   │       ├── repositories/
│   │   │       ├── services/
│   │   │       └── AchatApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── frontend/                   # React frontend
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Produit/
│   │   │   │   ├── ProduitList.jsx
│   │   │   │   └── ProduitList.css
│   │   │   └── Stock/
│   │   ├── services/
│   │   │   ├── apiClient.js
│   │   │   ├── produitService.js
│   │   │   └── stockService.js
│   │   ├── App.js
│   │   ├── App.css
│   │   └── index.js
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── .env
├── k8s/                        # Kubernetes manifests
├── terraform/                  # AWS infrastructure
├── Jenkinsfile                 # CI/CD pipeline
├── Dockerfile                  # Backend Dockerfile
├── docker-compose-fullstack.yml
└── pom.xml
```

---

## 🎯 Next Steps

1. **Run the Application:**
   ```bash
   docker-compose -f docker-compose-fullstack.yml up
   ```

2. **Access Frontend:**
   ```
   http://localhost:3000
   ```

3. **Test the Features:**
   - Add a product
   - Edit a product
   - Delete a product
   - View list updates in real-time

4. **Expand Functionality:**
   - Add Stock component (similar to Produit)
   - Add Fournisseur component
   - Add Facture component
   - Add authentication (JWT)
   - Add user roles (admin, user)

5. **Deploy to Production:**
   - Build frontend: `npm run build`
   - Push Docker images to Docker Hub
   - Deploy via Jenkins pipeline
   - Access via AWS/Kubernetes URLs

---

## 🤝 Contributing

This is a university project for **IGL5-G5**. Team members can:

1. Create feature branches
2. Implement new components
3. Test locally
4. Push to GitHub
5. Jenkins will automatically build and deploy

---

## 📝 License

University Project - IGL5-G5
© 2025 Mohamed Habib Manai & Team

---

## 🆘 Support

- **Swagger API Docs:** http://localhost:8089/SpringMVC/swagger-ui/index.html
- **Health Check:** http://localhost:8089/SpringMVC/actuator/health
- **GitHub:** https://github.com/MedHabibManai/IGL5-G5-achat

Happy Coding! 🚀
