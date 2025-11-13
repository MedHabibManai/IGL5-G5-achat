import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

function Stocks() {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchStocks();
  }, []);

  const fetchStocks = async () => {
    try {
      setLoading(true);
      const response = await apiService.getAllStocks();
      setStocks(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch stocks: ' + err.message);
      console.error('Error fetching stocks:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading stocks...</div>;
  if (error) return <div className="card"><p style={{ color: 'red' }}>{error}</p></div>;

  return (
    <div className="stocks">
      <div className="card">
        <h1>📊 Stock Management</h1>
        <p style={{ color: '#666', marginTop: '0.5rem' }}>
          Total Stocks: {stocks.length}
        </p>
        
        <div className="table-container">
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
                </tr>
              </thead>
              <tbody>
                {stocks.map((stock) => (
                  <tr key={stock.idStock}>
                    <td>{stock.idStock}</td>
                    <td>{stock.libelleStock}</td>
                    <td>{stock.qte || 0}</td>
                    <td>{stock.qteMin || 0}</td>
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

