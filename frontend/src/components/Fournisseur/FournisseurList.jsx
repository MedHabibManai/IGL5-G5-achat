import React, { useState, useEffect } from 'react';
import fournisseurService from '../../services/fournisseurService';
import secteurActiviteService from '../../services/secteurActiviteService';
import './FournisseurList.css';

const FournisseurList = () => {
  const [fournisseurs, setFournisseurs] = useState([]);
  const [secteurActivites, setSecteurActivites] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingFournisseur, setEditingFournisseur] = useState(null);
  const [formData, setFormData] = useState({
    code: '',
    libelle: '',
    categorieFournisseur: 'ORDINAIRE',
  });

  useEffect(() => {
    fetchFournisseurs();
    fetchSecteurActivites();
  }, []);

  const fetchFournisseurs = async () => {
    try {
      setLoading(true);
      const data = await fournisseurService.getAllFournisseurs();
      setFournisseurs(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch fournisseurs: ' + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchSecteurActivites = async () => {
    try {
      const data = await secteurActiviteService.getAllSecteurActivites();
      setSecteurActivites(data);
    } catch (err) {
      console.error('Failed to fetch secteur activites:', err);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this fournisseur?')) {
      try {
        await fournisseurService.deleteFournisseur(id);
        fetchFournisseurs();
      } catch (err) {
        alert('Failed to delete fournisseur: ' + err.message);
      }
    }
  };

  const handleEdit = (fournisseur) => {
    setEditingFournisseur(fournisseur);
    setFormData({
      code: fournisseur.code || '',
      libelle: fournisseur.libelle || '',
      categorieFournisseur: fournisseur.categorieFournisseur || 'ORDINAIRE',
    });
    setShowForm(true);
  };

  const handleAdd = () => {
    setEditingFournisseur(null);
    setFormData({
      code: '',
      libelle: '',
      categorieFournisseur: 'ORDINAIRE',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingFournisseur) {
        await fournisseurService.updateFournisseur({
          ...editingFournisseur,
          ...formData,
        });
      } else {
        await fournisseurService.addFournisseur(formData);
      }
      setShowForm(false);
      fetchFournisseurs();
    } catch (err) {
      alert('Failed to save fournisseur: ' + err.message);
    }
  };

  const handleAssignSecteur = async (fournisseurId) => {
    const secteurId = prompt('Enter Secteur Activite ID:');
    if (secteurId) {
      try {
        await fournisseurService.assignSecteurActiviteToFournisseur(parseInt(secteurId), fournisseurId);
        alert('Secteur Activite assigned successfully!');
        fetchFournisseurs();
      } catch (err) {
        alert('Failed to assign secteur activite: ' + err.message);
      }
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  if (loading) return <div className="loading">Loading fournisseurs...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="fournisseur-container">
      <div className="header">
        <h1>🏢 Gestion des Fournisseurs</h1>
        <button className="btn btn-primary" onClick={handleAdd}>
          + Ajouter Fournisseur
        </button>
      </div>

      {showForm && (
        <div className="modal">
          <div className="modal-content">
            <h2>{editingFournisseur ? 'Modifier Fournisseur' : 'Nouveau Fournisseur'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Code:</label>
                <input
                  type="text"
                  name="code"
                  value={formData.code}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Libellé:</label>
                <input
                  type="text"
                  name="libelle"
                  value={formData.libelle}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Catégorie:</label>
                <select
                  name="categorieFournisseur"
                  value={formData.categorieFournisseur}
                  onChange={handleInputChange}
                >
                  <option value="ORDINAIRE">ORDINAIRE</option>
                  <option value="CONVENTIONNE">CONVENTIONNE</option>
                </select>
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

      <div className="fournisseur-list">
        {fournisseurs.length === 0 ? (
          <p className="no-data">Aucun fournisseur disponible</p>
        ) : (
          <table className="fournisseur-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Code</th>
                <th>Libellé</th>
                <th>Catégorie</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {fournisseurs.map((fournisseur) => (
                <tr key={fournisseur.idFournisseur}>
                  <td>{fournisseur.idFournisseur}</td>
                  <td>{fournisseur.code}</td>
                  <td>{fournisseur.libelle}</td>
                  <td>{fournisseur.categorieFournisseur || 'N/A'}</td>
                  <td className="actions">
                    <button
                      className="btn btn-edit"
                      onClick={() => handleEdit(fournisseur)}
                    >
                      ✏️ Modifier
                    </button>
                    <button
                      className="btn btn-info"
                      onClick={() => handleAssignSecteur(fournisseur.idFournisseur)}
                    >
                      🏷️ Assigner Secteur
                    </button>
                    <button
                      className="btn btn-delete"
                      onClick={() => handleDelete(fournisseur.idFournisseur)}
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

export default FournisseurList;

