import React from 'react';

export default function Modal({ title, onClose, children, wide, className }) {
  const classes = [
    className ? 'modal' : '',
    wide ? 'modal-wide' : '',
    className || '',
  ].filter(Boolean).join(' ');
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className={`modal ${classes}`.trim()} onClick={(e) => e.stopPropagation()}>
        <h3>{title}</h3>
        {children}
      </div>
    </div>
  );
}