// src/components/AddInvoice.jsx
import React, { useState } from 'react';
import axios from '../services/api.interceptor';
import { toast } from 'react-toastify';
import { useTimesheet } from '../utils/TimesheetContext';

const AddInvoice = () => {
  const { timesheets } = useTimesheet();
  
  const [invoiceDate, setInvoiceDate] = useState('');
  const [workingHours, setWorkingHours] = useState('');
  const [amount, setAmount] = useState('');
  const [invoiceFile, setInvoiceFile] = useState(null);
  const [workMonth, setWorkMonth] = useState('');
  const [loading, setLoading] = useState(false);


  const getMonthName = (monthNumber) => {
    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    return monthNames[monthNumber - 1];
  };

  // Get available months from timesheets
  const availableMonths = Object.keys(timesheets).map(key => {
    const [year, month] = key.split('-');
    return {
      value: key,
      label: `${getMonthName(parseInt(month))} ${year}`
    };
  });

  const handleFileChange = (e) => {
    setInvoiceFile(e.target.files[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!invoiceDate || !workingHours || !amount || !invoiceFile || !workMonth) {
      toast.error('Please fill in all fields');
      return;
    }

    const formData = new FormData();
    formData.append('invoiceDate', invoiceDate);
    formData.append('workingHours', workingHours);
    formData.append('amount', amount);
    formData.append('invoiceFile', invoiceFile);
    formData.append('workMonth', workMonth);

    setLoading(true);

    try {
      await axios.post('/api/invoices', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      
      toast.success('Invoice submitted successfully');
      
      // Reset form
      setInvoiceDate('');
      setWorkingHours('');
      setAmount('');
      setInvoiceFile(null);
      setWorkMonth('');
      
      // Reset file input
      document.getElementById('invoice-file-input').value = '';
    } catch (error) {
      console.error('Error submitting invoice:', error);
      toast.error('Failed to submit invoice');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6 mx-auto max-w-2xl w-full">
      <h2 className="text-2xl font-medium text-gray-600 mb-6">Request Invoice</h2>
      
      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label htmlFor="invoice-date" className="block text-sm font-medium text-gray-500 mb-2">
            INVOICE DATE
          </label>
          <input
            type="date"
            id="invoice-date"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-200"
            value={invoiceDate}
            onChange={(e) => setInvoiceDate(e.target.value)}
          />
        </div>

        <div className="mb-4">
          <label htmlFor="working-hours" className="block text-sm font-medium text-gray-500 mb-2">
            WORKING HOURS
          </label>
          <input
            type="text"
            id="working-hours"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-200"
            value={workingHours}
            onChange={(e) => setWorkingHours(e.target.value)}
            placeholder="Enter hours for month"
          />
        </div>

        <div className="mb-4">
          <label htmlFor="amount" className="block text-sm font-medium text-gray-500 mb-2">
            AMOUNT (EXCL VAT)
          </label>
          <input
            type="number"
            id="amount"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-200"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="Enter Amount (Excl VAT)"
            step="0.01"
            min="0"
          />
        </div>

        <div className="mb-4">
          <label htmlFor="invoice-file-input" className="block text-sm font-medium text-gray-500 mb-2">
            ATTACH INVOICE *
          </label>
          <div className="flex items-center">
            <label className="w-full flex items-center px-3 py-2 border border-gray-300 rounded-md cursor-pointer hover:bg-gray-50">
              <span className="text-indigo-600 mr-2">Choose Files</span>
              <span className="text-gray-400 flex-1 truncate">{invoiceFile ? invoiceFile.name : "No file chosen"}</span>
              <input
                type="file"
                id="invoice-file-input"
                className="hidden"
                onChange={handleFileChange}
                accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png"
              />
            </label>
          </div>
        </div>

        <div className="mb-6">
          <label htmlFor="work-month" className="block text-sm font-medium text-gray-500 mb-2">
            WORK MONTH
          </label>
          <select
            id="work-month"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-200 appearance-none bg-white"
            value={workMonth}
            onChange={(e) => setWorkMonth(e.target.value)}
          >
            <option value="">--Select Month--</option>
            {availableMonths.map((month) => (
              <option key={month.value} value={month.value}>
                {month.label}
              </option>
            ))}
          </select>
        </div>

        <button
          type="submit"
          className="bg-indigo-500 hover:bg-indigo-600 text-white font-medium py-2 px-6 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-300 transition-colors duration-200"
          disabled={loading}
        >
          {loading ? 'Submitting...' : 'Submit Invoice'}
        </button>
      </form>
    </div>
  );
};

export default AddInvoice;