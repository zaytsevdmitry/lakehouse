import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import FormEditor from '../FormEditor';

const DATA_SET_SCHEMA = {
  kind: 'DataSet',
  fields: [
    { name: 'keyName', type: 'string', label: 'Key name', keyName: true },
    { name: 'description', type: 'string', label: 'Description' },
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

const ORDERS = {
  keyName: 'orders',
  sources: { jdbc: { properties: { url: 'jdbc:postgresql://one', user: 'sa' } } },
};

const REPORT = {
  keyName: 'report',
  sources: { jdbc: { properties: { url: 'jdbc:postgresql://two' } } },
};

function renderFormEditor(value, onChange = vi.fn()) {
  const result = render(
    <FormEditor schema={DATA_SET_SCHEMA} value={value} onChange={onChange} />
  );
  fireEvent.click(screen.getByRole('button', { name: 'Sources' }));
  return { ...result, onChange };
}

function openSourcesTab() {
  fireEvent.click(screen.getByRole('button', { name: 'Sources' }));
}

function selectSourceKey(key) {
  fireEvent.click(screen.getByRole('button', { name: key }));
}

describe('FormEditor DataSet sources tab', () => {
  it('shows the properties of the selected source key', () => {
    renderFormEditor(ORDERS);

    selectSourceKey('jdbc');

    expect(screen.getByText('url')).toBeInTheDocument();
    expect(screen.getByText('jdbc:postgresql://one')).toBeInTheDocument();
    expect(screen.queryByText('Select a key to edit its properties.')).not.toBeInTheDocument();
  });

  it('survives a document switch that leaves the selected source key behind', () => {
    const { rerender } = renderFormEditor(ORDERS);
    selectSourceKey('jdbc');

    // The next document has no such key: dereferencing it used to throw
    // "Cannot read properties of undefined (reading 'properties')".
    expect(() =>
      rerender(<FormEditor schema={DATA_SET_SCHEMA} value={{ keyName: 'empty' }} onChange={vi.fn()} />)
    ).not.toThrow();

    expect(screen.getByText('Select a key to edit its properties.')).toBeInTheDocument();
  });

  it('keeps a source key that exists in both documents selected', () => {
    const { rerender } = renderFormEditor(ORDERS);
    selectSourceKey('jdbc');

    rerender(<FormEditor schema={DATA_SET_SCHEMA} value={REPORT} onChange={vi.fn()} />);

    expect(screen.getByText('jdbc:postgresql://two')).toBeInTheDocument();
  });

  it('does not crash on a source entry that is not an object', () => {
    renderFormEditor({ keyName: 'broken', sources: { jdbc: null, other: 'text' } });

    expect(() => selectSourceKey('jdbc')).not.toThrow();
    expect(screen.getByText('(empty)')).toBeInTheDocument();
    expect(() => selectSourceKey('other')).not.toThrow();
  });

  it('never writes a stale key into a newly opened document', () => {
    const onChange = vi.fn();
    const { rerender } = renderFormEditor(ORDERS, onChange);
    selectSourceKey('jdbc');

    rerender(<FormEditor schema={DATA_SET_SCHEMA} value={REPORT} onChange={onChange} />);
    // Nothing is selected any more, so the property table is gone and cannot emit a change.
    fireEvent.click(screen.getByRole('button', { name: 'jdbc' }));
    expect(onChange).not.toHaveBeenCalled();
  });

  it('disables remove and edit while no key is selected', () => {
    renderFormEditor(ORDERS);

    const toolbar = screen.getByPlaceholderText('Filter by key…').closest('.pane-head');
    expect(toolbar.querySelector('button[title="Remove"]')).toBeDisabled();
    expect(toolbar.querySelector('button[title="Edit"]')).toBeDisabled();
  });
});

describe('FormEditor tab state', () => {
  it('starts on the General tab of every mounted instance', () => {
    const { unmount } = render(<FormEditor schema={DATA_SET_SCHEMA} value={ORDERS} onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'General' })).toHaveClass('tab--active');
    unmount();

    render(<FormEditor schema={DATA_SET_SCHEMA} value={REPORT} onChange={vi.fn()} />);
    openSourcesTab();
    expect(screen.getByRole('button', { name: 'Sources' })).toHaveClass('tab--active');
  });
});
