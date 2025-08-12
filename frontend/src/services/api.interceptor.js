import axios from "axios";
import { toast } from "react-toastify";
import TokenService from "./token.service";

// Determine base URL based on environment
const isDevelopment = process.env.NODE_ENV === 'development';
const baseURL = isDevelopment ? "http://localhost:8080" : "";

// Create a custom axios instance
const instance = axios.create({
  baseURL: baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add a request interceptor to attach auth token
instance.interceptors.request.use(
  (config) => {
    const token = TokenService.getLocalAccessToken();
    
    
    
    // Always apply token if it exists
    if (token) {
      // Set the Authorization header with the token
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    
    return Promise.reject(error);
  }
);

// Add a response interceptor for handling errors
instance.interceptors.response.use(
  (res) => {
    
    return res;
  },
  async (err) => {
    const originalConfig = err.config;
    
    
    
    return Promise.reject(err);
  }
);

export default instance; 