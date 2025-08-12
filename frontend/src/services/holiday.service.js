import axios from "./api.interceptor";

const API_URL = "/api/holidays/";

/**
 * Service for handling holiday-related API operations
 * Note: Holiday endpoints are public and don't require authentication
 */
class HolidayService {
  /**
   * Get all holiday types
   * @returns {Promise} API response with holiday types
   */
  getAllHolidayTypes() {
    console.log("🔍 Fetching all holiday types");
    return axios.get(API_URL)
      .then(response => {
        console.log("✅ Holiday types fetched successfully:", response.data.length);
        return response.data;
      })
      .catch(error => {
        console.error("❌ Error fetching holiday types:", error.response?.status, error.message);
        throw error;
      });
  }
  
  /**
   * Get only government holidays
   * @returns {Promise} API response with government holidays
   */
  getGovernmentHolidays() {
    console.log("🔍 Fetching government holidays");
    return axios.get(`${API_URL}government`)
      .then(response => {
        console.log("✅ Government holidays fetched successfully:", response.data.length);
        return response.data;
      })
      .catch(error => {
        console.error("❌ Error fetching government holidays:", error.response?.status, error.message);
        throw error;
      });
  }
  
  /**
   * Get holidays within a date range
   * @param {string} startDate - Start date in ISO format (YYYY-MM-DD)
   * @param {string} endDate - End date in ISO format (YYYY-MM-DD)
   * @returns {Promise} API response with holidays in the specified range
   */
  getHolidaysInRange(startDate, endDate) {
    console.log(`🔍 Fetching holidays in range: ${startDate} to ${endDate}`);
    return axios.get(`${API_URL}range?startDate=${startDate}&endDate=${endDate}`)
      .then(response => {
        console.log("✅ Holidays in range fetched successfully:", response.data.length);
        return response.data;
      })
      .catch(error => {
        console.error("❌ Error fetching holidays in range:", error.response?.status, error.message);
        throw error;
      });
  }

  /**
   * Get holidays for a specific month
   * @param {number} year - Year (e.g., 2025)
   * @param {number} month - Month (1-12)
   * @returns {Promise} API response with holidays for the month
   */
  getHolidaysForMonth(year, month) {
    const startDate = `${year}-${month.toString().padStart(2, '0')}-01`;
    // Calculate last day of month
    const lastDay = new Date(year, month, 0).getDate();
    const endDate = `${year}-${month.toString().padStart(2, '0')}-${lastDay}`;
    
    console.log(`🔍 Fetching holidays for ${year}/${month}`);
    return this.getHolidaysInRange(startDate, endDate);
  }
  
  /**
   * Get holidays for the current month
   * @returns {Promise} API response with holidays for the current month
   */
  getHolidaysForCurrentMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth() + 1; // JavaScript months are 0-11
    
    return this.getHolidaysForMonth(year, month);
  }

  getAllHolidays() {
    return axios.get(API_URL);
  }

  getHoliday(id) {
    return axios.get(API_URL + id);
  }

  createHoliday(holiday) {
    return axios.post(API_URL, holiday);
  }

  updateHoliday(id, holiday) {
    return axios.put(API_URL + id, holiday);
  }

  deleteHoliday(id) {
    return axios.delete(API_URL + id);
  }

  getHolidaysByMonth(year, month) {
    console.log(`🔍 Fetching holidays for ${year}/${month}`);
    return axios.get(`${API_URL}by-month?year=${year}&month=${month}`)
      .then(response => {
        console.log("✅ Holidays for month fetched successfully:", response.data.length);
        return response.data;
      })
      .catch(error => {
        console.error("❌ Error fetching holidays for month:", error.response?.status, error.message);
        throw error;
      });
  }

  getHolidaysByYear(year) {
    console.log(`🔍 Fetching holidays for year ${year}`);
    return axios.get(`${API_URL}by-year?year=${year}`)
      .then(response => {
        console.log("✅ Holidays for year fetched successfully:", response.data.length);
        return response.data;
      })
      .catch(error => {
        console.error("❌ Error fetching holidays for year:", error.response?.status, error.message);
        throw error;
      });
  }
}

export default new HolidayService(); 