import React, { useState, useEffect } from 'react';
import secteurActiviteService from '../../services/secteurActiviteService';
import './SecteurActiviteList.css';

const SecteurActiviteList = () => {
  const [secteurActivites, setSecteurActivites] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingSecteur, setEditingSecteur] = useState(null);
  const [formData, setFormData] = useState({
    codeSecteurActivite: '',
    libelleSecteurActivite: '',
  });

  useEffect(() => {
    fetchSecteurActivites();
  }, []);

  const fetchSecteurActivites = async () => {
    try {
      setLoading(true);
      const data = await secteurActiviteService.getAllSecteurActivites();
      setSecteurActivites(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch secteur activites: ' + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this secteur activite?')) {
      try {
        await secteurActiviteService.deleteSecteurActivite(id);
        fetchSecteurActivites();
      } catch (err) {
        alert('Failed to delete secteur activite: ' + err.message);
      }
    }
  };

  const handleEdit = (secteur) => {
    setEditingSecteur(secteur);
    setFormData({
      codeSecteurActivite: secteur.codeSecteurActivite || '',
      libelleSecteurActivite: secteur.libelleSecteurActivite || '',
    });
    setShowForm(true);
  };

  const handleAdd = () => {
    setEditingSecteur(null);
    setFormData({
      codeSecteurActivite: '',
      libelleSecteurActivite: '',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingSecteur) {
        await secteurActiviteService.updateSecteurActivite({
          ...editingSecteur,
          ...formData,
        });
      } else {
        await secteurActiviteService.addSecteurActivite(formData);
      }
      setShowForm(false);
      fetchSecteurActivites();
    } catch (err) {
      alert('Failed to save secteur activite: ' + err.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  if (loading) return <div className="loading">Loading secteur activites...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="secteur-container">
      <div className="header">
        <h1>🏭 Gestion des Secteurs d'Activité</h1>
        <button className="btn btn-primary" onClick={handleAdd}>
          + Ajouter Secteur
        </button>
      </div>

      {showForm && (
        <div className="modal">
          <div className="modal-content">
            <h2>{editingSecteur ? 'Modifier Secteur' : 'Nouveau Secteur'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Code Secteur:</label>
                <input
                  type="text"
                  name="codeSecteurActivite"
                  value={formData.codeSecteurActivite}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Libellé Secteur:</label>
                <input
                  type="text"
                  name="libelleSecteurActivite"
                  value={formData.libelleSecteurActivite}
                  onChange={handleInputChange}
                  required
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

      <div className="secteur-list">
        {secteurActivites.length === 0 ? (
          <p className="no-data">Aucun secteur d'activité disponible</p>
        ) : (
          <table className="secteur-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Code</th>
                <th>Libellé</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {secteurActivites.map((secteur) => (
                <tr key={secteur.idSecteurActivite}>
                  <td>{secteur.idSecteurActivite}</td>
                  <td>{secteur.codeSecteurActivite}</td>
                  <td>{secteur.libelleSecteurActivite}</td>
                  <td className="actions">
                    <button
                      className="btn btn-edit"
                      onClick={() => handleEdit(secteur)}
                    >
                      ✏️ Modifier
                    </button>
                    <button
                      className="btn btn-delete"
                      onClick={() => handleDelete(secteur.idSecteurActivite)}
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

export default SecteurActiviteList;

