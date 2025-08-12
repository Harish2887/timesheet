import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import adminService from '../../services/admin.service';

const AllTimesheets = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [summaries, setSummaries] = useState([]);
  const [search, setSearch] = useState('');
  const [filterYear, setFilterYear] = useState('');
  const [filterMonth, setFilterMonth] = useState('');

  useEffect(() => {
    setLoading(true);
    adminService.getMonthlyTimesheets()
      .then((res) => setSummaries(res.data || []))
      .catch((err) => {
        console.error('Failed to load monthly timesheets', err);
        toast.error('Failed to load timesheets');
      })
      .finally(() => setLoading(false));
  }, []);

  const years = useMemo(() => {
    const set = new Set(summaries.map((s) => s.year));
    return Array.from(set).sort((a, b) => b - a);
  }, [summaries]);

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
    { value: 12, label: 'December' },
  ];

  const filtered = useMemo(() => {
    return summaries
      .filter((s) => (filterYear ? s.year === Number(filterYear) : true))
      .filter((s) => (filterMonth ? s.month === Number(filterMonth) : true))
      .filter((s) => (search ? s.username.toLowerCase().includes(search.toLowerCase()) : true))
      .sort((a, b) => {
        if (a.year !== b.year) return b.year - a.year;
        if (a.month !== b.month) return b.month - a.month;
        return a.username.localeCompare(b.username);
      });
  }, [summaries, filterYear, filterMonth, search]);

  const getStatusClass = (status) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-green-100 text-green-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      case 'PAID':
        return 'bg-indigo-100 text-indigo-800';
      default:
        return 'bg-yellow-100 text-yellow-800';
    }
  };

  const exportUserTimesheet = async (userId, username, year, month) => {
    try {
      const response = await adminService.exportUserTimesheet(userId, year, month);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${username}-timesheet-${year}-${month}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error exporting user timesheet:', error);
      toast.error('Failed to export user timesheet');
    }
  };

  return (
    <div className="flex-1 bg-gray-100 p-8 overflow-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-800">All Timesheets</h1>
      </div>

      <div className="bg-white rounded-lg shadow-md p-4 mb-4 grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label className="block text-sm text-gray-600 mb-1">Search user</label>
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full border border-gray-300 rounded-md p-2"
            placeholder="e.g. john"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-600 mb-1">Year</label>
          <select
            value={filterYear}
            onChange={(e) => setFilterYear(e.target.value)}
            className="w-full border border-gray-300 rounded-md p-2"
          >
            <option value="">All</option>
            {years.map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-sm text-gray-600 mb-1">Month</label>
          <select
            value={filterMonth}
            onChange={(e) => setFilterMonth(e.target.value)}
            className="w-full border border-gray-300 rounded-md p-2"
          >
            <option value="">All</option>
            {months.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
        </div>
        <div className="flex items-end">
          <button
            onClick={() => { setSearch(''); setFilterYear(''); setFilterMonth(''); }}
            className="px-4 py-2 border border-gray-300 rounded-md"
          >
            Clear
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-auto">
        {loading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
          </div>
        ) : (
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Year</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Month</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Completion</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filtered.length > 0 ? filtered.map((t, idx) => (
                <tr key={`${t.userId}-${t.year}-${t.month}-${idx}`} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{t.username}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{t.year}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{t.monthName || t.month}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div className="w-40 bg-gray-200 rounded-full h-2">
                      <div className={`h-2 rounded-full ${t.complete ? 'bg-green-500' : 'bg-blue-500'}`} style={{ width: `${t.completionPercentage}%` }}></div>
                    </div>
                    <div className="text-xs mt-1">{Math.round(t.completionPercentage)}% ({t.filledWorkdays}/{t.totalWorkdays} days)</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusClass(t.status)}`}>
                      {t.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div className="flex space-x-2">
                      <button
                        onClick={() => navigate(`/admin/timesheets/${t.userId}/${t.year}/${t.month}`)}
                        className="text-gray-700 hover:text-gray-900"
                        title="View details"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.477 0 8.268 2.943 9.542 7-1.274 4.057-5.065 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
                      </button>
                      <button
                        onClick={() => exportUserTimesheet(t.userId, t.username, t.year, t.month)}
                        className="text-indigo-600 hover:text-indigo-900"
                        title="Export"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
                      </button>
                    </div>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan="6" className="px-6 py-4 text-center text-gray-500">No results</td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default AllTimesheets;


