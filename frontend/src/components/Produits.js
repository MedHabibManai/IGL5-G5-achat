import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Produits() {
  const [produits, setProduits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    codeProduit: '',
    libelleProduit: '',
    prix: ''
  });

  useEffect(() => {
    fetchProduits();
  }, []);

  const fetchProduits = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllProduits();
      // Ensure data is always an array
      const data = Array.isArray(response.data) ? response.data : [];
      setProduits(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch produits: ' + err.message);
      console.error('Error fetching produits:', err);
      setProduits([]);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        // Include the ID in the object for update
        await apiService.updateProduit({ ...formData, idProduit: editingId });
      } else {
        await apiService.addProduit(formData);
      }
      setShowForm(false);
      setEditingId(null);
      setFormData({ codeProduit: '', libelleProduit: '', prix: '' });
      fetchProduits();
    } catch (err) {
      setError('Failed to save produit: ' + err.message);
    }
  };

  const handleEdit = (produit) => {
    setEditingId(produit.idProduit);
    setFormData({
      codeProduit: produit.codeProduit,
      libelleProduit: produit.libelleProduit,
      prix: produit.prix
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this product?')) {
      try {
        await apiService.deleteProduit(id);
        fetchProduits();
      } catch (err) {
        setError('Failed to delete produit: ' + err.message);
      }
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingId(null);
    setFormData({ codeProduit: '', libelleProduit: '', prix: '' });
  };

  if (loading) return <div className="loading">Loading produits...</div>;
  if (error) return <div className="card"><p style={{ color: 'red' }}>{error}</p></div>;

  return (
    <div className="produits">
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <div>
            <h1>📦 Produits Management</h1>
            <p style={{ color: '#666', marginTop: '0.5rem' }}>
              Total Products: {produits.length}
            </p>
          </div>
          <button
            className="btn btn-primary"
            onClick={() => setShowForm(!showForm)}
            style={{ height: 'fit-content' }}
          >
            {showForm ? '✖ Cancel' : '➕ Add Product'}
          </button>
        </div>

        {showForm && (
          <div style={{ background: '#f0f9ff', padding: '1.5rem', borderRadius: '8px', marginBottom: '1.5rem' }}>
            <h3>{editingId ? 'Edit Product' : 'Add New Product'}</h3>
            <form onSubmit={handleSubmit} style={{ marginTop: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                    Product Code *
                  </label>
                  <input
                    type="text"
                    name="codeProduit"
                    value={formData.codeProduit}
                    onChange={handleInputChange}
                    required
                    style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #ddd' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                    Product Name *
                  </label>
                  <input
                    type="text"
                    name="libelleProduit"
                    value={formData.libelleProduit}
                    onChange={handleInputChange}
                    required
                    style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #ddd' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                    Price (TND) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    name="prix"
                    value={formData.prix}
                    onChange={handleInputChange}
                    required
                    style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #ddd' }}
                  />
                </div>
              </div>
              <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
                <button type="submit" className="btn btn-primary">
                  {editingId ? '💾 Update' : '➕ Create'}
                </button>
                <button type="button" className="btn btn-secondary" onClick={handleCancel}>
                  ✖ Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="table-container">
          {produits.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
              No products found. Click "Add Product" to get started!
            </p>
          ) : (
            <table>
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
                    <td>{produit.codeProduit}</td>
                    <td>{produit.libelleProduit}</td>
                    <td>{produit.prix ? `${produit.prix} TND` : 'N/A'}</td>
                    <td>{produit.dateCreation ? new Date(produit.dateCreation).toLocaleDateString() : 'N/A'}</td>
                    <td>
                      <button
                        className="btn btn-secondary"
                        onClick={() => handleEdit(produit)}
                        style={{ marginRight: '0.5rem', padding: '0.25rem 0.75rem', fontSize: '0.875rem' }}
                      >
                        ✏️ Edit
                      </button>
                      <button
                        className="btn"
                        onClick={() => handleDelete(produit.idProduit)}
                        style={{ padding: '0.25rem 0.75rem', fontSize: '0.875rem', background: '#ef4444', color: 'white' }}
                      >
                        🗑️ Delete
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

export default Produits;

