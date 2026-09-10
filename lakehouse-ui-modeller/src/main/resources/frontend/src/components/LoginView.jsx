import React from 'react';

export default function LoginView({ config, onLogin }) {
  return (
    <div className="login">
      <div className="login-card">
        <h1>Lakehouse Configurator</h1>
        <p className="muted">
          Sign in to edit lakehouse model metadata. Authentication is handled by
          {config ? ' the configured Keycloak realm.' : ' the identity provider.'}
        </p>
        {config && (
          <dl className="login-meta">
            <div><dt>Issuer</dt><dd>{config.issuerUri}</dd></div>
            <div><dt>Strategy</dt><dd>{config.authStrategy}</dd></div>
          </dl>
        )}
        <div className="btn-row">
          <button className="primary" onClick={onLogin}>Sign in</button>
        </div>
      </div>
    </div>
  );
}