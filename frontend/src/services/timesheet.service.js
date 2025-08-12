import axios from "./api.interceptor";

const API_URL = "/api/timesheet/";
const ADMIN_URL = "/api/admin";
const MONTHLY_TIMESHEET_API_URL = "/api/monthly-timesheet/";
const HOLIDAY_API_URL = "/api/holidays/";

class TimesheetService {
  getMyTimesheets() {
    return axios.get(API_URL);
  }

  getTimesheetsByDateRange(startDate, endDate) {
    return axios.get(API_URL + 'range', {
      params: { startDate, endDate }
    });
  }

  getHolidaysByDateRange(startDate, endDate) {
    return axios.get(`${HOLIDAY_API_URL}range?startDate=${startDate}&endDate=${endDate}`);
  }

  getTimesheet(id) {
    return axios.get(API_URL + id);
  }

  createTimesheet(timesheetData) {
    return axios.post(API_URL, timesheetData);
  }

  deleteTimesheet(id) {
    return axios.delete(API_URL + id);
  }

  getAllTimesheets() {
    return axios.get(`${ADMIN_URL}/timesheet`);
  }

  updateTimesheet(id, timesheetData) {
    return axios.put(API_URL + id, timesheetData);
  }

  getAllHolidayTypes() {
    return axios.get(HOLIDAY_API_URL);
  }
  
  approveTimesheet(id) {
    return axios.put(`${ADMIN_URL}/timesheet/${id}/approve`, {});
  }

  rejectTimesheet(id) {
    return axios.put(`${ADMIN_URL}/timesheet/${id}/reject`, {});
  }
  
  getMonthlyTimesheets() {
    return axios.get(`${ADMIN_URL}/timesheet/monthly`);
  }
  
  getMonthCompletion(year, month) {
    return axios.get(API_URL + 'month/completion', {
      params: { year, month }
    });
  }
  
  approveMonthlyTimesheet(userId, year, month) {
    return axios.put(
      `${ADMIN_URL}/timesheet/monthly/${userId}/${year}/${month}/approve`, 
      {}
    );
  }
  
  rejectMonthlyTimesheet(userId, year, month) {
    return axios.put(
      `${ADMIN_URL}/timesheet/monthly/${userId}/${year}/${month}/reject`, 
      {}
    );
  }

  getUserMonthlyTimesheet(userId, year, month) {
    console.log(`Getting monthly timesheet for ${userId}-${year}-${month}`);
    return axios.get(`${MONTHLY_TIMESHEET_API_URL}${userId}/${year}/${month}`);
  }

  getMonthlyTimesheet(year, month) {
    console.log(`Getting monthly timesheet for ${year}-${month}`);
    return axios.get(`${MONTHLY_TIMESHEET_API_URL}${year}/${month}`);
  }

  createOrUpdateMonthlyTimesheetWithEntries(year, month, data) {
    console.log(`Creating/updating timesheet for ${year}-${month} with entries:`, data);
    return axios.post(`${MONTHLY_TIMESHEET_API_URL}${year}/${month}/entries`, data);
  }

  createOrUpdateMonthlyTimesheet(year, month, data) {
    console.log(`Creating/updating timesheet for ${year}-${month} with data:`, data);
    return axios.post(`${MONTHLY_TIMESHEET_API_URL}${year}/${month}`, data);
  }

  submitMonthlyTimesheet(id) {
    return axios.post(`${API_URL}monthly-timesheet/${id}/submit`, {});
  }

  approveMonthlyTimesheet(id, comments) {
    return axios.post(`${API_URL}monthly-timesheet/${id}/approve`, { comments });
  }

  rejectMonthlyTimesheet(id, comments) {
    return axios.post(`${API_URL}monthly-timesheet/${id}/reject`, { comments });
  }

  getPendingTimesheets() {
    return axios.get(`${MONTHLY_TIMESHEET_API_URL}admin/pending`);
  }

  getMonthWorkdays(year, month) {
    return axios.get(API_URL + 'month/workdays', {
      params: { year, month }
    });
  }

  getRecentTimesheets(limit = 25) {
    return axios.get(`${API_URL}recent?limit=${limit}`);
  }

  saveTimesheet = async (year, month, entries, isSubmit = false) => {
    // ... existing saveTimesheet code ...
  };

  uploadTimesheetPdf = async (year, month, totalHours, file) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('totalHoursReported', totalHours);

    try {
      const response = await axios.post(`/api/monthly-timesheet/${year}/${month}/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data;
    } catch (error) {
      console.error('Error uploading timesheet PDF:', error);
      throw error;
    }
  };
}

export default new TimesheetService(); 