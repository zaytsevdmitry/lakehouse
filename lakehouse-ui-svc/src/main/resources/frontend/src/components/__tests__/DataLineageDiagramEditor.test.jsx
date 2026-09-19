import React from 'react';
import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MarkerType } from '@xyflow/react';
import DataLineageDiagramEditor, {
  buildLineageGraph,
  setNodePosition,
  addLineageDataset,
  removeLineageDataset,
} from '../DataLineageDiagramEditor';
import { api } from '../../api.js';

vi.mock('../../api.js', () => ({
  api: vi.fn(),
  encodePath: (path) => String(path).split('/').map((part) => encodeURIComponent(part)).join('/'),
}));

const DATASET_A_YAML = 'kind: DataSet\nkeyName: DataSet_A\nsources: {}\n';
const DATASET_B_YAML = (
  'kind: DataSet\n'
  + 'keyName: DataSet_B\n'
  + 'sources:\n'
  + '  DataSet_A:\n'
  + '    properties: {}\n'
);

const tree = [
  { path: 'datasets/DataSet_A.yaml', kind: 'DataSet', keyName: 'DataSet_A' },
  { path: 'datasets/DataSet_B.yaml', kind: 'DataSet', keyName: 'DataSet_B' },
];

const wsId = 'ws-test';

function makeDoc(positions = {}) {
  return {
    kind: 'DataLineageDiagram',
    spec: {
      datasets: ['DataSet_A', 'DataSet_B'],
      layout: { positions },
    },
  };
}

function mockWorkspaceReads() {
  api.mockImplementation(async (path) => {
    if (String(path).includes('DataSet_A')) return { yaml: DATASET_A_YAML };
    if (String(path).includes('DataSet_B')) return { yaml: DATASET_B_YAML };
    throw new Error('no data set file for ' + path);
  });
}

beforeEach(() => {
  api.mockReset();
  mockWorkspaceReads();
});

describe('buildLineageGraph', () => {
  it('exposes exactly two nodes', () => {
    const { nodes } = buildLineageGraph(makeDoc(), {
      DataSet_A: { keyName: 'DataSet_A', doc: { kind: 'DataSet', keyName: 'DataSet_A', sources: {} } },
      DataSet_B: {
        keyName: 'DataSet_B',
        doc: { kind: 'DataSet', keyName: 'DataSet_B', sources: { DataSet_A: { properties: {} } } },
      },
    });
    expect(nodes).toHaveLength(2);
    expect(nodes.map((n) => n.id)).toEqual(['DataSet_A', 'DataSet_B']);
    expect(nodes.every((n) => n.type === 'lineageNode')).toBe(true);
  });

  it('flows horizontally by default when no positions are stored', () => {
    const { nodes } = buildLineageGraph(makeDoc(), {});
    const [a, b] = nodes;
    expect(a.position.x).toBe(0);
    expect(a.position.y).toBe(0);
    expect(b.position.x).toBeGreaterThan(a.position.x);
  });

  it('creates a single directional A->B edge from DataSet.B sources', () => {
    const { edges } = buildLineageGraph(makeDoc(), {
      DataSet_A: { keyName: 'DataSet_A', doc: { kind: 'DataSet', keyName: 'DataSet_A', sources: {} } },
      DataSet_B: {
        keyName: 'DataSet_B',
        doc: { kind: 'DataSet', keyName: 'DataSet_B', sources: { DataSet_A: { properties: {} } } },
      },
    });
    expect(edges).toHaveLength(1);
    expect(edges[0].source).toBe('DataSet_A');
    expect(edges[0].target).toBe('DataSet_B');
    expect(edges[0].markerEnd.type).toBe(MarkerType.ArrowClosed);
    expect(edges[0]).not.toEqual(expect.objectContaining({ sourceHandle: expect.anything() }));
  });

  it('skips sources that are not part of the diagram', () => {
    const { edges } = buildLineageGraph(makeDoc(), {
      DataSet_B: {
        keyName: 'DataSet_B',
        doc: { kind: 'DataSet', keyName: 'DataSet_B', sources: { DataSet_Unknown: { properties: {} } } },
      },
    });
    expect(edges).toHaveLength(0);
  });
});

describe('setNodePosition', () => {
  it('writes rounded coordinates into doc.spec.layout.positions', () => {
    const doc = makeDoc({ DataSet_A: { x: 12.6, y: -3.2 } });
    const next = setNodePosition(doc, 'DataSet_A', { x: 41.4, y: 72.8 });
    expect(next).not.toBe(doc);
    expect(next.spec.layout.positions).toEqual({ DataSet_A: { x: 41, y: 73 } });
    expect(doc.spec.layout.positions.DataSet_A).toEqual({ x: 12.6, y: -3.2 });
  });

  it('preserves existing positions of peer nodes', () => {
    const doc = makeDoc({ DataSet_A: { x: 10, y: 20 } });
    const next = setNodePosition(doc, 'DataSet_B', { x: 5, y: 6 });
    expect(next.spec.layout.positions).toEqual({
      DataSet_A: { x: 10, y: 20 },
      DataSet_B: { x: 5, y: 6 },
    });
  });

  it('creates spec.layout.positions when missing', () => {
    const doc = { kind: 'DataLineageDiagram', spec: { datasets: ['DataSet_A'] } };
    const next = setNodePosition(doc, 'DataSet_A', { x: 100, y: 200 });
    expect(next.spec.layout.positions.DataSet_A).toEqual({ x: 100, y: 200 });
    expect(next.spec.datasets).toEqual(doc.spec.datasets);
  });
});

describe('addLineageDataset', () => {
  it('appends a key to spec.datasets and places it after the existing nodes', () => {
    const next = addLineageDataset(makeDoc(), 'DataSet_C');
    expect(next.spec.datasets).toEqual(['DataSet_A', 'DataSet_B', 'DataSet_C']);
    expect(next.spec.layout.positions.DataSet_C).toEqual({ x: 620, y: 0 });
  });

  it('does not duplicate an existing data set', () => {
    const doc = makeDoc();
    const next = addLineageDataset(doc, 'DataSet_A');
    expect(next).toBe(doc);
    expect(next.spec.datasets).toEqual(['DataSet_A', 'DataSet_B']);
  });

  it('creates spec.datasets when the diagram is brand new', () => {
    const doc = { kind: 'DataLineageDiagram', keyName: 'lineage_1' };
    const next = addLineageDataset(doc, 'DataSet_A');
    expect(next.spec.datasets).toEqual(['DataSet_A']);
    expect(next.spec.layout.positions.DataSet_A).toEqual({ x: 0, y: 0 });
  });
});

describe('removeLineageDataset', () => {
  it('drops the key from spec.datasets and its stored position', () => {
    const doc = makeDoc({ DataSet_A: { x: 0, y: 0 }, DataSet_B: { x: 310, y: 40 } });
    const next = removeLineageDataset(doc, 'DataSet_A');
    expect(next.spec.datasets).toEqual(['DataSet_B']);
    expect(next.spec.layout.positions).toEqual({ DataSet_B: { x: 310, y: 40 } });
  });

  it('returns the same document when the key is not on the diagram', () => {
    const doc = makeDoc();
    expect(removeLineageDataset(doc, 'DataSet_C')).toBe(doc);
  });
});

describe('DataLineageDiagramEditor', () => {
  it('mounts and resolves workspace data set documents', async () => {
    render(<DataLineageDiagramEditor doc={makeDoc()} onChange={vi.fn()} tree={tree} wsId={wsId} />);
    expect(await screen.findByText('DataSet_A')).toBeInTheDocument();
    expect(screen.getByText('DataSet_B')).toBeInTheDocument();
  });

  it('hands exactly 2 nodes and 1 directional edge over to the flow canvas', async () => {
    const { container } = render(
      <DataLineageDiagramEditor doc={makeDoc()} onChange={vi.fn()} tree={tree} wsId={wsId} />,
    );
    await screen.findByText('DataSet_A');
    await waitFor(() => {
      expect(container.querySelectorAll('.react-flow__node')).toHaveLength(2);
      expect(container.querySelectorAll('.react-flow__edge')).toHaveLength(1);
    });
    const firstNode = [...container.querySelectorAll('.react-flow__node')][0];
    expect(firstNode).toHaveClass('react-flow__node-lineageNode');
  });

  it('marks the left handle as target and the right handle as source', async () => {
    const { container } = render(
      <DataLineageDiagramEditor doc={makeDoc()} onChange={vi.fn()} tree={tree} wsId={wsId} />,
    );
    await screen.findByText('DataSet_A');
    await waitFor(() => {
      const left = container.querySelector('.lineage-handle--left');
      const right = container.querySelector('.lineage-handle--right');
      expect(left).not.toBeNull();
      expect(right).not.toBeNull();
      expect(left).toHaveClass('react-flow__handle-left', 'target');
      expect(right).toHaveClass('react-flow__handle-right', 'source');
    });
  });

  it('persists a dragged node position through onChange into spec.layout.positions', async () => {
    const onChange = vi.fn();
    render(<DataLineageDiagramEditor doc={makeDoc()} onChange={onChange} tree={tree} wsId={wsId} />);
    const nodeEl = (await screen.findByText('DataSet_A')).closest('.react-flow__node');
    expect(nodeEl).not.toBeNull();

    const view = document.defaultView._globalProxy;
    fireEvent.mouseDown(nodeEl, { view, clientX: 200, clientY: 200, button: 0, bubbles: true, cancelable: true });
    fireEvent.mouseMove(view, { view, clientX: 260, clientY: 210, button: 0, bubbles: true, cancelable: true });
    fireEvent.mouseUp(view, { view, clientX: 260, clientY: 210, button: 0, bubbles: true, cancelable: true });

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledTimes(1);
      const next = onChange.mock.calls[0][0];
      const position = next.spec.layout.positions.DataSet_A;
      expect(position).toEqual({ x: expect.any(Number), y: expect.any(Number) });
      expect(Array.isArray(next.spec.datasets)).toBe(true);
    });
  });

  it('renders Add/Remove/Edit on the canvas when editable', async () => {
    const { container } = render(
      <DataLineageDiagramEditor doc={makeDoc()} onChange={vi.fn()} tree={tree} wsId={wsId} />,
    );
    await screen.findByText('DataSet_A');
    await waitFor(() => {
      expect(container.querySelector('.lineage-toolbar')).not.toBeNull();
    });
    expect(screen.getByRole('button', { name: 'Add' })).not.toBeDisabled();
    expect(screen.getByRole('button', { name: 'Remove' })).toBeDisabled();
  });

  it('hides the toolbar in read-only mode', async () => {
    const { container } = render(
      <DataLineageDiagramEditor doc={makeDoc()} onChange={vi.fn()} tree={tree} wsId={wsId} readOnly />,
    );
    await screen.findByText('DataSet_A');
    await waitFor(() => {
      expect(container.querySelector('.react-flow__node')).not.toBeNull();
      expect(container.querySelector('.lineage-toolbar')).toBeNull();
    });
  });

  it('adds a data set through the picker modal', async () => {
    const onChange = vi.fn();
    render(
      <DataLineageDiagramEditor
        doc={{ kind: 'DataLineageDiagram', spec: { datasets: ['DataSet_A'], layout: { positions: {} } } }}
        onChange={onChange}
        tree={tree}
        wsId={wsId}
      />,
    );
    await screen.findByText('DataSet_A');
    fireEvent.click(screen.getByRole('button', { name: 'Add' }));
    const pickerRow = await waitFor(() => screen.getByText('DataSet_B'));
    fireEvent.click(pickerRow);
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await waitFor(() => {
      expect(onChange).toHaveBeenCalledTimes(1);
      const next = onChange.mock.calls[0][0];
      expect(next.spec.datasets).toEqual(['DataSet_A', 'DataSet_B']);
      expect(next.spec.layout.positions.DataSet_B).toEqual({ x: 400, y: 300 });
    });
  });

  it('asks for confirmation before removing the selected node', async () => {
    const onChange = vi.fn();
    render(<DataLineageDiagramEditor doc={makeDoc()} onChange={onChange} tree={tree} wsId={wsId} />);
    const nodeEl = (await screen.findByText('DataSet_A')).closest('.react-flow__node');
    fireEvent.click(nodeEl);
    const toolbarRemove = await waitFor(() => screen.getByRole('button', { name: 'Remove' }));
    await waitFor(() => expect(toolbarRemove).not.toBeDisabled());
    fireEvent.click(toolbarRemove);
    expect(await screen.findByRole('heading', { name: 'Remove data set' })).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: 'Remove' })[1]);
    await waitFor(() => {
      expect(onChange).toHaveBeenCalledTimes(1);
      expect(onChange.mock.calls[0][0].spec.datasets).toEqual(['DataSet_B']);
    });
  });

  it('cancelling the remove confirmation keeps the diagram intact', async () => {
    const onChange = vi.fn();
    render(<DataLineageDiagramEditor doc={makeDoc()} onChange={onChange} tree={tree} wsId={wsId} />);
    const nodeEl = (await screen.findByText('DataSet_A')).closest('.react-flow__node');
    fireEvent.click(nodeEl);
    const toolbarRemove = await waitFor(() => screen.getByRole('button', { name: 'Remove' }));
    await waitFor(() => expect(toolbarRemove).not.toBeDisabled());
    fireEvent.click(toolbarRemove);
    fireEvent.click(await screen.findByRole('button', { name: 'Cancel' }));
    await waitFor(() => {
      expect(onChange).not.toHaveBeenCalled();
      expect(screen.queryByRole('heading', { name: 'Remove data set' })).not.toBeInTheDocument();
    });
  });

  it('toggles the diagram to full-screen and back', async () => {
    const { container } = render(
      <DataLineageDiagramEditor doc={makeDoc()} onChange={vi.fn()} tree={tree} wsId={wsId} />,
    );
    await screen.findByText('DataSet_A');
    const host = container.querySelector('.lineage-host');
    expect(host).not.toHaveClass('diagram-fullscreen');
    fireEvent.click(screen.getByRole('button', { name: '<-->' }));
    await waitFor(() => expect(host).toHaveClass('diagram-fullscreen'));
    fireEvent.click(screen.getByRole('button', { name: '>-<' }));
    await waitFor(() => expect(host).not.toHaveClass('diagram-fullscreen'));
  });

  it('edits the selected data set in an in-canvas modal like ERDiagram', async () => {
    const onChange = vi.fn();
    const onNotice = vi.fn();
    const { container } = render(
      <DataLineageDiagramEditor
        doc={makeDoc()}
        onChange={onChange}
        tree={tree}
        wsId={wsId}
        onNotice={onNotice}
      />,
    );
    const nodeEl = (await screen.findByText('DataSet_A')).closest('.react-flow__node');
    fireEvent.click(nodeEl);
    const edit = await waitFor(() => screen.getByRole('button', { name: 'Edit' }));
    await waitFor(() => expect(edit).not.toBeDisabled());
    fireEvent.click(edit);
    expect(await screen.findByRole('heading', { name: 'kind: DataSet · DataSet_A' })).toBeInTheDocument();
    expect(container.querySelector('textarea.er-edit-yaml')).not.toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await waitFor(() => {
      const putCall = api.mock.calls.find(([path, opts]) => opts && opts.method === 'PUT');
      expect(putCall).toBeTruthy();
      expect(onNotice).toHaveBeenCalledWith('success', expect.stringContaining('DataSet_A'));
    });
  });
});