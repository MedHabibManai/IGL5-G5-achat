import apiClient from './apiClient';

const REGLEMENT_API = '/reglement';

const reglementService = {
  // Get all reglements
  getAllReglements: async () => {
    const response = await apiClient.get(`${REGLEMENT_API}/retrieve-all-reglements`);
    return response.data;
  },

  // Get reglement by ID
  getReglementById: async (id) => {
    const response = await apiClient.get(`${REGLEMENT_API}/retrieve-reglement/${id}`);
    return response.data;
  },

  // Add new reglement
  addReglement: async (reglement) => {
    const response = await apiClient.post(`${REGLEMENT_API}/add-reglement`, reglement);
    return response.data;
  },

  // Get reglements by facture
  getReglementsByFacture: async (factureId) => {
    const response = await apiClient.get(`${REGLEMENT_API}/retrieveReglementByFacture/${factureId}`);
    return response.data;
  },

  // Get chiffre affaire entre deux dates
  getChiffreAffaireEntreDeuxDate: async (startDate, endDate) => {
    const response = await apiClient.get(`${REGLEMENT_API}/getChiffreAffaireEntreDeuxDate/${startDate}/${endDate}`);
    return response.data;
  },
};

export default reglementService;

