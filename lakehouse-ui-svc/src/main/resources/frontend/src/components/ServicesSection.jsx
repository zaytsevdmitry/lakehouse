import React from 'react';

export default function ServicesSection({ services, error }) {
  return (
    <section className="section">
      <h2>Services</h2>
      {error && <div className="error-box">Error: {error}</div>}
      {!error && services.length === 0 && (
        <div className="empty-box">No services configured.</div>
      )}
      <ul className="services-list">
        {!error &&
          services.map((service) => (
            <li key={service.name} className="service-card">
              <div className="service-card-header">
                <span className="service-name">{service.name}</span>
                <span
                  className={`status-badge status-badge--${(service.status || 'DOWN').toLowerCase()}`}
                >
                  {service.status}
                </span>
              </div>
              <div className="service-card-body">
                <div>
                  <span className="service-label">URL: </span>
                  <a href={service.url} target="_blank" rel="noreferrer">
                    {service.url}
                  </a>
                </div>
                <div>
                  <span className="service-label">Health check: </span>
                  {service.healthCheckUrl}
                </div>
              </div>
            </li>
          ))}
      </ul>
    </section>
  );
}
