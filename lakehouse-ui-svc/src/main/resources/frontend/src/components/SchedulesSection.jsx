import React, { useState } from 'react';
import { fetchSchedules } from '../api.js';

function toDateTimeLocalValue(date) {
  const offsetMs = date.getTimezoneOffset() * 60000;
  const local = new Date(date.getTime() - offsetMs);
  return local.toISOString().slice(0, 16);
}

function getInitialDates() {
  const now = new Date();
  const from = new Date(now.getTime() - 24 * 60 * 60 * 1000);
  return { fromDate: toDateTimeLocalValue(from), toDate: toDateTimeLocalValue(now) };
}

export default function SchedulesSection() {
  const initialDates = getInitialDates();
  const [fromDate, setFromDate] = useState(initialDates.fromDate);
  const [toDate, setToDate] = useState(initialDates.toDate);
  const [schedules, setSchedules] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = () => {
    setLoading(true);
    setError('');
    fetchSchedules(
      new Date(fromDate).toISOString(),
      new Date(toDate).toISOString()
    )
      .then(setSchedules)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  return (
    <section className="section">
      <h2>Schedules</h2>
      <div className="states-filter">
        <div className="states-filter-field">
          <label>From</label>
          <input
            type="datetime-local"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </div>
        <div className="states-filter-field">
          <label>To</label>
          <input
            type="datetime-local"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </div>
        <button className="states-filter-button" onClick={load} disabled={loading}>
          Refresh
        </button>
      </div>
      {error && <div className="error-box">Error: {error}</div>}
      {loading && <div className="empty-box">Loading...</div>}
      {!error && !loading && schedules.length === 0 && (
        <div className="empty-box">No schedules found.</div>
      )}
      {!error && !loading && schedules.length > 0 && (
        <table className="states-table">
          <thead>
            <tr>
              <th>Id</th>
              <th>Schedule</th>
              <th>Target execution time</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {schedules.map((schedule) => (
              <tr key={schedule.id}>
                <td>{schedule.id}</td>
                <td>{schedule.configScheduleKeyName}</td>
                <td>{schedule.targetExecutionDateTime}</td>
                <td>{schedule.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
