import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Operateurs() {
  const [operateurs, setOperateurs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    password: '',
  });

  useEffect(() => {
    fetchOperateurs();
  }, []);

  const fetchOperateurs = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllOperateurs();
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

  const resetForm = () => {
    setFormData({ nom: '', prenom: '', password: '' });
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
        nom: formData.nom,
        prenom: formData.prenom,
        password: formData.password,
      };

      if (editingId) {
        await apiService.updateOperateur({ ...payload, idOperateur: editingId });
      } else {
        await apiService.addOperateur(payload);
      }

      resetForm();
      fetchOperateurs();
    } catch (err) {
      setError('Failed to save operateur: ' + err.message);
      console.error('Error saving operateur:', err);
    }
  };

  const handleEdit = (operateur) => {
    setEditingId(operateur.idOperateur);
    setFormData({
      nom: operateur.nom || '',
      prenom: operateur.prenom || '',
      password: '',
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this operateur?')) return;
    try {
      await apiService.deleteOperateur(id);
      fetchOperateurs();
    } catch (err) {
      setError('Failed to delete operateur: ' + err.message);
      console.error('Error deleting operateur:', err);
    }
  };

  const handleCancel = () => {
    resetForm();
  };

  if (loading) return <div className="loading">Loading operateurs...</div>;

  return (
    <div className="operateurs">
      <div className="card">
        <h1>👥 Opérateurs Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Operators: {operateurs.length}
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
            {showForm ? 'Close form' : 'Add new operateur'}
          </button>
        </div>

        {showForm && (
          <form onSubmit={handleSubmit} style={{ marginBottom: '2rem' }}>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 200px' }}>
                <label>Nom</label>
                <input
                  type="text"
                  name="nom"
                  value={formData.nom}
                  onChange={handleInputChange}
                  required
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 200px' }}>
                <label>Prénom</label>
                <input
                  type="text"
                  name="prenom"
                  value={formData.prenom}
                  onChange={handleInputChange}
                  required
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 200px' }}>
                <label>Password</label>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  required
                  className="form-input"
                />
              </div>
            </div>
            <div style={{ marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update operateur' : 'Create operateur'}
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
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {operateurs.map((operateur) => (
                  <tr key={operateur.idOperateur}>
                    <td>{operateur.idOperateur}</td>
                    <td>{operateur.nom}</td>
                    <td>{operateur.prenom}</td>
                    <td>{'•'.repeat(8)}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-secondary"
                        onClick={() => handleEdit(operateur)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-primary"
                        style={{ background: '#e53e3e', marginLeft: '0.5rem' }}
                        onClick={() => handleDelete(operateur.idOperateur)}
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

export default Operateurs;

