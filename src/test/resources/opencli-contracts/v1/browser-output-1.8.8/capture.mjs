import os from 'node:os';
import { syncBuiltinESMExports } from 'node:module';
import { pathToFileURL } from 'node:url';
import path from 'node:path';

os.homedir = () => process.env.HOME;
syncBuiltinESMExports();
const { BrowserBridge } = await import(pathToFileURL(path.join(path.dirname(process.argv[1]), 'browser/index.js')));
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
    async startNetworkCapture() { return true; },
    async goto(url) { if (url !== resultUrl) throw new Error('Unexpected fixture URL'); },
    async wait() {},
    async getCurrentUrl() { return resultUrl; },
    getActivePage() { return '00017'; },
  };
};
