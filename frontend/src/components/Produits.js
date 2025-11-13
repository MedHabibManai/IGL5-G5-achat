import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Produits() {
  const [produits, setProduits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchProduits();
  }, []);

  const fetchProduits = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllProduits();
      // Ensure data is always an array
      const data = Array.isArray(response.data) ? response.data : [];
      setProduits(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch produits: ' + err.message);
      console.error('Error fetching produits:', err);
      setProduits([]);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading produits...</div>;
  if (error) return <div className="card"><p style={{ color: 'red' }}>{error}</p></div>;

  return (
    <div className="produits">
      <div className="card">
        <h1>📦 Produits Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Products: {produits.length}
        </p>
        
        <div className="table-container">
          {produits.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
              No products found. Add your first product to get started!
            </p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Code</th>
                  <th>Libellé</th>
                  <th>Prix</th>
                  <th>Date Création</th>
                  <th>Date Modification</th>
                  <th>Catégorie</th>
                </tr>
              </thead>
              <tbody>
                {produits.map((produit) => (
                  <tr key={produit.idProduit}>
                    <td>{produit.idProduit}</td>
                    <td>{produit.codeProduit}</td>
                    <td>{produit.libelleProduit}</td>
                    <td>{produit.prix ? `${produit.prix} TND` : 'N/A'}</td>
                    <td>{produit.dateCreation ? new Date(produit.dateCreation).toLocaleDateString() : 'N/A'}</td>
                    <td>{produit.dateDerniereModification ? new Date(produit.dateDerniereModification).toLocaleDateString() : 'N/A'}</td>
                    <td>{produit.categorieProduit || 'N/A'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

export default Produits;

