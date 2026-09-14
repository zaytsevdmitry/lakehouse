import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { api, encodePath } from '../api';
import { parseYaml, stringifyYaml } from '../yaml';
import FormEditor from './FormEditor';
import ErDiagramEditor from './ErDiagramEditor';
import Modal from './Modal';

const basename = (path) => (path || '').split('/').pop();
const parentDir = (path) => {
  const i = (path || '').lastIndexOf('/');
  return i > 0 ? path.substring(0, i) : '';
};
const DND_MIME = 'application/x-lakehouse-file';
const DND_DIR_MIME = 'application/x-lakehouse-dir';

export default function EditorView({ profile, workspace, onBack, onNotice }) {
  const wsId = workspace.id;
  const canEdit = profile.effectiveRole === 'EDITOR' || profile.effectiveRole === 'ADMIN';
  const readOnly = !canEdit || !workspace.own;

  const [schemas, setSchemas] = useState([]);
  const [tree, setTree] = useState([]);
  const [dirs, setDirs] = useState([]);
  const [selected, setSelected] = useState(null);
  const [selectedFolder, setSelectedFolder] = useState(null);
  const [filter, setFilter] = useState('');
  const [yaml, setYaml] = useState('');
  const [doc, setDoc] = useState(null);
  const [keyNameEditable, setKeyNameEditable] = useState(true);
  const [mode, setMode] = useState('form');
  const [dirty, setDirty] = useState(false);
  const [busy, setBusy] = useState(false);
  const [newModal, setNewModal] = useState(false);
  const [reviewModal, setReviewModal] = useState(false);
  const [deleteModal, setDeleteModal] = useState(false);
  const [renameModal, setRenameModal] = useState(false);
  const [createDirModal, setCreateDirModal] = useState(false);
  const [deleteDirTarget, setDeleteDirTarget] = useState(null);
  const [dragOverRoot, setDragOverRoot] = useState(false);
  const [confirmSave, setConfirmSave] = useState(false);
  const [pendingOpen, setPendingOpen] = useState(null);

  const schemaMap = useMemo(() => Object.fromEntries(schemas.map((s) => [s.kind, s])), [schemas]);

  const loadTree = useCallback(async () => {
    const data = await api(`/v1_0/workspaces/${encodePath(wsId)}/tree`);
    setTree(Array.isArray(data) ? data : []);
  }, [wsId]);

  const loadDirs = useCallback(async () => {
    const data = await api(`/v1_0/workspaces/${encodePath(wsId)}/dirs`);
    setDirs(Array.isArray(data) ? data : []);
  }, [wsId]);

  const refreshTree = useCallback(async () => {
    await Promise.all([loadTree(), loadDirs()]);
  }, [loadTree, loadDirs]);

  const workspaceGone = (message) => /does not exist|was deleted|not found/i.test(message || '');

  const notifyError = (e) => {
    if (workspaceGone(e.message)) {
      setSelected(null);
      setReviewModal(false);
      onNotice('error', 'This workspace no longer exists (it was submitted for review or deleted). Open another one from My workspaces.');
      onBack();
    } else {
      onNotice('error', e.message);
    }
  };

  useEffect(() => {
    (async () => {
      try {
        const [schemaData] = await Promise.all([api('/v1_0/schema'), loadTree(), loadDirs()]);
        setSchemas(Array.isArray(schemaData) ? schemaData : []);
      } catch (e) {
        if (workspaceGone(e.message)) {
          onNotice('error', 'This workspace no longer exists (it was submitted for review or deleted). Open another one from My workspaces.');
          onBack();
        } else {
          onNotice('error', e.message);
        }
      }
    })();
  }, [loadTree, loadDirs, onNotice, onBack]);

  const openFile = (entry) => {
    if (selected && dirty && !readOnly) {
      setPendingOpen(entry);
      return;
    }
    doOpen(entry);
  };

  const doOpen = async (entry) => {
    try {
      setBusy(true);
      const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`);
      setSelected({ path: resp.path || entry.path, kind: resp.kind, keyName: resp.keyName || basename(resp.path) });
      setSelectedFolder(null);
      setYaml(resp.yaml);
      let parsed = null;
      try { parsed = parseYaml(resp.yaml); } catch (e) { parsed = null; }
      setDoc(parsed && typeof parsed === 'object' ? parsed : {});
      setKeyNameEditable(resp.isKeyNameEditable);
      setMode(schemaMap[resp.kind] ? 'form' : 'yaml');
      setDirty(false);
    } catch (e) {
      notifyError(e);
    } finally {
      setBusy(false);
    }
  };

  const save = async () => {
    if (!selected || readOnly) return true;
    const yamlToSave = mode === 'form' ? stringifyYaml(doc) : yaml;
    setBusy(true);
    try {
      const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(selected.path)}`, {
        method: 'PUT',
        body: { path: selected.path, yaml: yamlToSave, keyName: selected.keyName },
      });
      setYaml(resp.yaml);
      if (mode === 'form') setDoc(parseYaml(resp.yaml));
      setKeyNameEditable(resp.isKeyNameEditable);
      setDirty(false);
      await refreshTree();
      onNotice('success', `Saved ${selected.keyName}.`);
      return true;
    } catch (e) {
      if (workspaceGone(e.message)) {
        onNotice('error', 'This workspace no longer exists (it was submitted for review or deleted). Your changes were not saved.');
      } else {
        onNotice('error', e.message);
      }
      return false;
    } finally {
      setBusy(false);
    }
  };

  const requestSave = () => {
    if (selected && dirty && !readOnly) setConfirmSave(true);
  };

  const confirmSaveYes = async () => {
    setConfirmSave(false);
    await save();
  };

  const confirmSwitchYes = async () => {
    const entry = pendingOpen;
    setPendingOpen(null);
    const ok = await save();
    if (ok && entry) doOpen(entry);
  };

  const confirmSwitchNo = () => {
    const entry = pendingOpen;
    setPendingOpen(null);
    if (entry) doOpen(entry);
  };

  const createFile = async ({ kind, keyName }) => {
    const targetDir = selectedFolder || (selected ? parentDir(selected.path) : '');
    setBusy(true);
    try {
      const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files`, {
        method: 'POST',
        body: { kind, keyName, directory: targetDir || undefined },
      });
      setNewModal(false);
      await refreshTree();
      onNotice('success', `Created ${kind} "${keyName}".`);
      setSelectedFolder(targetDir || null);
      await doOpen({ path: resp.path, kind: resp.kind, keyName: resp.keyName });
    } catch (e) {
      notifyError(e);
    } finally {
      setBusy(false);
    }
  };

  const removeFile = async () => {
    if (!selected) return;
    setBusy(true);
    try {
      await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(selected.path)}`, {
        method: 'DELETE',
      });
      setDeleteModal(false);
      setSelected(null);
      setDoc(null);
      setDirty(false);
      await refreshTree();
      onNotice('success', `Deleted ${selected.keyName}.`);
    } catch (e) {
      notifyError(e);
    } finally {
      setBusy(false);
    }
  };

  const submitReview = async ({ comment, commitMessage }) => {
    setBusy(true);
    try {
      const resp = await api(`/v1_0/vcs/review/${encodePath(wsId)}`, {
        method: 'POST',
        body: { comment, commitMessage },
      });
      setReviewModal(false);
      setDirty(false);
      onNotice('success', `Review submitted: ${resp.status}${resp.url ? ` — ${resp.url}` : ''}`);
      window.setTimeout(onBack, 1200);
    } catch (e) {
      const gone = workspaceGone(e.message);
      setReviewModal(false);
      setDirty(false);
      if (gone) {
        onNotice('error', 'This workspace was already submitted or closed. Reopen it from My workspaces to continue editing.');
        window.setTimeout(onBack, 2500);
      } else {
        onNotice('error', e.message);
      }
    } finally {
      setBusy(false);
    }
  };

  const folderTree = useMemo(() => {
    const query = filter.trim().toLowerCase();
    const filterOn = query.length >= 3;
    const root = { name: '', children: {}, files: [] };
    for (const entry of tree) {
      const parts = entry.path.split('/');
      const fileName = parts.pop();
      let node = root;
      for (const seg of parts) {
        if (!node.children[seg]) node.children[seg] = { name: seg, children: {}, files: [] };
        node = node.children[seg];
      }
      node.files.push({ ...entry, label: fileName });
    }
    for (const dir of dirs) {
      const parts = dir.split('/');
      let node = root;
      for (const seg of parts) {
        if (!node.children[seg]) node.children[seg] = { name: seg, children: {}, files: [] };
        node = node.children[seg];
      }
    }
    if (!filterOn) return root;
    const prune = (node) => {
      const keptChildren = {};
      for (const name of Object.keys(node.children)) {
        const child = prune(node.children[name]);
        if (child) keptChildren[name] = child;
      }
      const keptFiles = node.files.filter((f) => f.label.toLowerCase().includes(query));
      if (keptFiles.length === 0 && Object.keys(keptChildren).length === 0) return null;
      return { name: node.name, children: keptChildren, files: keptFiles };
    };
    const pruned = prune(root);
    return pruned || { name: '', children: {}, files: [] };
  }, [tree, dirs, filter]);

  const folderNames = Object.keys(folderTree.children).sort();
  const rootFiles = folderTree.files.sort((a, b) => a.path.localeCompare(b.path));

  const activeSchema = selected ? schemaMap[selected.kind] : null;

  const uniqueByField = useMemo(() => {
    if (!selected || selected.kind !== 'Script') return {};
    const used = new Set();
    for (const e of tree) {
      if (e.kind === 'Script' && e.path !== selected.path && e.keyName) used.add(e.keyName);
    }
    return { key: [...used] };
  }, [tree, selected]);

  const dataSetSummaryProvider = useCallback(async () => {
    const results = [];
    for (const entry of tree) {
      if (entry.kind !== 'DataSet') continue;
      try {
        const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`);
        const doc = parseYaml(resp.yaml || '');
        if (!doc || !doc.keyName) continue;
        const constraints = [];
        if (doc.constraints && typeof doc.constraints === 'object') {
          for (const [key, c] of Object.entries(doc.constraints)) {
            if (c && typeof c === 'object') {
              const type = c.type == null ? 'primary' : String(c.type);
              if (type === 'primary' || type === 'unique') constraints.push({ key, type });
            }
          }
        }
        results.push({ keyName: doc.keyName, constraints });
      } catch (e) {
        // unreadable data set — skip it for the picker
      }
    }
    return results;
  }, [tree, wsId]);

  const scriptSummaryProvider = useCallback(async () => {
    const results = [];
    for (const entry of tree) {
      if (entry.kind !== 'Script') continue;
      try {
        const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`);
        const doc = parseYaml(resp.yaml || '');
        if (!doc || !doc.key) continue;
        results.push({ key: doc.key, value: doc.value || '' });
      } catch (e) {
        // unreadable script — skip it for the picker
      }
    }
    return results;
  }, [tree, wsId]);

  const nameSpaceSummaryProvider = useCallback(async () => {
    const results = [];
    for (const entry of tree) {
      if (entry.kind !== 'NameSpace') continue;
      try {
        const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`);
        const doc = parseYaml(resp.yaml || '');
        if (!doc || !doc.keyName) continue;
        results.push({ keyName: doc.keyName, description: doc.description || '' });
      } catch (e) {
        // unreadable name space — skip it for the picker
      }
    }
    return results;
  }, [tree, wsId]);

  // Catalog providers for the read-only picker fields (Data Source Key Name,
  // Task Template, Task Execution Service Group Name, Driver Key Name). Each
  // lists the identifier+description of every workspace document of a kind.
  const catalogProviders = useMemo(() => {
    const build = (kind, idField) => async () => {
      const results = [];
      for (const entry of tree) {
        if (entry.kind !== kind) continue;
        try {
          const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`);
          const doc = parseYaml(resp.yaml || '');
          if (!doc || !doc[idField]) continue;
          results.push({ [idField]: doc[idField], description: doc.description || '' });
        } catch (e) {
          // unreadable document — skip it for the picker
        }
      }
      return results;
    };
    return {
      dataSource: build('DataSource', 'keyName'),
      task: build('Task', 'name'),
      taskExecutionServiceGroup: build('TaskExecutionServiceGroup', 'name'),
      driver: build('Driver', 'keyName'),
    };
  }, [tree, wsId]);

  const renameFile = async (newName) => {
    if (!selected) return;
    setBusy(true);
    try {
      const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/rename`, {
        method: 'POST',
        body: { path: selected.path, newName: newName.trim() },
      });
      setRenameModal(false);
      await refreshTree();
      await doOpen({ path: resp.path, kind: resp.kind, keyName: resp.keyName });
      onNotice('success', `Renamed to ${basename(resp.path)}.`);
    } catch (e) {
      notifyError(e);
    } finally {
      setBusy(false);
    }
  };

  const moveFile = async (source, targetDirectory) => {
    if (!source) return;
    if (selected && selected.path === source && dirty && !readOnly) {
      onNotice('error', 'Save your changes before moving this file.');
      return;
    }
    setBusy(true);
    try {
      await api(`/v1_0/workspaces/${encodePath(wsId)}/files/move`, {
        method: 'POST',
        body: { source, targetDirectory },
      });
      await refreshTree();
      const newPath = targetDirectory ? `${targetDirectory}/${basename(source)}` : basename(source);
      if (selected && selected.path === source) {
        await doOpen({ path: newPath, kind: selected.kind, keyName: selected.keyName });
        setSelectedFolder(targetDirectory || null);
      }
      onNotice('success', `Moved ${basename(source)} to ${targetDirectory || 'the workspace root'}.`);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setBusy(false);
    }
  };

  const moveDirectory = async (source, targetDirectory) => {
    if (!source) return;
    if (selected && dirty && !readOnly && (selected.path === source || selected.path.startsWith(`${source}/`))) {
      onNotice('error', 'Save your changes before moving this folder.');
      return;
    }
    setBusy(true);
    try {
      await api(`/v1_0/workspaces/${encodePath(wsId)}/dirs/move`, {
        method: 'POST',
        body: { source, targetDirectory },
      });
      await refreshTree();
      const name = source.split('/').pop();
      const newPath = targetDirectory ? `${targetDirectory}/${name}` : name;
      if (selected && selected.path.startsWith(`${source}/`)) {
        const relative = selected.path.substring(source.length + 1);
        await doOpen({ path: `${newPath}/${relative}`, kind: selected.kind, keyName: selected.keyName });
        setSelectedFolder(targetDirectory || null);
      } else {
        setSelectedFolder(null);
      }
      onNotice('success', `Moved ${source} to ${targetDirectory || 'the workspace root'}.`);
    } catch (e) {
      notifyError(e);
    } finally {
      setBusy(false);
    }
  };

  const createDir = async (name) => {
    const target = selectedFolder ? `${selectedFolder}/${name}` : name;
    setBusy(true);
    try {
      await api(`/v1_0/workspaces/${encodePath(wsId)}/dirs`, {
        method: 'POST',
        body: { path: target },
      });
      setCreateDirModal(false);
      await refreshTree();
      setSelectedFolder(target);
      onNotice('success', `Created folder ${target}.`);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setBusy(false);
    }
  };

  const deleteDir = async () => {
    if (!deleteDirTarget) return;
    setBusy(true);
    try {
      await api(`/v1_0/workspaces/${encodePath(wsId)}/dirs/${encodePath(deleteDirTarget)}`, {
        method: 'DELETE',
      });
      setDeleteDirTarget(null);
      setSelectedFolder(null);
      if (selected && selected.path.startsWith(`${deleteDirTarget}/`)) setSelected(null);
      await refreshTree();
      onNotice('success', `Deleted folder ${deleteDirTarget} and its contents.`);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setBusy(false);
    }
  };

  const revert = async () => {
    const target = selected ? selected.path : selectedFolder;
    if (!target || readOnly) return;
    setBusy(true);
    try {
      const resp = await api(`/v1_0/vcs/workspace/${encodePath(wsId)}/restore`, {
        method: 'POST',
        body: { path: target },
      });
      await refreshTree();
      if (selected) await doOpen({ path: selected.path });
      setSelectedFolder(null);
      onNotice('success', resp && resp.restored != null ? `Restored ${resp.restored} file(s).` : 'Restored.');
    } catch (e) {
      notifyError(e);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="editor">
      <aside className="sidebar">
        <div className="sidebar-head">
          <button className="back" onClick={onBack}>← Workspaces</button>
          <div className="sidebar-title">
            <div className="branch-label">{workspace.branch}</div>
            <div className="muted small ws-id">{workspace.id}</div>
          </div>
          {readOnly && <span className="badge viewer">read-only</span>}
          <input
            className="filter-input"
            type="text"
            value={filter}
            placeholder="Filter files… (3+ chars)"
            onChange={(e) => setFilter(e.target.value)}
          />
        </div>
        {!readOnly && (
          <div className="dir-toolbar">
            <button className="btn-mini" title="New folder" onClick={() => setCreateDirModal(true)}>New folder</button>
            <button
              className="btn-mini danger"
              title="Delete the selected folder"
              disabled={!selectedFolder}
              onClick={() => setDeleteDirTarget(selectedFolder)}
            >Delete folder</button>
          </div>
        )}
        <div
          className={`tree${dragOverRoot ? ' drag-over' : ''}`}
          onDragOver={(e) => { e.preventDefault(); setDragOverRoot(true); }}
          onDragLeave={(e) => { e.stopPropagation(); if (e.target === e.currentTarget) setDragOverRoot(false); }}
          onDragEnd={() => setDragOverRoot(false)}
          onDrop={(e) => {
            e.preventDefault();
            setDragOverRoot(false);
            const src = e.dataTransfer.getData(DND_MIME);
            const dirSrc = e.dataTransfer.getData(DND_DIR_MIME);
            if (dirSrc) moveDirectory(dirSrc, '');
            else if (src) moveFile(src, '');
          }}
        >
          {filter.trim().length > 0 && filter.trim().length < 3 && (
            <p className="muted empty-hint">Type at least 3 characters to filter.</p>
          )}
          {filter.trim().length >= 3 && tree.length > 0 && (folderNames.length === 0 && rootFiles.length === 0) && (
            <p className="muted empty-hint">No files matching "{filter.trim()}".</p>
          )}
          <RootFolder
            selectedFolder={selectedFolder}
            onSelectFolder={setSelectedFolder}
            onMove={moveFile}
            onMoveDir={moveDirectory}
          />
          {folderNames.map((name) => (
            <Folder
              key={name}
              node={folderTree.children[name]}
              dirPath={name}
              selectedPath={selected?.path}
              selectedFolder={selectedFolder}
              onOpen={openFile}
              onSelectFolder={setSelectedFolder}
              onMove={moveFile}
              onMoveDir={moveDirectory}
            />
          ))}
          {rootFiles.map((entry) => (
            <button
              key={entry.path}
              className={`tree-item${selected && selected.path === entry.path ? ' active' : ''}`}
              onClick={() => openFile(entry)}
              draggable={!readOnly}
              onDragStart={(e) => {
                e.dataTransfer.setData(DND_MIME, entry.path);
                e.dataTransfer.effectAllowed = 'move';
              }}
            >
              <span className="file-name">{entry.label}</span>
              {entry.modified && <span className="dirty-dot" title="modified" />}
            </button>
          ))}
          {tree.length === 0 && <p className="muted empty-hint">No files in this workspace yet.</p>}
        </div>
        {!readOnly && (
          <div className="sidebar-foot">
            <div className="foot-btns">
              <button className="square primary" title="New file" onClick={() => setNewModal(true)}>+</button>
              <button
                className="square danger"
                title="Delete file"
                disabled={!selected}
                onClick={() => setDeleteModal(true)}
              >-</button>
              <button className="square" title="Rename file" disabled={!selected} onClick={() => setRenameModal(true)}>*</button>
              <button
                className="square"
                title="Revert from VCS"
                disabled={!selected && !selectedFolder}
                onClick={revert}
              >⟲</button>
            </div>
          </div>
        )}
      </aside>

      <section className="main catalog-pane catalog-pane--tabs">
        {!selected ? (
          <div className="empty-state">
            <p className="muted">Select a file from the tree {readOnly ? 'to view it.' : '— or create a new one.'}</p>
          </div>
        ) : (
          <>
            <div className="main-head">
              <div>
                <div className="main-title">{selected.keyName}</div>
                <div className="muted small">{selected.path} · {selected.kind}</div>
              </div>
              <div className="btn-row head-actions">
                {!readOnly && !dirty && (
                  <button onClick={() => { setMode(mode === 'form' ? 'yaml' : 'form'); }}>
                    {mode === 'form' ? 'Raw YAML' : 'Form view'}
                  </button>
                )}
                {!readOnly && dirty && <span className="badge editor">unsaved</span>}
                {!readOnly && (
                  <>
                    <button className="primary" disabled={busy || !dirty} onClick={requestSave}>Save</button>
                    <button className="danger" onClick={() => setDeleteModal(true)}>Delete</button>
                  </>
                )}
                {!readOnly && !dirty && (
                  <button className="primary" disabled={busy} onClick={() => setReviewModal(true)}>Submit review</button>
                )}
              </div>
            </div>

            {mode === 'form' ? (
              activeSchema ? (
                selected.kind === 'ERDiagram' ? (
                  <ErDiagramEditor
                    doc={doc || {}}
                    onDocChange={(next) => { setDoc(next); setDirty(true); }}
                    readOnly={readOnly}
                    tree={tree}
                    wsId={wsId}
                    onNotice={onNotice}
                    schemas={schemas}
                    keyNameEditable={keyNameEditable && !readOnly}
                    uniqueByField={uniqueByField}
                    dataSetSummaryProvider={dataSetSummaryProvider}
                    scriptSummaryProvider={scriptSummaryProvider}
                    nameSpaceSummaryProvider={nameSpaceSummaryProvider}
                    catalogProviders={catalogProviders}
                  />
                ) : (
                  <FormEditor
                    schema={activeSchema}
                    value={doc || {}}
                    onChange={(next) => { setDoc(next); setDirty(true); }}
                    readOnly={readOnly}
                    keyNameEditable={keyNameEditable && !readOnly}
                    uniqueByField={uniqueByField}
                    dataSetSummaryProvider={dataSetSummaryProvider}
                    scriptSummaryProvider={scriptSummaryProvider}
                    nameSpaceSummaryProvider={nameSpaceSummaryProvider}
                    catalogProviders={catalogProviders}
                  />
                )
              ) : (
                <p className="muted">
                  No schema available for kind "{selected.kind}".{' '}
                  {!readOnly && (
                    <button className="link-btn" onClick={() => setMode('yaml')}>Edit raw YAML</button>
                  )}
                </p>
              )
            ) : (
              <textarea
                className="yaml-area"
                spellCheck="false"
                value={yaml}
                readOnly={readOnly}
                onChange={(e) => { setYaml(e.target.value); setDirty(true); }}
              />
            )}
          </>
        )}
      </section>

      {newModal && (
        <NewFileModal
          schemas={schemas}
          directory={selectedFolder || (selected ? parentDir(selected.path) : null)}
          onClose={() => setNewModal(false)}
          onSubmit={createFile}
          busy={busy}
        />
      )}
      {createDirModal && (
        <CreateDirModal onClose={() => setCreateDirModal(false)} onSubmit={createDir} busy={busy} />
      )}
      {deleteDirTarget && (
        <DeleteDirModal
          target={deleteDirTarget}
          onClose={() => setDeleteDirTarget(null)}
          onSubmit={deleteDir}
          busy={busy}
        />
      )}
      {reviewModal && (
        <ReviewModal
          onClose={() => setReviewModal(false)}
          onSubmit={submitReview}
          busy={busy}
        />
      )}
      {renameModal && selected && (
        <RenameModal initial={basename(selected.path)} onClose={() => setRenameModal(false)} onSubmit={renameFile} busy={busy} />
      )}
      {deleteModal && (
        <Modal title="Delete file" onClose={() => setDeleteModal(false)}>
          <p>
            Delete <strong>{selected.keyName}</strong> from this workspace?
          </p>
          <div className="btn-row">
            <button className="danger" disabled={busy} onClick={removeFile}>Delete</button>
            <button onClick={() => setDeleteModal(false)}>Cancel</button>
          </div>
        </Modal>
      )}
      {confirmSave && selected && (
        <Modal title="Save file" onClose={() => setConfirmSave(false)}>
          <p>
            Save changes to <strong>{selected.keyName}</strong>?
          </p>
          <div className="btn-row">
            <button className="primary" autoFocus disabled={busy} onClick={confirmSaveYes}>Yes</button>
            <button onClick={() => setConfirmSave(false)}>No</button>
          </div>
        </Modal>
      )}
      {pendingOpen && selected && (
        <Modal title="Unsaved changes" onClose={() => setPendingOpen(null)}>
          <p>
            <strong>{selected.keyName}</strong> has unsaved changes. Save them before opening{' '}
            <strong>{basename(pendingOpen.path)}</strong>?
          </p>
          <div className="btn-row">
            <button className="primary" autoFocus disabled={busy} onClick={confirmSwitchYes}>Yes</button>
            <button onClick={confirmSwitchNo}>No</button>
          </div>
        </Modal>
      )}
    </div>
  );
}

/**
 * The visible root catalog node. The workspace root is the "highest" directory,
 * so it is a drop target for both files and directories — dropping a nested
 * folder here moves it up to the root (a higher-level catalog). The root itself
 * is not a drag source (it cannot be moved), so it has no onDragStart.
 */
function RootFolder({ selectedFolder, onSelectFolder, onMove, onMoveDir }) {
  const [overs, setOvers] = useState(0);
  const active = selectedFolder === '';

  // Files can always be moved to the root. A directory is moved to the root
  // unless it is already a direct child (no '/' in its path) — that would be a
  // no-op rejected by the server.
  const dragAllows = (e) => {
    const types = Array.from(e.dataTransfer.types || []);
    if (types.includes(DND_DIR_MIME)) {
      const dirSrc = e.dataTransfer.getData(DND_DIR_MIME);
      return dirSrc.includes('/');
    }
    return true;
  };

  const dragEnter = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (dragAllows(e)) setOvers((n) => n + 1);
  };
  const dragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    e.dataTransfer.dropEffect = 'move';
  };
  const dragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setOvers((n) => Math.max(0, n - 1));
  };
  const drop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setOvers(0);
    const dirSrc = e.dataTransfer.getData(DND_DIR_MIME);
    if (dirSrc) {
      if (dirSrc.includes('/')) onMoveDir(dirSrc, '');
      return;
    }
    const src = e.dataTransfer.getData(DND_MIME);
    if (src) onMove(src, '');
  };

  return (
    <div
      className={`tree-folder tree-root-folder${overs > 0 ? ' drag-over' : ''}${active ? ' active' : ''}`}
      onDragEnter={dragEnter}
      onDragOver={dragOver}
      onDragLeave={dragLeave}
      onDrop={drop}
    >
      <button
        className={`tree-folder-name${active ? ' active' : ''}`}
        onClick={() => onSelectFolder('')}
      >
        <span className="caret tree-root-caret" aria-hidden="true">◉</span>
        <span className="tree-root-label">Root</span>
        <span className="folder-count" />
      </button>
    </div>
  );
}

function Folder({ node, dirPath, selectedPath, selectedFolder, onOpen, onSelectFolder, onMove, onMoveDir }) {
  const [open, setOpen] = useState(false);
  const [overs, setOvers] = useState(0);
  const names = Object.keys(node.children).sort();
  const files = [...node.files].sort((a, b) => a.path.localeCompare(b.path));
  const active = dirPath === selectedFolder;

  // A dragged directory cannot be dropped onto itself or into its own subtree,
  // so those targets are not highlighted.
  const dragAllows = (e) => {
    const types = Array.from(e.dataTransfer.types || []);
    if (types.includes(DND_DIR_MIME)) {
      const dirSrc = e.dataTransfer.getData(DND_DIR_MIME);
      // The root is the highest catalog: a nested directory (whose path contains
      // '/') can be moved up into it, which is the "move to a higher directory"
      // case. A directory already at the root has no '/' and is a no-op, so it
      // is not highlighted.
      return dirSrc.includes('/');
    }
    return true;
  };

  const dragEnter = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (dragAllows(e)) setOvers((n) => n + 1);
  };
  const dragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    e.dataTransfer.dropEffect = 'move';
  };
  const dragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setOvers((n) => Math.max(0, n - 1));
  };
  const drop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setOvers(0);
    const dirSrc = e.dataTransfer.getData(DND_DIR_MIME);
    if (dirSrc) {
      if (dirSrc !== dirPath && !dirPath.startsWith(`${dirSrc}/`)) onMoveDir(dirSrc, dirPath);
      return;
    }
    const src = e.dataTransfer.getData(DND_MIME);
    if (src) onMove(src, dirPath);
  };
  return (
    <div
      className={`tree-folder${overs > 0 ? ' drag-over' : ''}`}
      onDragEnter={dragEnter}
      onDragOver={dragOver}
      onDragLeave={dragLeave}
      onDrop={drop}
    >
      <button
        className={`tree-folder-name${open ? ' open' : ''}${active ? ' active' : ''}`}
        draggable={true}
        onDragStart={(e) => {
          e.stopPropagation();
          e.dataTransfer.setData(DND_DIR_MIME, dirPath);
          e.dataTransfer.effectAllowed = 'move';
        }}
        onClick={() => { setOpen(!open); onSelectFolder(dirPath); }}
      >
        <span className="caret">{open ? '▾' : '▸'}</span>
        <span className="key-name">{node.name}</span>
        <span className="folder-count">{files.length + names.length}</span>
      </button>
      {open && (
        <div className="tree-folder-children">
          {names.map((name) => (
            <Folder
              key={name}
              node={node.children[name]}
              dirPath={`${dirPath}/${name}`}
              selectedPath={selectedPath}
              selectedFolder={selectedFolder}
              onOpen={onOpen}
              onSelectFolder={onSelectFolder}
              onMove={onMove}
              onMoveDir={onMoveDir}
            />
          ))}
          {files.map((entry) => (
            <button
              key={entry.path}
              className={`tree-item${selectedPath === entry.path ? ' active' : ''}`}
              onClick={() => onOpen(entry)}
              draggable={true}
              onDragStart={(e) => {
                e.stopPropagation();
                e.dataTransfer.setData(DND_MIME, entry.path);
                e.dataTransfer.effectAllowed = 'move';
              }}
            >
              <span className="file-name">{entry.label}</span>
              {entry.modified && <span className="dirty-dot" title="modified" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function NewFileModal({ schemas, directory, onClose, onSubmit, busy }) {
  const [kind, setKind] = useState(schemas.length ? schemas[0].kind : 'schedule');
  const [keyName, setKeyName] = useState('');
  const submit = (e) => {
    e.preventDefault();
    if (keyName.trim()) onSubmit({ kind, keyName: keyName.trim() });
  };
  return (
    <Modal title="New metadata file" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field">
          <span className="label">Kind</span>
          <select value={kind} onChange={(e) => setKind(e.target.value)}>
            {schemas.map((s) => <option key={s.kind} value={s.kind}>{s.kind}</option>)}
          </select>
        </div>
        <div className="field">
          <span className="label">Key name</span>
          <input value={keyName} onChange={(e) => setKeyName(e.target.value)} placeholder="e.g. my_config" autoFocus />
        </div>
        <div className="field">
          <span className="label">Folder</span>
          <div className="muted small">{directory ? `Will be created in "${directory}/"` : 'Kind default folder'}</div>
        </div>
        <div className="btn-row">
          <button type="submit" className="primary" disabled={busy || !keyName.trim()}>Create</button>
          <button type="button" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}

function RenameModal({ initial, onClose, onSubmit, busy }) {
  const [name, setName] = useState(initial);
  const submit = (e) => {
    e.preventDefault();
    if (name.trim()) onSubmit(name);
  };
  return (
    <Modal title="Rename file" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field">
          <span className="label">New file name</span>
          <input value={name} onChange={(e) => setName(e.target.value)} autoFocus />
        </div>
        <div className="btn-row">
          <button type="submit" className="primary" disabled={busy || !name.trim()}>Rename</button>
          <button type="button" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}

function ReviewModal({ onClose, onSubmit, busy }) {
  const [comment, setComment] = useState('');
  const [commitMessage, setCommitMessage] = useState('');
  const submit = (e) => {
    e.preventDefault();
    onSubmit({ comment, commitMessage });
  };
  return (
    <Modal title="Submit for review" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field">
          <span className="label">Commit message</span>
          <input
            value={commitMessage}
            onChange={(e) => setCommitMessage(e.target.value)}
            placeholder="Describe the change"
          />
        </div>
        <div className="field">
          <span className="label">Review comment (optional)</span>
          <textarea
            rows={4}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Context for reviewers"
          />
        </div>
        <div className="btn-row">
          <button type="submit" className="primary" disabled={busy}>Submit</button>
          <button type="button" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}

function CreateDirModal({ onSubmit, onClose, busy }) {
  const [name, setName] = useState('');
  const submit = (e) => {
    e.preventDefault();
    if (name.trim()) onSubmit(name.trim());
  };
  return (
    <Modal title="New folder" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field">
          <span className="label">Folder name</span>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. archive" autoFocus />
        </div>
        <div className="btn-row">
          <button type="submit" className="primary" disabled={busy || !name.trim()}>Create folder</button>
          <button type="button" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}

function DeleteDirModal({ target, onClose, onSubmit, busy }) {
  return (
    <Modal title="Delete folder" onClose={onClose}>
      <p className="confirm-text">
        Delete folder <strong>{target}</strong> and everything inside it? This cannot be undone.
      </p>
      <div className="btn-row">
        <button type="button" className="danger" disabled={busy} onClick={onSubmit}>Delete folder</button>
        <button type="button" onClick={onClose}>Cancel</button>
      </div>
    </Modal>
  );
}