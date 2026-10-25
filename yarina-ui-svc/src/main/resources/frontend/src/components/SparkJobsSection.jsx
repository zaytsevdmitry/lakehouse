import React, { useEffect, useRef, useState } from 'react';
import {
  fetchSparkSubmissions,
  fetchSparkProperties,
  createSparkSubmission,
  fetchSparkStatus,
  killSparkSubmission,
  killAllSparkSubmissions,
  clearSparkCompleted,
} from '../api.js';

const EXTERNAL_STATUSES = [
  'WAITING',
  'SUBMITTED',
  'RUNNING',
  'FINISHED',
  'FAILED',
  'ERROR',
  'KILLED',
  'UNKNOWN',
];

const PAGE_SIZES = [20, 40, 60, 100];

const TABLE_MIN_PERCENT = 30;
const TABLE_MAX_PERCENT = 90;
const TABLE_DEFAULT_PERCENT = 70;

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

export default function SparkJobsSection() {
  const containerRef = useRef(null);
  const initialDates = getInitialDates();
  const [fromDate, setFromDate] = useState(initialDates.fromDate);
  const [toDate, setToDate] = useState(initialDates.toDate);
  const [submissionId, setSubmissionId] = useState('');
  const [status, setStatus] = useState('');
  const [pageSize, setPageSize] = useState(PAGE_SIZES[0]);
  const [tablePercent, setTablePercent] = useState(TABLE_DEFAULT_PERCENT);
  const [dragging, setDragging] = useState(false);

  const [items, setItems] = useState([]);
  const [meta, setMeta] = useState(null);
  const [lastId, setLastId] = useState(null);
  const [cursorHistory, setCursorHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [selectedId, setSelectedId] = useState(null);
  const [selectedRow, setSelectedRow] = useState(null);
  const [sparkProperties, setSparkProperties] = useState('');
  const [propertiesLoading, setPropertiesLoading] = useState(false);
  const [propertiesError, setPropertiesError] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({
    appResource: '',
    mainClass: '',
    clientSparkVersion: '',
    appArgs: '',
    sparkProperties: '',
  });
  const [actionResult, setActionResult] = useState('');

  useEffect(() => {
    if (!dragging) return;
    const handleMove = (e) => {
      const rect = containerRef.current.getBoundingClientRect();
      if (rect.width === 0) return;
      let percent = ((e.clientX - rect.left) / rect.width) * 100;
      percent = Math.min(TABLE_MAX_PERCENT, Math.max(TABLE_MIN_PERCENT, percent));
      setTablePercent(percent);
    };
    const handleUp = () => setDragging(false);
    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [dragging]);

  const load = (opts = {}) => {
    const { nextLastId = null, size = pageSize, history = [] } = opts;
    setLoading(true);
    setError('');
    fetchSparkSubmissions({
      limit: size,
      lastId: nextLastId,
      id: submissionId ? Number(submissionId) : undefined,
      status: status || undefined,
      dateFrom: fromDate ? new Date(fromDate).toISOString() : undefined,
      dateTo: toDate ? new Date(toDate).toISOString() : undefined,
    })
      .then((response) => {
        setItems(response.items);
        setMeta(response.meta);
        setLastId(nextLastId);
        setCursorHistory(history);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  const refresh = () => load();

  const nextPage = () => {
    if (!meta || !meta.next_cursor) return;
    load({
      nextLastId: meta.next_cursor,
      size: pageSize,
      history: [...cursorHistory, lastId],
    });
  };

  const prevPage = () => {
    if (cursorHistory.length === 0) return;
    const newHistory = cursorHistory.slice(0, -1);
    const prevCursor = cursorHistory[cursorHistory.length - 1];
    load({ nextLastId: prevCursor, size: pageSize, history: newHistory });
  };

  const changePageSize = (size) => {
    setPageSize(size);
    load({ nextLastId: null, size, history: [] });
  };

  const selectRow = (row) => {
    setSelectedId(row.id);
    setSelectedRow(row);
    setSparkProperties('');
    setPropertiesError('');
    setPropertiesLoading(true);
    fetchSparkProperties(row.id)
      .then((dto) => {
        setSparkProperties(
          dto && dto.spark_properties != null
            ? JSON.stringify(dto.spark_properties, null, 2)
            : ''
        );
      })
      .catch((e) => setPropertiesError(e.message))
      .finally(() => setPropertiesLoading(false));
  };

  const targetId = selectedId;

  const runAction = (label, action) => {
    setActionResult('');
    Promise.resolve()
      .then(action)
      .then((result) => setActionResult(`${label}: ${JSON.stringify(result)}`))
      .catch((e) => setActionResult(`${label} error: ${e.message}`));
  };

  const refreshRow = (rowId) => {
    fetchSparkSubmissions({ id: rowId })
      .then((response) => {
        const updated = response.items && response.items[0];
        if (!updated) return;
        setItems((current) => current.map((it) => (it.id === rowId ? updated : it)));
        setSelectedRow((current) => (current && current.id === rowId ? updated : current));
      })
      .catch(() => {});
  };

  const updateStatus = () => {
    if (targetId == null) return;
    runAction('StatusResponse', () => fetchSparkStatus(targetId));
    refreshRow(targetId);
  };

  const submitCreate = (e) => {
    e.preventDefault();
    runAction('CreateSubmissionResponse', () =>
      createSparkSubmission({
        action: 'CreateSubmissionRequest',
        appArgs: parseJsonList(createForm.appArgs),
        appResource: createForm.appResource,
        clientSparkVersion: createForm.clientSparkVersion || null,
        mainClass: createForm.mainClass,
        sparkProperties: parseJsonObject(createForm.sparkProperties),
        environmentVariables: {},
      })
    );
  };

  const cancelCreate = () => {
    setCreateOpen(false);
    setCreateForm({
      appResource: '',
      mainClass: '',
      clientSparkVersion: '',
      appArgs: '',
      sparkProperties: '',
    });
  };

  return (
    <section className="section">
      <h2>Spark Jobs</h2>
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
          <label>Id</label>
          <input
            type="text"
            value={submissionId}
            placeholder="numeric id"
            onChange={(e) => setSubmissionId(e.target.value)}
          />
        </div>
        <div className="states-filter-field">
          <label>Status</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All</option>
            {EXTERNAL_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <button className="states-filter-button" onClick={refresh} disabled={loading}>
          Refresh
        </button>
      </div>

      {error && <div className="error-box">Error: {error}</div>}
      {loading && <div className="empty-box">Loading...</div>}

      <div className="spark-actions">
        <button
          className="states-filter-button"
          onClick={() => setCreateOpen((open) => !open)}
        >
          Create
        </button>
        <button
          className="states-filter-button"
          disabled={targetId == null}
          onClick={updateStatus}
        >
          Status
        </button>
        <button
          className="states-filter-button"
          disabled={targetId == null}
          onClick={() =>
            runAction('KillResponse', () => killSparkSubmission(targetId))
          }
        >
          Kill
        </button>
        <button
          className="states-filter-button"
          onClick={() =>
            runAction('KillAllResponse', () => killAllSparkSubmissions())
          }
        >
          Kill All
        </button>
        <button
          className="states-filter-button"
          onClick={() =>
            runAction('ClearResponse', () => clearSparkCompleted())
          }
        >
          Clear
        </button>
      </div>

      <div className="catalogs-layout" ref={containerRef}>
        <div
          className="catalog-pane spark-table-pane"
          style={{ width: `${tablePercent}%` }}
        >
          {!error && !loading && items.length === 0 && (
            <div className="empty-box">No submissions found.</div>
          )}
          {!error && !loading && items.length > 0 && (
            <table className="states-table">
              <thead>
                <tr>
                  <th>Id</th>
                  <th>SubmissionId</th>
                  <th>Status</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {items.map((row) => (
                  <tr
                    key={row.id}
                    className={`runs-row${selectedId === row.id ? ' runs-row--selected' : ''}`}
                    onClick={() => selectRow(row)}
                  >
                    <td>{row.id}</td>
                    <td>{row.submission_id}</td>
                    <td>{row.status}</td>
                    <td>{row.created_at}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <div className="spark-pagination">
            <label className="states-filter-field">
              <span>Rows per page</span>
              <select value={pageSize} onChange={(e) => changePageSize(Number(e.target.value))}>
                {PAGE_SIZES.map((size) => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
            </label>
            <button
              className="states-filter-button"
              onClick={prevPage}
              disabled={cursorHistory.length === 0 || loading}
            >
              Prev
            </button>
            <button
              className="states-filter-button"
              onClick={nextPage}
              disabled={!meta || !meta.has_more || loading}
            >
              Next
            </button>
          </div>

          {createOpen && (
            <form className="spark-create-form" onSubmit={submitCreate}>
              <div className="states-filter-field">
                <label>App resource</label>
                <input
                  type="text"
                  value={createForm.appResource}
                  onChange={(e) =>
                    setCreateForm({ ...createForm, appResource: e.target.value })
                  }
                />
              </div>
              <div className="states-filter-field">
                <label>Main class</label>
                <input
                  type="text"
                  value={createForm.mainClass}
                  onChange={(e) =>
                    setCreateForm({ ...createForm, mainClass: e.target.value })
                  }
                />
              </div>
              <div className="states-filter-field">
                <label>Client Spark version</label>
                <input
                  type="text"
                  value={createForm.clientSparkVersion}
                  onChange={(e) =>
                    setCreateForm({ ...createForm, clientSparkVersion: e.target.value })
                  }
                />
              </div>
              <div className="states-filter-field">
                <label>App args (JSON array)</label>
                <input
                  type="text"
                  value={createForm.appArgs}
                  onChange={(e) =>
                    setCreateForm({ ...createForm, appArgs: e.target.value })
                  }
                />
              </div>
              <div className="states-filter-field">
                <label>Spark properties (JSON object)</label>
                <input
                  type="text"
                  value={createForm.sparkProperties}
                  onChange={(e) =>
                    setCreateForm({ ...createForm, sparkProperties: e.target.value })
                  }
                />
              </div>
              <div className="spark-create-actions">
                <button className="states-filter-button" type="submit">
                  Submit
                </button>
                <button
                  className="states-filter-button spark-button--secondary"
                  type="button"
                  onClick={cancelCreate}
                >
                  Cancel
                </button>
              </div>
            </form>
          )}

          {actionResult && <div className="spark-action-result">{actionResult}</div>}
        </div>

        <div
          className={`catalog-splitter ${dragging ? 'catalog-splitter--dragging' : ''}`}
          onMouseDown={(e) => {
            e.preventDefault();
            setDragging(true);
          }}
        />

        <div className="catalog-pane spark-details-pane">
          <h3 className="schedule-pane-title">Submission details</h3>
          <div className="spark-details-field">
            <label>App resource</label>
            <input
              type="text"
              readOnly
              value={selectedRow ? selectedRow.app_resource || '' : ''}
            />
          </div>
          <div className="spark-details-field">
            <label>Main class</label>
            <input
              type="text"
              readOnly
              value={selectedRow ? selectedRow.main_class || '' : ''}
            />
          </div>
          {propertiesLoading && <div className="empty-box">Loading spark properties...</div>}
          {propertiesError && <div className="error-box">Error: {propertiesError}</div>}
          <textarea
            className="spark-details-textarea"
            readOnly
            value={selectedId == null ? 'Select a submission row to view spark properties.' : sparkProperties}
          />
        </div>
      </div>
    </section>
  );
}

function parseJsonList(value) {
  if (!value || !value.trim()) {
    return [];
  }
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter((item) => item.length > 0);
  }
}

function parseJsonObject(value) {
  if (!value || !value.trim()) {
    return {};
  }
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}
