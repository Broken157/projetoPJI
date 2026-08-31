import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './app/App';
import './styles/foundation.css';
import './styles/password-recovery.css';
import './styles/rf03-react.css';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
