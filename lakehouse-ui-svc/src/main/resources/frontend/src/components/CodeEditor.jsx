import React, { useEffect, useMemo, useRef, useState } from 'react';

const LANGUAGES = [
  { value: 'sql', label: 'SQL' },
  { value: 'python', label: 'Python' },
  { value: 'scala', label: 'Scala' },
  { value: 'java', label: 'Java' },
  { value: 'yaml', label: 'YAML' },
  { value: 'text', label: 'Plain text' },
];

const KEYWORDS = {
  sql: 'SELECT FROM WHERE JOIN INNER LEFT RIGHT OUTER FULL CROSS ON AS WITH INSERT INTO VALUES UPDATE SET DELETE CREATE TABLE ALTER ADD DROP COLUMN CONSTRAINT UNIQUE PRIMARY FOREIGN KEY REFERENCES CHECK NOT NULL DEFAULT AND OR CASE WHEN THEN ELSE END BETWEEN LIKE IN EXISTS GROUP ORDER BY HAVING LIMIT OFFSET DISTINCT UNION ALL EXCEPT INTERSECT PARTITION OVER DESC ASC USING BEGIN COMMIT ROLLBACK TRUNCATE MERGE CAST CONVERT COALESCE NULL'
    .split(/\s+/),
  python: 'def return if elif else for while in not and or import from as class try except finally with lambda pass break continue None True False yield global raise del assert is'
    .split(/\s+/),
  scala: 'def val var if else for while return import from as class object trait extends with match case try catch finally new null this super throw yield'.split(/\s+/),
  java: 'public private protected static final void class interface extends implements new return if else for while switch case try catch throw throws import package this null true false int long double float boolean string char byte'.split(/\s+/),
  yaml: ['kind', 'key', 'value'],
  text: [],
};

/**
 * Jinjava helpers shipped with the engine (JinJavaFactory). Surfaced in the
 * ctrl+space completion next to the words already present in the document.
 */
const JINJA_COMPLETIONS = [
  { label: 'adddays', hint: 'addDaysISO(dateTimeStr, days)' },
  { label: 'addmonths', hint: 'addMonthsISO(dateTimeStr, months)' },
  { label: 'ref', hint: 'ref(dataSetKeyName)' },
  { label: 'refCat', hint: 'refCat(dataSetKeyName)' },
  { label: 'refCatSchema', hint: 'refCatSchema(dataSetKeyName)' },
  { label: 'extractColumnsDDL', hint: 'extractColumnsDDL(columnList)' },
  { label: 'extractMergeOn', hint: 'extractMergeOn(dataSetMap, targetAlias, queryAlias)' },
  { label: 'extractMergeUpdate', hint: 'extractMergeUpdate(dataSetMap, queryAlias)' },
  { label: 'extractMergeInsertValues', hint: 'extractMergeInsertValues(dataSetMap, queryAlias)' },
  { label: 'extractColumnsCS', hint: 'extractColumnsCS(dataSetMap)' },
  { label: 'getTaskFullName', hint: 'getTaskFullName()' },
  { label: 'dataSets', hint: 'dataSets[\'<key>\']' },
  { label: 'intervalStartDateTime', hint: 'intervalStartDateTime' },
  { label: 'intervalEndDateTime', hint: 'intervalEndDateTime' },
];

function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

const TOKEN_RE = /(--[^\n]*)|(\/\*[\s\S]*?\*\/)|(#[^\n]*)|('(?:[^'\\]|\\.)*'|"(?:[^"\\]|\\.)*")|(\{[\s\S]*?\})|(\b\d+(?:\.\d+)?\b)|(\b[A-Za-z_][A-Za-z0-9_]*\b)/g;

function highlight(code, language) {
  const kwSet = new Set((KEYWORDS[language] || []).map((k) => k.toUpperCase()));
  let out = '';
  let last = 0;
  for (const match of code.matchAll(TOKEN_RE)) {
    const i = match.index;
    out += escapeHtml(code.slice(last, i));
    const whole = match[0];
    const [, comment, block, hash] = match;
    const str = match[4];
    const jinja = match[5];
    const num = match[6];
    const word = match[7];
    if (comment || block || hash) out += `<span class="tok-comment">${escapeHtml(whole)}</span>`;
    else if (str) out += `<span class="tok-str">${escapeHtml(whole)}</span>`;
    else if (jinja) out += `<span class="tok-jinja">${escapeHtml(whole)}</span>`;
    else if (num) out += `<span class="tok-num">${escapeHtml(whole)}</span>`;
    else if (word) out += kwSet.has(word.toUpperCase()) ? `<span class="tok-kw">${escapeHtml(whole)}</span>` : escapeHtml(whole);
    last = i + whole.length;
  }
  out += escapeHtml(code.slice(last));
  return out;
}

function wordBefore(text, pos) {
  const m = text.slice(0, pos).match(/[A-Za-z_][A-Za-z0-9_]*$/);
  return m ? m[0] : '';
}

export default function CodeEditor({ value = '', onChange, readOnly = false, className }) {
  const [language, setLanguage] = useState('sql');
  const [selection, setSelection] = useState({ start: 0, end: 0 });
  const [suggest, setSuggest] = useState(null); // { items, index, top, left, prefix }
  const taRef = useRef(null);
  const preRef = useRef(null);
  const mainRef = useRef(null);
  const gutterRef = useRef(null);

  const linesCount = useMemo(() => (value ? value.split('\n').length : 1), [value]);
  const lineNumbers = useMemo(() => {
    const nums = [];
    for (let i = 1; i <= linesCount; i += 1) nums.push(i);
    return nums;
  }, [linesCount]);

  const highlighted = useMemo(() => highlight(value, language), [value, language]);

  useEffect(() => {
    setSuggest(null);
  }, [value, language]);

  const syncScroll = () => {
    const ta = taRef.current;
    if (!ta) return;
    if (preRef.current) {
      preRef.current.scrollTop = ta.scrollTop;
      preRef.current.scrollLeft = ta.scrollLeft;
    }
    if (gutterRef.current) gutterRef.current.scrollTop = ta.scrollTop;
  };

  const positionPopup = () => {
    const ta = taRef.current;
    const host = mainRef.current;
    if (!ta || !host) return { top: 10, left: 10 };
    const pos = ta.selectionStart;
    const before = value.slice(0, pos);
    const lineIndex = (before.match(/\n/g) || []).length;
    const lineStart = before.lastIndexOf('\n') + 1;
    const col = pos - lineStart;
    const lineH = parseFloat(getComputedStyle(ta).lineHeight) || 20;
    const charW = (parseFloat(getComputedStyle(ta).fontSize) || 13) * 0.6;
    const pad = parseFloat(getComputedStyle(ta).paddingLeft) || 10;
    const hostH = host.clientHeight;
    let top = ta.scrollTop + (lineIndex + 1) * lineH + 8;
    if (top > hostH - 140) top = Math.max(8, hostH - 160);
    const left = Math.min(Math.max(4, pad + col * charW - ta.scrollLeft), Math.max(4, host.clientWidth - 320));
    return { top, left };
  };

  const openSuggestions = (e) => {
    const ta = taRef.current;
    if (!ta) return;
    const pos = ta.selectionStart;
    const prefix = wordBefore(ta.value, pos).toLowerCase();
    const fnItems = JINJA_COMPLETIONS
      .filter((c) => !prefix || c.label.toLowerCase().startsWith(prefix))
      .map((c) => ({ ...c, kind: 'fn' }));
    const words = new Set();
    for (const w of (ta.value.match(/[A-Za-z_][A-Za-z0-9_]{2,}/g) || [])) words.add(w.toLowerCase());
    const wordItems = [...words]
      .filter((w) => !prefix || w.startsWith(prefix))
      .filter((w) => !JINJA_COMPLETIONS.some((c) => c.label.toLowerCase() === w))
      .sort()
      .map((w) => ({ label: w, hint: '', kind: 'word' }));
    const items = [...fnItems, ...wordItems];
    if (items.length === 0) {
      setSuggest(null);
      return;
    }
    const pos2 = positionPopup();
    setSuggest({ items, index: 0, top: pos2.top, left: pos2.left, prefix });
    e.preventDefault();
  };

  const insertSuggestion = (item, e) => {
    const ta = taRef.current;
    if (!ta) return;
    const pos = ta.selectionStart;
    const start = pos - wordBefore(ta.value, pos).length;
    const next = ta.value.slice(0, start) + item.label + ta.value.slice(pos);
    setSelection({ start: start + item.label.length, end: start + item.label.length });
    onChange(next);
    setSuggest(null);
    requestAnimationFrame(() => {
      ta.focus();
      ta.setSelectionRange(start + item.label.length, start + item.label.length);
    });
    if (e) e.preventDefault();
  };

  const onKeyDown = (e) => {
    if (e.ctrlKey && (e.key === ' ' || e.code === 'Space')) {
      openSuggestions(e);
      return;
    }
    if (!suggest) return;
    if (e.key === 'ArrowDown') {
      setSuggest((s) => (s ? { ...s, index: (s.index + 1) % s.items.length } : s));
      e.preventDefault();
    } else if (e.key === 'ArrowUp') {
      setSuggest((s) => (s ? { ...s, index: (s.index - 1 + s.items.length) % s.items.length } : s));
      e.preventDefault();
    } else if (e.key === 'Enter' || e.key === 'Tab') {
      insertSuggestion(suggest.items[suggest.index], e);
    } else if (e.key === 'Escape') {
      setSuggest(null);
      e.preventDefault();
    }
  };

  const onSelect = () => {
    if (taRef.current) {
      const { selectionStart, selectionEnd } = taRef.current;
      setSelection({ start: selectionStart, end: selectionEnd });
    }
    if (suggest) {
      // Reposition on caret moves while the list is open.
      const pos2 = positionPopup();
      setSuggest((s) => (s ? { ...s, top: pos2.top, left: pos2.left } : s));
    }
  };

  return (
    <div className={`code-editor${className ? ` ${className}` : ''}`}>
      <div className="code-editor-toolbar">
        <span className="code-editor-toolbar-label">Language</span>
        <select
          value={language}
          onChange={(e) => setLanguage(e.target.value)}
        >
          {LANGUAGES.map((l) => <option key={l.value} value={l.value}>{l.label}</option>)}
        </select>
      </div>
      <div className="code-editor-body">
        <div className="code-editor-gutter" ref={gutterRef}>
          {lineNumbers.map((n) => <div className="code-editor-gutter-line" key={n}>{n}</div>)}
        </div>
        <div className="code-editor-main" ref={mainRef}>
          <pre className="code-highlight" ref={preRef} aria-hidden="true" dangerouslySetInnerHTML={{ __html: highlighted }} />
          <textarea
            ref={taRef}
            className="code-input"
            spellCheck="false"
            value={value}
            readOnly={readOnly || value === undefined}
            onScroll={syncScroll}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={onKeyDown}
            onBlur={() => setSuggest(null)}
            onSelect={onSelect}
            onClick={onSelect}
            onKeyUp={onSelect}
          />
          {suggest && (
            <div className="code-suggest" style={{ top: suggest.top, left: suggest.left }}>
              {suggest.items.map((item, i) => (
                <button
                  key={item.kind + ':' + item.label}
                  className={`code-suggest-item${i === suggest.index ? ' active' : ''}`}
                  onMouseEnter={() => setSuggest((s) => (s ? { ...s, index: i } : s))}
                  onClick={() => insertSuggestion(item)}
                >
                  <span className="code-suggest-label">{item.label}</span>
                  {item.hint && <span className="code-suggest-hint">{item.hint}</span>}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}