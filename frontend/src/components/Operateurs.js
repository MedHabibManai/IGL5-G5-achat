import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Operateurs() {
  const [operateurs, setOperateurs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchOperateurs();
  }, []);

  const fetchOperateurs = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllOperateurs();
      // Ensure data is always an array
      const data = Array.isArray(response.data) ? response.data : [];
      setOperateurs(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch operateurs: ' + err.message);
      console.error('Error fetching operateurs:', err);
      setOperateurs([]);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading operateurs...</div>;
  if (error) return <div className="card"><p style={{ color: 'red' }}>{error}</p></div>;

  return (
    <div className="operateurs">
      <div className="card">
        <h1>👥 Opérateurs Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Operators: {operateurs.length}
        </p>
        
        <div className="table-container">
          {operateurs.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
              No operators found. Add your first operator to get started!
            </p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Nom</th>
                  <th>Prénom</th>
                  <th>Password</th>
                </tr>
              </thead>
              <tbody>
                {operateurs.map((operateur) => (
                  <tr key={operateur.idOperateur}>
                    <td>{operateur.idOperateur}</td>
                    <td>{operateur.nom}</td>
                    <td>{operateur.prenom}</td>
                    <td>{'*'.repeat(8)}</td>
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

export default Operateurs;

