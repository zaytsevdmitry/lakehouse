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

const profile = { effectiveRole: 'EDITOR' };

const ORDERS = 'platform (main)/orders.yaml';
const REPORT = 'analytics (dev)/report.yaml';

const TREE = [
  { path: ORDERS, kind: 'DataSet', keyName: 'orders' },
  { path: REPORT, kind: 'DataSet', keyName: 'report' },
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

let reviewResult;
let moved;
let createdDir;

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
    if (String(path).includes('/review/')) return reviewResult;
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

  it('moves a file dropped on the root to the root of its own domain', async () => {
    const { onNotice } = await renderEditor();
    fireEvent.click(await screen.findByRole('button', { name: /analytics/ }));
    const file = await screen.findByRole('button', { name: /report/ });
    const root = screen.getByRole('button', { name: /Root/ }).parentElement;

    const transfer = {
      types: ['application/x-lakehouse-file'],
      getData: (type) => (type === 'application/x-lakehouse-file' ? REPORT : ''),
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
