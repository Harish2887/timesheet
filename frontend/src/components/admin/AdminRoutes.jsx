import React from 'react';
import { Routes, Route } from 'react-router-dom';
import AdminDashboard from './AdminDashboard';
import UserManagement from './UserManagement';
import Sidebar from '../Sidebar';
import TimesheetDetails from './TimesheetDetails';
import AllTimesheets from './AllTimesheets';

const AdminRoutes = () => {
  return (
    <div className="flex flex-col h-screen">
      <Sidebar />
      <div className="flex-1 overflow-auto mt-10">
        <Routes>
          <Route path="/" element={<AdminDashboard />} />
          <Route path="/users" element={<UserManagement />} />
          <Route path="/all-timesheets" element={<AllTimesheets />} />
          <Route path="/timesheets/:userId/:year/:month" element={<TimesheetDetails />} />
        </Routes>
      </div>
    </div>
  );
};

export default AdminRoutes; 