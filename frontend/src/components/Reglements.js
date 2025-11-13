import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Reglements() {
  const [reglements, setReglements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    montantPaye: '',
    montantRestant: '',
    dateReglement: '',
    payee: false,
  });

  useEffect(() => {
    fetchReglements();
  }, []);

  const fetchReglements = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllReglements();
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

  const resetForm = () => {
    setFormData({ montantPaye: '', montantRestant: '', dateReglement: '', payee: false });
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
        montantPaye:
          formData.montantPaye === '' ? 0 : Number(formData.montantPaye),
        montantRestant:
          formData.montantRestant === '' ? 0 : Number(formData.montantRestant),
        payee: formData.payee,
        dateReglement: formData.dateReglement || null,
      };

      if (editingId) {
        await apiService.addReglement({ ...payload, idReglement: editingId });
      } else {
        await apiService.addReglement(payload);
      }

      resetForm();
      fetchReglements();
    } catch (err) {
      setError('Failed to save reglement: ' + err.message);
      console.error('Error saving reglement:', err);
    }
  };

  const handleEdit = (reglement) => {
    setEditingId(reglement.idReglement);
    setFormData({
      montantPaye:
        reglement.montantPaye != null ? String(reglement.montantPaye) : '',
      montantRestant:
        reglement.montantRestant != null
          ? String(reglement.montantRestant)
          : '',
      dateReglement: reglement.dateReglement
        ? new Date(reglement.dateReglement).toISOString().slice(0, 10)
        : '',
      payee: !!reglement.payee,
    });
    setShowForm(true);
  };

  const handleCancel = () => {
    resetForm();
  };

  if (loading) return <div className="loading">Loading reglements...</div>;

  return (
    <div className="reglements">
      <div className="card">
        <h1>💳 Règlements Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Payments: {reglements.length}
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
            {showForm ? 'Close form' : 'Add new reglement'}
          </button>
        </div>

        {showForm && (
          <form onSubmit={handleSubmit} style={{ marginBottom: '2rem' }}>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 160px' }}>
                <label>Montant payé (TND)</label>
                <input
                  type="number"
                  step="0.01"
                  name="montantPaye"
                  value={formData.montantPaye}
                  onChange={handleInputChange}
                  min="0"
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 160px' }}>
                <label>Montant restant (TND)</label>
                <input
                  type="number"
                  step="0.01"
                  name="montantRestant"
                  value={formData.montantRestant}
                  onChange={handleInputChange}
                  min="0"
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 160px' }}>
                <label>Date règlement</label>
                <input
                  type="date"
                  name="dateReglement"
                  value={formData.dateReglement}
                  onChange={handleInputChange}
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 120px', display: 'flex', alignItems: 'center' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <input
                    type="checkbox"
                    name="payee"
                    checked={formData.payee}
                    onChange={handleInputChange}
                  />
                  Payé
                </label>
              </div>
            </div>
            <div style={{ marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update reglement' : 'Create reglement'}
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
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {reglements.map((reglement) => (
                  <tr key={reglement.idReglement}>
                    <td>{reglement.idReglement}</td>
                    <td>
                      {reglement.montantPaye
                        ? `${reglement.montantPaye} TND`
                        : 'N/A'}
                    </td>
                    <td>
                      {reglement.montantRestant
                        ? `${reglement.montantRestant} TND`
                        : 'N/A'}
                    </td>
                    <td>
                      {reglement.dateReglement
                        ? new Date(reglement.dateReglement).toLocaleDateString()
                        : 'N/A'}
                    </td>
                    <td>{reglement.payee ? '✓' : '✗'}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-secondary"
                        onClick={() => handleEdit(reglement)}
                      >
                        Edit
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

export default Reglements;

