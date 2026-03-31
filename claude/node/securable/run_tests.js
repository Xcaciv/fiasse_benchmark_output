'use strict';
const {spawn} = require('child_process');
const fs = require('fs');
const path = require('path');
const cwd = __dirname;
let output = 'STARTED\n';
const outfile = path.join(cwd, 'test_results.txt');
fs.writeFileSync(outfile, output);

const jest_bin = path.join(cwd, 'node_modules', '.bin', 'jest');
const proc = spawn(jest_bin, ['--forceExit', '--bail', '--testTimeout=10000'], {
  cwd,
  shell: true,
  windowsHide: true
});

proc.stdout.on('data', d => {
  output += d.toString();
  fs.writeFileSync(outfile, output);
});
proc.stderr.on('data', d => {
  output += d.toString();
  fs.writeFileSync(outfile, output);
});
proc.on('close', code => {
  output += '\nEXIT_CODE: ' + code;
  fs.writeFileSync(outfile, output);
  process.exit(0);
});
proc.on('error', e => {
  output += '\nERROR: ' + e.message;
  fs.writeFileSync(outfile, output);
  process.exit(1);
});
setTimeout(() => {
  output += '\nTIMEOUT_AFTER_70s';
  fs.writeFileSync(outfile, output);
  proc.kill('SIGKILL');
  setTimeout(() => process.exit(0), 1000);
}, 70000);
