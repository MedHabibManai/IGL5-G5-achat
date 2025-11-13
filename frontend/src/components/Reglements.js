import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Reglements() {
  const [reglements, setReglements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchReglements();
  }, []);

  const fetchReglements = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllReglements();
      // Ensure data is always an array
      const data = Array.isArray(response.data) ? response.data : [];
      setReglements(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch reglements: ' + err.message);
      console.error('Error fetching reglements:', err);
      setReglements([]);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading reglements...</div>;
  if (error) return <div className="card"><p style={{ color: 'red' }}>{error}</p></div>;

  return (
    <div className="reglements">
      <div className="card">
        <h1>💳 Règlements Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Payments: {reglements.length}
        </p>
        
        <div className="table-container">
          {reglements.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
              No payments found. Add your first payment to get started!
            </p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Montant Payé</th>
                  <th>Montant Restant</th>
                  <th>Date Règlement</th>
                  <th>Payé</th>
                </tr>
              </thead>
              <tbody>
                {reglements.map((reglement) => (
                  <tr key={reglement.idReglement}>
                    <td>{reglement.idReglement}</td>
                    <td>{reglement.montantPaye ? `${reglement.montantPaye} TND` : 'N/A'}</td>
                    <td>{reglement.montantRestant ? `${reglement.montantRestant} TND` : 'N/A'}</td>
                    <td>{reglement.dateReglement ? new Date(reglement.dateReglement).toLocaleDateString() : 'N/A'}</td>
                    <td>{reglement.payee ? '✓' : '✗'}</td>
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

export default Reglements;

