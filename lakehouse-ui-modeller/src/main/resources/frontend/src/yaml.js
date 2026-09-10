/**
 * Minimal YAML (subset) reader/writer for lakehouse config documents.
 * Supports maps, lists, scalars (incl. quoted strings), indentation-based
 * nesting and inline list objects ("- key: value"). Comments and anchors are
 * dropped. The backend re-validates everything with its own YAML parser.
 */

function stripComment(line) {
  let inSingle = false;
  let inDouble = false;
  for (let i = 0; i < line.length; i += 1) {
    const c = line[i];
    if (c === "'" && !inDouble) inSingle = !inSingle;
    else if (c === '"' && !inSingle) inDouble = !inDouble;
    else if (c === '#' && !inSingle && !inDouble && (i === 0 || line[i - 1] === ' ' || line[i - 1] === '\t')) {
      return line.slice(0, i);
    }
  }
  return line;
}

function parseScalar(raw) {
  const value = raw.trim();
  if (value === '' || value === 'null' || value === '~') return null;
  if (value.startsWith('"') && value.endsWith('"') && value.length >= 2) {
    try { return JSON.parse(value); } catch (e) { /* keep raw */ }
  }
  if (value.startsWith("'") && value.endsWith("'") && value.length >= 2) {
    return value.slice(1, -1).replace(/''/g, "'");
  }
  if (value === 'true') return true;
  if (value === 'false') return false;
  if (/^-?\d+$/.test(value)) {
    const n = parseInt(value, 10);
    if (Number.isSafeInteger(n)) return n;
  }
  if (/^-?\d+\.\d+$/.test(value)) return parseFloat(value);
  return value;
}

function tokenize(text) {
  const lines = (text || '').split('\n').map((l) => l.replace(/\r$/, ''));
  const tokens = [];
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    const stripped = stripComment(line);
    if (/^\s*$/.test(stripped)) continue;
    const indent = stripped.match(/^\s*/)[0].length;
    const content = stripped.slice(indent).trim();
    if (!content) continue;
    if (content === '-') {
      tokens.push({ indent, kind: 'list', key: null, value: null });
    } else if (/^-\s/.test(stripped.slice(indent)) || content.startsWith('-')) {
      const body = content.slice(1).trim();
      const m = body.match(/^([^:]+?):\s*(.*)$/);
      if (m) {
        tokens.push({ indent, kind: 'listKey', key: m[1].replace(/^["']|["']$/g, '').trim(), value: m[2] });
      } else {
        tokens.push({ indent, kind: 'listScalar', key: null, value: body });
      }
    } else {
      const m = content.match(/^([^:]+?):\s*(.*)$/);
      if (m) {
        const key = m[1].replace(/^["']|["']$/g, '').trim();
        const rest = m[2];
        const blockMatch = rest.match(/^([|>])([+-]?)\s*$/);
        if (blockMatch) {
          const block = [];
          let j = i + 1;
          while (j < lines.length) {
            const sub = lines[j];
            if (sub.trim() === '') {
              block.push('');
              j += 1;
              continue;
            }
            const subIndent = sub.match(/^\s*/)[0].length;
            if (subIndent <= indent) break;
            block.push(sub);
            j += 1;
          }
          tokens.push({ indent, kind: 'block', key, block, chomp: blockMatch[2] });
          i = j - 1;
        } else {
          tokens.push({ indent, kind: 'key', key, value: rest });
        }
      } else {
        tokens.push({ indent, kind: 'scalar', key: null, value: content });
      }
    }
  }
  return tokens;
}

function parseScalarToken(v) {
  return v && v.trim() !== '' ? parseScalar(v) : null;
}

/** Parent decides whether a mapping/list continues at the same indentation. */
function parseMapping(tokens, i, indent) {
  const out = {};
  while (i < tokens.length && tokens[i].indent === indent
      && (tokens[i].kind === 'key' || tokens[i].kind === 'block')) {
    const tok = tokens[i];
    if (tok.kind === 'block') {
      out[tok.key] = blockValue(tok);
      i += 1;
    } else if (tok.value && tok.value.trim() !== '') {
      out[tok.key] = parseScalarToken(tok.value);
      i += 1;
    } else {
      const child = parseBlock(tokens, i + 1, indent + 1);
      out[tok.key] = child.node;
      i = child.next;
    }
  }
  return { node: out, next: i };
}

/** Strips the block indentation and applies the requested chomping ('|', '|-', '|+'). */
function blockValue(tok) {
  let minIndent = Infinity;
  for (const line of tok.block) {
    if (line.trim() === '') continue;
    const m = line.match(/^\s*/)[0].length;
    if (m < minIndent) minIndent = m;
  }
  if (!Number.isFinite(minIndent)) minIndent = 2;
  const body = tok.block.map((line) => (line.trim() === '' ? '' : line.slice(Math.min(minIndent, line.length)))).join('\n');
  if (tok.chomp === '-') return body.replace(/\n+$/, '');
  if (tok.chomp === '+') return body.replace(/\n*$/, '') + '\n';
  return body.replace(/\n*$/, '') + '\n';
}

function parseList(tokens, i, indent) {
  const out = [];
  while (i < tokens.length && tokens[i].indent === indent
      && tokens[i].kind.startsWith('list')) {
    const tok = tokens[i];
    if (tok.kind === 'listScalar') {
      out.push(parseScalarToken(tok.value));
      i += 1;
    } else if (tok.kind === 'list') {
      const child = parseBlock(tokens, i + 1, indent + 1);
      out.push(child.node);
      i = child.next;
      if (i < tokens.length && tokens[i].indent === indent && tokens[i].kind === 'list') {
        // handled by loop
      }
    } else {
      // listKey: object starting with "key: value"
      const obj = {};
      if (tok.value && tok.value.trim() !== '') {
        obj[tok.key] = parseScalarToken(tok.value);
        i += 1;
      } else {
        const child = parseBlock(tokens, i + 1, indent + 1);
        obj[tok.key] = child.node;
        i = child.next;
      }
      // additional keys for the same object at indent+1 (deeper)
      const contToken = tokens[i];
      const contIndent = contToken && contToken.indent > indent ? contToken.indent : indent + 1;
      const rest = parseMapping(tokens, i, contIndent);
      Object.assign(obj, rest.node);
      i = rest.next;
      out.push(obj);
    }
  }
  return { node: out, next: i };
}

function parseBlock(tokens, i, indent) {
  if (i >= tokens.length) return { node: null, next: i };
  const tok = tokens[i];
  if (tok.indent > indent) {
    // deeper than the requested indentation: descend to the actual content column
    return parseBlock(tokens, i, tok.indent);
  }
  if (tok.indent < indent) {
    // we've exited the current block
    return { node: null, next: i };
  }
  if (tok.kind.startsWith('list')) return parseList(tokens, i, indent);
  if (tok.kind === 'key' || tok.kind === 'block') return parseMapping(tokens, i, indent);
  return { node: parseScalarToken(tok.value), next: i + 1 };
}

export function parseYaml(text) {
  const tokens = tokenize(text);
  if (tokens.length === 0) return {};
  const result = parseBlock(tokens, 0, tokens[0].indent);
  return result.node ?? {};
}

/* ------------------------------------------------------------------ */

const KEY_SAFE = /^[A-Za-z0-9_.-]+$/;

function quoteScalar(value) {
  if (value === null) return 'null';
  const str = String(value);
  const needsQuotes =
    str === ''
    || /^[\s]|[\s]$/.test(str)
    || /^['"!&*{}[\],#|>%@`]/.test(str)
    || /[#:]/.test(str);
  if (needsQuotes) return JSON.stringify(str);
  return str;
}

/** Serializes a JS object/list/scalar tree to YAML text. */
export function stringifyYaml(node) {
  const lines = [];
  writeNode(lines, node, 0);
  return lines.length ? lines.join('\n') : '';
}

function writeNode(lines, node, indent) {
  const pad = ' '.repeat(indent);
  if (Array.isArray(node)) {
    if (node.length === 0) {
      lines.push(`${pad}[]`);
      return;
    }
    for (const item of node) {
      if (Array.isArray(item)) {
        lines.push(`${pad}-`);
        writeNode(lines, item, indent + 2);
      } else if (item !== null && typeof item === 'object' && Object.keys(item).length > 0) {
        const entries = Object.entries(item);
        const [k, v] = entries[0];
        writeInline(lines, `${pad}- `, k, v);
        for (const [k2, v2] of entries.slice(1)) {
          writeInline(lines, `${' '.repeat(indent + 2)}`, k2, v2);
        }
      } else {
        lines.push(`${pad}- ${quoteScalar(item)}`);
      }
    }
  } else if (node !== null && typeof node === 'object') {
    const entries = Object.entries(node);
    if (entries.length === 0) {
      lines.push(`${pad}{}`);
      return;
    }
    for (const [k, v] of entries) {
      writeInline(lines, pad, k, v);
    }
  } else {
    lines.push(`${pad}${quoteScalar(node)}`);
  }
}

function writeInline(lines, prefixPad, key, value) {
  const k = KEY_SAFE.test(key) ? key : JSON.stringify(key);
  const entryPad = prefixPad.endsWith('- ') ? prefixPad : prefixPad;
  const indent = prefixPad.length + 2;
  if (value === null || value === undefined) {
    lines.push(`${entryPad}${k}:`);
  } else if (typeof value === 'string' && value.includes('\n')) {
    // Block scalar preserves the exact text of the code/value field.
    const clip = value.endsWith('\n');
    const body = value.replace(/\n$/, '');
    lines.push(`${entryPad}${k}: ${clip ? '|' : '|-'}`);
    for (const line of body.split('\n')) {
      lines.push(`${' '.repeat(indent)}${line}`);
    }
  } else if (Array.isArray(value)) {
    if (value.length === 0) {
      lines.push(`${entryPad}${k}: []`);
    } else {
      lines.push(`${entryPad}${k}:`);
      writeNode(lines, value, indent);
    }
  } else if (value !== null && typeof value === 'object' && Object.keys(value).length === 0) {
    lines.push(`${entryPad}${k}: {}`);
  } else if (value !== null && typeof value === 'object') {
    lines.push(`${entryPad}${k}:`);
    writeNode(lines, value, indent);
  } else {
    lines.push(`${entryPad}${k}: ${quoteScalar(value)}`);
  }
}