import React, { useState, useEffect } from "react";
import { useTimesheet } from "../utils/TimesheetContext";
import { toast } from "react-toastify";
import timesheetService from "../services/timesheet.service.js";

const DashboardHome = () => {
    const { 
        loading: contextLoading, 
        holidayTypes = [], 
        timesheets = {}, 
        getMonthWorkdays, 
        getMonthHolidays,
        error: contextError,
        currentUser
    } = useTimesheet();

    const today = new Date();
    const [selectedYear, setSelectedYear] = useState(today.getFullYear());
    const [selectedMonth, setSelectedMonth] = useState(today.getMonth() +1);
    const [recentEntries, setRecentEntries] = useState([]);
    const [entriesLimit, setEntriesLimit] = useState(25);
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);
    const [monthData, setMonthData] = useState(null);
    const [monthHolidays, setMonthHolidays] = useState([]);
    const [error, setError] = useState(null);

    const months = [
        { value: 1, label: 'January' },
        { value: 2, label: 'February' },
        { value: 3, label: 'March' },
        { value: 4, label: 'April' },
        { value: 5, label: 'May' },
        { value: 6, label: 'June' },
        { value: 7, label: 'July' },
        { value: 8, label: 'August' },
        { value: 9, label: 'September' },
        { value: 10, label: 'October' },
        { value: 11, label: 'November' },
        { value: 12, label: 'December' }
    ];

    // Handle month selection and fetch month data
    const handleMonthSelect = async (value) => {
        const monthValue = value;
        setSelectedMonth(monthValue);

        if (!monthValue) {
            setMonthData(null);
            setMonthHolidays([]);
            return;
        }

        setLoading(true);
        setError(null);
        try {
            console.log(`DashboardHome - Selecting month ${selectedYear}-${monthValue}`);

            const workdaysData = await timesheetService.getMonthlyTimesheet(selectedYear, monthValue);
            console.log("Month workdays:", workdaysData);
            console.log("???????????????????????", workdaysData)
            setMonthData({totalHoursWorked: workdaysData.data.regularHours || 0,
                holidayHours: workdaysData.data.holidayHours || 0 });


            if (getMonthHolidays) {
                const holidaysData = await getMonthHolidays(selectedYear, parseInt(monthValue, 10));
                console.log("Month holidays:", holidaysData);
                setMonthHolidays(holidaysData || []);
            } else {
                console.warn("getMonthHolidays function is not available");
                setMonthHolidays([]);
            }
            console.log("???????????????????????", monthData)
        } catch (error) {
            console.error("Error fetching month data:", error);
            setError("Failed to load month information. Please try again.");
            toast.error("Failed to load month information. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if(selectedMonth) {
            handleMonthSelect(selectedMonth)
        }
    }, [])

    // Process timesheets from context
    useEffect(() => {
        console.log("DashboardHome - Processing timesheets:", timesheets ? "Has data" : "No data");
        try {
            if (timesheets && Object.keys(timesheets).length > 0) {
                console.log(timesheets)
                const processedEntries = processTimesheetData(timesheets);
                setRecentEntries(processedEntries);
            } else {
                setRecentEntries(processTimesheetData(null));
            }
        } catch (err) {
            console.error("Error processing timesheet data:", err);
            setError("Failed to process timesheet data");
            // Use mock data as fallback
            setRecentEntries(processTimesheetData(null));
        }
    }, [timesheets]);

    // Process timesheet data into the format needed for the table
    const processTimesheetData = (timesheetData) => {
        // If no data, use mock data for now
        if (!timesheetData || Object.keys(timesheetData).length === 0) {
            console.log("DashboardHome - Using mock timesheet data");
            return [
                { year: 2024, month: 3, workingDays: 21.00, standardHours: 168, weekendHours: 0, overtimeHours: 0 },
                { year: 2024, month: 2, workingDays: 20.00, standardHours: 160, weekendHours: 0, overtimeHours: 0 },
                { year: 2024, month: 1, workingDays: 22.00, standardHours: 176, weekendHours: 0, overtimeHours: 0 },
                { year: 2023, month: 12, workingDays: 3.00, standardHours: 24, weekendHours: 0, overtimeHours: 0 }
            ];
        }
        
        // Convert timesheets object to array format for display
        try {
            return Object.values(timesheetData)
                .map(timesheet => {
                    const data = {
                        year: timesheet.year,
                        month: timesheet.month,
                        workingDays: timesheet.entries?.length || 0,
                        standardHours: 0,
                        weekendHours: 0,
                        overtimeHours: 0
                    };
                    
                    // Calculate hours if entries exist
                    if (timesheet.entries && timesheet.entries.length > 0) {
                        timesheet.entries.forEach(entry => {
                            if (entry.hoursWorked) {
                                const date = new Date(entry.date);
                                const isWeekend = [0, 6].includes(date.getDay());
                                
                                if (isWeekend) {
                                    data.weekendHours += entry.hoursWorked;
                                } else {
                                    data.standardHours += entry.hoursWorked;
                                }
                                // Add overtime logic if needed
                            }
                        });
                    }
                    
                    return data;
                })
                .sort((a, b) => {
                    if (a.year !== b.year) return b.year - a.year;
                    return b.month - a.month;
                });
        } catch (err) {
            console.error("Error converting timesheet data:", err);
            return [];
        }
    };

    const filteredEntries = recentEntries.filter(entry => {
        if (!searchTerm) return true;

        const term = searchTerm.toLowerCase();
        const monthLabel = months.find(m => m.value === entry.month)?.label.toLowerCase() || '';
        
        return (
            entry.year.toString().includes(term) ||
            entry.month.toString().includes(term) ||
            monthLabel.includes(term) ||
            entry.workingDays.toString().includes(term) ||
            entry.standardHours.toString().includes(term)
        );
    });

    // Show informative message if there's an error in the context or component
    if (contextError || error) {
        return (
            <div className="flex-1 bg-gray-100 p-8 overflow-auto">
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded relative mb-6">
                    <strong className="font-bold">Error: </strong>
                    <span className="block sm:inline">{contextError || error}</span>
                    <p className="mt-2">Try refreshing the page or check your network connection.</p>
                </div>
                
                {/* Display mock data anyway */}
                <div className="bg-white rounded-lg shadow-md p-6">
                    <h2 className="text-xl font-semibold text-gray-800 mb-6">
                        Recently Inserted data (Mock)
                    </h2>
                    {/* Simplified table with mock data */}
                    <table className="min-w-full">
                        <thead>
                        <tr className="bg-gray-50">
                            <th className="p-2 text-left">YEAR</th>
                            <th className="p-2 text-left">MONTH</th>
                            <th className="p-2 text-left">WORKING DAYS</th>
                        </tr>
                        </thead>
                        <tbody>
                            {processTimesheetData(null).map((entry, i) => (
                                <tr key={i}>
                                    <td className="p-2">{entry.year}</td>
                                    <td className="p-2">{months.find(m => m.value === entry.month)?.label}</td>
                                    <td className="p-2">{entry.workingDays}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    if (contextLoading || loading) {
        return (
            <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
            </div>
        );
    }

    return (
        <div className="flex-1 bg-gray-100 p-8 overflow-auto">
            <div className="bg-white rounded-lg shadow-md p-6 mb-8">
                <h1 className="text-xl font-semibold text-gray-800 mb-6">
                    Monthly Dashboard
                </h1>

                <div className="grid grid-cols-2 gap-6 mb-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            WORK YEAR
                        </label>
                        <input
                            type="number"
                            value={selectedYear}
                            onChange={(e) => setSelectedYear(e.target.value)}
                            className="w-full p-2 border border-gray-300 rounded-md"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            WORK MONTH
                        </label>
                        <select
                            value={selectedMonth}
                            onChange={(e) => handleMonthSelect(e.target.value)}
                            className="w-full p-2 border border-gray-300 rounded-md"
                        >
                            <option value="">--Select Month--</option>
                            {months.map(month => (
                                <option key={month.value} value={month.value}>
                                    {month.label}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {selectedMonth !== '' ? (
                    <div>
                        {monthData && (
                            <div className="mb-4 p-4 bg-blue-50 rounded-md">
                                <h3 className="font-medium text-blue-700 mb-2">Month Summary</h3>
                                <p>Applied working hours: {monthData.totalHoursWorked || 'N/A'}</p>
                                <p>Applied holiday hours: {monthData.holidayHours || 'N/A'}</p>
                            </div>
                        )}
                        
                        {monthHolidays && monthHolidays.length > 0 && (
                            <div className="mt-4 p-4 bg-green-50 rounded-md">
                                <h3 className="font-medium text-green-700 mb-2">Holidays This Month</h3>
                                <ul className="list-disc pl-5">
                                    {monthHolidays.map((holiday, index) => (
                                        <li key={index} className="text-sm">
                                            {holiday.date}: {holiday.name}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="py-8 text-center text-gray-500">
                        Please select a year and month to view the timesheet form
                    </div>
                )}
            </div>

            {/* Recently Inserted Data Section */}
            <div className="bg-white rounded-lg shadow-md p-6">
                <h2 className="text-xl font-semibold text-gray-800 mb-6">
                    Recently Inserted data
                </h2>

                {/* Table Controls */}
                <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center">
                        <span className="mr-2 text-sm text-gray-600">Show</span>
                        <select
                            className="border border-gray-300 rounded px-2 py-1 text-sm"
                            value={entriesLimit}
                            onChange={(e) => setEntriesLimit(Number(e.target.value))}
                        >
                            <option value="10">10</option>
                            <option value="25">25</option>
                            <option value="50">50</option>
                        </select>
                        <span className="ml-2 text-sm text-gray-600">entries</span>
                    </div>

                    <div className="flex items-center">
                        <label className="text-sm text-gray-600 mr-2">Search:</label>
                        <input
                            type="text"
                            className="border border-gray-300 rounded px-2 py-1 text-sm"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead>
                        <tr className="bg-gray-50">
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                YEAR
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                MONTH
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                WORKING DAYS
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                STANDARD HOURS
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                WEEKEND HOURS
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                OVERTIME HOURS
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                ATTACHMENT
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                TIMESHEET
                            </th>
                        </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                        {filteredEntries.length > 0 ? (
                            filteredEntries.slice(0, entriesLimit).map((entry, index) => (
                                <tr key={index} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {entry.year}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {months.find(m => m.value === entry.month)?.label || entry.month}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {typeof entry.workingDays === 'number' ? entry.workingDays.toFixed(2) : 'N/A'}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {entry.standardHours}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {entry.weekendHours}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {entry.overtimeHours}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        <button className="text-indigo-600 hover:text-indigo-900">
                                            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                                            </svg>
                                        </button>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        <button className="text-indigo-600 hover:text-indigo-900">
                                            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                            </svg>
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan="8" className="px-6 py-4 text-center text-sm text-gray-500">
                                    {loading ? 'Loading data...' : 'No entries found'}
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default DashboardHome; 