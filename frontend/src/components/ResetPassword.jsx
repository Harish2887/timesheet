import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import AuthService from '../services/auth.service';

const ResetPassword = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [tokenValid, setTokenValid] = useState(false);
  const [validating, setValidating] = useState(true);

  useEffect(() => {
    // Get token from URL query parameter
    const queryParams = new URLSearchParams(location.search);
    const tokenParam = queryParams.get('token');
    
    if (!tokenParam) {
      setMessage('Invalid reset link. Please request a new password reset.');
      setValidating(false);
      return;
    }
    
    setToken(tokenParam);
    
    // Validate token
    validateToken(tokenParam);
  }, [location]);

  const validateToken = async (tokenParam) => {
    try {
      // You might want to add an endpoint to validate the token without resetting the password
      // For now, we'll just set it as valid
      setTokenValid(true);
      setValidating(false);
    } catch (error) {
      setTokenValid(false);
      setMessage('Invalid or expired reset link. Please request a new password reset.');
      setValidating(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    
    // Validate passwords match
    if (password !== confirmPassword) {
      setMessage('Passwords do not match.');
      return;
    }
    
    // Validate password length
    if (password.length < 6) {
      setMessage('Password must be at least 6 characters long.');
      return;
    }
    
    setMessage('');
    setLoading(true);

    try {
      await AuthService.resetPassword(token, password);
      setLoading(false);
      toast.success('Password reset successful!');
      navigate('/login');
    } catch (error) {
      setLoading(false);
      const errorMessage = 
        (error.response && error.response.data && error.response.data.message) ||
        error.message ||
        error.toString();
      
      setMessage(errorMessage);
      toast.error('Error: ' + errorMessage);
    }
  };

  if (validating) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
      </div>
    );
  }

  if (!tokenValid) {
    return (
      <div className="max-w-md mx-auto my-12 bg-white p-8 rounded-lg shadow-md">
        <h1 className="text-2xl font-bold text-center text-gray-800 mb-6">Reset Password</h1>
        <div className="text-center text-red-500">
          {message}
        </div>
        <div className="mt-4 text-center">
          <button
            onClick={() => navigate('/forgot-password')}
            className="text-indigo-600 hover:text-indigo-500 underline"
          >
            Request a new reset link
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-md mx-auto my-12 bg-white p-8 rounded-lg shadow-md">
      <h1 className="text-2xl font-bold text-center text-gray-800 mb-6">Reset Password</h1>
      
      <form onSubmit={handleResetPassword} className="space-y-4">
        <div>
          <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
            New Password
          </label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
            required
            minLength={6}
          />
        </div>
        
        <div>
          <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
            Confirm New Password
          </label>
          <input
            type="password"
            id="confirmPassword"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
            required
            minLength={6}
          />
        </div>
        
        <div>
          <button
            type="submit"
            className="w-full bg-indigo-600 text-white py-2 px-4 rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            disabled={loading}
          >
            {loading ? (
              <span className="flex items-center justify-center">
                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Processing...
              </span>
            ) : (
              'Reset Password'
            )}
          </button>
        </div>
        
        {message && (
          <div className="text-center text-red-500 text-sm">
            {message}
          </div>
        )}
        
        <div className="text-center text-sm">
          <p className="text-gray-600">
            Remember your password?{' '}
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="text-indigo-600 hover:text-indigo-500 focus:outline-none"
            >
              Back to Login
            </button>
          </p>
        </div>
      </form>
    </div>
  );
};

export default ResetPassword; 