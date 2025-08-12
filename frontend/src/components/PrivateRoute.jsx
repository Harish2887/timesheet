import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import AuthService from '../services/auth.service';

const PrivateRoute = ({ children, roles = [] }) => {
  const location = useLocation();
  const [isAuthenticated, setIsAuthenticated] = useState(null);
  const [hasRole, setHasRole] = useState(true);

  useEffect(() => {
    // Check authentication status when the component mounts or route changes
    checkAuth();
  }, [location.pathname]);

  const checkAuth = () => {
    const user = AuthService.getCurrentUser();
    
    // Check if user exists
    if (!user) {
      setIsAuthenticated(false);
      return;
    }

    // Verify token format and validity
    if (user.token) {
      try {
        // Check token format - at minimum, it should be a JWT token (starts with "ey")
        if (!user.token.startsWith('ey')) {
          setIsAuthenticated(false);
          toast.error('Your session is invalid. Please log in again.');
          AuthService.logout();
          return;
        }
        
        // Basic expiration check - this is a simple check and not foolproof
        // Decode the middle part of the JWT to check expiration
        const tokenParts = user.token.split('.');
        if (tokenParts.length !== 3) {
          setIsAuthenticated(false);
          AuthService.logout();
          return;
        }
        
        // Parse the payload
        const payload = JSON.parse(atob(tokenParts[1]));
        const expirationTime = payload.exp * 1000; // Convert to milliseconds
        const currentTime = Date.now();
        const timeRemaining = Math.round((expirationTime - currentTime) / 1000 / 60); // minutes
        
        
        if (currentTime >= expirationTime) {
          setIsAuthenticated(false);
          toast.error('Your session has expired. Please log in again.');
          AuthService.logout();
          return;
        }
        
        // Token is valid
        console.log('✅ PrivateRoute: Token is valid');
        setIsAuthenticated(true);
      } catch (error) {
        console.error('❌ PrivateRoute: Error validating token', error);
        setIsAuthenticated(false);
        AuthService.logout();
        return;
      }
    } else {
      setIsAuthenticated(false);
      return;
    }

    // Check role requirements if specified
    if (roles.length > 0) {
      const userRoles = user.roles || [];
      
      const hasRequiredRole = userRoles.some(role => roles.includes(role));
      
      if (!hasRequiredRole) {
        setHasRole(false);
        toast.error('You do not have permission to access this page');
      } else {
        setHasRole(true);
      }
    }
  };

  // Show loading state while checking authentication
  if (isAuthenticated === null) {
    return <div className="flex justify-center items-center h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
    </div>;
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Redirect to dashboard if authenticated but missing required role
  if (!hasRole) {
    return <Navigate to="/dashboard" replace />;
  }

  // Render children if all checks pass
  return children;
};

export default PrivateRoute; 