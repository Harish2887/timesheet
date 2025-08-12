// src/components/MonthlyTimesheetEntry.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { useTimesheet } from '../utils/TimesheetContext';
import { toast } from 'react-toastify';
import authService from '../services/auth.service.js';
import timesheetService from '../services/timesheet.service.js';

const MonthlyTimesheetEntry = ({year, month}) => {
  const {
    holidayTypes,
    loading: contextLoading,
    getTimesheet,
    saveTimesheet,
    generateEmptyMonth,
    formatDate
  } = useTimesheet();
  
  const currentUser = authService.getCurrentUser();
  const userRoles = currentUser && currentUser.roles ? currentUser.roles : [];
  const isPayOrSub = userRoles.includes("ROLE_USER_PAY") || userRoles.includes("ROLE_USER_SUB");
  const isEmp = (userRoles.includes("ROLE_USER_EMP") || userRoles.includes("ROLE_ADMIN")) && !isPayOrSub;

  const [selectedYear, setSelectedYear] = useState(year || new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(month || new Date().getMonth() + 1);
  
  // State for detailed form (EMP)
  const [days, setDays] = useState([]);
  
  // State for upload form (PAY/SUB)
  const [totalHoursReported, setTotalHoursReported] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [fileError, setFileError] = useState(null);

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

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

  const fetchMonthData = useCallback(async () => {
    if (!selectedMonth || !selectedYear || !isEmp) {
      if(isEmp) setDays([]);
      return;
    }
    setLoading(true);
    try {
      const data = await getTimesheet(selectedYear, selectedMonth);
      if (data && data.entries && data.entries.length > 0) {
        const monthDays = generateEmptyMonth(selectedYear, selectedMonth);
        const entryMap = new Map(data.entries.map(e => [e.date, e]));
        const populatedDays = monthDays.map(day => {
          const entry = entryMap.get(day.date);
          return entry ? { ...day, ...entry, notes: entry.description, holidayTypeId: entry.holidayType?.id || '' } : day;
        });
        setDays(populatedDays);
        console.log('populatedDays', populatedDays);
      } else {
        setDays(generateEmptyMonth(selectedYear, selectedMonth));
      }
    } catch (error) {
      toast.error('Failed to load timesheet data.');
      console.error("Error fetching month data", error);
      setDays(generateEmptyMonth(selectedYear, selectedMonth));
    } finally {
      setLoading(false);
    }
  }, [selectedMonth, selectedYear, getTimesheet, generateEmptyMonth, isEmp]);

  useEffect(() => {
    if (isEmp) {
      fetchMonthData();
    } else {
      setTotalHoursReported('');
      setSelectedFile(null);
      setFileError(null);
    }
  }, [selectedMonth, selectedYear, isEmp, fetchMonthData]);

  const handleDayChange = (index, field, value) => {
    const updatedDays = [...days];
    updatedDays[index][field] = value;
    setDays(updatedDays);
  };

  const handleSave = async (isSubmit = false) => {
    if (!isEmp) return;

    setSaving(true);
    try {
      const entriesToSave = days
        .filter(day => day.hoursWorked > 0 || (parseFloat(day.supportHours) || 0) > 0 || day.holidayTypeId || day.notes)
        .map(day => ({
          date: day.date,
          hoursWorked: parseFloat(day.hoursWorked) || 0,
          supportHours: parseFloat(day.supportHours) || 0,
          holidayTypeId: day.holidayTypeId ? parseInt(day.holidayTypeId) : null,
          notes: day.notes
        }));
      await saveTimesheet(selectedYear, selectedMonth, entriesToSave, isSubmit);
    } catch (error) {
    } finally {
      setSaving(false);
    }
  };
  
  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      if (file.type === "application/pdf") {
        setSelectedFile(file);
        setFileError(null);
      } else {
        setSelectedFile(null);
        setFileError("Invalid file type. Only PDF is allowed.");
        toast.error("Invalid file type. Only PDF is allowed.");
      }
    }
  };

  const handleUploadSubmit = async () => {
    if (!isPayOrSub || !selectedFile || !totalHoursReported) {
      toast.error("Please fill in all fields and select a PDF file.");
      return;
    }
    if (fileError) {
        toast.error(fileError);
        return;
    }

    setSaving(true);
    try {
      await timesheetService.uploadTimesheetPdf(
        selectedYear,
        selectedMonth,
        totalHoursReported,
        selectedFile
      );
      toast.success('Timesheet PDF uploaded successfully!');
      setTotalHoursReported('');
      setSelectedFile(null);
      setFileError(null);
    } catch (error) {
      console.error("Error uploading timesheet PDF:", error);
      const errorMessage = error.response?.data?.message || error.response?.data || 'Failed to upload timesheet PDF.';
      toast.error(errorMessage);
    } finally {
      setSaving(false);
    }
  };

  const calculateTotalHours = () => {
    if (!isEmp) return 0;
    return days.reduce((acc, curr) => acc + (parseFloat(curr.hoursWorked) || 0) + (parseFloat(curr.supportHours) || 0), 0);
  };

  if (contextLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
      </div>
    );
  }
  
  const renderYearMonthSelector = () => (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
      <div>
        <label htmlFor="year-select" className="block text-sm font-medium text-gray-700 mb-2">
          Year
        </label>
        <input
          id="year-select"
          type="number"
          value={selectedYear}
          onChange={(e) => setSelectedYear(parseInt(e.target.value))}
          className="w-full p-2 border border-gray-300 rounded-md"
        />
      </div>
      <div>
        <label htmlFor="month-select" className="block text-sm font-medium text-gray-700 mb-2">
          Month
        </label>
        <select
          id="month-select"
          value={selectedMonth}
          onChange={(e) => setSelectedMonth(parseInt(e.target.value))}
          className="w-full p-2 border border-gray-300 rounded-md"
        >
          {months.map(month => (
            <option key={month.value} value={month.value}>
              {month.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );

  return (
    <div className="flex-1 bg-gray-100 p-4 md:p-8 overflow-auto">
      <div className="bg-white rounded-lg shadow-md p-4 md:p-6 mb-6">
        <h1 className="text-xl md:text-2xl font-semibold text-gray-800 mb-4">
          {isPayOrSub ? 'Upload Monthly Timesheet' : 'Monthly Timesheet Entry'}
        </h1>
        
        {renderYearMonthSelector()}
        
        {isPayOrSub && (
          <div className="bg-indigo-50 rounded-lg p-4 mb-6">
            <div className="mb-4">
              <label htmlFor="total-hours" className="block text-sm font-medium text-gray-700 mb-2">
                Total Hours Reported in PDF
              </label>
              <input
                id="total-hours"
                type="number"
                value={totalHoursReported}
                onChange={(e) => setTotalHoursReported(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-md"
                placeholder="e.g., 160"
              />
            </div>
            <div className="mb-4">
              <label htmlFor="pdf-upload" className="block text-sm font-medium text-gray-700 mb-2">
                Upload Timesheet PDF
              </label>
              <input
                id="pdf-upload"
                type="file"
                accept=".pdf"
                onChange={handleFileChange}
                className="w-full p-2 border border-gray-300 rounded-md file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
              />
              {selectedFile && <p className="text-sm text-gray-600 mt-1">Selected: {selectedFile.name}</p>}
              {fileError && <p className="text-sm text-red-600 mt-1">{fileError}</p>}
            </div>
            <div className="flex justify-end">
              <button
                onClick={handleUploadSubmit}
                disabled={saving || !selectedFile || !totalHoursReported}
                className="px-6 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50"
              >
                {saving ? 'Uploading...' : 'Upload & Submit'}
              </button>
            </div>
          </div>
        )}

        {isEmp && (
          <>
            <div className="bg-indigo-50 rounded-lg p-4 mb-6">
              <div className="flex justify-between items-center">
                <span className="text-indigo-700 font-medium">Total Hours:</span>
                <span className="text-indigo-700 font-bold text-xl">{calculateTotalHours().toFixed(2)}</span>
              </div>
            </div>
            <div className="flex flex-col sm:flex-row justify-end space-y-2 sm:space-y-0 sm:space-x-2 mb-6">
              <button
                onClick={() => handleSave(false)}
                disabled={saving}
                className="px-6 py-2 border border-indigo-600 text-indigo-600 rounded-md hover:bg-indigo-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Save Draft'}
              </button>
              <button
                onClick={() => handleSave(true)}
                disabled={saving}
                className="px-6 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50"
              >
                {saving ? 'Submitting...' : 'Submit Timesheet'}
              </button>
            </div>
          </>
        )}
      </div>

      {isEmp && loading && (
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
        </div>
      )}
      
      {isEmp && !loading && days.length > 0 && (
        <div className="bg-white rounded-lg shadow-md overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Date</th>
                <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Day</th>
                <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Hours Worked</th>
                <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Support Hours</th>
                <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Holiday/Leave Type</th>
                <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Notes</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {days.map((day, index) => {
                const dateObj = new Date(day.date + 'T00:00:00');
                const dayName = dateObj.toLocaleDateString('en-US', { weekday: 'short' });
                const isWeekend = dateObj.getDay() === 0 || dateObj.getDay() === 6;

                return (
                  <tr key={day.date} className={`${isWeekend ? 'bg-gray-100' : ''} ${day.holidayTypeId ? 'bg-amber-100' : ''}`}>
                    <td className="px-3 py-3 whitespace-nowrap text-sm text-gray-700">{formatDate(dateObj)}</td>
                    <td className="px-3 py-3 whitespace-nowrap text-sm text-gray-500">{dayName}</td>
                    <td className="px-3 py-3 whitespace-nowrap">
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max="24"
                        value={day.hoursWorked || ''}
                        onChange={(e) => handleDayChange(index, 'hoursWorked', e.target.value)}
                        disabled={ isWeekend}
                        className="w-24 p-1 border border-gray-300 rounded-md text-sm disabled:bg-gray-200"
                      />
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap">
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max="24"
                        value={day.supportHours || ''}
                        onChange={(e) => handleDayChange(index, 'supportHours', e.target.value)}
                        className="w-24 p-1 border border-gray-300 rounded-md text-sm"
                      />
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap">
                      <select
                        value={day.holidayTypeId || ''}
                        onChange={(e) => handleDayChange(index, 'holidayTypeId', e.target.value)}
                        disabled={isWeekend}
                        className="w-full p-1 border border-gray-300 rounded-md text-sm disabled:bg-gray-200"
                      >
                        <option value="">-- Select --</option>
                        {holidayTypes.map(ht => (
                          <option key={ht.id} value={ht.id}>{ht.name}</option>
                        ))}
                      </select>
                    </td>
                    <td className="px-3 py-3">
                      <input
                        type="text"
                        value={day.notes || ''}
                        onChange={(e) => handleDayChange(index, 'notes', e.target.value)}
                        className="w-full p-1 border border-gray-300 rounded-md text-sm"
                        maxLength="100"
                      />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {isEmp && !loading && days.length === 0 && (
         <div className="text-center py-8 text-gray-500">
            Please select a year and month to view the timesheet form, or no data is available for the selection.
        </div>
      )}
    </div>
  );
};

export default MonthlyTimesheetEntry;