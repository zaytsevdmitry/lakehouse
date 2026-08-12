import React, { useCallback, useEffect, useRef, useState } from 'react';
import { fetchScheduleHeaders, fetchSchedules } from '../api.js';
import PipelineSection from './PipelineSection.jsx';

const NAMES_MIN_PERCENT = 20;
const NAMES_MAX_PERCENT = 60;
const NAMES_DEFAULT_PERCENT = 30;

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
  const containerRef = useRef(null);
  const initialDates = getInitialDates();
  const [fromDate, setFromDate] = useState(initialDates.fromDate);
  const [toDate, setToDate] = useState(initialDates.toDate);
  const [schedules, setSchedules] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [headers, setHeaders] = useState([]);
  const [headersError, setHeadersError] = useState('');
  const [filter, setFilter] = useState('');
  const [onlyEnabled, setOnlyEnabled] = useState(false);
  const [selected, setSelected] = useState(() => new Set());
  const [selectedRunId, setSelectedRunId] = useState(null);

  const [namesPercent, setNamesPercent] = useState(NAMES_DEFAULT_PERCENT);
  const [dragging, setDragging] = useState(false);

  useEffect(() => {
    fetchScheduleHeaders()
      .then(setHeaders)
      .catch((e) => setHeadersError(e.message));
  }, []);

  useEffect(() => {
    if (!dragging) return;
    const handleMove = (e) => {
      const rect = containerRef.current.getBoundingClientRect();
      if (rect.width === 0) return;
      let percent = ((e.clientX - rect.left) / rect.width) * 100;
      percent = Math.min(NAMES_MAX_PERCENT, Math.max(NAMES_MIN_PERCENT, percent));
      setNamesPercent(percent);
    };
    const handleUp = () => setDragging(false);
    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [dragging]);

  const load = () => {
    setLoading(true);
    setError('');
    fetchSchedules(
      new Date(fromDate).toISOString(),
      new Date(toDate).toISOString(),
      Array.from(selected)
    )
      .then(setSchedules)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  const toggleSelected = (keyName) => {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(keyName)) {
        next.delete(keyName);
      } else {
        next.add(keyName);
      }
      return next;
    });
  };

  const selectAll = () => {
    setSelected(new Set(filteredHeaders.map((header) => header.keyName)));
  };

  const selectNone = () => {
    setSelected(new Set());
  };

  const toggleRunSelected = (scheduleId) => {
    setSelectedRunId((current) => (current === scheduleId ? null : scheduleId));
  };

  const normalizedFilter = filter.trim().toLowerCase();
  const filteredHeaders = headers.filter((header) => {
    if (onlyEnabled && !header.enabled) {
      return false;
    }
    if (normalizedFilter && !header.keyName.toLowerCase().includes(normalizedFilter)) {
      return false;
    }
    return true;
  });

  return (
    <section className="section">
      <h2>Schedules</h2>
      <div className="catalogs-layout" ref={containerRef}>
        <div
          className="catalog-pane schedule-names-pane"
          style={{ width: `${namesPercent}%` }}
        >
          <h3 className="schedule-pane-title">Schedule names</h3>
          <div className="schedule-names-filter">
            <div className="states-filter-field">
              <label>Filter</label>
              <input
                type="text"
                value={filter}
                placeholder="Search by name"
                onChange={(e) => setFilter(e.target.value)}
              />
            </div>
            <div className="schedule-select-buttons">
              <button className="states-filter-button" onClick={selectAll}>All</button>
              <button className="states-filter-button" onClick={selectNone}>None</button>
            </div>
            <label className="schedule-enabled-filter">
              <input
                type="checkbox"
                checked={onlyEnabled}
                onChange={(e) => setOnlyEnabled(e.target.checked)}
              />
              Enabled only
            </label>
          </div>
          {headersError && <div className="error-box">Error: {headersError}</div>}
          {!headersError && headers.length === 0 && (
            <div className="empty-box">No schedule names found.</div>
          )}
          {!headersError && filteredHeaders.length === 0 && headers.length > 0 && (
            <div className="empty-box">No schedule names match the filter.</div>
          )}
          {!headersError && filteredHeaders.length > 0 && (
            <ul className="schedule-names-list">
              {filteredHeaders.map((header) => (
                <li key={header.keyName}>
                  <label className="schedule-name-row">
                    <input
                      type="checkbox"
                      checked={selected.has(header.keyName)}
                      onChange={() => toggleSelected(header.keyName)}
                    />
                    <span className="schedule-name-label" title={header.keyName}>
                      {header.keyName}
                    </span>
                    {!header.enabled && (
                      <span className="tree-badge">disabled</span>
                    )}
                  </label>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div
          className={`catalog-splitter ${dragging ? 'catalog-splitter--dragging' : ''}`}
          onMouseDown={(e) => {
            e.preventDefault();
            setDragging(true);
          }}
        />
        <div className="catalog-pane catalog-pane--tabs">
          <div className="schedule-runs-pane">
            <h3 className="schedule-pane-title">Schedule runs</h3>
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
                    <tr
                      key={schedule.id}
                      className={`runs-row${selectedRunId === schedule.id ? ' runs-row--selected' : ''}`}
                      onClick={() => toggleRunSelected(schedule.id)}
                    >
                      <td>{schedule.id}</td>
                      <td>{schedule.configScheduleKeyName}</td>
                      <td>{schedule.targetExecutionDateTime}</td>
                      <td>{schedule.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
      <PipelineSection instanceId={selectedRunId} />
    </section>
  );
}
