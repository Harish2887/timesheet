import React, { useState, useEffect, useRef } from 'react';
import { toast } from 'react-toastify';
import { Link, useNavigate } from 'react-router-dom';
import adminService from '../../services/admin.service';
import { Pie } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';

ChartJS.register(ArcElement, Tooltip, Legend);

const AdminDashboard = () => {
  const [loading, setLoading] = useState(false);
  const [userStats, setUserStats] = useState({
    total: 0,
    admins: 0,
    employees: 0,
    paymentManagers: 0,
    subcontractors: 0
  });
  const [monthlyTimesheets, setMonthlyTimesheets] = useState([]);
  const chartRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchData();

    return () => {
      if (chartRef.current) {
        chartRef.current.destroy();
        chartRef.current = null;
      }
    };
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [usersResponse, monthlyResponse] = await Promise.all([
        adminService.getAllUsers(),
        adminService.getMonthlyTimesheets()
      ]);

      // Process user stats
      const allUsers = usersResponse.data;
      const stats = {
        total: allUsers.length,
        admins: countUsersByRole(allUsers, 'ROLE_ADMIN'),
        employees: countUsersByRole(allUsers, 'ROLE_USER_EMP'),
        paymentManagers: countUsersByRole(allUsers, 'ROLE_USER_PAY'),
        subcontractors: countUsersByRole(allUsers, 'ROLE_USER_SUB')
      };
      setUserStats(stats);
      
      // Identify admin user IDs
      const adminUserIds = new Set(
        allUsers
          .filter(user => user.roles && user.roles.some(role => role.name === 'ROLE_ADMIN'))
          .map(user => user.id)
      );

      // Process monthly timesheets: filter out admins, then get latest per user
      const allMonthlySummaries = monthlyResponse.data;
      
      const nonAdminSummaries = allMonthlySummaries.filter(summary => !adminUserIds.has(summary.userId));
      
      const latestTimesheetsByUser = {};
      nonAdminSummaries.forEach(summary => {
        if (!latestTimesheetsByUser[summary.userId] || 
            summary.year > latestTimesheetsByUser[summary.userId].year ||
            (summary.year === latestTimesheetsByUser[summary.userId].year && summary.month > latestTimesheetsByUser[summary.userId].month)) {
          latestTimesheetsByUser[summary.userId] = summary;
        }
      });
      
      const displayableTimesheets = Object.values(latestTimesheetsByUser);
      
      // Sort the final list: newest year, then newest month, then by username
      displayableTimesheets.sort((a, b) => {
        if (a.year !== b.year) return b.year - a.year;
        if (a.month !== b.month) return b.month - a.month;
        return a.username.localeCompare(b.username);
      });
      
      setMonthlyTimesheets(displayableTimesheets);

    } catch (error) {
      console.error('Error fetching dashboard data:', error);
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const countUsersByRole = (users, roleName) => {
    return users.filter(user => 
      user.roles && user.roles.some(role => role.name === roleName)
    ).length;
  };

  const getStatusClass = (status) => {
    switch(status) {
      case 'APPROVED': return 'bg-green-100 text-green-800';
      case 'REJECTED': return 'bg-red-100 text-red-800';
      default: return 'bg-yellow-100 text-yellow-800';
    }
  };

  const approveTimesheet = (userId, year, month) => {
    setLoading(true);
    adminService.approveMonthlyTimesheet(userId, year, month)
      .then(() => {
        toast.success('Timesheet approved');
        fetchData();
      })
      .catch(error => {
        console.error('Error approving timesheet:', error);
        toast.error('Failed to approve timesheet');
      })
      .finally(() => setLoading(false));
  };

  const rejectTimesheet = (userId, year, month) => {
    setLoading(true);
    adminService.rejectMonthlyTimesheet(userId, year, month)
      .then(() => {
        toast.success('Timesheet rejected');
        fetchData();
      })
      .catch(error => {
        console.error('Error rejecting timesheet:', error);
        toast.error('Failed to reject timesheet');
      })
      .finally(() => setLoading(false));
  };

  const markAsPaid = (userId, year, month) => {
    setLoading(true);
    adminService.markTimesheetAsPaid(userId, year, month)
      .then(() => {
        toast.success('Timesheet marked as paid');
        fetchData();
      })
      .catch(error => {
        console.error('Error marking timesheet as paid:', error);
        toast.error(error.response?.data || 'Failed to mark as paid');
      })
      .finally(() => setLoading(false));
  };

  const exportTimesheets = async (year, month) => {
    try {
      const response = await adminService.exportTimesheets(year, month);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `timesheets-${year}-${month}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error exporting timesheets:', error);
      toast.error('Failed to export timesheets');
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

  const userChartData = {
    labels: ['Admins', 'Employees', 'Payment Managers', 'Subcontractors'],
    datasets: [{
      data: [userStats.admins, userStats.employees, userStats.paymentManagers, userStats.subcontractors],
      backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0'],
      hoverBackgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0']
    }]
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            let label = context.label || '';
            if (label) {
              label += ': ';
            }
            if (context.parsed !== null) {
              label += context.parsed;
            }
            return label;
          }
        }
      }
    }
  };

  return (
    <div className="flex-1 bg-gray-100 p-8 overflow-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-800">Admin Dashboard</h1>
      </div>
      
      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-center">
            <div className="text-xl font-bold">{userStats.total}</div>
            <div className="text-sm text-gray-500">Total Users</div>
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-center">
            <div className="text-xl font-bold">{userStats.employees}</div>
            <div className="text-sm text-gray-500">Employees</div>
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-center">
            <div className="text-xl font-bold">{userStats.subcontractors}</div>
            <div className="text-sm text-gray-500">Subcontractors</div>
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-6">
          <Link to="/admin/users" className="no-underline">
            <button className="w-full px-4 py-2 border border-indigo-500 text-indigo-500 hover:bg-indigo-50 rounded flex items-center justify-center">
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
              Manage Users
            </button>
          </Link>
        </div>
      </div>
      
      {/* Chart and Table */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="bg-white rounded-lg shadow-md p-6 lg:col-span-1">
          <h2 className="text-lg font-semibold text-gray-700 mb-4">User Distribution</h2>
          <div className="h-64 relative">
            {loading && !userStats.total ? (
              <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-70">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
              </div>
            ) : userStats.total > 0 ? (
              <Pie ref={chartRef} data={userChartData} options={chartOptions} />
            ) : (
              <p className="text-center text-gray-500">No user data to display.</p>
            )}
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-6 lg:col-span-2">
          <h2 className="text-lg font-semibold text-gray-700 mb-4">Recent Timesheets</h2>
          
          {loading ? (
            <div className="flex justify-center items-center h-64">
              <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Month</th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Year</th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Completion</th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {monthlyTimesheets.length > 0 ? (
                    monthlyTimesheets.slice(0, 5).map((timesheet, index) => (
                      console.log('timesheet ++++++++++++++++++++++++++++++++',timesheet),
                      <tr onClick={() => navigate(`/admin/timesheets/${timesheet.userId}/${timesheet.year}/${timesheet.month}`)} 
                        key={index} className="hover:bg-gray-50 cursor-pointer">
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                          {timesheet.username}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {timesheet.monthName}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {timesheet.year}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div 
                              className={`h-2 rounded-full ${timesheet.complete ? 'bg-green-500' : 'bg-blue-500'}`} 
                              style={{ width: `${timesheet.completionPercentage}%` }}
                            ></div>
                          </div>
                          <div className="text-xs mt-1">
                            {Math.round(timesheet.completionPercentage)}% ({timesheet.filledWorkdays}/{timesheet.totalWorkdays} days)
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusClass(timesheet.status)}`}>
                            {timesheet.status}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          <div className="flex space-x-2">
                            <button
                              onClick={() => approveTimesheet(timesheet.userId, timesheet.year, timesheet.month)}
                              disabled={timesheet.status === 'APPROVED'}
                              className={`text-green-600 hover:text-green-900 ${timesheet.status === 'APPROVED' ? 'opacity-50 cursor-not-allowed' : ''}`}
                              title="Approve"
                            >
                              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                              </svg>
                            </button>
                            <button
                              onClick={() => rejectTimesheet(timesheet.userId, timesheet.year, timesheet.month)}
                              disabled={timesheet.status === 'REJECTED'}
                              className={`text-red-600 hover:text-red-900 ${timesheet.status === 'REJECTED' ? 'opacity-50 cursor-not-allowed' : ''}`}
                              title="Reject"
                            >
                              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                              </svg>
                            </button>
                            <button
                              onClick={() => exportUserTimesheet(timesheet.userId, timesheet.username, timesheet.year, timesheet.month)}
                              className="text-indigo-600 hover:text-indigo-900"
                              title="Export user timesheet"
                            >
                              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                              </svg>
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="6" className="px-6 py-4 text-center text-gray-500">
                        No timesheets found
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
          
          <div className="mt-4 flex justify-end">
            <button
              disabled
              className="text-indigo-600 hover:text-indigo-900 flex items-center opacity-50 cursor-not-allowed"
            >
              <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              View All Timesheets
            </button>
          </div>
        </div>
      </div>
      
      {/* Export Section */}
      
    </div>
  );
};

export default AdminDashboard;