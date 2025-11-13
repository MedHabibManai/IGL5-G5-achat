import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { apiService } from '../services/api';

function Home() {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchHealth();
  }, []);

  const fetchHealth = async () => {
    try {
      const response = await apiService.getHealth();
      setHealth(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching health:', error);
      setLoading(false);
    }
  };

  return (
    <div className="home">
      <div className="card">
        <h1>Welcome to Achat E-Commerce Platform</h1>
        <p style={{ fontSize: '1.1rem', color: '#666', marginTop: '1rem' }}>
          A complete e-commerce management system built with Spring Boot and React
        </p>
        
        {loading ? (
          <div className="loading">Loading system status...</div>
        ) : health ? (
          <div style={{ marginTop: '2rem', padding: '1rem', background: '#f0f9ff', borderRadius: '8px' }}>
            <h3 style={{ color: '#0ea5e9' }}>System Status: {health.status}</h3>
            <p style={{ marginTop: '0.5rem', color: '#666' }}>
              Database: {health.components?.db?.details?.database || 'Connected'}
            </p>
          </div>
        ) : (
          <div style={{ marginTop: '2rem', padding: '1rem', background: '#fef2f2', borderRadius: '8px' }}>
            <h3 style={{ color: '#ef4444' }}>Unable to connect to backend</h3>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Available Modules</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem', marginTop: '1.5rem' }}>
          <ModuleCard 
            title="Produits" 
            icon="📦" 
            description="Manage products and inventory"
            link="/produits"
          />
          <ModuleCard 
            title="Stocks" 
            icon="📊" 
            description="Track stock levels and movements"
            link="/stocks"
          />
          <ModuleCard 
            title="Fournisseurs" 
            icon="🏢" 
            description="Manage suppliers and vendors"
            link="/fournisseurs"
          />
          <ModuleCard 
            title="Factures" 
            icon="🧾" 
            description="Handle invoices and billing"
            link="/factures"
          />
          <ModuleCard 
            title="Opérateurs" 
            icon="👥" 
            description="Manage system operators"
            link="/operateurs"
          />
          <ModuleCard 
            title="Règlements" 
            icon="💳" 
            description="Process payments and settlements"
            link="/reglements"
          />
        </div>
      </div>

      <div className="card">
        <h2>Quick Links</h2>
        <div style={{ marginTop: '1rem' }}>
          <a href="/SpringMVC/swagger-ui/index.html" target="_blank" rel="noopener noreferrer" className="btn btn-primary">
            📚 API Documentation (Swagger)
          </a>
          <a href="/SpringMVC/actuator/health" target="_blank" rel="noopener noreferrer" className="btn btn-secondary">
            ❤️ Health Check
          </a>
        </div>
      </div>
    </div>
  );
}

function ModuleCard({ title, icon, description, link }) {
  return (
    <Link to={link} style={{ textDecoration: 'none' }}>
      <div style={{
        padding: '1.5rem',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        borderRadius: '10px',
        color: 'white',
        cursor: 'pointer',
        transition: 'transform 0.3s, box-shadow 0.3s',
        height: '100%'
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-5px)';
        e.currentTarget.style.boxShadow = '0 10px 20px rgba(0,0,0,0.2)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.boxShadow = 'none';
      }}>
        <div style={{ fontSize: '3rem', marginBottom: '0.5rem' }}>{icon}</div>
        <h3 style={{ marginBottom: '0.5rem' }}>{title}</h3>
        <p style={{ fontSize: '0.9rem', opacity: 0.9 }}>{description}</p>
      </div>
    </Link>
  );
}

export default Home;

