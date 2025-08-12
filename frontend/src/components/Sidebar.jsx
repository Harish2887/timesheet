import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import ClockIcon from "./icons/ClockIcon.jsx";
import authService from "../services/auth.service.js";

const Sidebar = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const location = useLocation();
  const currentUser = authService.getCurrentUser();
  const navigate = useNavigate();
  const isAdmin = currentUser && currentUser.roles && currentUser.roles.includes("ROLE_ADMIN");
  const userRoles = currentUser && currentUser.roles ? currentUser.roles : [];

  const isPayOrSub = userRoles.includes("ROLE_USER_PAY") || userRoles.includes("ROLE_USER_SUB");
  const canSubmitDetailedTimesheet = userRoles.includes("ROLE_USER_EMP") && !isPayOrSub;
  
  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };
  
  const closeMenu = () => {
    setIsMenuOpen(false);
  };

  const handleLogout = () => {
    authService.logout();
  };

  const isActive = (path) => {
    return location.pathname === path ? 
      "text-indigo-600 border-indigo-600" : 
      "text-gray-600 hover:text-indigo-500 border-transparent hover:border-indigo-300";
  };
  
  // Menu items for regular users
  let userMenuItems = [
    {
      path: "/dashboard",
      icon: <ClockIcon className="w-5 h-5" />,
      label: "Dashboard"
    },
    {
      path: "/dashboard/timesheet", 
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      ),
      label: isPayOrSub ? "Upload Timesheet" : "Submit Timesheet"
    },
    {
      path: "/dashboard/add-invoice",
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
      ),
      label: "Upload Invoice"
    }
  ];

  if (!canSubmitDetailedTimesheet && !isPayOrSub) {
    userMenuItems = userMenuItems.filter(item => item.path !== "/dashboard/timesheet");
  }
  
  // Only show Upload Invoice for ROLE_USER_SUB users
  const isSubcontractor = userRoles.includes("ROLE_USER_SUB");
  if (!isSubcontractor) {
    userMenuItems = userMenuItems.filter(item => item.path !== "/dashboard/add-invoice");
  }
  
  // Menu items for admin users
  const adminMenuItems = [
    {
      path: "/admin",
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 8v8m-4-5v5m-4-2v2m-2 4h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      ),
      label: "Dashboard"
    },
    {
      path: "/admin/users",
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      ),
      label: "User Management"
    }
    ,
    {
      path: "/admin/all-timesheets",
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7h18M3 12h18M3 17h18" />
        </svg>
      ),
      label: "All Timesheets"
    }
  ];
  
  // Choose menu items based on user role
  const menuItems = isAdmin ? adminMenuItems : userMenuItems;

  return (
    <>
      {/* Top Navigation Bar - Reduced height with smaller padding */}
      <div className="bg-white shadow-sm w-full fixed top-0 z-10">
        <div className="max-w-7xl mx-auto px-2 sm:px-4">
          <div className="flex justify-between items-center py-2">
            {/* Logo - Reduced size */}
            <div className="flex justify-start lg:w-0 lg:flex-1">
              <div className="flex items-center">
                <img className="h-6 w-auto mr-2 cursor-pointer" src="/logo_black.png" alt="Logo" onClick={() => navigate(menuItems[0].path)} />
              </div>
            </div>
            
            {/* Mobile menu button - Smaller */}
            <div className="-mr-1 -my-1 md:hidden">
              <button
                type="button"
                className="bg-white rounded-md p-1 inline-flex items-center justify-center text-gray-500 hover:text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-500"
                onClick={toggleMenu}
              >
                <span className="sr-only">Open menu</span>
                <svg
                  className="h-5 w-5"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M4 6h16M4 12h16M4 18h16"
                  />
                </svg>
              </button>
            </div>
            
            {/* Desktop navigation - More compact */}
            <nav className="hidden md:flex space-x-6">
              {menuItems.map((item) => (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center border-b-2 px-1 pt-1 text-sm font-medium ${isActive(item.path)}`}
                >
                  <span className="hidden lg:block">{item.label}</span>
                  <span className="block lg:hidden" title={item.label}>{item.icon}</span>
                </Link>
              ))}
            </nav>
            
            {/* Logout button - Desktop */}
            <div className="hidden md:flex items-center justify-end md:flex-1 lg:w-0">
              <button 
                onClick={handleLogout}
                className="ml-4 whitespace-nowrap inline-flex items-center justify-center px-3 py-1 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700"
              >
                <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                <span>Logout</span>
              </button>
            </div>
          </div>
        </div>
        
        {/* Mobile menu */}
        <div className={`${isMenuOpen ? 'block' : 'hidden'} md:hidden absolute z-50 inset-x-0 transition transform origin-top-right`}>
          <div className="rounded-lg shadow-lg ring-1 ring-black ring-opacity-5 bg-white divide-y-2 divide-gray-50">
            <div className="pt-5 pb-6 px-5">
              <div className="flex items-center justify-between">
                <div>
                  <img className="h-6 w-auto" src="/logo_black.png" alt="Logo" />
                </div>
                <div className="-mr-2">
                  <button
                    type="button"
                    className="bg-white rounded-md p-2 inline-flex items-center justify-center text-gray-400 hover:text-gray-500 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-500"
                    onClick={toggleMenu}
                  >
                    <span className="sr-only">Close menu</span>
                    <svg
                      className="h-5 w-5"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      aria-hidden="true"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth="2"
                        d="M6 18L18 6M6 6l12 12"
                      />
                    </svg>
                  </button>
                </div>
              </div>
              <div className="mt-4">
                <nav className="grid gap-y-4">
                  {menuItems.map((item) => (
                    <Link
                      key={item.path}
                      to={item.path}
                      className="flex items-center p-2 -m-3 rounded-md hover:bg-gray-50"
                      onClick={closeMenu}
                    >
                      <div className={`flex-shrink-0 ${location.pathname === item.path ? 'text-indigo-600' : 'text-gray-600'}`}>
                        {item.icon}
                      </div>
                      <span className={`ml-3 text-base font-medium ${location.pathname === item.path ? 'text-indigo-600' : 'text-gray-900'}`}>
                        {item.label}
                      </span>
                    </Link>
                  ))}
                </nav>
              </div>
            </div>
            <div className="py-4 px-5">
              <button
                onClick={handleLogout}
                className="w-full flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-base font-medium text-white bg-red-600 hover:bg-red-700"
              >
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                Logout
              </button>
            </div>
          </div>
        </div>
      </div>
      
      {/* Add spacing to prevent content from being hidden under the navbar */}
      <div className="h-10 md:h-10"></div>
    </>
  );
};

export default Sidebar;