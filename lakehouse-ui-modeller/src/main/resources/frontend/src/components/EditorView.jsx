import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { api, encodePath } from '../api';
import { parseYaml, stringifyYaml } from '../yaml';
import FormEditor from './FormEditor';
import Modal from './Modal';

const basename = (path) => (path || '').split('/').pop();

export default function EditorView({ session, profile, workspace, onBack, onNotice }) {
  const token = session?.accessToken;
  const wsId = workspace.id;
  const canEdit = profile.effectiveRole === 'EDITOR' || profile.effectiveRole === 'ADMIN';
  const readOnly = !canEdit || !workspace.own;

  const [schemas, setSchemas] = useState([]);
  const [tree, setTree] = useState([]);
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
  const [confirmSave, setConfirmSave] = useState(false);
  const [pendingOpen, setPendingOpen] = useState(null);

  const schemaMap = useMemo(() => Object.fromEntries(schemas.map((s) => [s.kind, s])), [schemas]);

  const loadTree = useCallback(async () => {
    const data = await api(`/v1_0/workspaces/${encodePath(wsId)}/tree`, { token });
    setTree(Array.isArray(data) ? data : []);
  }, [wsId, token]);

  useEffect(() => {
    (async () => {
      try {
        const [schemaData] = await Promise.all([api('/v1_0/schema', { token }), loadTree()]);
        setSchemas(Array.isArray(schemaData) ? schemaData : []);
      } catch (e) {
        onNotice('error', e.message);
      }
    })();
  }, [loadTree, token, onNotice]);

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
      const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`, { token });
      setSelected({ path: resp.path || entry.path, kind: resp.kind, keyName: resp.keyName || basename(resp.path) });
      setSelectedFolder(null);
      setYaml(resp.yaml);
      setDoc(parseYaml(resp.yaml));
      setKeyNameEditable(resp.isKeyNameEditable);
      setMode(schemaMap[resp.kind] ? 'form' : 'yaml');
      setDirty(false);
    } catch (e) {
      onNotice('error', e.message);
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
        token,
        body: { path: selected.path, yaml: yamlToSave, keyName: selected.keyName },
      });
      setYaml(resp.yaml);
      if (mode === 'form') setDoc(parseYaml(resp.yaml));
      setKeyNameEditable(resp.isKeyNameEditable);
      setDirty(false);
      await loadTree();
      onNotice('success', `Saved ${selected.keyName}.`);
      return true;
    } catch (e) {
      onNotice('error', e.message);
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
    setBusy(true);
    try {
      const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files`, {
        method: 'POST',
        token,
        body: { kind, keyName },
      });
      setNewModal(false);
      await loadTree();
      onNotice('success', `Created ${kind} "${keyName}".`);
      await doOpen({ path: resp.path, kind: resp.kind, keyName: resp.keyName });
    } catch (e) {
      onNotice('error', e.message);
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
        token,
      });
      setDeleteModal(false);
      setSelected(null);
      setDoc(null);
      setDirty(false);
      await loadTree();
      onNotice('success', `Deleted ${selected.keyName}.`);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setBusy(false);
    }
  };

  const submitReview = async ({ comment, commitMessage }) => {
    setBusy(true);
    try {
      const resp = await api(`/v1_0/vcs/review/${encodePath(wsId)}`, {
        method: 'POST',
        token,
        body: { comment, commitMessage },
      });
      setReviewModal(false);
      setDirty(false);
      onNotice('success', `Review submitted: ${resp.status}${resp.url ? ` — ${resp.url}` : ''}`);
      window.setTimeout(onBack, 1200);
    } catch (e) {
      const gone = /does not exist|was deleted|not found/i.test(e.message);
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
  }, [tree, filter]);

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
        const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`, { token });
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
  }, [tree, wsId, token]);

  const scriptSummaryProvider = useCallback(async () => {
    const results = [];
    for (const entry of tree) {
      if (entry.kind !== 'Script') continue;
      try {
        const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/${encodePath(entry.path)}`, { token });
        const doc = parseYaml(resp.yaml || '');
        if (!doc || !doc.key) continue;
        results.push({ key: doc.key, value: doc.value || '' });
      } catch (e) {
        // unreadable script — skip it for the picker
      }
    }
    return results;
  }, [tree, wsId, token]);

  const renameFile = async (newName) => {
    if (!selected) return;
    setBusy(true);
    try {
      const resp = await api(`/v1_0/workspaces/${encodePath(wsId)}/files/rename`, {
        method: 'POST',
        token,
        body: { path: selected.path, newName: newName.trim() },
      });
      setRenameModal(false);
      await loadTree();
      await doOpen({ path: resp.path, kind: resp.kind, keyName: resp.keyName });
      onNotice('success', `Renamed to ${basename(resp.path)}.`);
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
        token,
        body: { path: target },
      });
      await loadTree();
      if (selected) await doOpen({ path: selected.path });
      setSelectedFolder(null);
      onNotice('success', resp && resp.restored != null ? `Restored ${resp.restored} file(s).` : 'Restored.');
    } catch (e) {
      onNotice('error', e.message);
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
        <div className="tree">
          {filter.trim().length > 0 && filter.trim().length < 3 && (
            <p className="muted empty-hint">Type at least 3 characters to filter.</p>
          )}
          {filter.trim().length >= 3 && tree.length > 0 && (folderNames.length === 0 && rootFiles.length === 0) && (
            <p className="muted empty-hint">No files matching "{filter.trim()}".</p>
          )}
          {folderNames.map((name) => (
            <Folder
              key={name}
              node={folderTree.children[name]}
              dirPath={name}
              selectedPath={selected?.path}
              selectedFolder={selectedFolder}
              onOpen={openFile}
              onSelectFolder={setSelectedFolder}
            />
          ))}
          {rootFiles.map((entry) => (
            <button
              key={entry.path}
              className={`tree-item${selected && selected.path === entry.path ? ' active' : ''}`}
              onClick={() => openFile(entry)}
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
                <FormEditor
                  schema={activeSchema}
                  value={doc || {}}
                  onChange={(next) => { setDoc(next); setDirty(true); }}
                  readOnly={readOnly}
                  keyNameEditable={keyNameEditable && !readOnly}
                  uniqueByField={uniqueByField}
                  dataSetSummaryProvider={dataSetSummaryProvider}
                  scriptSummaryProvider={scriptSummaryProvider}
                />
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
          onClose={() => setNewModal(false)}
          onSubmit={createFile}
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

function Folder({ node, dirPath, selectedPath, selectedFolder, onOpen, onSelectFolder }) {
  const [open, setOpen] = useState(true);
  const names = Object.keys(node.children).sort();
  const files = [...node.files].sort((a, b) => a.path.localeCompare(b.path));
  const active = dirPath === selectedFolder;
  return (
    <div className="tree-folder">
      <button
        className={`tree-folder-name${open ? ' open' : ''}${active ? ' active' : ''}`}
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
            />
          ))}
          {files.map((entry) => (
            <button
              key={entry.path}
              className={`tree-item${selectedPath === entry.path ? ' active' : ''}`}
              onClick={() => onOpen(entry)}
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

function NewFileModal({ schemas, onClose, onSubmit, busy }) {
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