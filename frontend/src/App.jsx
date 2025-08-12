import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

// Import components
import Login from './components/Login';
import PrivateRoute from './components/PrivateRoute';
import Dashboard from "./components/Dashboard.jsx";
import AdminRoutes from "./components/admin/AdminRoutes.jsx";
import AuthService from './services/auth.service';
import ForgotPassword from './components/ForgotPassword';
import ResetPassword from './components/ResetPassword';
 

function App() {
  const [currentUser, setCurrentUser] = useState(undefined);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check if user is logged in
    const checkUserSession = () => {
      const user = AuthService.getCurrentUser();
      if (user) {
        console.log("App - User session found:", {
          username: user.username,
          roles: user.roles,
          tokenExists: !!user.token
        });
        setCurrentUser(user);
      } else {
        console.log("App - No user session found");
      }
      setIsLoading(false);
    };
    
    checkUserSession();

    // If an admin is logged in but currently on /dashboard, redirect to /admin
    try {
      const stored = localStorage.getItem('user');
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed.roles) && parsed.roles.includes('ROLE_ADMIN')) {
          if (window.location.pathname.startsWith('/dashboard')) {
            window.location.replace('/admin');
          }
        }
      }
    } catch (_) {}

    // Add event listener for storage changes to handle logout in other tabs
    const handleStorageChange = (e) => {
      if (e.key === 'user') {
        console.log("App - Local storage 'user' change detected");
        checkUserSession();
      }
    };

    window.addEventListener('storage', handleStorageChange);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  // Show loading spinner while checking authentication
  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
      </div>
    );
  }

  return (
    <Router>
      <div className="min-h-screen bg-gray-100">
        <ToastContainer 
          position="top-right"
          autoClose={3000}
          hideProgressBar={false}
          newestOnTop
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
        />
        
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<Navigate to={currentUser ? (currentUser.roles.includes("ROLE_ADMIN") ? "/admin" : "/dashboard") : "/login"} />} />
          <Route path="/login" element={
            currentUser ? <Navigate to={currentUser.roles.includes("ROLE_ADMIN") ? "/admin" : "/dashboard"} /> : 
              <div className='flex justify-center items-center h-screen'><Login /></div>
          } />
          <Route path="/forgot-password" element={
            currentUser ? <Navigate to={currentUser.roles.includes("ROLE_ADMIN") ? "/admin" : "/dashboard"} /> : <ForgotPassword />
          } />
          <Route path="/reset-password" element={
            currentUser ? <Navigate to={currentUser.roles.includes("ROLE_ADMIN") ? "/admin" : "/dashboard"} /> : <ResetPassword />
          } />
          
          
          
          {/* Protected routes */}
          <Route 
            path="/dashboard/*" 
            element={
              <PrivateRoute>
                <Dashboard />
              </PrivateRoute>
            }
          />
          
          {/* Admin routes */}
          <Route 
            path="/admin/*" 
            element={
              <PrivateRoute roles={["ROLE_ADMIN"]}>
                <AdminRoutes />
              </PrivateRoute>
            }
          />
          
          {/* Catch all route - redirect to login or dashboard */}
          <Route path="*" element={<Navigate to={currentUser ? (currentUser.roles.includes("ROLE_ADMIN") ? "/admin" : "/dashboard") : "/login"} />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App; 

