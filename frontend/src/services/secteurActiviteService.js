import apiClient from './apiClient';

const SECTEUR_ACTIVITE_API = '/secteurActivite';

const secteurActiviteService = {
  // Get all secteur activites
  getAllSecteurActivites: async () => {
    const response = await apiClient.get(`${SECTEUR_ACTIVITE_API}/retrieve-all-secteurActivite`);
    return response.data;
  },

  // Get secteur activite by ID
  getSecteurActiviteById: async (id) => {
    const response = await apiClient.get(`${SECTEUR_ACTIVITE_API}/retrieve-secteurActivite/${id}`);
    return response.data;
  },

  // Add new secteur activite
  addSecteurActivite: async (secteurActivite) => {
    const response = await apiClient.post(`${SECTEUR_ACTIVITE_API}/add-secteurActivite`, secteurActivite);
    return response.data;
  },

  // Update secteur activite
  updateSecteurActivite: async (secteurActivite) => {
    const response = await apiClient.put(`${SECTEUR_ACTIVITE_API}/modify-secteurActivite`, secteurActivite);
    return response.data;
  },

  // Delete secteur activite
  deleteSecteurActivite: async (id) => {
    await apiClient.delete(`${SECTEUR_ACTIVITE_API}/remove-secteurActivite/${id}`);
  },
};

export default secteurActiviteService;

