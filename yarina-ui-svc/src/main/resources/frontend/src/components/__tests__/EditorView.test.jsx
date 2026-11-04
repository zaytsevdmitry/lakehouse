import React from 'react';
import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import EditorView from '../EditorView';
import { api } from '../../api.js';

vi.mock('../../api.js', () => ({
  api: vi.fn(),
  encodePath: (path) => String(path).split('/').map((part) => encodeURIComponent(part)).join('/'),
}));

const workspace = {
  id: 'ws-1',
  own: true,
  branches: [
    { domain: 'platform', branch: 'main' },
    { domain: 'analytics', branch: 'dev' },
  ],
};

const profile = {
  username: 'tester',
  domains: {
    platform: { viewer: true, editor: true, control: false },
    analytics: { viewer: true, editor: true, control: false },
  },
};

const ORDERS = 'platform (main)/orders.yaml';
const REPORT = 'analytics (dev)/report.yaml';
const METRICS = 'platform (main)/quality/metrics/metrics_file.yaml';
const NOTES = 'platform (main)/notes/readme.txt';

const TREE = [
  { path: ORDERS, kind: 'DataSet', keyName: 'orders' },
  { path: REPORT, kind: 'DataSet', keyName: 'report' },
  { path: METRICS, kind: 'DataSet', keyName: 'metrics_file' },
  { path: NOTES, kind: 'unknown', keyName: NOTES },
];

const DATA_SET_SCHEMA = {
  kind: 'DataSet',
  fields: [
    { name: 'keyName', type: 'string', label: 'Key name', keyName: true },
    {
      name: 'sources',
      type: 'map',
      label: 'Sources',
      children: [
        {
          name: 'properties',
          type: 'map',
          label: 'Properties',
          children: [{ name: '__value__', type: 'string', label: 'Value' }],
        },
      ],
    },
  ],
};

const ORDERS_YAML = [
  'kind: DataSet',
  'keyName: orders',
  'sources:',
  '  jdbc:',
  '    properties:',
  '      url: jdbc:postgresql://one',
  '',
].join('\n');

const REPORT_YAML = [
  'kind: DataSet',
  'keyName: report',
  'sources:',
  '  kafka:',
  '    properties:',
  '      topic: orders',
  '',
].join('\n');

const METRICS_YAML = [
  'kind: DataSet',
  'keyName: metrics_file',
  'sources:',
  '  jdbc:',
  '    properties:',
  '      url: jdbc:postgresql://metrics',
  '',
].join('\n');

let reviewResult;
let moved;
let createdDir;
let createdFile;

function mockApi() {
  api.mockImplementation(async (path, options = {}) => {
    if (path === '/api/schema') return [DATA_SET_SCHEMA];
    if (path.endsWith('/tree')) return TREE;
    if (path.endsWith('/dirs') && options.method === undefined) {
      return ['platform (main)', 'analytics (dev)'];
    }
    if (path.endsWith('/files/move')) {
      moved = options.body;
      return null;
    }
    if (path.endsWith('/dirs/move')) {
      moved = options.body;
      return null;
    }
    if (path.endsWith('/dirs') && options.method === 'POST') {
      createdDir = options.body;
      return null;
    }
    if (path.endsWith('/files') && options.method === 'POST') {
      createdFile = options.body;
      return {
        path: `${options.body.directory}/${options.body.keyName}.yaml`,
        kind: options.body.kind,
        keyName: options.body.keyName,
        yaml: `kind: ${options.body.kind}\nkeyName: ${options.body.keyName}\n`,
      };
    }
    if (String(path).includes('/review/')) return reviewResult;
    if (String(path).endsWith('metrics_file.yaml') && String(path).includes('/files/')) {
      return { path: METRICS, kind: 'DataSet', keyName: 'metrics_file', yaml: METRICS_YAML };
    }
    if (String(path).endsWith('readme.txt') && String(path).includes('/files/')) {
      return { path: NOTES, kind: 'unknown', keyName: null, yaml: 'hello\n' };
    }
    if (String(path).endsWith('orders.yaml') && String(path).includes('/files/')) {
      return { path: ORDERS, kind: 'DataSet', keyName: 'orders', yaml: ORDERS_YAML };
    }
    if (String(path).endsWith('report.yaml') && String(path).includes('/files/')) {
      return { path: REPORT, kind: 'DataSet', keyName: 'report', yaml: REPORT_YAML };
    }
    throw new Error(`unexpected call ${path}`);
  });
}

async function renderEditor() {
  const onBack = vi.fn();
  const onNotice = vi.fn();
  render(<EditorView profile={profile} workspace={workspace} onBack={onBack} onNotice={onNotice} />);
  await waitFor(() => expect(screen.getByText('ws-1')).toBeInTheDocument());
  return { onBack, onNotice };
}

async function openFile(name) {
  const file = await screen.findByRole('button', { name: new RegExp(name) });
  fireEvent.click(file);
  await waitFor(() => expect(screen.getByRole('button', { name: 'Submit review' })).toBeInTheDocument());
}

beforeEach(() => {
  api.mockReset();
  moved = null;
  createdDir = null;
  createdFile = null;
  reviewResult = { status: 'OK', url: 'https://git/merge/1' };
  mockApi();
});

describe('EditorView multi-domain workspace', () => {
  it('creates a new folder inside the first domain folder when nothing is selected', async () => {
    await renderEditor();

    fireEvent.click(screen.getByTitle('New folder'));
    fireEvent.change(await screen.findByPlaceholderText('e.g. archive'), { target: { value: 'archive' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create folder' }));

    await waitFor(() => expect(createdDir).not.toBeNull());
    expect(createdDir.path).toBe('platform (main)/archive');
  });

  it('creates a new file in the selected folder instead of a kind-derived folder', async () => {
    await renderEditor();

    fireEvent.click(await screen.findByRole('button', { name: /platform/ }));
    fireEvent.click(screen.getByTitle('New file'));

    fireEvent.change(await screen.findByPlaceholderText('e.g. my_config'), { target: { value: 'new_orders' } });
    const folder = screen.getByPlaceholderText('e.g. platform (main)/quality/metrics');
    expect(folder).toHaveValue('platform (main)');

    fireEvent.click(screen.getByRole('button', { name: 'Create' }));

    await waitFor(() => expect(createdFile).not.toBeNull());
    expect(createdFile).toEqual({ kind: 'DataSet', keyName: 'new_orders', directory: 'platform (main)' });
  });

  it('picks the editor from the kind field of a file in an arbitrary directory', async () => {
    await renderEditor();

    fireEvent.click(await screen.findByRole('button', { name: /platform/ }));
    fireEvent.click(await screen.findByRole('button', { name: /quality/ }));
    fireEvent.click(await screen.findByRole('button', { name: /metrics/ }));
    fireEvent.click(await screen.findByRole('button', { name: /metrics_file/ }));

    // DataSet form editor, even though the file lives in quality/metrics.
    await waitFor(() => expect(screen.getByRole('button', { name: 'Sources' })).toBeInTheDocument());
    expect(screen.queryByText(/No schema available/)).not.toBeInTheDocument();
  });

  it('opens a file without a metadata kind as a plain text file', async () => {
    await renderEditor();

    fireEvent.click(await screen.findByRole('button', { name: /platform/ }));
    fireEvent.click(await screen.findByRole('button', { name: /notes/ }));
    fireEvent.click(await screen.findByRole('button', { name: /readme/ }));

    await waitFor(() => expect(screen.getByDisplayValue('hello')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'Form view' })).not.toBeInTheDocument();
    expect(screen.getByText(/plain file/)).toBeInTheDocument();
  });

  it('moves a file dropped on the root to the root of its own domain', async () => {
    const { onNotice } = await renderEditor();
    fireEvent.click(await screen.findByRole('button', { name: /analytics/ }));
    const file = await screen.findByRole('button', { name: /report/ });
    const root = screen.getByRole('button', { name: /Root/ }).parentElement;

    const transfer = {
      types: ['application/x-yarina-file'],
      getData: (type) => (type === 'application/x-yarina-file' ? REPORT : ''),
      setData: () => {},
      effectAllowed: 'move',
      dropEffect: 'none',
    };
    fireEvent.dragStart(file, { dataTransfer: transfer });
    fireEvent.dragEnter(root, { dataTransfer: transfer });
    fireEvent.drop(root, { dataTransfer: transfer });

    await waitFor(() => expect(moved).not.toBeNull());
    expect(moved.source).toBe(REPORT);
    expect(moved.targetDirectory).toBe('');
    await waitFor(() =>
      expect(onNotice).toHaveBeenCalledWith('success', 'Moved report.yaml to analytics (dev).'));
  });

  it('keeps files of a VIEWER-only domain read-only while other domains stay editable', async () => {
    render(
      <EditorView
        profile={{
          username: 'viewer',
          domains: {
            platform: { viewer: true, editor: true, control: false },
            analytics: { viewer: true, editor: false, control: false },
          },
        }}
        workspace={workspace}
        onBack={vi.fn()}
        onNotice={vi.fn()}
      />
    );
    await waitFor(() => expect(screen.getByText('ws-1')).toBeInTheDocument());

    fireEvent.click(await screen.findByRole('button', { name: /analytics/ }));
    const report = await screen.findByRole('button', { name: /report/ });
    fireEvent.click(report);

    await waitFor(() => expect(screen.getByText('read-only')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'Submit review' })).not.toBeInTheDocument();
    expect(screen.queryByTitle('New folder')).not.toBeInTheDocument();

    fireEvent.click(await screen.findByRole('button', { name: /platform/ }));
    const orders = await screen.findByRole('button', { name: /orders/ });
    fireEvent.click(orders);

    await waitFor(() => expect(screen.getByRole('button', { name: 'Submit review' })).toBeInTheDocument());
    expect(screen.queryByText('read-only')).not.toBeInTheDocument();
  });

  it('stays editable for a domain holding both CONTROL and EDITOR', async () => {
    render(
      <EditorView
        profile={{
          username: 'analytics_user',
          domains: {
            platform: { viewer: true, editor: false, control: false },
            analytics: { viewer: true, editor: true, control: true },
          },
        }}
        workspace={workspace}
        onBack={vi.fn()}
        onNotice={vi.fn()}
      />
    );
    await waitFor(() => expect(screen.getByText('ws-1')).toBeInTheDocument());

    fireEvent.click(await screen.findByRole('button', { name: /analytics/ }));
    const report = await screen.findByRole('button', { name: /report/ });
    fireEvent.click(report);

    await waitFor(() => expect(screen.getByRole('button', { name: 'Submit review' })).toBeInTheDocument());
    expect(screen.queryByText('read-only')).not.toBeInTheDocument();
  });

  it('keeps the workspace open when a review has no changes', async () => {
    reviewResult = { status: 'NO_CHANGES', url: null };
    const { onBack, onNotice } = await renderEditor();

    fireEvent.click(await screen.findByRole('button', { name: /platform/ }));
    await openFile('orders');
    fireEvent.click(screen.getByRole('button', { name: 'Submit review' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Submit' }));

    await waitFor(() =>
      expect(onNotice).toHaveBeenCalledWith('success', expect.stringContaining('No changes')));
    expect(onBack).not.toHaveBeenCalled();
  });

  it('leaves the workspace when a review was submitted', async () => {
    const { onBack, onNotice } = await renderEditor();

    fireEvent.click(await screen.findByRole('button', { name: /platform/ }));
    await openFile('orders');
    fireEvent.click(screen.getByRole('button', { name: 'Submit review' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Submit' }));

    await waitFor(() =>
      expect(onNotice).toHaveBeenCalledWith('success', expect.stringContaining('https://git/merge/1')));
    await new Promise((resolve) => { setTimeout(resolve, 1300); });
    expect(onBack).toHaveBeenCalled();
  });

  it('reports the domains skipped for a missing EDITOR role', async () => {
    reviewResult = { status: 'OK', url: 'https://git/merge/1', skippedDomains: ['analytics'] };
    const { onNotice } = await renderEditor();

    fireEvent.click(await screen.findByRole('button', { name: /platform/ }));
    await openFile('orders');
    fireEvent.click(screen.getByRole('button', { name: 'Submit review' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Submit' }));

    await waitFor(() =>
      expect(onNotice).toHaveBeenCalledWith('success',
        expect.stringContaining('Skipped (no EDITOR role): analytics')));
  });
});

describe('EditorView document switch', () => {
  it('drops the selected source key of the previously opened data set', async () => {
    await renderEditor();

    fireEvent.click(await screen.findByRole('button', { name: /platform/ }));
    await openFile('orders');
    fireEvent.click(screen.getByRole('button', { name: 'Sources' }));
    fireEvent.click(screen.getByRole('button', { name: 'jdbc' }));
    expect(screen.getByText('jdbc:postgresql://one')).toBeInTheDocument();

    // The other data set has no "jdbc" key: reusing the selection used to throw
    // "Cannot read properties of undefined (reading 'properties')".
    fireEvent.click(await screen.findByRole('button', { name: /analytics/ }));
    const report = await screen.findByRole('button', { name: /report/ });
    expect(() => fireEvent.click(report)).not.toThrow();

    await waitFor(() => expect(screen.getByRole('button', { name: 'General' })).toHaveClass('tab--active'));
    expect(screen.getByRole('button', { name: 'Sources' })).not.toHaveClass('tab--active');
    expect(screen.queryByText('jdbc:postgresql://one')).not.toBeInTheDocument();
  });
});
