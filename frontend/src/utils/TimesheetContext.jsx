// src/context/TimesheetContext.jsx
import React, { createContext, useState, useContext, useEffect } from 'react';
import axios from '../services/api.interceptor';
import { toast } from 'react-toastify';

// Create the context
const TimesheetContext = createContext();

// Custom hook for easy context consumption
export const useTimesheet = () => useContext(TimesheetContext);

export const TimesheetProvider = ({ children }) => {
  // State variables
  const [holidayTypes, setHolidayTypes] = useState([]);
  const [timesheets, setTimesheets] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  
  // Fetch holiday types on component mount
  useEffect(() => {
    const fetchHolidayTypes = async () => {
      try {
        console.log('Fetching holiday types...');
        const response = await axios.get('/api/holidays');
        console.log('Holiday types response:', response.data);
        setHolidayTypes(response.data);
      } catch (err) {
        console.error('Error fetching holiday types:', err);
        setError('Failed to load holiday types');
        toast.error('Failed to load holiday data');
      }
    };
    
    fetchHolidayTypes();
  }, []);
  
  // Fetch user's timesheets after holiday types are loaded
  useEffect(() => {
    const fetchTimesheets = async () => {
      setLoading(true);
      try {
        console.log('Fetching monthly timesheets...');
        const response = await axios.get('/api/monthly-timesheet/');
        console.log('Monthly timesheets response:', response.data);
        
        // Convert array to an object keyed by year-month for easier access
        const timesheetMap = {};
        response.data.forEach(timesheet => {
          const key = `${timesheet.year}-${timesheet.month}`;
          timesheetMap[key] = timesheet;
        });
        
        setTimesheets(timesheetMap);
        setError(null);
      } catch (err) {
        console.error('Error fetching timesheets:', err);
        setError('Failed to load timesheet data');
        toast.error('Failed to load timesheet data');
      } finally {
        setLoading(false);
      }
    };
    
    // Only fetch timesheets after holiday types are loaded
    if (holidayTypes.length > 0) {
      fetchTimesheets();
    }
  }, [holidayTypes]);
  
  // Get timesheet for specific year and month
  const getTimesheet = async (year, month) => {
    const key = `${year}-${month}`;
    
    // Return from cache if available
    if (timesheets[key]) {
      return timesheets[key];
    }
    
    // Otherwise fetch from API
    try {
      const response = await axios.get(`/api/monthly-timesheet/${year}/${month}`);
      
      // Update the timesheets state with the new data
      setTimesheets(prev => ({
        ...prev,
        [key]: response.data
      }));
      
      return response.data;
    } catch (err) {
      console.error(`Error fetching timesheet for ${year}-${month}:`, err);
      toast.error('Failed to load timesheet data');
      return null;
    }
  };
  
  // Save timesheet entries for a specific month
  const saveTimesheet = async (year, month, entries, isSubmit = false) => {
    try {
      const response = await axios.post(`/api/monthly-timesheet/${year}/${month}/entries`, {
        entries,
        submit: isSubmit
      });
      
      // Update the timesheets state with the updated data
      const key = `${year}-${month}`;
      setTimesheets(prev => ({
        ...prev,
        [key]: response.data
      }));
      
      toast.success(isSubmit ? 'Timesheet submitted successfully' : 'Timesheet saved successfully');
      return response.data;
    } catch (err) {
      console.error('Error saving timesheet:', err);
      toast.error('Failed to save timesheet');
      throw err;
    }
  };
  
  // Generate empty days for a month
  const generateEmptyMonth = (year, month) => {
    const daysInMonth = new Date(year, month, 0).getDate();
    const days = [];
    
    for (let i = 1; i <= daysInMonth; i++) {
      const date = new Date(year, month - 1, i);
      const dayOfWeek = date.getDay();
      const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
      
      // Format date as YYYY-MM-DD
      const formattedDate = formatDate(date);
      
      days.push({
        date: formattedDate,
        hoursWorked: 0,
        holidayTypeId: null,
        notes: '',
        isWeekend
      });
    }
    
    return days;
  };
  
  // Format date as YYYY-MM-DD
  const formatDate = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  
  // Value object to be provided to consumers
  const value = {
    holidayTypes,
    timesheets,
    loading,
    error,
    currentUser,
    getTimesheet,
    saveTimesheet,
    generateEmptyMonth,
    formatDate
  };
  
  return (
    <TimesheetContext.Provider value={value}>
      {children}
    </TimesheetContext.Provider>
  );
};