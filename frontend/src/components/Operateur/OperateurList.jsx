import React, { useState, useEffect } from 'react';
import operateurService from '../../services/operateurService';
import './OperateurList.css';

const OperateurList = () => {
  const [operateurs, setOperateurs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingOperateur, setEditingOperateur] = useState(null);
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
      const data = await operateurService.getAllOperateurs();
      setOperateurs(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch operateurs: ' + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this operateur?')) {
      try {
        await operateurService.deleteOperateur(id);
        fetchOperateurs();
      } catch (err) {
        alert('Failed to delete operateur: ' + err.message);
      }
    }
  };

  const handleEdit = (operateur) => {
    setEditingOperateur(operateur);
    setFormData({
      nom: operateur.nom || '',
      prenom: operateur.prenom || '',
      password: '',
    });
    setShowForm(true);
  };

  const handleAdd = () => {
    setEditingOperateur(null);
    setFormData({
      nom: '',
      prenom: '',
      password: '',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingOperateur) {
        await operateurService.updateOperateur({
          ...editingOperateur,
          ...formData,
        });
      } else {
        await operateurService.addOperateur(formData);
      }
      setShowForm(false);
      fetchOperateurs();
    } catch (err) {
      alert('Failed to save operateur: ' + err.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  if (loading) return <div className="loading">Loading operateurs...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="operateur-container">
      <div className="header">
        <h1>👤 Gestion des Opérateurs</h1>
        <button className="btn btn-primary" onClick={handleAdd}>
          + Ajouter Opérateur
        </button>
      </div>

      {showForm && (
        <div className="modal">
          <div className="modal-content">
            <h2>{editingOperateur ? 'Modifier Opérateur' : 'Nouveau Opérateur'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Nom:</label>
                <input
                  type="text"
                  name="nom"
                  value={formData.nom}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Prénom:</label>
                <input
                  type="text"
                  name="prenom"
                  value={formData.prenom}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Password:</label>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  required={!editingOperateur}
                />
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary">
                  Enregistrer
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowForm(false)}
                >
                  Annuler
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="operateur-list">
        {operateurs.length === 0 ? (
          <p className="no-data">Aucun opérateur disponible</p>
        ) : (
          <table className="operateur-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nom</th>
                <th>Prénom</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {operateurs.map((operateur) => (
                <tr key={operateur.idOperateur}>
                  <td>{operateur.idOperateur}</td>
                  <td>{operateur.nom}</td>
                  <td>{operateur.prenom}</td>
                  <td className="actions">
                    <button
                      className="btn btn-edit"
                      onClick={() => handleEdit(operateur)}
                    >
                      ✏️ Modifier
                    </button>
                    <button
                      className="btn btn-delete"
                      onClick={() => handleDelete(operateur.idOperateur)}
                    >
                      🗑️ Supprimer
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default OperateurList;

