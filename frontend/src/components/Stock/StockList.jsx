import React, { useState, useEffect } from 'react';
import stockService from '../../services/stockService';
import './StockList.css';

const StockList = () => {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingStock, setEditingStock] = useState(null);
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
      const data = await stockService.getAllStocks();
      setStocks(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch stocks: ' + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this stock?')) {
      try {
        await stockService.deleteStock(id);
        fetchStocks();
      } catch (err) {
        alert('Failed to delete stock: ' + err.message);
      }
    }
  };

  const handleEdit = (stock) => {
    setEditingStock(stock);
    setFormData({
      libelleStock: stock.libelleStock || '',
      qte: stock.qte || '',
      qteMin: stock.qteMin || '',
    });
    setShowForm(true);
  };

  const handleAdd = () => {
    setEditingStock(null);
    setFormData({
      libelleStock: '',
      qte: '',
      qteMin: '',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingStock) {
        await stockService.updateStock({
          ...editingStock,
          ...formData,
          qte: parseInt(formData.qte),
          qteMin: parseInt(formData.qteMin),
        });
      } else {
        await stockService.addStock({
          ...formData,
          qte: parseInt(formData.qte),
          qteMin: parseInt(formData.qteMin),
        });
      }
      setShowForm(false);
      fetchStocks();
    } catch (err) {
      alert('Failed to save stock: ' + err.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  if (loading) return <div className="loading">Loading stocks...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="stock-container">
      <div className="header">
        <h1>📦 Gestion des Stocks</h1>
        <button className="btn btn-primary" onClick={handleAdd}>
          + Ajouter Stock
        </button>
      </div>

      {showForm && (
        <div className="modal">
          <div className="modal-content">
            <h2>{editingStock ? 'Modifier Stock' : 'Nouveau Stock'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Libellé:</label>
                <input
                  type="text"
                  name="libelleStock"
                  value={formData.libelleStock}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Quantité:</label>
                <input
                  type="number"
                  name="qte"
                  value={formData.qte}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group">
                <label>Quantité Minimum:</label>
                <input
                  type="number"
                  name="qteMin"
                  value={formData.qteMin}
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

      <div className="stock-list">
        {stocks.length === 0 ? (
          <p className="no-data">Aucun stock disponible</p>
        ) : (
          <table className="stock-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Libellé</th>
                <th>Quantité</th>
                <th>Quantité Min</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {stocks.map((stock) => (
                <tr key={stock.idStock}>
                  <td>{stock.idStock}</td>
                  <td>{stock.libelleStock}</td>
                  <td>{stock.qte}</td>
                  <td>{stock.qteMin}</td>
                  <td className="actions">
                    <button
                      className="btn btn-edit"
                      onClick={() => handleEdit(stock)}
                    >
                      ✏️ Modifier
                    </button>
                    <button
                      className="btn btn-delete"
                      onClick={() => handleDelete(stock.idStock)}
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

export default StockList;

