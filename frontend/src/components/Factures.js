import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Factures() {
  const [factures, setFactures] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    montantRemise: '',
    montantFacture: '',
    archivee: false,
  });

  useEffect(() => {
    fetchFactures();
  }, []);

  const fetchFactures = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllFactures();
      const data = Array.isArray(response.data) ? response.data : [];
      setFactures(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch factures: ' + err.message);
      console.error('Error fetching factures:', err);
      setFactures([]);
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFormData({ montantRemise: '', montantFacture: '', archivee: false });
    setEditingId(null);
    setShowForm(false);
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        montantRemise:
          formData.montantRemise === '' ? 0 : Number(formData.montantRemise),
        montantFacture:
          formData.montantFacture === '' ? 0 : Number(formData.montantFacture),
        archivee: formData.archivee,
      };

      if (editingId) {
        await apiService.addFacture({ ...payload, idFacture: editingId });
      } else {
        await apiService.addFacture(payload);
      }

      resetForm();
      fetchFactures();
    } catch (err) {
      setError('Failed to save facture: ' + err.message);
      console.error('Error saving facture:', err);
    }
  };

  const handleEdit = (facture) => {
    setEditingId(facture.idFacture);
    setFormData({
      montantRemise:
        facture.montantRemise != null ? String(facture.montantRemise) : '',
      montantFacture:
        facture.montantFacture != null ? String(facture.montantFacture) : '',
      archivee: !!facture.archivee,
    });
    setShowForm(true);
  };

  const handleCancel = () => {
    resetForm();
  };

  const handleArchive = async (id) => {
    if (!window.confirm('Archive this facture?')) return;
    try {
      await apiService.cancelFacture(id);
      fetchFactures();
    } catch (err) {
      setError('Failed to archive facture: ' + err.message);
      console.error('Error archiving facture:', err);
    }
  };

  if (loading) return <div className="loading">Loading factures...</div>;

  return (
    <div className="factures">
      <div className="card">
        <h1>🧾 Factures Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Invoices: {factures.length}
        </p>

        <div style={{ marginTop: '1rem', marginBottom: '1.5rem' }}>
          <button
            className="btn btn-primary"
            onClick={() => {
              setShowForm((prev) => !prev);
              if (showForm) {
                resetForm();
              }
            }}
          >
            {showForm ? 'Close form' : 'Add new facture'}
          </button>
        </div>

        {showForm && (
          <form onSubmit={handleSubmit} style={{ marginBottom: '2rem' }}>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 200px' }}>
                <label>Montant remise (TND)</label>
                <input
                  type="number"
                  step="0.01"
                  name="montantRemise"
                  value={formData.montantRemise}
                  onChange={handleInputChange}
                  min="0"
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 200px' }}>
                <label>Montant facture (TND)</label>
                <input
                  type="number"
                  step="0.01"
                  name="montantFacture"
                  value={formData.montantFacture}
                  onChange={handleInputChange}
                  min="0"
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 160px', display: 'flex', alignItems: 'center' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <input
                    type="checkbox"
                    name="archivee"
                    checked={formData.archivee}
                    onChange={handleInputChange}
                  />
                  Archivée
                </label>
              </div>
            </div>
            <div style={{ marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update facture' : 'Create facture'}
              </button>
              {editingId && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={handleCancel}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        )}

        <div className="table-container">
          {error && (
            <p style={{ color: 'red', marginBottom: '1rem' }}>{error}</p>
          )}
          {factures.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
              No invoices found. Add your first invoice to get started!
            </p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Montant Remise</th>
                  <th>Montant Facture</th>
                  <th>Date Création</th>
                  <th>Date Modification</th>
                  <th>Archivée</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {factures.map((facture) => (
                  <tr key={facture.idFacture}>
                    <td>{facture.idFacture}</td>
                    <td>
                      {facture.montantRemise
                        ? `${facture.montantRemise} TND`
                        : 'N/A'}
                    </td>
                    <td>
                      {facture.montantFacture
                        ? `${facture.montantFacture} TND`
                        : 'N/A'}
                    </td>
                    <td>
                      {facture.dateCreationFacture
                        ? new Date(
                            facture.dateCreationFacture
                          ).toLocaleDateString()
                        : 'N/A'}
                    </td>
                    <td>
                      {facture.dateDerniereModificationFacture
                        ? new Date(
                            facture.dateDerniereModificationFacture
                          ).toLocaleDateString()
                        : 'N/A'}
                    </td>
                    <td>{facture.archivee ? '✓' : '✗'}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-secondary"
                        onClick={() => handleEdit(facture)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-primary"
                        style={{ background: '#e53e3e', marginLeft: '0.5rem' }}
                        onClick={() => handleArchive(facture.idFacture)}
                      >
                        Archive
                      </button>
                    </td>
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

