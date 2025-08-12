import React from "react";
import { Routes, Route } from "react-router-dom";
import Sidebar from "./Sidebar.jsx";
import AddInvoice from "./AddInvoice.jsx";
import MonthlyTimesheetEntry from "./MonthlyTimesheetEntry.jsx";
import { TimesheetProvider } from "../utils/TimesheetContext.jsx";
import DashboardHome from "./DashboardHome.jsx";

// Mock data for recent timesheet entries
const mockRecentEntries = [
    { year: 2025, month: 3, workingDays: 21.00, standardHours: 168, weekendHours: 0, overtimeHours: 0 },
    { year: 2025, month: 2, workingDays: 20.00, standardHours: 160, weekendHours: 0, overtimeHours: 0 },
    { year: 2025, month: 1, workingDays: 22.00, standardHours: 176, weekendHours: 0, overtimeHours: 0 },
    { year: 2024, month: 12, workingDays: 3.00, standardHours: 24, weekendHours: 0, overtimeHours: 0 }
];

const Dashboard = () => {
    return (
        <TimesheetProvider>
            <div className="flex flex-col h-screen">
                <Sidebar />
                <div className="flex-1 overflow-auto mt-10">
                    <Routes>
                        <Route path="/" element={<DashboardHome />} />
                        <Route path="/add-invoice" element={<AddInvoice />} />
                        <Route path="/timesheet" element={<MonthlyTimesheetEntry />} />
                        
                    </Routes>
                </div>
            </div>
        </TimesheetProvider>
    );
};

export default Dashboard;