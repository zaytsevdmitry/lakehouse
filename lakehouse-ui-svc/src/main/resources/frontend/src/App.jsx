import React, { useCallback, useEffect, useState } from 'react';
import { fetchCatalogTree, fetchServices } from './api.js';
import CatalogsSection from './components/CatalogsSection.jsx';
import SchedulesSection from './components/SchedulesSection.jsx';
import ServicesSection from './components/ServicesSection.jsx';

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
        {createdSections.has('services') && (
          <div className="section-pane" hidden={activeSection !== 'services'}>
            <ServicesSection services={services} error={servicesError} />
          </div>
        )}
      </main>
    </div>
  );
}
