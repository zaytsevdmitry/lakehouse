import React, { useCallback, useEffect, useRef, useState } from 'react';
import { fetchConstraints, fetchDataSet, fetchDataSource, fetchStates } from '../api.js';
import LineageTab from './LineageTab.jsx';
import ModelTab from './ModelTab.jsx';
import RelationsTab from './RelationsTab.jsx';

const TREE_MIN_PERCENT = 20;
const TREE_MAX_PERCENT = 80;
const TREE_DEFAULT_PERCENT = 30;

function TreeNode({ node, depth, selected, onSelect }) {
  const [expanded, setExpanded] = useState(depth === 0);
  const hasChildren = node.children && node.children.length > 0;
  const isSelected = selected && selected.nodeId === node.nodeId;

  const handleClick = (e) => {
    e.stopPropagation();
    if (hasChildren) {
      setExpanded(!expanded);
    }
    if (onSelect) {
      onSelect(node);
    }
  };

  return (
    <div className="tree-node">
      <div
        className={`tree-row ${hasChildren ? 'tree-row--branch' : ''} ${isSelected ? 'tree-row--selected' : ''}`}
        style={{ paddingLeft: `${depth * 20 + 8}px` }}
        onClick={handleClick}
        title={node.label}
      >
        <span className="tree-chevron">
          {hasChildren ? (expanded ? '\u25BC' : '\u25B6') : ''}
        </span>
        <span className="tree-icon">{node.icon}</span>
        <span className="tree-label">{node.label}</span>
        {node.badge != null && <span className="tree-badge">{node.badge}</span>}
      </div>
      {expanded &&
        hasChildren &&
        node.children.map((child) => (
          <TreeNode
            key={child.nodeId}
            node={child}
            depth={depth + 1}
            selected={selected}
            onSelect={onSelect}
          />
        ))}
    </div>
  );
}

function toDateInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getInitialDates() {
  const today = new Date();
  const from = new Date();
  from.setMonth(from.getMonth() - 1);
  return { fromDate: toDateInputValue(from), toDate: toDateInputValue(today) };
}

function Field({ label, value }) {
  return (
    <div className="dataset-field">
      <label>{label}</label>
      <input type="text" value={value || ''} readOnly />
    </div>
  );
}

function ServicePropertiesTable({ properties }) {
  const [filter, setFilter] = useState('');
  const entries = Object.entries(properties || {}).filter(([key, value]) => {
    const needle = filter.trim().toLowerCase();
    if (!needle) return true;
    return String(key).toLowerCase().includes(needle) || String(value).toLowerCase().includes(needle);
  });

  return (
    <div className="service-properties">
      <input
        className="service-properties-filter"
        type="text"
        placeholder="Filter key / value..."
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
      />
      <table className="states-table">
        <thead>
          <tr>
            <th>Key</th>
            <th>Value</th>
          </tr>
        </thead>
        <tbody>
          {entries.length === 0 ? (
            <tr>
              <td colSpan={2}>No properties found.</td>
            </tr>
          ) : (
            entries.map(([key, value]) => (
              <tr key={key}>
                <td>{key}</td>
                <td>{value}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

function DataSourcePanel({ dataSourceKeyName }) {
  const [dataSource, setDataSource] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('datasource');

  useEffect(() => {
    if (!dataSourceKeyName) {
      setDataSource(null);
      return;
    }
    setLoading(true);
    setError('');
    fetchDataSource(dataSourceKeyName)
      .then(setDataSource)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [dataSourceKeyName]);

  if (!dataSourceKeyName) {
    return <div className="empty-box">Select a data source in the tree.</div>;
  }
  if (loading) {
    return <div className="empty-box">Loading...</div>;
  }
  if (error) {
    return <div className="error-box">Error: {error}</div>;
  }
  if (!dataSource) {
    return <div className="empty-box">No data source found.</div>;
  }
  const service = dataSource.service;
  return (
    <div className="tabs">
      <div className="tab-list">
        <button
          className={`tab ${activeTab === 'datasource' ? 'tab--active' : ''}`}
          onClick={() => setActiveTab('datasource')}
        >
          DataSource
        </button>
        <button
          className={`tab ${activeTab === 'service' ? 'tab--active' : ''}`}
          onClick={() => setActiveTab('service')}
        >
          Service
        </button>
      </div>
      <div className="tab-content">
        {activeTab === 'datasource' && (
          <div className="dataset-fields">
            <Field label="Key name" value={dataSource.keyName} />
            <Field label="Description" value={dataSource.description} />
            <Field label="Database protocol" value={dataSource.databaseProtocol} />
            <Field label="Data source type" value={dataSource.dataSourceType} />
          </div>
        )}
        {activeTab === 'service' &&
          (service ? (
            <div className="dataset-fields">
              <Field label="Host" value={service.host} />
              <Field label="Port" value={service.port} />
              <Field label="Urn" value={service.urn} />
            </div>
          ) : (
            <div className="empty-box">No service found.</div>
          ))}
        {activeTab === 'service' && service && <ServicePropertiesTable properties={service.properties} />}
      </div>
    </div>
  );
}

function SchemaPanel({ schema }) {
  return (
    <div className="tabs">
      <div className="tab-list">
        <button className="tab tab--active">Schema</button>
      </div>
      <div className="tab-content">
        <div className="dataset-fields">
          <Field label="Key name" value={schema.keyName} />
          <Field label="Data source" value={schema.dataSourceKeyName} />
          <Field label="Database schema" value={schema.databaseSchemaName} />
        </div>
      </div>
    </div>
  );
}

function StatesTab({ dataSetKeyName }) {
  const initialDates = getInitialDates();
  const [fromDate, setFromDate] = useState(initialDates.fromDate);
  const [toDate, setToDate] = useState(initialDates.toDate);
  const [states, setStates] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = () => {
    if (!dataSetKeyName) return;
    setLoading(true);
    setError('');
    fetchStates(dataSetKeyName, fromDate, toDate)
      .then(setStates)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  return (
    <div className="states-tab">
      <div className="states-filter">
        <div className="states-filter-field">
          <label>From</label>
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </div>
        <div className="states-filter-field">
          <label>To</label>
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </div>
        <button
          className="states-filter-button"
          onClick={load}
          disabled={!dataSetKeyName || loading}
        >
          Show states
        </button>
      </div>
      {error && <div className="error-box">Error: {error}</div>}
      {loading && <div className="empty-box">Loading...</div>}
      {!error && !loading && states.length === 0 && (
        <div className="empty-box">No states found.</div>
      )}
      {!error && !loading && states.length > 0 && (
        <table className="states-table">
          <thead>
            <tr>
              <th>Dataset</th>
              <th>From</th>
              <th>To</th>
              <th>Status</th>
              <th>Lock source</th>
            </tr>
          </thead>
          <tbody>
            {states.map((state, idx) => (
              <tr key={idx}>
                <td>{state.dataSetKeyName}</td>
                <td>{state.intervalStartDateTime}</td>
                <td>{state.intervalEndDateTime}</td>
                <td>{state.status}</td>
                <td>{state.lockSource}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function ColumnsTab({ dataSet }) {
  const columns = (dataSet && dataSet.columnSchema) || [];
  if (!dataSet) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }
  if (columns.length === 0) {
    return <div className="empty-box">No columns found.</div>;
  }
  return (
    <table className="states-table">
      <thead>
        <tr>
          <th>Order</th>
          <th>Name</th>
          <th>Data type</th>
          <th>Nullable</th>
          <th>Sequence</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        {columns.map((column, idx) => (
          <tr key={idx}>
            <td>{column.order != null ? column.order : ''}</td>
            <td>{column.name}</td>
            <td>{column.dataType}</td>
            <td>{column.nullable ? 'true' : 'false'}</td>
            <td>{column.sequence ? 'true' : 'false'}</td>
            <td>{column.description || ''}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function ConstraintsTab({ dataSetKeyName }) {
  const [constraints, setConstraints] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!dataSetKeyName) {
      setConstraints([]);
      return;
    }
    setLoading(true);
    setError('');
    fetchConstraints(dataSetKeyName)
      .then(setConstraints)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [dataSetKeyName]);

  if (!dataSetKeyName) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }
  if (loading) {
    return <div className="empty-box">Loading...</div>;
  }
  if (error) {
    return <div className="error-box">Error: {error}</div>;
  }
  if (constraints.length === 0) {
    return <div className="empty-box">No constraints found.</div>;
  }
  return (
    <table className="states-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Type</th>
          <th>Columns</th>
          <th>Enabled</th>
          <th>Level check</th>
          <th>Check expr</th>
          <th>DDL create override</th>
          <th>DDL add override</th>
          <th>Referenced table</th>
          <th>Reference constraint</th>
          <th>On delete</th>
          <th>On update</th>
        </tr>
      </thead>
      <tbody>
        {constraints.map((constraint, idx) => (
          <tr key={idx}>
            <td>{constraint.name}</td>
            <td>{constraint.type}</td>
            <td>{constraint.columns}</td>
            <td>{constraint.enabled ? 'true' : 'false'}</td>
            <td>{constraint.constraintLevelCheck || ''}</td>
            <td>{constraint.checkExpr || ''}</td>
            <td>{constraint.tableConstraintDDLCreateOverride || ''}</td>
            <td>{constraint.tableConstraintDDLAddOverride || ''}</td>
            <td>{constraint.referencedTable || ''}</td>
            <td>{constraint.referenceConstraintName || ''}</td>
            <td>{constraint.onDelete || ''}</td>
            <td>{constraint.onUpdate || ''}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function DatasetFields({ dataSet, loading, error }) {
  if (loading) {
    return <div className="empty-box">Loading...</div>;
  }
  if (error) {
    return <div className="error-box">Error: {error}</div>;
  }
  if (!dataSet) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }
  const fields = [
    ['Key name', dataSet.keyName],
    ['Namespace', dataSet.nameSpaceKeyName],
    ['Data source', dataSet.dataSourceKeyName],
    ['Schema', dataSet.databaseSchemaName],
    ['Table', dataSet.tableName],
    ['Partition stmt', dataSet.partitionStmt],
    ['Description', dataSet.description],
  ];
  return (
    <div className="dataset-fields">
      {fields.map(([label, value]) => (
        <Field key={label} label={label} value={value} />
      ))}
    </div>
  );
}

function TableTabs({ dataSet, loading, error, activeTab, onTabChange }) {
  return (
    <div className="tabs">
      <div className="tab-list">
        <button
          className={`tab ${activeTab === 'dataset' ? 'tab--active' : ''}`}
          onClick={() => onTabChange('dataset')}
        >
          Dataset
        </button>
        <button
          className={`tab ${activeTab === 'states' ? 'tab--active' : ''}`}
          onClick={() => onTabChange('states')}
        >
          States
        </button>
        <button
          className={`tab ${activeTab === 'columns' ? 'tab--active' : ''}`}
          onClick={() => onTabChange('columns')}
        >
          Columns
        </button>
        <button
          className={`tab ${activeTab === 'constraints' ? 'tab--active' : ''}`}
          onClick={() => onTabChange('constraints')}
        >
          Constraints
        </button>
        <button
          className={`tab ${activeTab === 'lineage' ? 'tab--active' : ''}`}
          onClick={() => onTabChange('lineage')}
        >
          Lineage
        </button>
        <button
          className={`tab ${activeTab === 'model' ? 'tab--active' : ''}`}
          onClick={() => onTabChange('model')}
        >
          Model
        </button>
        <button
          className={`tab ${activeTab === 'relations' ? 'tab--active' : ''}`}
          onClick={() => onTabChange('relations')}
        >
          Relations
        </button>
      </div>
      <div className="tab-content">
        {activeTab === 'dataset' && (
          <DatasetFields dataSet={dataSet} loading={loading} error={error} />
        )}
        {activeTab === 'states' && (
          <StatesTab dataSetKeyName={dataSet ? dataSet.keyName : null} />
        )}
        {activeTab === 'columns' && <ColumnsTab dataSet={dataSet} />}
        {activeTab === 'constraints' && (
          <ConstraintsTab dataSetKeyName={dataSet ? dataSet.keyName : null} />
        )}
        {activeTab === 'lineage' && (
          <LineageTab dataSetKeyName={dataSet ? dataSet.keyName : null} />
        )}
        {activeTab === 'model' && <ModelTab dataSet={dataSet} />}
        {activeTab === 'relations' && <RelationsTab dataSet={dataSet} />}
      </div>
    </div>
  );
}

export default function CatalogsSection({ catalogs, error }) {
  const containerRef = useRef(null);
  const [treePercent, setTreePercent] = useState(TREE_DEFAULT_PERCENT);
  const [dragging, setDragging] = useState(false);
  const [selectedNode, setSelectedNode] = useState(null);
  const [dataSet, setDataSet] = useState(null);
  const [dataSetError, setDataSetError] = useState('');
  const [loadingDataSet, setLoadingDataSet] = useState(false);
  const [activeTab, setActiveTab] = useState('dataset');

  useEffect(() => {
    if (!dragging) return;
    const handleMove = (e) => {
      const rect = containerRef.current.getBoundingClientRect();
      if (rect.width === 0) return;
      let percent = ((e.clientX - rect.left) / rect.width) * 100;
      percent = Math.min(TREE_MAX_PERCENT, Math.max(TREE_MIN_PERCENT, percent));
      setTreePercent(percent);
    };
    const handleUp = () => setDragging(false);
    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [dragging]);

  const selectNode = useCallback((node) => {
    setSelectedNode(node);
    setActiveTab('dataset');
    if (node.type === 'table') {
      setLoadingDataSet(true);
      setDataSetError('');
      fetchDataSet(node.keyName)
        .then(setDataSet)
        .catch((e) => setDataSetError(e.message))
        .finally(() => setLoadingDataSet(false));
    }
  }, []);

  const buildTree = (rootNodes) =>
    rootNodes.map((dataSource) => ({
      type: 'datasource',
      keyName: dataSource.keyName,
      nodeId: `datasource/${dataSource.keyName}`,
      label: dataSource.keyName,
      icon: '\u{1F5C4}\u{FE0F}',
      badge: dataSource.badge,
      children: dataSource.children.map((schema) => ({
        type: 'schema',
        keyName: schema.keyName,
        dataSourceKeyName: dataSource.keyName,
        databaseSchemaName: schema.keyName,
        nodeId: `datasource/${dataSource.keyName}/schema/${schema.keyName}`,
        label: schema.keyName,
        icon: '\u{1F4C1}',
        badge: schema.badge,
        children: schema.children.map((table) => ({
          type: 'table',
          keyName: table.keyName,
          nodeId: table.keyName,
          label: table.tableName || table.keyName,
          icon: '\u{1F4BE}',
          badge: table.badge,
          children: [],
        })),
      })),
    }));

  return (
    <section className="section">
      <h2>Catalog</h2>
      {error && <div className="error-box">Error: {error}</div>}
      {!error && catalogs.length === 0 && (
        <div className="empty-box">No catalogs found.</div>
      )}
      {!error && catalogs.length > 0 && (
        <div className="catalogs-layout" ref={containerRef}>
          <div className="catalog-pane catalog-pane--tree" style={{ width: `${treePercent}%` }}>
            <div className="tree">
              {buildTree(catalogs).map((node) => (
                <TreeNode
                  key={node.nodeId}
                  node={node}
                  depth={0}
                  selected={selectedNode}
                  onSelect={selectNode}
                />
              ))}
            </div>
          </div>
          <div
            className={`catalog-splitter ${dragging ? 'catalog-splitter--dragging' : ''}`}
            onMouseDown={(e) => {
              e.preventDefault();
              setDragging(true);
            }}
          />
          <div className="catalog-pane catalog-pane--tabs">
            {!selectedNode && <div className="empty-box">Select an item in the tree.</div>}
            {selectedNode && selectedNode.type === 'datasource' && (
              <DataSourcePanel dataSourceKeyName={selectedNode.keyName} />
            )}
            {selectedNode && selectedNode.type === 'schema' && (
              <SchemaPanel schema={selectedNode} />
            )}
            {selectedNode && selectedNode.type === 'table' && (
              <TableTabs
                dataSet={dataSet}
                loading={loadingDataSet}
                error={dataSetError}
                activeTab={activeTab}
                onTabChange={setActiveTab}
              />
            )}
          </div>
        </div>
      )}
    </section>
  );
}
