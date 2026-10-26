import React from 'react';
import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MarkerType } from '@xyflow/react';
import DataLineageDiagramEditor, {
  buildLineageGraph,
  setNodePosition,
  addLineageDataset,
  addLineageDatasets,
  distributeLineagePositions,
  removeLineageDataset,
  EDGE_TYPE,
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

  it('draws the connectors with the rounded custom edge type', () => {
    const { edges } = buildLineageGraph(makeDoc(), {
      DataSet_A: { keyName: 'DataSet_A', doc: { kind: 'DataSet', keyName: 'DataSet_A', sources: {} } },
      DataSet_B: {
        keyName: 'DataSet_B',
        doc: { kind: 'DataSet', keyName: 'DataSet_B', sources: { DataSet_A: { properties: {} } } },
      },
    });
    expect(edges[0].type).toBe(EDGE_TYPE);
    expect(edges[0].type).not.toBe('smoothstep');
    expect(edges[0].style.strokeLinecap).toBe('round');
    expect(edges[0].style.strokeLinejoin).toBe('round');
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

describe('distributeLineagePositions', () => {
  it('places a source batch in one column left of the anchor and spreads it on Y', () => {
    const positions = distributeLineagePositions({ x: 1000, y: 500 }, ['A', 'B', 'C'], 'left');
    expect(Object.keys(positions)).toEqual(['A', 'B', 'C']);
    expect(Object.values(positions).every((position) => position.x === 690)).toBe(true);
    expect(positions.A.y).toBeLessThan(positions.B.y);
    expect(positions.B.y).toBeLessThan(positions.C.y);
    expect(positions.B.y).toBe(500);
    expect(positions.A.y).toBe(500 - (positions.B.y - positions.A.y));
    expect(positions.C.y).toBe(500 + (positions.B.y - positions.A.y));
  });

  it('places a receiver batch in one column right of the anchor', () => {
    const positions = distributeLineagePositions({ x: 10, y: 0 }, ['X', 'Y'], 'right');
    expect(Object.values(positions).every((position) => position.x === 320)).toBe(true);
    expect(positions.X.y).toBe(-73);
    expect(positions.Y.y).toBe(73);
  });

  it('keeps a single node at the anchor height and handles an empty batch', () => {
    expect(distributeLineagePositions({ x: 300, y: 120 }, ['Only'], 'left')).toEqual({
      Only: { x: -10, y: 120 },
    });
    expect(distributeLineagePositions({ x: 300, y: 120 }, [], 'right')).toEqual({});
  });
});

describe('addLineageDatasets', () => {
  it('appends a batch once and registers all supplied positions', () => {
    const positions = { C: { x: 1.6, y: 2.4 }, D: { x: -30, y: 40 } };
    const next = addLineageDatasets(makeDoc({ DataSet_A: { x: 5, y: 6 } }), ['C', 'D'], positions);
    expect(next.spec.datasets).toEqual(['DataSet_A', 'DataSet_B', 'C', 'D']);
    expect(next.spec.layout.positions.DataSet_A).toEqual({ x: 5, y: 6 });
    expect(next.spec.layout.positions.C).toEqual({ x: 2, y: 2 });
    expect(next.spec.layout.positions.D).toEqual({ x: -30, y: 40 });
  });

  it('ignores data sets already on the canvas and returns the same document', () => {
    const doc = makeDoc();
    expect(addLineageDatasets(doc, ['DataSet_A', 'DataSet_B'], {})).toBe(doc);
  });

  it('creates the layout when the diagram has none', () => {
    const doc = { kind: 'DataLineageDiagram', spec: { datasets: ['A'] } };
    const next = addLineageDatasets(doc, ['B'], { B: { x: 10, y: 20 } });
    expect(next.spec.datasets).toEqual(['A', 'B']);
    expect(next.spec.layout.positions).toEqual({ B: { x: 10, y: 20 } });
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
    expect(container.querySelectorAll(`.react-flow__edge-${EDGE_TYPE}`)).toHaveLength(1);
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

  it('enables Add sources / Add receivers only for a selected data set node', async () => {
    render(<DataLineageDiagramEditor doc={makeDoc()} onChange={vi.fn()} tree={tree} wsId={wsId} />);
    await screen.findByText('DataSet_A');
    const addSources = screen.getByRole('button', { name: 'Add sources' });
    const addReceivers = screen.getByRole('button', { name: 'Add receivers' });
    expect(addSources).toBeDisabled();
    expect(addReceivers).toBeDisabled();

    fireEvent.click((await screen.findByText('DataSet_B')).closest('.react-flow__node'));
    await waitFor(() => expect(addSources).not.toBeDisabled());
    expect(addReceivers).not.toBeDisabled();
  });

  it('adds the recursively discovered sources left of the selected node', async () => {
    api.mockImplementation(async (path) => {
      const target = String(path);
      if (target.includes('/lineage/sources')) {
        return [
          { path: 'datasets/DataSet_Src1.yaml', kind: 'DataSet', keyName: 'DataSet_Src1', yaml: 'kind: DataSet\nkeyName: DataSet_Src1\nsources: {}\n' },
          { path: 'datasets/DataSet_Src2.yaml', kind: 'DataSet', keyName: 'DataSet_Src2', yaml: 'kind: DataSet\nkeyName: DataSet_Src2\nsources: {}\n' },
        ];
      }
      if (target.includes('DataSet_A')) return { yaml: DATASET_A_YAML };
      if (target.includes('DataSet_B')) return { yaml: DATASET_B_YAML };
      throw new Error('unexpected ' + target);
    });
    const onChange = vi.fn();
    render(<DataLineageDiagramEditor doc={makeDoc({ DataSet_A: { x: 0, y: 0 }, DataSet_B: { x: 310, y: 0 } })} onChange={onChange} tree={tree} wsId={wsId} />);
    fireEvent.click((await screen.findByText('DataSet_B')).closest('.react-flow__node'));
    const button = screen.getByRole('button', { name: 'Add sources' });
    await waitFor(() => expect(button).not.toBeDisabled());
    fireEvent.click(button);

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));
    const next = onChange.mock.calls[0][0];
    expect(next.spec.datasets).toEqual(['DataSet_A', 'DataSet_B', 'DataSet_Src1', 'DataSet_Src2']);
    const sourceX = next.spec.layout.positions.DataSet_Src1.x;
    expect(sourceX).toBeLessThan(next.spec.layout.positions.DataSet_B.x);
    expect(next.spec.layout.positions.DataSet_Src2.x).toBe(sourceX);
    expect(next.spec.layout.positions.DataSet_Src1.y)
      .not.toBe(next.spec.layout.positions.DataSet_Src2.y);
    const lineageCall = api.mock.calls.find(([path]) => String(path).includes('/lineage/sources'));
    expect(lineageCall).toBeTruthy();
    expect(lineageCall[0]).toContain('keyName=DataSet_B');
  });

  it('adds the recursively discovered receivers right of the selected node', async () => {
    api.mockImplementation(async (path) => {
      const target = String(path);
      if (target.includes('/lineage/receivers')) {
        return [
          { path: 'datasets/DataSet_Recv1.yaml', kind: 'DataSet', keyName: 'DataSet_Recv1', yaml: 'kind: DataSet\nkeyName: DataSet_Recv1\nsources: { DataSet_A: {} }\n' },
          { path: 'datasets/DataSet_Recv2.yaml', kind: 'DataSet', keyName: 'DataSet_Recv2', yaml: 'kind: DataSet\nkeyName: DataSet_Recv2\nsources: { DataSet_A: {} }\n' },
        ];
      }
      if (target.includes('DataSet_A')) return { yaml: DATASET_A_YAML };
      if (target.includes('DataSet_B')) return { yaml: DATASET_B_YAML };
      throw new Error('unexpected ' + target);
    });
    const onChange = vi.fn();
    render(<DataLineageDiagramEditor doc={makeDoc({ DataSet_A: { x: 0, y: 0 }, DataSet_B: { x: 310, y: 0 } })} onChange={onChange} tree={tree} wsId={wsId} />);
    fireEvent.click((await screen.findByText('DataSet_A')).closest('.react-flow__node'));
    const button = screen.getByRole('button', { name: 'Add receivers' });
    await waitFor(() => expect(button).not.toBeDisabled());
    fireEvent.click(button);

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));
    const next = onChange.mock.calls[0][0];
    expect(next.spec.datasets).toEqual(['DataSet_A', 'DataSet_B', 'DataSet_Recv1', 'DataSet_Recv2']);
    const receiverX = next.spec.layout.positions.DataSet_Recv1.x;
    expect(receiverX).toBeGreaterThan(next.spec.layout.positions.DataSet_A.x);
    expect(next.spec.layout.positions.DataSet_Recv2.x).toBe(receiverX);
    expect(next.spec.layout.positions.DataSet_Recv1.y)
      .not.toBe(next.spec.layout.positions.DataSet_Recv2.y);
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