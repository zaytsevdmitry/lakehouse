import React, { useCallback, useEffect, useState } from 'react';
import { fetchCatalogTree, fetchServices, fetchCurrentUser, logout } from './api.js';
import CatalogsSection from './components/CatalogsSection.jsx';
import SchedulesSection from './components/SchedulesSection.jsx';
import ServicesSection from './components/ServicesSection.jsx';
import SparkJobsSection from './components/SparkJobsSection.jsx';
import VcsSection from './components/VcsSection.jsx';

const THEME_KEY = 'lakehouse-theme';

function getInitialTheme() {
  try {
    return localStorage.getItem(THEME_KEY) || 'light';
  } catch {
    return 'light';
  }
}

export default function App() {
  const [catalogTree, setCatalogTree] = useState([]);
  const [services, setServices] = useState([]);
  const [catalogError, setCatalogError] = useState('');
  const [servicesError, setServicesError] = useState('');
  const [theme, setTheme] = useState(getInitialTheme);
  const [username, setUsername] = useState('');
  const [activeSection, setActiveSection] = useState('services');
  const [createdSections, setCreatedSections] = useState(() => new Set(['services']));

  const activateSection = (section) => {
    setActiveSection(section);
    setCreatedSections((current) =>
      current.has(section) ? current : new Set([...current, section])
    );
  };

  const reloadServices = useCallback(() => {
    fetchServices()
      .then(setServices)
      .catch((e) => setServicesError(e.message));
  }, []);

  useEffect(() => {
    fetchCatalogTree()
      .then(setCatalogTree)
      .catch((e) => setCatalogError(e.message));
    reloadServices();
    fetchCurrentUser()
      .then((user) => setUsername(user.username))
      .catch(() => setUsername(''));
  }, [reloadServices]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch {
      // ignore
    }
  }, [theme]);

  const toggleTheme = () => {
    setTheme((current) => (current === 'dark' ? 'light' : 'dark'));
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>Lakehouse</h1>
        <div className="header-actions">
          {username && <span className="user-label">{username}</span>}
          <button className="theme-toggle" onClick={() => logout()}>
            Switch user
          </button>
          <button className="theme-toggle" onClick={toggleTheme}>
            {theme === 'dark' ? 'Day mode' : 'Night mode'}
          </button>
        </div>
      </header>
      <nav className="section-switcher">
        <button
          className={`section-switcher-button ${activeSection === 'services' ? 'section-switcher-button--active' : ''}`}
          onClick={() => activateSection('services')}
        >
          Services
        </button>
        <button
          className={`section-switcher-button ${activeSection === 'catalog' ? 'section-switcher-button--active' : ''}`}
          onClick={() => activateSection('catalog')}
        >
          Catalog
        </button>
        <button
          className={`section-switcher-button ${activeSection === 'schedules' ? 'section-switcher-button--active' : ''}`}
          onClick={() => activateSection('schedules')}
        >
          Schedules
        </button>
        <button
          className={`section-switcher-button ${activeSection === 'sparkjobs' ? 'section-switcher-button--active' : ''}`}
          onClick={() => activateSection('sparkjobs')}
        >
          SparkJobs
        </button>
        <button
          className={`section-switcher-button ${activeSection === 'vcs' ? 'section-switcher-button--active' : ''}`}
          onClick={() => activateSection('vcs')}
        >
          VCS
        </button>
      </nav>
      <main className="app-main">
        {createdSections.has('catalog') && (
          <div className="section-pane" hidden={activeSection !== 'catalog'}>
            <CatalogsSection catalogs={catalogTree} error={catalogError} />
          </div>
        )}
        {createdSections.has('schedules') && (
          <div className="section-pane" hidden={activeSection !== 'schedules'}>
            <SchedulesSection />
          </div>
        )}
        {createdSections.has('sparkjobs') && (
          <div className="section-pane" hidden={activeSection !== 'sparkjobs'}>
            <SparkJobsSection />
          </div>
        )}
        {createdSections.has('services') && (
          <div className="section-pane" hidden={activeSection !== 'services'}>
            <ServicesSection services={services} error={servicesError} />
          </div>
        )}
        {createdSections.has('vcs') && (
          <div className="section-pane" hidden={activeSection !== 'vcs'}>
            <VcsSection />
          </div>
        )}
      </main>
    </div>
  );
}
