import apiClient from './apiClient';

const OPERATEUR_API = '/operateur';

const operateurService = {
  // Get all operateurs
  getAllOperateurs: async () => {
    const response = await apiClient.get(`${OPERATEUR_API}/retrieve-all-operateurs`);
    return response.data;
  },

  // Get operateur by ID
  getOperateurById: async (id) => {
    const response = await apiClient.get(`${OPERATEUR_API}/retrieve-operateur/${id}`);
    return response.data;
  },

  // Add new operateur
  addOperateur: async (operateur) => {
    const response = await apiClient.post(`${OPERATEUR_API}/add-operateur`, operateur);
    return response.data;
  },

  // Update operateur
  updateOperateur: async (operateur) => {
    const response = await apiClient.put(`${OPERATEUR_API}/modify-operateur`, operateur);
    return response.data;
  },

  // Delete operateur
  deleteOperateur: async (id) => {
    await apiClient.delete(`${OPERATEUR_API}/remove-operateur/${id}`);
  },
};

export default operateurService;

