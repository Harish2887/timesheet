import axios from "./api.interceptor";

const API_URL = "/api/admin/";

class AdminService {
  getAllUsers() {
    return axios.get(API_URL + "users");
  }

  getUsersByRole(role) {
    return axios.get(API_URL + `users/by-role/${role}`);
  }

  createUser(userData) {
    return axios.post(API_URL + "users", userData);
  }

  getAllTimesheets() {
    return axios.get(API_URL + "timesheet");
  }

  getUserTimesheets(userId) {
    return axios.get(API_URL + "timesheet/" + userId);
  }

  getMonthlyTimesheets() {
    return axios.get(API_URL + "timesheet/monthly");
  }

  updateTimesheetStatus(timesheetId, status) {
    return axios.put(
      API_URL + "timesheet/" + timesheetId + "/status", 
      {}, 
      { 
        params: { status }
      }
    );
  }
  
  approveTimesheet(id) {
    return axios.put(API_URL + `timesheet/${id}/approve`, {});
  }

  rejectTimesheet(id) {
    return axios.put(API_URL + `timesheet/${id}/reject`, {});
  }

  approveMonthlyTimesheet(userId, year, month) {
    return axios.put(API_URL + `timesheet/monthly/${userId}/${year}/${month}/approve`, {});
  }

  rejectMonthlyTimesheet(userId, year, month) {
    return axios.put(API_URL + `timesheet/monthly/${userId}/${year}/${month}/reject`, {});
  }

  markTimesheetAsPaid(userId, year, month) {
    return axios.put(API_URL + `timesheet/monthly/${userId}/${year}/${month}/pay`, {});
  }

  exportTimesheets(year, month) {
    return axios.get(API_URL + `export/timesheets/${year}/${month}`, {
      headers: {
        'Accept': 'application/octet-stream'
      },
      responseType: 'blob'
    });
  }

  exportUserTimesheet(userId, year, month) {
    return axios.get(API_URL + `export/timesheets/${userId}/${year}/${month}`, {
      headers: {
        'Accept': 'application/octet-stream'
      },
      responseType: 'blob'
    });
  }

  exportInvoices(year, month) {
    return axios.get(API_URL + `export/invoices/${year}/${month}`, {
      headers: {
        'Accept': 'application/octet-stream'
      },
      responseType: 'blob'
    });
  }
}

export default new AdminService(); 