import React, { useCallback, useEffect, useState } from 'react';
import { fetchCatalogTree, fetchServices } from './api.js';
import CatalogsSection from './components/CatalogsSection.jsx';
import ServicesSection from './components/ServicesSection.jsx';

export default function App() {
  const [catalogTree, setCatalogTree] = useState([]);
  const [services, setServices] = useState([]);
  const [catalogError, setCatalogError] = useState('');
  const [servicesError, setServicesError] = useState('');

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

  return (
    <div className="app">
      <header className="app-header">
        <h1>Lakehouse</h1>
        <button className="refresh-button" onClick={reloadServices}>
          Refresh services status
        </button>
      </header>
      <main className="app-main">
        <CatalogsSection catalogs={catalogTree} error={catalogError} />
        <ServicesSection services={services} error={servicesError} />
      </main>
    </div>
  );
}
