import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Stocks() {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    libelleStock: '',
    qte: '',
    qteMin: '',
  });

  useEffect(() => {
    fetchStocks();
  }, []);

  const fetchStocks = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllStocks();
      const data = Array.isArray(response.data) ? response.data : [];
      setStocks(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch stocks: ' + err.message);
      console.error('Error fetching stocks:', err);
      setStocks([]);
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFormData({ libelleStock: '', qte: '', qteMin: '' });
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
        libelleStock: formData.libelleStock,
        qte: formData.qte === '' ? 0 : Number(formData.qte),
        qteMin: formData.qteMin === '' ? 0 : Number(formData.qteMin),
      };

      if (editingId) {
        await apiService.updateStock({ ...payload, idStock: editingId });
      } else {
        await apiService.addStock(payload);
      }

      resetForm();
      fetchStocks();
    } catch (err) {
      setError('Failed to save stock: ' + err.message);
      console.error('Error saving stock:', err);
    }
  };

  const handleEdit = (stock) => {
    setEditingId(stock.idStock);
    setFormData({
      libelleStock: stock.libelleStock || '',
      qte: stock.qte != null ? String(stock.qte) : '',
      qteMin: stock.qteMin != null ? String(stock.qteMin) : '',
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this stock?')) return;
    try {
      await apiService.deleteStock(id);
      fetchStocks();
    } catch (err) {
      setError('Failed to delete stock: ' + err.message);
      console.error('Error deleting stock:', err);
    }
  };

  const handleCancel = () => {
    resetForm();
  };

  if (loading) return <div className="loading">Loading stocks...</div>;

  return (
    <div className="stocks">
      <div className="card">
        <h1>
          📊 Stock Management
        </h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Stocks: {stocks.length}
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
            {showForm ? 'Close form' : 'Add new stock'}
          </button>
        </div>

        {showForm && (
          <form onSubmit={handleSubmit} style={{ marginBottom: '2rem' }}>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 200px' }}>
                <label>Libellé du stock</label>
                <input
                  type="text"
                  name="libelleStock"
                  value={formData.libelleStock}
                  onChange={handleInputChange}
                  required
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 120px' }}>
                <label>Quantité</label>
                <input
                  type="number"
                  name="qte"
                  value={formData.qte}
                  onChange={handleInputChange}
                  min="0"
                  className="form-input"
                />
              </div>
              <div style={{ flex: '1 1 120px' }}>
                <label>Quantité minimale</label>
                <input
                  type="number"
                  name="qteMin"
                  value={formData.qteMin}
                  onChange={handleInputChange}
                  min="0"
                  className="form-input"
                />
              </div>
            </div>
            <div style={{ marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update stock' : 'Create stock'}
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
          {stocks.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
              No stocks found. Add your first stock to get started!
            </p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Libellé</th>
                  <th>Quantité</th>
                  <th>Quantité Min</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {stocks.map((stock) => (
                  <tr key={stock.idStock}>
                    <td>{stock.idStock}</td>
                    <td>{stock.libelleStock}</td>
                    <td>{stock.qte || 0}</td>
                    <td>{stock.qteMin || 0}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-secondary"
                        onClick={() => handleEdit(stock)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-primary"
                        style={{ background: '#e53e3e', marginLeft: '0.5rem' }}
                        onClick={() => handleDelete(stock.idStock)}
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

export default Stocks;

