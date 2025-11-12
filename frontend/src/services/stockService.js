import apiClient from './apiClient';

const STOCK_API = '/stock';

const stockService = {
  // Get all stocks
  getAllStocks: async () => {
    const response = await apiClient.get(`${STOCK_API}/retrieve-all-stocks`);
    return response.data;
  },

  // Get stock by ID
  getStockById: async (id) => {
    const response = await apiClient.get(`${STOCK_API}/retrieve-stock/${id}`);
    return response.data;
  },

  // Add new stock
  addStock: async (stock) => {
    const response = await apiClient.post(`${STOCK_API}/add-stock`, stock);
    return response.data;
  },

  // Update stock
  updateStock: async (stock) => {
    const response = await apiClient.put(`${STOCK_API}/modify-stock`, stock);
    return response.data;
  },

  // Delete stock
  deleteStock: async (id) => {
    await apiClient.delete(`${STOCK_API}/remove-stock/${id}`);
  },
};

export default stockService;
