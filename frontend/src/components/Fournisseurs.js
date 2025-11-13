import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Fournisseurs() {
  const [fournisseurs, setFournisseurs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    code: '',
    libelle: '',
    categorieFournisseur: 'ORDINAIRE',
  });

  useEffect(() => {
    fetchFournisseurs();
  }, []);

  const fetchFournisseurs = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllFournisseurs();
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

  const resetForm = () => {
    setFormData({ code: '', libelle: '', categorieFournisseur: 'ORDINAIRE' });
    setEditingId(null);
    setShowForm(false);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        code: formData.code,
        libelle: formData.libelle,
        categorieFournisseur: formData.categorieFournisseur,
      };

      if (editingId) {
        await apiService.updateFournisseur({ ...payload, idFournisseur: editingId });
      } else {
        await apiService.addFournisseur(payload);
      }

      resetForm();
      fetchFournisseurs();
    } catch (err) {
      setError('Failed to save fournisseur: ' + err.message);
      console.error('Error saving fournisseur:', err);
    }
  };

  const handleEdit = (fournisseur) => {
    setEditingId(fournisseur.idFournisseur);
    setFormData({
      code: fournisseur.code || '',
      libelle: fournisseur.libelle || '',
      categorieFournisseur: fournisseur.categorieFournisseur || 'ORDINAIRE',
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this fournisseur?')) return;
    try {
      await apiService.deleteFournisseur(id);
      fetchFournisseurs();
    } catch (err) {
      setError('Failed to delete fournisseur: ' + err.message);
      console.error('Error deleting fournisseur:', err);
    }
  };

  const handleCancel = () => {
    resetForm();
  };

  if (loading) return <div className="loading">Loading fournisseurs...</div>;

  return (
    <div className="fournisseurs">
      <div className="card">
        <h1>🏢 Fournisseurs Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Suppliers: {fournisseurs.length}
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
            {showForm ? 'Close form' : 'Add new fournisseur'}
          </button>
        </div>

        {showForm && (
          <form onSubmit={handleSubmit} style={{ marginBottom: '2rem' }}>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 200px' }}>
                <label>Code</label>
                <input
                  type="text"
                  name="code"
                  value={formData.code}
                  onChange={handleInputChange}
                  required
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 200px' }}>
                <label>Libellé</label>
                <input
                  type="text"
                  name="libelle"
                  value={formData.libelle}
                  onChange={handleInputChange}
                  required
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 180px' }}>
                <label>Catégorie</label>
                <select
                  name="categorieFournisseur"
                  value={formData.categorieFournisseur}
                  onChange={handleInputChange}
                  className="form-input"
                >
                  <option value="ORDINAIRE">Ordinaire</option>
                  <option value="CONVENTIONNE">Conventionné</option>
                </select>
              </div>
            </div>
            <div style={{ marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update fournisseur' : 'Create fournisseur'}
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
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {fournisseurs.map((fournisseur) => (
                  <tr key={fournisseur.idFournisseur}>
                    <td>{fournisseur.idFournisseur}</td>
                    <td>{fournisseur.code}</td>
                    <td>{fournisseur.libelle}</td>
                    <td>{fournisseur.categorieFournisseur || 'N/A'}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-secondary"
                        onClick={() => handleEdit(fournisseur)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-primary"
                        style={{ background: '#e53e3e', marginLeft: '0.5rem' }}
                        onClick={() => handleDelete(fournisseur.idFournisseur)}
                      >
                        Delete
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

export default Fournisseurs;

