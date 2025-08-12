import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import TimesheetService from '../../services/timesheet.service';

const TimesheetDetails = () => {
  const { userId, year, month } = useParams();
  const navigate = useNavigate();
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    TimesheetService.getUserMonthlyTimesheet(userId, year, month)
      .then((res) => {
        setEntries(res.data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || 'Failed to fetch timesheet');
        setLoading(false);
      });
  }, [userId, year, month]);

  if (loading) return <div className="p-8 text-center">Loading timesheet...</div>;
  if (error) return <div className="p-8 text-center text-red-500">{error}</div>;

  return (
    <div className="max-w-2xl mx-auto my-10 bg-white p-8 rounded-lg shadow-md">
      <button
        className="mb-4 text-indigo-600 hover:underline text-sm"
        onClick={() => navigate(-1)}
      >
        &larr; Back
      </button>
      <h2 className="text-2xl font-bold mb-2 text-stone-700">Timesheet Details</h2>
      <div className="mb-4">
        <div><span className="font-semibold">User ID:</span> {userId}</div>
        <div><span className="font-semibold">Month:</span> {month}</div>
        <div><span className="font-semibold">Year:</span> {year}</div>
      </div>
      <div>
        <h3 className="font-semibold mb-2">Reported Days</h3>
        <table className="w-full border text-sm">
          <thead>
            <tr className="bg-gray-100">
              <th className="py-2 px-3 border">Date</th>
              <th className="py-2 px-3 border">Reported Time</th>
              <th className="py-2 px-3 border">Support Hours</th>
              <th className="py-2 px-3 border">Notes</th>
            </tr>
          </thead>
          <tbody>
            {entries && entries.length > 0 ? (
              entries.map((entry, idx) => (
                <tr key={idx} className={entry?.holidayType ? "bg-amber-100" : ""}>
                  <td className="py-1 px-3 border">{entry.date}</td>
                  <td className="py-1 px-3 border">{entry.hoursWorked} h</td>
                  <td className="py-1 px-3 border">{(entry.supportHours || 0)} h</td>
                  <td className="py-1 px-3 border">{entry.description || entry?.holidayType?.name}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={3} className="py-2 px-3 text-center text-gray-400">No entries</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default TimesheetDetails; 