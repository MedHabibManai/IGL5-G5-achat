import apiClient from './apiClient';

const FOURNISSEUR_API = '/fournisseur';

const fournisseurService = {
  // Get all fournisseurs
  getAllFournisseurs: async () => {
    const response = await apiClient.get(`${FOURNISSEUR_API}/retrieve-all-fournisseurs`);
    return response.data;
  },

  // Get fournisseur by ID
  getFournisseurById: async (id) => {
    const response = await apiClient.get(`${FOURNISSEUR_API}/retrieve-fournisseur/${id}`);
    return response.data;
  },

  // Add new fournisseur
  addFournisseur: async (fournisseur) => {
    const response = await apiClient.post(`${FOURNISSEUR_API}/add-fournisseur`, fournisseur);
    return response.data;
  },

  // Update fournisseur
  updateFournisseur: async (fournisseur) => {
    const response = await apiClient.put(`${FOURNISSEUR_API}/modify-fournisseur`, fournisseur);
    return response.data;
  },

  // Delete fournisseur
  deleteFournisseur: async (id) => {
    await apiClient.delete(`${FOURNISSEUR_API}/remove-fournisseur/${id}`);
  },

  // Assign secteur activite to fournisseur
  assignSecteurActiviteToFournisseur: async (idSecteurActivite, idFournisseur) => {
    await apiClient.put(`${FOURNISSEUR_API}/assignSecteurActiviteToFournisseur/${idSecteurActivite}/${idFournisseur}`);
  },
};

export default fournisseurService;

