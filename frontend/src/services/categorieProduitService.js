import apiClient from './apiClient';

const CATEGORIE_PRODUIT_API = '/categorieProduit';

const categorieProduitService = {
  // Get all categorie produits
  getAllCategorieProduits: async () => {
    const response = await apiClient.get(`${CATEGORIE_PRODUIT_API}/retrieve-all-categorieProduit`);
    return response.data;
  },

  // Get categorie produit by ID
  getCategorieProduitById: async (id) => {
    const response = await apiClient.get(`${CATEGORIE_PRODUIT_API}/retrieve-categorieProduit/${id}`);
    return response.data;
  },

  // Add new categorie produit
  addCategorieProduit: async (categorieProduit) => {
    const response = await apiClient.post(`${CATEGORIE_PRODUIT_API}/add-categorieProduit`, categorieProduit);
    return response.data;
  },

  // Update categorie produit
  updateCategorieProduit: async (categorieProduit) => {
    const response = await apiClient.put(`${CATEGORIE_PRODUIT_API}/modify-categorieProduit`, categorieProduit);
    return response.data;
  },

  // Delete categorie produit
  deleteCategorieProduit: async (id) => {
    await apiClient.delete(`${CATEGORIE_PRODUIT_API}/remove-categorieProduit/${id}`);
  },
};

export default categorieProduitService;

