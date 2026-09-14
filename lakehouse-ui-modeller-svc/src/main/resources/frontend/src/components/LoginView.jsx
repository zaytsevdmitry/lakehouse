import React from 'react';

export default function LoginView({ onLogin }) {
  return (
    <div className="login">
      <div className="login-card">
        <h1>Lakehouse Modeller</h1>
        <p className="muted">
          Sign in to edit lakehouse model metadata. Authentication is handled by the server-side session.
        </p>
        <div className="btn-row">
          <button className="primary" onClick={onLogin}>Sign in</button>
        </div>
      </div>
    </div>
  );
}