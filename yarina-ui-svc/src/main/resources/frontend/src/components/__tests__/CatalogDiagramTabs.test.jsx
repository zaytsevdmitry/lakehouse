import React from 'react';
import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LineageTab, { buildLineageLayout } from '../LineageTab';
import RelationsTab, { buildRelationsLayout } from '../RelationsTab';
import { fetchLineage, fetchDataSet } from '../../api.js';

vi.mock('../../api.js', () => ({
  fetchLineage: vi.fn(),
  fetchDataSet: vi.fn(),
}));

// jsdom does not implement SVG geometry; React Flow measures edge labels with it.
if (!SVGElement.prototype.getBBox) {
  SVGElement.prototype.getBBox = () => ({ x: 0, y: 0, width: 0, height: 0 });
}

const CENTER = 'orders';
const CUSTOMER = 'customer';

const CUSTOMER_DS = {
  keyName: CUSTOMER,
  dataSourceKeyName: 'pg',
  databaseSchemaName: 'public',
  tableName: 'customer',
  columnSchema: [{ name: 'id', dataType: 'int' }],
  constraints: {},
};

const ORDERS_DS = {
  keyName: CENTER,
  dataSourceKeyName: 'pg',
  databaseSchemaName: 'public',
  tableName: 'orders',
  columnSchema: [{ name: 'id', dataType: 'int' }],
  constraints: {
    fk_customer: { type: 'foreign', columns: 'customer_id', reference: { dataSetKeyName: CUSTOMER } },
  },
};

const LINEAGE = {
  vertices: ['root_source', 'upstream', CENTER, 'downstream', 'leaf_receiver'],
  edges: [
    { from: 'root_source', to: 'upstream' },
    { from: 'upstream', to: CENTER },
    { from: CENTER, to: 'downstream' },
    { from: 'downstream', to: 'leaf_receiver' },
  ],
};

function nodeById(nodes, key) {
  return nodes.find((n) => n.id === `node:${key}`);
}

beforeEach(() => {
  fetchLineage.mockReset();
  fetchDataSet.mockReset();
  fetchLineage.mockResolvedValue(LINEAGE);
  fetchDataSet.mockImplementation(async (key) => (key === CUSTOMER ? CUSTOMER_DS : null));
});

describe('buildLineageLayout', () => {
  it('places incoming data sets on the left and outgoing ones on the right', () => {
    const { nodes } = buildLineageLayout(CENTER, LINEAGE.vertices, LINEAGE.edges);

    expect(nodeById(nodes, CENTER).position).toEqual({ x: 0, y: 0 });
    expect(nodeById(nodes, 'upstream').position.x).toBeLessThan(0);
    expect(nodeById(nodes, 'root_source').position.x).toBeLessThan(nodeById(nodes, 'upstream').position.x);
    expect(nodeById(nodes, 'downstream').position.x).toBeGreaterThan(0);
    expect(nodeById(nodes, 'leaf_receiver').position.x).toBeGreaterThan(nodeById(nodes, 'downstream').position.x);
  });

  it('keeps every vertex of the response on the canvas', () => {
    const { nodes } = buildLineageLayout(CENTER, [...LINEAGE.vertices, 'floating'], LINEAGE.edges);
    expect(nodes).toHaveLength(LINEAGE.vertices.length + 1);
    expect(nodeById(nodes, 'floating')).toBeDefined();
  });
});

describe('LineageTab', () => {
  it('renders the auto-layout and toggles full screen', async () => {
    const { container } = render(<LineageTab dataSetKeyName={CENTER} />);

    expect(await screen.findByText(CENTER)).toBeInTheDocument();
    const host = container.querySelector('.lineage-host');
    expect(host).not.toHaveClass('diagram-fullscreen');

    fireEvent.click(screen.getByRole('button', { name: '<-->' }));
    await waitFor(() => expect(host).toHaveClass('diagram-fullscreen'));

    fireEvent.click(screen.getByRole('button', { name: '>-<' }));
    await waitFor(() => expect(host).not.toHaveClass('diagram-fullscreen'));
  });

  it('marks the nodes as draggable for manual rearrangement', async () => {
    render(<LineageTab dataSetKeyName={CENTER} />);
    const nodeEl = (await screen.findByText(CENTER)).closest('.react-flow__node');
    expect(nodeEl).not.toBeNull();
    expect(nodeEl).toHaveClass('draggable');
    expect(nodeEl).not.toHaveClass('nodrag');
  });
});

describe('buildRelationsLayout', () => {
  it('keeps the selected table on the left and referenced tables on the right', () => {
    const nodes = buildRelationsLayout(ORDERS_DS, [CUSTOMER_DS]);

    expect(nodes[0].position).toEqual({ x: 0, y: 0 });
    expect(nodes[0].id).toBe(`node:${CENTER}`);
    expect(nodes[1].position.x).toBeGreaterThan(0);
  });
});

describe('RelationsTab', () => {
  it('renders the referenced table at the right and toggles full screen', async () => {
    const { container } = render(<RelationsTab dataSet={ORDERS_DS} />);

    expect(await screen.findByText(CUSTOMER)).toBeInTheDocument();
    expect(await screen.findByText('fk_customer')).toBeInTheDocument();
    expect(screen.getByText(CUSTOMER).closest('.react-flow__node')).toHaveClass('draggable');

    const host = container.querySelector('.relations-host');
    expect(host).not.toHaveClass('diagram-fullscreen');

    fireEvent.click(screen.getByRole('button', { name: '<-->' }));
    await waitFor(() => expect(host).toHaveClass('diagram-fullscreen'));

    fireEvent.click(screen.getByRole('button', { name: '>-<' }));
    await waitFor(() => expect(host).not.toHaveClass('diagram-fullscreen'));
  });
});
