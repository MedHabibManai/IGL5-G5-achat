import apiClient from './apiClient';

const PRODUIT_API = '/produit';

const produitService = {
  // Get all products
  getAllProduits: async () => {
    const response = await apiClient.get(`${PRODUIT_API}/retrieve-all-produits`);
    return response.data;
  },

  // Get product by ID
  getProduitById: async (id) => {
    const response = await apiClient.get(`${PRODUIT_API}/retrieve-produit/${id}`);
    return response.data;
  },

  // Add new product
  addProduit: async (produit) => {
    const response = await apiClient.post(`${PRODUIT_API}/add-produit`, produit);
    return response.data;
  },

  // Update product
  updateProduit: async (produit) => {
    const response = await apiClient.put(`${PRODUIT_API}/modify-produit`, produit);
    return response.data;
  },

  // Delete product
  deleteProduit: async (id) => {
    await apiClient.delete(`${PRODUIT_API}/remove-produit/${id}`);
  },

  // Assign product to stock
  assignProduitToStock: async (idProduit, idStock) => {
    await apiClient.put(`${PRODUIT_API}/assignProduitToStock/${idProduit}/${idStock}`);
  },
};

export default produitService;
