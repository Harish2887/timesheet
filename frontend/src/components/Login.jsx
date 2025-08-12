import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import AuthService from '../services/auth.service';

const Login = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const handleLogin = (e) => {
    e.preventDefault();
    setMessage('');
    setLoading(true);

    console.log("Login attempt with:", { username, password: "*".repeat(password.length) });
    
    AuthService.login(username, password)
      .then((response) => {
        console.log("Login successful! User data:", response);
        const roles = response?.roles || [];
        const target = roles.includes('ROLE_ADMIN') ? '/admin' : '/dashboard';
        // Force a full navigation so app state picks up the new session immediately
        window.location.replace(target);
        toast.success('Login successful!');
      })
      .catch(error => {
        const errorMessage = 
          (error.response && error.response.data && error.response.data.message) ||
          error.message ||
          error.toString();
          setPassword('');
        
        setLoading(false);
        setMessage(errorMessage);
        toast.error('Login failed: ' + errorMessage);
      });
  };
  
  // Debug login removed for production

  return (
    <div className="mx-auto my-12 bg-white p-8 rounded-lg shadow-md min-h-[40vh] md:my-20 flex flex-col justify-center w-full max-w-[90vw] md:max-w-[60vw] lg:max-w-[40vw]">
      <div className="flex justify-center ">
        <div className="flex items-center">
          <img className="h-10 w-auto " src="/logo_black.png" alt="Logo" />
        </div>
      </div>
      <h1 className="text-2xl font-bold text-center text-stone-600 mb-6">Login</h1>
      
      <form onSubmit={handleLogin} className="space-y-4">
        <div>
          <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
            Username
          </label>
          <input
            type="text"
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
            required
          />
        </div>
        
        <div>
          <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
            Password
          </label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 border-gray-300`}
            required
          />
          <div className="flex justify-end mt-1">
            <button
              type="button"
              onClick={() => navigate('/forgot-password')}
              className="text-xs text-indigo-600 hover:text-indigo-500 focus:outline-none"
            >
              Forgot Password?
            </button>
          </div>
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
                Signing In...
              </span>
            ) : (
              'Sign In'
            )}
          </button>
        </div>
        
        
        
        {message && (
          <div className="text-center text-red-500 text-sm">
            {message}
          </div>
        )}
      </form>
    </div>
  );
};

export default Login; 