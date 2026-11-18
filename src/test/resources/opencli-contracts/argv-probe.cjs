const args = process.argv.slice(2);
console.log(`argc:${args.length}`);
for (const arg of args) {
  console.log(`arg:${Buffer.from(arg, 'utf8').toString('base64')}`);
}
