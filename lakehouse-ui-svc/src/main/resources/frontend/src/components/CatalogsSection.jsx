import React, { useState } from 'react';

function TreeNode({ nodeId, label, badge, icon, depth, children }) {
  const [expanded, setExpanded] = useState(depth === 0);
  const hasChildren = children && children.length > 0;

  const toggle = (e) => {
    e.stopPropagation();
    setExpanded(!expanded);
  };

  return (
    <div className="tree-node">
      <div
        className={`tree-row ${hasChildren ? 'tree-row--branch' : ''}`}
        style={{ paddingLeft: `${depth * 20 + 8}px` }}
        onClick={toggle}
        title={label}
      >
        <span className="tree-chevron">
          {hasChildren ? (expanded ? '\u25BC' : '\u25B6') : ''}
        </span>
        <span className="tree-icon">{icon}</span>
        <span className="tree-label">{label}</span>
        {badge != null && <span className="tree-badge">{badge}</span>}
      </div>
      {expanded &&
        hasChildren &&
        children.map((child) => (
          <TreeNode key={child.nodeId} {...child} depth={depth + 1} />
        ))}
    </div>
  );
}

export default function CatalogsSection({ catalogs, error }) {
  return (
    <section className="section">
      <h2>Catalogs</h2>
      {error && <div className="error-box">Error: {error}</div>}
      {!error && catalogs.length === 0 && (
        <div className="empty-box">No catalogs found.</div>
      )}
      <div className="tree">
        {!error &&
          catalogs.map((catalog) => {
            const catalogId = `catalog/${catalog.catalogKeyName}`;
            const dataSourceNodes = catalog.dataSources.map((ds) => {
              const dsId = `${catalogId}/datasource/${ds.keyName}`;
              const dataSetNodes = ds.dataSets.map((dset) => ({
                nodeId: `${dsId}/dataset/${dset.keyName}`,
                label: dset.tableName || dset.keyName,
                icon: '\u{1F4BE}',
                badge: dset.databaseSchemaName,
                children: [],
              }));
              return {
                nodeId: dsId,
                label: ds.keyName,
                icon: '\u{1F5C4}\u{FE0F}',
                badge: ds.dataSets.length,
                children: dataSetNodes,
              };
            });
            return (
              <TreeNode
                key={catalogId}
                nodeId={catalogId}
                label={catalog.catalogKeyName}
                icon={'\u{1F4CB}'}
                badge={catalog.dataSources.length}
                depth={0}
                children={dataSourceNodes}
              />
            );
          })}
      </div>
    </section>
  );
}
