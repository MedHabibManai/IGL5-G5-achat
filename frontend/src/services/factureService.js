import apiClient from './apiClient';

const FACTURE_API = '/facture';

const factureService = {
  // Get all factures
  getAllFactures: async () => {
    const response = await apiClient.get(`${FACTURE_API}/retrieve-all-factures`);
    return response.data;
  },

  // Get facture by ID
  getFactureById: async (id) => {
    const response = await apiClient.get(`${FACTURE_API}/retrieve-facture/${id}`);
    return response.data;
  },

  // Add new facture
  addFacture: async (facture) => {
    const response = await apiClient.post(`${FACTURE_API}/add-facture`, facture);
    return response.data;
  },

  // Cancel facture
  cancelFacture: async (id) => {
    await apiClient.put(`${FACTURE_API}/cancel-facture/${id}`);
  },

  // Get factures by fournisseur
  getFacturesByFournisseur: async (fournisseurId) => {
    const response = await apiClient.get(`${FACTURE_API}/getFactureByFournisseur/${fournisseurId}`);
    return response.data;
  },

  // Assign operateur to facture
  assignOperateurToFacture: async (idOperateur, idFacture) => {
    await apiClient.put(`${FACTURE_API}/assignOperateurToFacture/${idOperateur}/${idFacture}`);
  },

  // Get pourcentage recouvrement
  getPourcentageRecouvrement: async (startDate, endDate) => {
    const response = await apiClient.get(`${FACTURE_API}/pourcentageRecouvrement/${startDate}/${endDate}`);
    return response.data;
  },
};

export default factureService;

