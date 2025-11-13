import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Fournisseurs() {
  const [fournisseurs, setFournisseurs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchFournisseurs();
  }, []);

  const fetchFournisseurs = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllFournisseurs();
      // Ensure data is always an array
      const data = Array.isArray(response.data) ? response.data : [];
      setFournisseurs(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch fournisseurs: ' + err.message);
      console.error('Error fetching fournisseurs:', err);
      setFournisseurs([]);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading fournisseurs...</div>;
  if (error) return <div className="card"><p style={{ color: 'red' }}>{error}</p></div>;

  return (
    <div className="fournisseurs">
      <div className="card">
        <h1>🏢 Fournisseurs Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Suppliers: {fournisseurs.length}
        </p>
        
        <div className="table-container">
          {fournisseurs.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
              No suppliers found. Add your first supplier to get started!
            </p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Code</th>
                  <th>Libellé</th>
                  <th>Catégorie</th>
                </tr>
              </thead>
              <tbody>
                {fournisseurs.map((fournisseur) => (
                  <tr key={fournisseur.idFournisseur}>
                    <td>{fournisseur.idFournisseur}</td>
                    <td>{fournisseur.codeFournisseur}</td>
                    <td>{fournisseur.libelleFournisseur}</td>
                    <td>{fournisseur.categorieFournisseur || 'N/A'}</td>
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

export default Fournisseurs;

