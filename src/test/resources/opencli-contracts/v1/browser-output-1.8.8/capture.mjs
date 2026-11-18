import os from 'node:os';
import { syncBuiltinESMExports } from 'node:module';
import { pathToFileURL } from 'node:url';
import path from 'node:path';

os.homedir = () => process.env.OPENCLI_FIXTURE_HOME;
syncBuiltinESMExports();
const browserDir = path.join(path.dirname(process.argv[1]), 'browser');
const { BrowserBridge } = await import(pathToFileURL(path.join(browserDir, 'index.js')));
const { buildAxSnapshot } = await import(pathToFileURL(path.join(browserDir, 'ax-snapshot.js')));
if (process.env.OPENCLI_FIXTURE_FIXED_CLOCK === '1') {
  const OriginalDate = Date;
  globalThis.Date = class extends OriginalDate {
    constructor(...args) { super(...(args.length ? args : [1735689600000])); }
    static now() { return 1735689600000; }
  };
}
const resultUrl = 'http://127.0.0.1:18765/basic.html';
BrowserBridge.prototype.connect = async ({ session }) => {
  if (session !== 'fixture') throw new Error('Unexpected fixture session');
  return {
    session,
    async tabs() {
      return process.env.OPENCLI_FIXTURE_EMPTY === '1'
        ? []
        : [{ index: 0, page: '00017', url: resultUrl, title: 'Fixture page', active: true }];
    },
    async snapshot({ source }) {
      if (source === 'dom') {
        return 'title: Fixture page\n---\n[1] button "Continue"\n---\ninteractive: 1 | iframes: 0';
      }
      if (source !== 'ax') throw new Error('Unexpected snapshot source');
      if (process.env.OPENCLI_FIXTURE_AX_FAILURE === '1') {
        throw Object.assign(new Error('Fixture CDP unavailable'), { code: 'BROWSER_AX_UNAVAILABLE' });
      }
      return buildAxSnapshot({ nodes: [
        { nodeId: '1', role: { value: 'RootWebArea' }, childIds: ['2'] },
        { nodeId: '2', backendDOMNodeId: 22, role: { value: 'button' }, name: { value: 'Continue' } },
      ] }).text;
    },
    async consoleMessages() {
      if (process.env.OPENCLI_FIXTURE_CONSOLE_LEVEL === '1') {
        return [{ level: 'warning', text: 'Fixture warning', timestamp: 1735689600000 }];
      }
      return [{ type: 'warning', text: 'Fixture warning', timestamp: 1735689600000 }];
    },
    async readNetworkCapture() {
      return [{
        url: 'http://127.0.0.1:18765/api/items',
        method: 'GET',
        responseStatus: 200,
        responseContentType: 'application/json',
        responsePreview: '{"items":[{"id":17,"name":"Fixture"}]}',
        timestamp: 1735689600000,
      }];
    },
    async getCookies() { return []; },
    async evaluate(script) {
      if (script.includes('cookieNames:')) {
        return {
          cookieNames: [],
          initialState: { __INITIAL_STATE__: false, __NUXT__: false, __NEXT_DATA__: false, __APOLLO_STATE__: false },
          title: 'Fixture page',
          finalUrl: resultUrl,
        };
      }
      if (script.includes('document.querySelectorAll')) {
        return {
          matches_n: 2,
          entries: [{ nth: 0, ref: 17, tag: 'button', role: 'button', text: 'Continue', attrs: { id: 'fixture' }, visible: true }],
        };
      }
      throw new Error('Unexpected evaluation');
    },
    async startNetworkCapture() { return true; },
    async goto(url) { if (url !== resultUrl) throw new Error('Unexpected fixture URL'); },
    async wait() {},
    async getCurrentUrl() { return resultUrl; },
    getActivePage() { return '00017'; },
  };
};
