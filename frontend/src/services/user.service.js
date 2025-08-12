import axios from "./api.interceptor";

// Update URL paths to match backend controller mappings
const TIMESHEET_URL = "/api/timesheet/";
const MONTHLY_URL = "/api/monthly-timesheet/";
const ADMIN_URL = "/api/admin/";
const USER_URL = "/api/user/";

class UserService {
  // Timesheet operations
  getAllTimesheets() {
    console.log("Getting all timesheets");
    return axios.get(TIMESHEET_URL);
  }

  getTimesheetById(id) {
    return axios.get(TIMESHEET_URL + id);
  }

  createTimesheet(timesheet) {
    return axios.post(TIMESHEET_URL, timesheet);
  }

  updateTimesheet(id, timesheet) {
    return axios.put(TIMESHEET_URL + id, timesheet);
  }

  // Specific endpoints that are missing
  getMonthWorkdays(year, month) {
    console.log(`Getting workdays for ${year}-${month}`);
    return axios.get(`${TIMESHEET_URL}month/workdays?year=${year}&month=${month}`);
  }
  
  getMonthCompletion(year, month) {
    console.log(`Getting completion for ${year}-${month}`);
    return axios.get(`${TIMESHEET_URL}month/completion?year=${year}&month=${month}`);
  }

  // Monthly timesheet operations
  getMonthlyTimesheets() {
    return axios.get(MONTHLY_URL);
  }

  getMonthlyTimesheetByMonth(year, month) {
    return axios.get(MONTHLY_URL + year + "/" + month);
  }

  // Admin operations
  getAllUsers() {
    return axios.get(ADMIN_URL + "users");
  }

  getPublicContent() {
    return axios.get(USER_URL + "all");
  }

  getUserBoard() {
    return axios.get(USER_URL + "user");
  }

  getAdminBoard() {
    return axios.get(USER_URL + "admin");
  }

  updateUser(userData) {
    return axios.put(USER_URL + "profile", userData);
  }

  changePassword(passwordData) {
    return axios.post(USER_URL + "change-password", passwordData);
  }
}

export default new UserService(); 