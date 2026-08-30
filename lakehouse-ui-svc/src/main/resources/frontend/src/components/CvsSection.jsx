import React, { useCallback, useEffect, useRef, useState } from 'react';
import { fetchCvsSyncLogs, fetchCvsObjectLogs } from '../api.js';

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

const LOG_LEFT_MIN_PERCENT = 15;
const LOG_LEFT_MAX_PERCENT = 50;
const LOG_LEFT_DEFAULT_PERCENT = 25;

export default function CvsSection() {
  return (
    <section className="section">
      <h2>CVS</h2>
      <CvsLogPanel />
      <CvsObjectsSearchPanel />
    </section>
  );
}

function CvsLogPanel() {
  const containerRef = useRef(null);
  const initialDates = getInitialDates();
  const [fromDate, setFromDate] = useState(initialDates.fromDate);
  const [toDate, setToDate] = useState(initialDates.toDate);
  const [status, setStatus] = useState('');
  const [commitId, setCommitId] = useState('');
  const [syncLogs, setSyncLogs] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [objectLogs, setObjectLogs] = useState([]);
  const [selectedCommitId, setSelectedCommitId] = useState(null);
  const [objectsError, setObjectsError] = useState('');

  const [leftPercent, setLeftPercent] = useState(LOG_LEFT_DEFAULT_PERCENT);
  const [dragging, setDragging] = useState(false);

  useEffect(() => {
    if (!dragging) return;
    const handleMove = (e) => {
      const rect = containerRef.current.getBoundingClientRect();
      if (rect.width === 0) return;
      let percent = ((e.clientX - rect.left) / rect.width) * 100;
      percent = Math.min(LOG_LEFT_MAX_PERCENT, Math.max(LOG_LEFT_MIN_PERCENT, percent));
      setLeftPercent(percent);
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
    fetchCvsSyncLogs({
      from: new Date(fromDate).toISOString(),
      to: new Date(toDate).toISOString(),
      status: status || null,
      commitId: commitId || null,
    })
      .then(setSyncLogs)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  const selectRow = (row) => {
    setSelectedCommitId(row.commitId);
    setObjectsError('');
    fetchCvsObjectLogs({ commitId: row.commitId })
      .then(setObjectLogs)
      .catch((e) => setObjectsError(e.message));
  };

  return (
    <div className="cvs-panel">
      <h3 className="cvs-panel-title">CVSLog</h3>
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
        <div className="states-filter-field">
          <label>Status</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Any</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
        <div className="states-filter-field">
          <label>CommitId</label>
          <input
            type="text"
            value={commitId}
            placeholder="Commit id"
            onChange={(e) => setCommitId(e.target.value)}
          />
        </div>
        <button className="states-filter-button" onClick={load} disabled={loading}>
          Refresh
        </button>
      </div>
      <div className="cvs-log-layout" ref={containerRef}>
        <div className="catalog-pane" style={{ width: `${leftPercent}%` }}>
          {error && <div className="error-box">Error: {error}</div>}
          {loading && <div className="empty-box">Loading...</div>}
          {!error && !loading && syncLogs.length === 0 && (
            <div className="empty-box">No CVS sync log entries found.</div>
          )}
          {!error && !loading && syncLogs.length > 0 && (
            <table className="states-table">
              <thead>
                <tr>
                  <th>Id</th>
                  <th>CommitId</th>
                  <th>Sync date time</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {syncLogs.map((log) => (
                  <tr
                    key={log.id}
                    className={`runs-row${selectedCommitId === log.commitId ? ' runs-row--selected' : ''}`}
                    onClick={() => selectRow(log)}
                  >
                    <td>{log.id}</td>
                    <td title={log.commitId}>{log.commitId}</td>
                    <td>{log.syncDateTime}</td>
                    <td>{log.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        <div className="catalog-splitter" onMouseDown={(e) => {
          e.preventDefault();
          setDragging(true);
        }} />
        <div className="catalog-pane">
          {objectsError && <div className="error-box">Error: {objectsError}</div>}
          {!selectedCommitId && <div className="empty-box">Select a CVS log row to see its objects.</div>}
          {selectedCommitId && !objectsError && objectLogs.length === 0 && (
            <div className="empty-box">No objects found for commit {selectedCommitId}.</div>
          )}
          {selectedCommitId && objectLogs.length > 0 && (
            <table className="states-table">
              <thead>
                <tr>
                  <th>Id</th>
                  <th>Date time</th>
                  <th>Kind</th>
                  <th>Object name</th>
                  <th>File path</th>
                </tr>
              </thead>
              <tbody>
                {objectLogs.map((obj) => (
                  <tr key={obj.id}>
                    <td>{obj.id}</td>
                    <td>{obj.dateTimeRec}</td>
                    <td>{obj.kind}</td>
                    <td>{obj.objectName}</td>
                    <td>{obj.filePath}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function CvsObjectsSearchPanel() {
  const initialDates = getInitialDates();
  const [kind, setKind] = useState('');
  const [fromDate, setFromDate] = useState(initialDates.fromDate);
  const [toDate, setToDate] = useState(initialDates.toDate);
  const [filePath, setFilePath] = useState('');
  const [objectName, setObjectName] = useState('');
  const [objects, setObjects] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = () => {
    setLoading(true);
    setError('');
    fetchCvsObjectLogs({
      commitId: null,
      kind: kind || null,
      from: new Date(fromDate).toISOString(),
      to: new Date(toDate).toISOString(),
      filePath: filePath || null,
      objectName: objectName || null,
    })
      .then(setObjects)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  return (
    <div className="cvs-panel">
      <h3 className="cvs-panel-title">CVSObjectsSearch</h3>
      <div className="states-filter">
        <div className="states-filter-field">
          <label>Kind</label>
          <input
            type="text"
            value={kind}
            placeholder="Kind"
            onChange={(e) => setKind(e.target.value)}
          />
        </div>
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
        <div className="states-filter-field">
          <label>File path</label>
          <input
            type="text"
            value={filePath}
            placeholder="File path"
            onChange={(e) => setFilePath(e.target.value)}
          />
        </div>
        <div className="states-filter-field">
          <label>Object name</label>
          <input
            type="text"
            value={objectName}
            placeholder="Object name"
            onChange={(e) => setObjectName(e.target.value)}
          />
        </div>
        <button className="states-filter-button" onClick={load} disabled={loading}>
          Refresh
        </button>
      </div>
      {error && <div className="error-box">Error: {error}</div>}
      {loading && <div className="empty-box">Loading...</div>}
      {!error && !loading && objects.length === 0 && (
        <div className="empty-box">No CVS objects found.</div>
      )}
      {!error && !loading && objects.length > 0 && (
        <table className="states-table">
          <thead>
            <tr>
              <th>Id</th>
              <th>Date time</th>
              <th>Kind</th>
              <th>Object name</th>
              <th>File path</th>
              <th>CommitId</th>
            </tr>
          </thead>
          <tbody>
            {objects.map((obj) => (
              <tr key={obj.id}>
                <td>{obj.id}</td>
                <td>{obj.dateTimeRec}</td>
                <td>{obj.kind}</td>
                <td>{obj.objectName}</td>
                <td>{obj.filePath}</td>
                <td title={obj.commitId}>{obj.commitId}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}