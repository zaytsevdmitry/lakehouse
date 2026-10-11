import React, { useEffect, useState } from 'react';
import { PrismLight as SyntaxHighlighter } from 'react-syntax-highlighter';
import sql from 'react-syntax-highlighter/dist/esm/languages/prism/sql';
import scala from 'react-syntax-highlighter/dist/esm/languages/prism/scala';
import python from 'react-syntax-highlighter/dist/esm/languages/prism/python';
import r from 'react-syntax-highlighter/dist/esm/languages/prism/r';
import go from 'react-syntax-highlighter/dist/esm/languages/prism/go';
import java from 'react-syntax-highlighter/dist/esm/languages/prism/java';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { fetchDataSetModelScript, fetchScript } from '../api.js';

SyntaxHighlighter.registerLanguage('sql', sql);
SyntaxHighlighter.registerLanguage('scala', scala);
SyntaxHighlighter.registerLanguage('python', python);
SyntaxHighlighter.registerLanguage('r', r);
SyntaxHighlighter.registerLanguage('go', go);
SyntaxHighlighter.registerLanguage('java', java);

const LANGUAGE_OPTIONS = ['SQL', 'Scala', 'Python', 'R', 'GO', 'Java'];

const LANGUAGE_TO_PRISM = {
  SQL: 'sql',
  Scala: 'scala',
  Python: 'python',
  R: 'r',
  GO: 'go',
  Java: 'java',
};

function isDarkTheme() {
  try {
    return document.documentElement.getAttribute('data-theme') === 'dark';
  } catch {
    return false;
  }
}

function usePageTheme() {
  const [dark, setDark] = useState(isDarkTheme);
  useEffect(() => {
    const root = document.documentElement;
    const observer = new MutationObserver(() => setDark(isDarkTheme()));
    observer.observe(root, { attributes: true, attributeFilter: ['data-theme'] });
    return () => observer.disconnect();
  }, []);
  return dark;
}

function CodeBlock({ language, code }) {
  const dark = usePageTheme();
  const style = { ...(dark ? oneDark : oneLight), padding: '0' };
  return (
    <SyntaxHighlighter
      language={LANGUAGE_TO_PRISM[language] || 'sql'}
      style={style}
      customStyle={{ margin: 0, borderRadius: 0, fontSize: 12 }}
    >
      {code || ''}
    </SyntaxHighlighter>
  );
}

function ScriptsTab({ dataSet, language }) {
  const [scripts, setScripts] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!dataSet) {
      setScripts([]);
      return;
    }
    setLoading(true);
    setError('');
    const refs = dataSet.scripts || [];
    Promise.all(
      refs.map((ref) =>
        fetchScript(ref.key)
          .then((content) => ({ ref, content, status: 'ok' }))
          .catch((e) => ({ ref, content: '', status: 'error', message: e.message }))
      )
    )
      .then(setScripts)
      .finally(() => setLoading(false));
  }, [dataSet]);

  if (!dataSet) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }
  if (loading) {
    return <div className="empty-box">Loading...</div>;
  }
  if (error) {
    return <div className="error-box">Error: {error}</div>;
  }
  if (scripts.length === 0) {
    return <div className="empty-box">No scripts found.</div>;
  }
  return (
    <div className="model-scripts">
      {scripts.map((entry) => (
        <div className="model-script" key={entry.ref.key}>
          <div className="model-script-title">{entry.ref.key}</div>
          {entry.status === 'error' ? (
            <div className="error-box">Error: {entry.message}</div>
          ) : (
            <div className="model-code">
              <CodeBlock language={language} code={entry.content} />
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function CompleteTab({ dataSetKeyName, language }) {
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!dataSetKeyName) {
      setCode('');
      return;
    }
    setLoading(true);
    setError('');
    fetchDataSetModelScript(dataSetKeyName)
      .then(setCode)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [dataSetKeyName]);

  if (!dataSetKeyName) {
    return <div className="empty-box">Select a table in the tree.</div>;
  }
  if (loading) {
    return <div className="empty-box">Loading...</div>;
  }
  if (error) {
    return <div className="error-box">Error: {error}</div>;
  }
  return (
    <div className="model-code">
      <CodeBlock language={language} code={code} />
    </div>
  );
}

export default function ModelTab({ dataSet }) {
  const [language, setLanguage] = useState('SQL');
  const [activeTab, setActiveTab] = useState('scripts');

  const dataSetKeyName = dataSet ? dataSet.keyName : null;

  return (
    <div className="model-tab">
      <div className="model-controls">
        <label className="model-controls-label" htmlFor="model-language">
          Language
        </label>
        <select
          id="model-language"
          className="model-language-select"
          value={language}
          onChange={(e) => setLanguage(e.target.value)}
        >
          {LANGUAGE_OPTIONS.map((lang) => (
            <option key={lang} value={lang}>
              {lang}
            </option>
          ))}
        </select>
      </div>
      <div className="model-tabs">
        <div className="model-tab-list">
          <button
            className={`model-tab-item ${activeTab === 'scripts' ? 'model-tab-item--active' : ''}`}
            onClick={() => setActiveTab('scripts')}
          >
            <span className="model-tab-item-text">Scripts</span>
          </button>
          <button
            className={`model-tab-item ${activeTab === 'complete' ? 'model-tab-item--active' : ''}`}
            onClick={() => setActiveTab('complete')}
          >
            <span className="model-tab-item-text">Complete</span>
          </button>
        </div>
        <div className="model-tab-content">
          {activeTab === 'scripts' && <ScriptsTab dataSet={dataSet} language={language} />}
          {activeTab === 'complete' && (
            <CompleteTab dataSetKeyName={dataSetKeyName} language={language} />
          )}
        </div>
      </div>
    </div>
  );
}
