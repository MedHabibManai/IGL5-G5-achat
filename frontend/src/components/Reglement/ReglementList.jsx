import React, { useState, useEffect } from 'react';
import reglementService from '../../services/reglementService';
import './ReglementList.css';

const ReglementList = () => {
  const [reglements, setReglements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    montantPaye: '',
    montantRestant: '',
    payee: false,
    dateReglement: '',
  });
  const [showChiffreAffaire, setShowChiffreAffaire] = useState(false);
  const [chiffreAffaireData, setChiffreAffaireData] = useState({ startDate: '', endDate: '' });
  const [chiffreAffaireResult, setChiffreAffaireResult] = useState(null);

  useEffect(() => {
    fetchReglements();
  }, []);

  const fetchReglements = async () => {
    try {
      setLoading(true);
      const data = await reglementService.getAllReglements();
      setReglements(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch reglements: ' + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setFormData({
      montantPaye: '',
      montantRestant: '',
      payee: false,
      dateReglement: '',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await reglementService.addReglement({
        ...formData,
        montantPaye: parseFloat(formData.montantPaye),
        montantRestant: parseFloat(formData.montantRestant),
      });
      setShowForm(false);
      fetchReglements();
    } catch (err) {
      alert('Failed to save reglement: ' + err.message);
    }
  };

  const handleViewByFacture = async (factureId) => {
    try {
      const data = await reglementService.getReglementsByFacture(factureId);
      alert(`Reglements for Facture ${factureId}: ${data.length} found`);
    } catch (err) {
      alert('Failed to fetch reglements: ' + err.message);
    }
  };

  const handleCalculateChiffreAffaire = async () => {
    try {
      const result = await reglementService.getChiffreAffaireEntreDeuxDate(
        chiffreAffaireData.startDate,
        chiffreAffaireData.endDate
      );
      setChiffreAffaireResult(result);
    } catch (err) {
      alert('Failed to calculate chiffre affaire: ' + err.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  if (loading) return <div className="loading">Loading reglements...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="reglement-container">
      <div className="header">
        <h1>💰 Gestion des Règlements</h1>
        <div>
          <button className="btn btn-primary" onClick={handleAdd}>
            + Ajouter Règlement
          </button>
          <button className="btn btn-info" onClick={() => setShowChiffreAffaire(!showChiffreAffaire)}>
            📊 Chiffre d'Affaire
          </button>
        </div>
      </div>

      {showChiffreAffaire && (
        <div className="chiffre-affaire-panel">
          <h3>Calculer Chiffre d'Affaire</h3>
          <div className="form-group">
            <label>Date Début:</label>
            <input
              type="date"
              value={chiffreAffaireData.startDate}
              onChange={(e) => setChiffreAffaireData({ ...chiffreAffaireData, startDate: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Date Fin:</label>
            <input
              type="date"
              value={chiffreAffaireData.endDate}
              onChange={(e) => setChiffreAffaireData({ ...chiffreAffaireData, endDate: e.target.value })}
            />
          </div>
          <button className="btn btn-primary" onClick={handleCalculateChiffreAffaire}>
            Calculer
          </button>
          {chiffreAffaireResult !== null && (
            <div className="result">
              <strong>Chiffre d'Affaire: {chiffreAffaireResult} DT</strong>
            </div>
          )}
        </div>
      )}

      {showForm && (
        <div className="modal">
          <div className="modal-content">
            <h2>Nouveau Règlement</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Montant Payé:</label>
                <input
                  type="number"
                  name="montantPaye"
                  value={formData.montantPaye}
                  onChange={handleInputChange}
                  step="0.01"
                  required
                />
              </div>
              <div className="form-group">
                <label>Montant Restant:</label>
                <input
                  type="number"
                  name="montantRestant"
                  value={formData.montantRestant}
                  onChange={handleInputChange}
                  step="0.01"
                  required
                />
              </div>
              <div className="form-group">
                <label>Date Règlement:</label>
                <input
                  type="date"
                  name="dateReglement"
                  value={formData.dateReglement}
                  onChange={handleInputChange}
                />
              </div>
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    name="payee"
                    checked={formData.payee}
                    onChange={handleInputChange}
                  />
                  Payée
                </label>
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

      <div className="reglement-list">
        {reglements.length === 0 ? (
          <p className="no-data">Aucun règlement disponible</p>
        ) : (
          <table className="reglement-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Montant Payé</th>
                <th>Montant Restant</th>
                <th>Date Règlement</th>
                <th>Payée</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {reglements.map((reglement) => (
                <tr key={reglement.idReglement}>
                  <td>{reglement.idReglement}</td>
                  <td>{reglement.montantPaye} DT</td>
                  <td>{reglement.montantRestant} DT</td>
                  <td>{reglement.dateReglement || 'N/A'}</td>
                  <td>{reglement.payee ? 'Oui' : 'Non'}</td>
                  <td className="actions">
                    {reglement.facture && (
                      <button
                        className="btn btn-info"
                        onClick={() => handleViewByFacture(reglement.facture.idFacture)}
                      >
                        📋 Voir Facture
                      </button>
                    )}
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

export default ReglementList;

