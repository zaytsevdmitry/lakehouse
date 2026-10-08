import React from 'react';
import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import WorkspacePicker from '../WorkspacePicker';
import { api } from '../../api.js';

vi.mock('../../api.js', () => ({
  api: vi.fn(),
  workspaceUrl: (id) => `/ui?section=modeller&workspace=${id}`,
}));

const profile = { effectiveRole: 'EDITOR' };

const PLATFORM = { domain: 'platform', branchMain: 'main', branches: ['main', 'feature'] };
const ANALYTICS = { domain: 'analytics', branchMain: 'main', branches: ['main'] };

let domains = [];
let workspaces = [];
let created = null;

function mockLists() {
  api.mockImplementation(async (path, options) => {
    if (path === '/api/vcs/branches') return domains;
    if (path === '/api/vcs/workspaces') return workspaces;
    if (path === '/api/vcs/workspace') {
      created = options.body;
      return { id: 'ws-1' };
    }
    throw new Error(`unexpected call ${path}`);
  });
}

const selection = () => screen.getByText('Select at least one branch').textContent;

beforeEach(() => {
  api.mockReset();
  created = null;
  window.open = vi.fn(() => null);
  mockLists();
});

async function renderPicker() {
  const onOpen = vi.fn();
  const onNotice = vi.fn();
  render(<WorkspacePicker profile={profile} onOpen={onOpen} onNotice={onNotice} />);
  await waitFor(() => expect(screen.queryByText(/Loading/)).not.toBeInTheDocument());
  return { onOpen, onNotice };
}

describe('WorkspacePicker domain and branch selection', () => {
  it('preselects the main branch of every domain', async () => {
    domains = [PLATFORM, ANALYTICS];
    await renderPicker();

    await waitFor(() =>
      expect(screen.getByText('platform (main), analytics (main)')).toBeInTheDocument());
  });

  it('keeps a non-default branch picked by the user', async () => {
    domains = [PLATFORM];
    await renderPicker();

    fireEvent.click(screen.getByRole('checkbox', { name: /feature/ }));
    await waitFor(() => expect(screen.getByText('platform (feature)')).toBeInTheDocument());
  });

  it('forgets a domain that the server no longer reports', async () => {
    domains = [PLATFORM, ANALYTICS];
    await renderPicker();
    await waitFor(() =>
      expect(screen.getByText('platform (main), analytics (main)')).toBeInTheDocument());

    domains = [PLATFORM];
    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => expect(screen.getByText('platform (main)')).toBeInTheDocument());
  });

  it('resets a selection whose branch no longer exists', async () => {
    domains = [PLATFORM];
    await renderPicker();
    fireEvent.click(screen.getByRole('checkbox', { name: /feature/ }));
    await waitFor(() => expect(screen.getByText('platform (feature)')).toBeInTheDocument());

    domains = [{ ...PLATFORM, branches: ['main'] }];
    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => expect(screen.getByText('platform (main)')).toBeInTheDocument());
  });

  it('opens a workspace with the branches selected per domain', async () => {
    domains = [PLATFORM, ANALYTICS];
    await renderPicker();
    fireEvent.click(screen.getByRole('checkbox', { name: /feature/ }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Open' })).toBeEnabled());

    fireEvent.click(screen.getByRole('button', { name: 'Open' }));

    await waitFor(() => expect(created).not.toBeNull());
    expect(created.branches).toEqual([
      { domain: 'platform', branch: 'feature' },
      { domain: 'analytics', branch: 'main' },
    ]);
  });
});
