import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Factures() {
  const [factures, setFactures] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchFactures();
  }, []);

  const fetchFactures = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllFactures();
      setFactures(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch factures: ' + err.message);
      console.error('Error fetching factures:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading factures...</div>;
  if (error) return <div className="card"><p style={{ color: 'red' }}>{error}</p></div>;

  return (
    <div className="factures">
      <div className="card">
        <h1>🧾 Factures Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Invoices: {factures.length}
        </p>
        
        <div className="table-container">
          {factures.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
              No invoices found. Add your first invoice to get started!
            </p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Montant Escompte</th>
                  <th>Montant Remise</th>
                  <th>Montant Facture</th>
                  <th>Date Création</th>
                  <th>Date Modification</th>
                  <th>Archivée</th>
                </tr>
              </thead>
              <tbody>
                {factures.map((facture) => (
                  <tr key={facture.idFacture}>
                    <td>{facture.idFacture}</td>
                    <td>{facture.montantEscompte ? `${facture.montantEscompte} TND` : 'N/A'}</td>
                    <td>{facture.montantRemise ? `${facture.montantRemise} TND` : 'N/A'}</td>
                    <td>{facture.montantFacture ? `${facture.montantFacture} TND` : 'N/A'}</td>
                    <td>{facture.dateCreationFacture ? new Date(facture.dateCreationFacture).toLocaleDateString() : 'N/A'}</td>
                    <td>{facture.dateDerniereModificationFacture ? new Date(facture.dateDerniereModificationFacture).toLocaleDateString() : 'N/A'}</td>
                    <td>{facture.archivee ? '✓' : '✗'}</td>
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

export default Factures;

