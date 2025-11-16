import React, { useState, useEffect } from 'react';
import produitService from '../../services/produitService';
import './ProduitList.css';

const ProduitList = () => {
  const [produits, setProduits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingProduit, setEditingProduit] = useState(null);
  const [formData, setFormData] = useState({
    codeProduit: '',
    libelleProduit: '',
    prix: '',
    dateCreation: '',
    dateDerniereModification: '',
  });

  useEffect(() => {
    fetchProduits();
  }, []);

  const fetchProduits = async () => {
    try {
      setLoading(true);
      const data = await produitService.getAllProduits();
      setProduits(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch products: ' + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this product?')) {
      try {
        await produitService.deleteProduit(id);
        fetchProduits(); // Refresh list
      } catch (err) {
        alert('Failed to delete product: ' + err.message);
      }
    }
  };

  const handleEdit = (produit) => {
    setEditingProduit(produit);
    setFormData({
      codeProduit: produit.codeProduit || '',
      libelleProduit: produit.libelleProduit || '',
      prix: produit.prix || '',
      dateCreation: produit.dateCreation || '',
      dateDerniereModification: produit.dateDerniereModification || '',
    });
    setShowForm(true);
  };

  const handleAdd = () => {
    setEditingProduit(null);
    setFormData({
      codeProduit: '',
      libelleProduit: '',
      prix: '',
      dateCreation: '',
      dateDerniereModification: '',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingProduit) {
        // Update existing
        await produitService.updateProduit({
          ...editingProduit,
          ...formData,
          prix: parseFloat(formData.prix),
        });
      } else {
        // Add new
        await produitService.addProduit({
          ...formData,
          prix: parseFloat(formData.prix),
        });
      }
      setShowForm(false);
      fetchProduits(); // Refresh list
    } catch (err) {
      alert('Failed to save product: ' + err.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  if (loading) return <div className="loading">Loading products...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="produit-container">
      <div className="header">
        <h1>🛍️ Gestion des Produits</h1>
        <button className="btn btn-primary" onClick={handleAdd}>
          + Ajouter Produit
        </button>
      </div>

      {showForm && (
        <div className="modal">
          <div className="modal-content">
            <h2>{editingProduit ? 'Modifier Produit' : 'Nouveau Produit'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Code Produit:</label>
                <input
                  type="text"
                  name="codeProduit"
                  value={formData.codeProduit}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Libellé:</label>
                <input
                  type="text"
                  name="libelleProduit"
                  value={formData.libelleProduit}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Prix:</label>
                <input
                  type="number"
                  name="prix"
                  value={formData.prix}
                  onChange={handleInputChange}
                  step="0.01"
                  required
                />
              </div>
              <div className="form-group">
                <label>Date de Création:</label>
                <input
                  type="date"
                  name="dateCreation"
                  value={formData.dateCreation}
                  onChange={handleInputChange}
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

      <div className="produit-list">
        {produits.length === 0 ? (
          <p className="no-data">Aucun produit disponible</p>
        ) : (
          <table className="produit-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Code</th>
                <th>Libellé</th>
                <th>Prix</th>
                <th>Date Création</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {produits.map((produit) => (
                <tr key={produit.idProduit}>
                  <td>{produit.idProduit}</td>
                  <td>{produit.codeProduit || 'N/A'}</td>
                  <td>{produit.libelleProduit}</td>
                  <td>{produit.prix} DT</td>
                  <td>{produit.dateCreation || 'N/A'}</td>
                  <td className="actions">
                    <button
                      className="btn btn-edit"
                      onClick={() => handleEdit(produit)}
                    >
                      ✏️ Modifier
                    </button>
                    <button
                      className="btn btn-delete"
                      onClick={() => handleDelete(produit.idProduit)}
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

export default ProduitList;
