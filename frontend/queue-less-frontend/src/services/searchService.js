import axiosInstance from '../utils/axiosInstance';

export const searchService = {
  // Comprehensive search
  comprehensiveSearch: async (filters, page = 0, size = 20, sortBy = 'name', sortDirection = 'asc') => {
    try {
      const response = await axiosInstance.post('/search/comprehensive', filters, {
        params: { page, size, sortBy, sortDirection }
      });
      return response.data;
    } catch (error) {
      console.error('Search error:', error);
      throw error;
    }
  },

  // Nearby search
  searchNearby: async (filters) => {
    try {
      const response = await axiosInstance.post('/search/nearby', filters);
      return response.data;
    } catch (error) {
      console.error('Nearby search error:', error);
      throw error;
    }
  },

  // Quick search
  quickSearch: async (query, limit = 5) => {
    try {
      const response = await axiosInstance.get(`/search/quick/${query}`, {
        params: { limit }
      });
      return response.data;
    } catch (error) {
      console.error('Quick search error:', error);
      throw error;
    }
  },

  // Get filter options
  getFilterOptions: async () => {
    try {
      const response = await axiosInstance.get('/search/filter-options');
      return response.data;
    } catch (error) {
      console.error('Filter options error:', error);
      throw error;
    }
  },

  // Get search statistics
  getSearchStatistics: async (filters) => {
    try {
      const response = await axiosInstance.post('/search/statistics', filters);
      return response.data;
    } catch (error) {
      console.error('Statistics error:', error);
      throw error;
    }
  }
};