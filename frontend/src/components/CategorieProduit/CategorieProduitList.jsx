import React, { useState, useEffect } from 'react';
import categorieProduitService from '../../services/categorieProduitService';
import './CategorieProduitList.css';

const CategorieProduitList = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingCategorie, setEditingCategorie] = useState(null);
  const [formData, setFormData] = useState({
    codeCategorie: '',
    libelleCategorie: '',
  });

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    try {
      setLoading(true);
      const data = await categorieProduitService.getAllCategorieProduits();
      setCategories(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch categories: ' + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this category?')) {
      try {
        await categorieProduitService.deleteCategorieProduit(id);
        fetchCategories();
      } catch (err) {
        alert('Failed to delete category: ' + err.message);
      }
    }
  };

  const handleEdit = (categorie) => {
    setEditingCategorie(categorie);
    setFormData({
      codeCategorie: categorie.codeCategorie || '',
      libelleCategorie: categorie.libelleCategorie || '',
    });
    setShowForm(true);
  };

  const handleAdd = () => {
    setEditingCategorie(null);
    setFormData({
      codeCategorie: '',
      libelleCategorie: '',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingCategorie) {
        await categorieProduitService.updateCategorieProduit({
          ...editingCategorie,
          ...formData,
        });
      } else {
        await categorieProduitService.addCategorieProduit(formData);
      }
      setShowForm(false);
      fetchCategories();
    } catch (err) {
      alert('Failed to save category: ' + err.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  if (loading) return <div className="loading">Loading categories...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="categorie-container">
      <div className="header">
        <h1>🏷️ Gestion des Catégories Produit</h1>
        <button className="btn btn-primary" onClick={handleAdd}>
          + Ajouter Catégorie
        </button>
      </div>

      {showForm && (
        <div className="modal">
          <div className="modal-content">
            <h2>{editingCategorie ? 'Modifier Catégorie' : 'Nouvelle Catégorie'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Code Catégorie:</label>
                <input
                  type="text"
                  name="codeCategorie"
                  value={formData.codeCategorie}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Libellé Catégorie:</label>
                <input
                  type="text"
                  name="libelleCategorie"
                  value={formData.libelleCategorie}
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

      <div className="categorie-list">
        {categories.length === 0 ? (
          <p className="no-data">Aucune catégorie disponible</p>
        ) : (
          <table className="categorie-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Code</th>
                <th>Libellé</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((categorie) => (
                <tr key={categorie.idCategorieProduit}>
                  <td>{categorie.idCategorieProduit}</td>
                  <td>{categorie.codeCategorie}</td>
                  <td>{categorie.libelleCategorie}</td>
                  <td className="actions">
                    <button
                      className="btn btn-edit"
                      onClick={() => handleEdit(categorie)}
                    >
                      ✏️ Modifier
                    </button>
                    <button
                      className="btn btn-delete"
                      onClick={() => handleDelete(categorie.idCategorieProduit)}
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

export default CategorieProduitList;

