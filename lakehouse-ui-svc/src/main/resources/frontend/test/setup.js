import '@testing-library/jest-dom/vitest';

class ResizeObserverMock {
  constructor(callback) {
    this.callback = callback;
  }
  observe(target) {
    globalThis.__rfObserved = (globalThis.__rfObserved || 0) + 1;
    const rect = { x: 0, y: 0, top: 0, left: 0, right: 200, bottom: 120, width: 200, height: 120 };
    queueMicrotask(() => {
      globalThis.__rfCallbacks = (globalThis.__rfCallbacks || 0) + 1;
      this.callback([{ target, contentRect: rect }], this);
    });
  }
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserverMock;

if (!globalThis.DOMMatrixReadOnly) {
  class DOMMatrixReadOnly {
    constructor(init) {
      const m = [1, 0, 0, 1, 0, 0];
      if (typeof init === 'string') {
        const match = /matrix\(\s*(-?[\d.eE-]+)\s*,\s*(-?[\d.eE-]+)\s*,\s*(-?[\d.eE-]+)\s*,\s*(-?[\d.eE-]+)\s*,\s*(-?[\d.eE-]+)\s*,\s*(-?[\d.eE-]+)\s*\)/.exec(init);
        if (match) {
          for (let i = 0; i < 6; i += 1) m[i] = parseFloat(match[i + 1]);
        }
      } else if (init && Array.isArray(init.m)) {
        for (let i = 0; i < 6; i += 1) m[i] = Number(init.m[i]) || 0;
      }
      this.a = m[0];
      this.b = m[1];
      this.c = m[2];
      this.d = m[3];
      this.e = m[4];
      this.f = m[5];
      this.m11 = m[0];
      this.m12 = m[1];
      this.m13 = 0;
      this.m14 = 0;
      this.m21 = m[2];
      this.m22 = m[3];
      this.m23 = 0;
      this.m24 = 0;
      this.m31 = 0;
      this.m32 = 0;
      this.m33 = 1;
      this.m34 = 0;
      this.m41 = m[4];
      this.m42 = m[5];
      this.m43 = 0;
      this.m44 = 1;
      this.is2D = true;
      this.isIdentity = m[0] === 1 && m[3] === 1 && (m[1] === 0) && (m[2] === 0) && (m[4] === 0) && (m[5] === 0);
    }
    static fromMatrix(other) {
      return new DOMMatrixReadOnly(`matrix(${other.a},${other.b},${other.c},${other.d},${other.e},${other.f})`);
    }
    static fromFloat32Array(m) {
      return new DOMMatrixReadOnly({
        m: [m[0], m[1], m[4], m[5], m[12], m[13]],
      });
    }
  }
  globalThis.DOMMatrixReadOnly = DOMMatrixReadOnly;
}

if (!Element.prototype.setPointerCapture) {
  Element.prototype.setPointerCapture = () => {};
}
if (!Element.prototype.hasPointerCapture) {
  Element.prototype.hasPointerCapture = () => false;
}
if (!Element.prototype.releasePointerCapture) {
  Element.prototype.releasePointerCapture = () => {};
}

Element.prototype.getBoundingClientRect = function getBoundingClientRect() {
  return {
    x: 0,
    y: 0,
    top: 0,
    left: 0,
    right: 800,
    bottom: 600,
    width: 800,
    height: 600,
    toJSON() {},
  };
};

Object.defineProperties(HTMLElement.prototype, {
  offsetWidth: {
    configurable: true,
    get() {
      const value = parseFloat(this.getAttribute('data-test-width'));
      return Number.isFinite(value) ? value : 200;
    },
  },
  offsetHeight: {
    configurable: true,
    get() {
      const value = parseFloat(this.getAttribute('data-test-height'));
      return Number.isFinite(value) ? value : 120;
    },
  },
  offsetLeft: {
    configurable: true,
    get() {
      return 0;
    },
  },
  offsetTop: {
    configurable: true,
    get() {
      return 0;
    },
  },
});

window.matchMedia = window.matchMedia || (() => ({
  matches: false,
  addListener() {},
  removeListener() {},
  addEventListener() {},
  removeEventListener() {},
  dispatchEvent() { return false; },
}));