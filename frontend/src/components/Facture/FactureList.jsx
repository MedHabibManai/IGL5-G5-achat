import React, { useState, useEffect } from 'react';
import factureService from '../../services/factureService';
import './FactureList.css';

const FactureList = () => {
  const [factures, setFactures] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    montantRemise: '',
    montantFacture: '',
    dateCreationFacture: '',
    archivee: false,
  });
  const [showRecouvrement, setShowRecouvrement] = useState(false);
  const [recouvrementData, setRecouvrementData] = useState({ startDate: '', endDate: '' });
  const [recouvrementResult, setRecouvrementResult] = useState(null);

  useEffect(() => {
    fetchFactures();
  }, []);

  const fetchFactures = async () => {
    try {
      setLoading(true);
      const data = await factureService.getAllFactures();
      setFactures(data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch factures: ' + err.message);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to cancel this facture?')) {
      try {
        await factureService.cancelFacture(id);
        fetchFactures();
      } catch (err) {
        alert('Failed to cancel facture: ' + err.message);
      }
    }
  };

  const handleAdd = () => {
    setFormData({
      montantRemise: '',
      montantFacture: '',
      dateCreationFacture: '',
      archivee: false,
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await factureService.addFacture({
        ...formData,
        montantRemise: parseFloat(formData.montantRemise),
        montantFacture: parseFloat(formData.montantFacture),
      });
      setShowForm(false);
      fetchFactures();
    } catch (err) {
      alert('Failed to save facture: ' + err.message);
    }
  };

  const handleAssignOperateur = async (factureId) => {
    const operateurId = prompt('Enter Operateur ID:');
    if (operateurId) {
      try {
        await factureService.assignOperateurToFacture(parseInt(operateurId), factureId);
        alert('Operateur assigned successfully!');
        fetchFactures();
      } catch (err) {
        alert('Failed to assign operateur: ' + err.message);
      }
    }
  };

  const handleCalculateRecouvrement = async () => {
    try {
      const result = await factureService.getPourcentageRecouvrement(
        recouvrementData.startDate,
        recouvrementData.endDate
      );
      setRecouvrementResult(result);
    } catch (err) {
      alert('Failed to calculate recouvrement: ' + err.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  if (loading) return <div className="loading">Loading factures...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="facture-container">
      <div className="header">
        <h1>🧾 Gestion des Factures</h1>
        <div>
          <button className="btn btn-primary" onClick={handleAdd}>
            + Ajouter Facture
          </button>
          <button className="btn btn-info" onClick={() => setShowRecouvrement(!showRecouvrement)}>
            📊 Recouvrement
          </button>
        </div>
      </div>

      {showRecouvrement && (
        <div className="recouvrement-panel">
          <h3>Calculer Pourcentage de Recouvrement</h3>
          <div className="form-group">
            <label>Date Début:</label>
            <input
              type="date"
              value={recouvrementData.startDate}
              onChange={(e) => setRecouvrementData({ ...recouvrementData, startDate: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Date Fin:</label>
            <input
              type="date"
              value={recouvrementData.endDate}
              onChange={(e) => setRecouvrementData({ ...recouvrementData, endDate: e.target.value })}
            />
          </div>
          <button className="btn btn-primary" onClick={handleCalculateRecouvrement}>
            Calculer
          </button>
          {recouvrementResult !== null && (
            <div className="result">
              <strong>Pourcentage de Recouvrement: {recouvrementResult}%</strong>
            </div>
          )}
        </div>
      )}

      {showForm && (
        <div className="modal">
          <div className="modal-content">
            <h2>Nouvelle Facture</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Montant Remise:</label>
                <input
                  type="number"
                  name="montantRemise"
                  value={formData.montantRemise}
                  onChange={handleInputChange}
                  step="0.01"
                  required
                />
              </div>
              <div className="form-group">
                <label>Montant Facture:</label>
                <input
                  type="number"
                  name="montantFacture"
                  value={formData.montantFacture}
                  onChange={handleInputChange}
                  step="0.01"
                  required
                />
              </div>
              <div className="form-group">
                <label>Date Création:</label>
                <input
                  type="date"
                  name="dateCreationFacture"
                  value={formData.dateCreationFacture}
                  onChange={handleInputChange}
                />
              </div>
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    name="archivee"
                    checked={formData.archivee}
                    onChange={handleInputChange}
                  />
                  Archivée
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

      <div className="facture-list">
        {factures.length === 0 ? (
          <p className="no-data">Aucune facture disponible</p>
        ) : (
          <table className="facture-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Montant Remise</th>
                <th>Montant Facture</th>
                <th>Date Création</th>
                <th>Archivée</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {factures.map((facture) => (
                <tr key={facture.idFacture}>
                  <td>{facture.idFacture}</td>
                  <td>{facture.montantRemise} DT</td>
                  <td>{facture.montantFacture} DT</td>
                  <td>{facture.dateCreationFacture || 'N/A'}</td>
                  <td>{facture.archivee ? 'Oui' : 'Non'}</td>
                  <td className="actions">
                    <button
                      className="btn btn-edit"
                      onClick={() => handleAssignOperateur(facture.idFacture)}
                    >
                      👤 Assigner Opérateur
                    </button>
                    <button
                      className="btn btn-delete"
                      onClick={() => handleDelete(facture.idFacture)}
                    >
                      ❌ Annuler
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

export default FactureList;

