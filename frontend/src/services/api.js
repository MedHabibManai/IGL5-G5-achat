import axios from 'axios';

// Get API base URL from environment variable or use default
const API_BASE_URL = process.env.REACT_APP_API_URL || '/SpringMVC';

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// API endpoints
export const apiService = {
  // Health check
  getHealth: () => api.get('/actuator/health'),

  // Produits
  getAllProduits: () => api.get('/produit/retrieve-all-produits'),
  getProduitById: (id) => api.get(`/produit/retrieve-produit/${id}`),
  addProduit: (produit) => api.post('/produit/add-produit', produit),
  updateProduit: (produit) => api.put('/produit/modify-produit', produit),
  deleteProduit: (id) => api.delete(`/produit/remove-produit/${id}`),

  // Stocks
  getAllStocks: () => api.get('/stock/retrieve-all-stocks'),
  getStockById: (id) => api.get(`/stock/retrieve-stock/${id}`),
  addStock: (stock) => api.post('/stock/add-stock', stock),
  updateStock: (stock) => api.put('/stock/modify-stock', stock),
  deleteStock: (id) => api.delete(`/stock/remove-stock/${id}`),

  // Fournisseurs
  getAllFournisseurs: () => api.get('/fournisseur/retrieve-all-fournisseurs'),
  getFournisseurById: (id) => api.get(`/fournisseur/retrieve-fournisseur/${id}`),
  addFournisseur: (fournisseur) => api.post('/fournisseur/add-fournisseur', fournisseur),
  updateFournisseur: (fournisseur) => api.put('/fournisseur/modify-fournisseur', fournisseur),
  deleteFournisseur: (id) => api.delete(`/fournisseur/remove-fournisseur/${id}`),

  // Factures
  getAllFactures: () => api.get('/facture/retrieve-all-factures'),
  getFactureById: (id) => api.get(`/facture/retrieve-facture/${id}`),
  addFacture: (facture) => api.post('/facture/add-facture', facture),
  cancelFacture: (id) => api.put(`/facture/cancel-facture/${id}`),

  // Operateurs
  getAllOperateurs: () => api.get('/operateur/retrieve-all-operateurs'),
  getOperateurById: (id) => api.get(`/operateur/retrieve-operateur/${id}`),
  addOperateur: (operateur) => api.post('/operateur/add-operateur', operateur),
  updateOperateur: (operateur) => api.put('/operateur/modify-operateur', operateur),
  deleteOperateur: (id) => api.delete(`/operateur/remove-operateur/${id}`),

  // Reglements
  getAllReglements: () => api.get('/reglement/retrieve-all-reglements'),
  getReglementById: (id) => api.get(`/reglement/retrieve-reglement/${id}`),
  addReglement: (reglement) => api.post('/reglement/add-reglement', reglement),
};

export default api;

