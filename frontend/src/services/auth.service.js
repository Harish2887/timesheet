import axios from "./api.interceptor";

const API_URL = "/api/auth/";

class AuthService {
  login(username, password) {
    return axios
      .post(API_URL + "login", {
        username,
        password
      })
      .then(response => {
        if (response.data.token) {
          localStorage.setItem("user", JSON.stringify(response.data));
        } else {
          // No token received
        }
        return response.data;
      })
      .catch(error => {
        throw error;
      });
  }

  logout() {
    // Clear all storage and state
    localStorage.clear();
    sessionStorage.clear();
    
    // Force reload to clear all React state
    window.location.href = "/login";
  }

  forgotPassword(email) {
    return axios
      .post(API_URL + "forgot-password", {
        email
      })
      .then(response => {
        return response.data;
      })
      .catch(error => {
        throw error;
      });
  }

  resetPassword(token, password) {
    return axios
      .post(API_URL + "reset-password", {
        token,
        password
      })
      .then(response => {
        return response.data;
      })
      .catch(error => {
        throw error;
      });
  }

  register(username, email, password) {
    return axios.post(API_URL + "register", {
      username,
      email,
      password
    });
  }

  getCurrentUser() {
    try {
      const userStr = localStorage.getItem("user");
      if (!userStr) {
        return null;
      }
      
      const user = JSON.parse(userStr);
      
      // Validate token format
      if (!user.token || typeof user.token !== 'string' || !user.token.startsWith("ey")) {
        this.logout();
        return null;
      }
      
      return user;
    } catch (error) {
      console.error("Error retrieving current user:", error);
      // If there's an error, clean up localStorage to prevent future errors
      localStorage.removeItem("user");
      return null;
    }
  }
}

export default new AuthService(); 