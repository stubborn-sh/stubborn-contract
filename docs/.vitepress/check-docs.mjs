// Validates that the docs are self-contained and do not link into the void. Three checks,
// all fail-fast (the script lists every problem and exits non-zero):
//
//   1. INCLUDES / SNIPPETS EXIST — every `<!--@include: X-->` partial and every `<<< X`
//      snippet-import target resolves to a real file on disk. VitePress silently inlines a
//      missing @include as an error string instead of failing the build, so we gate it here.
//
//   2. LOCAL LINKS RESOLVE — every relative / root-absolute Markdown link points at a doc page
//      (or asset) that exists. This backs up VitePress' own dead-link detection with an explicit,
//      readable error, and it covers the cross-site `/stubborn/*` links that VitePress is told to
//      ignore (they are checked as external URLs in step 3 instead).
//
//   3. EXTERNAL LINKS ARE LIVE — every `http(s)://` Markdown link is fetched. A link that
//      "resolves to a 404" (HTTP 404 or 410) is a hard failure — that is exactly what we refuse
//      to ship. Everything else (rate-limiting, bot-blocking 403/429, 5xx, timeouts, DNS/proxy
//      errors) is reported as a non-fatal warning so the gate never flakes on a transient blip.
//      Set SKIP_EXTERNAL=1 to skip this step entirely (e.g. offline local runs).
//
// Run: npm run docs:check
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const DOCS = join(ROOT, 'docs');

const problems = [];
const warnings = [];
const fail = (msg) => problems.push(msg);
const warn = (msg) => warnings.push(msg);

// Cross-site link prefixes that live on another docs site (see `ignoreDeadLinks` in config.ts).
// They are not local pages, so they are validated as external URLs, not as files.
const CROSS_SITE = ['/stubborn/'];

// --- collect the markdown sources ----------------------------------------------------------
function walk(dir, out = []) {
	for (const e of readdirSync(dir, { withFileTypes: true })) {
		const p = join(dir, e.name);
		if (e.isDirectory()) {
			if (e.name === 'node_modules' || e.name === 'dist' || e.name === '.vitepress') continue;
			walk(p, out);
		}
		else if (e.name.endsWith('.md')) {
			out.push(p);
		}
	}
	return out;
}
const mdFiles = walk(DOCS);

// Remove fenced (``` / ~~~) code blocks and inline `code` spans so that URLs and paths shown
// inside code examples are never mistaken for real links or includes.
function stripCode(text) {
	return text
		.replace(/^([ \t]*)(`{3,}|~{3,})[^\n]*\n[\s\S]*?\n\1\2[ \t]*$/gm, '')
		.replace(/`[^`\n]*`/g, '');
}

// --- 1 + 2: file-based checks (no network) -------------------------------------------------
// Resolve a snippet/include target (may carry a `#region` and/or a `{meta}` suffix) to a path.
function resolveIncludePath(fromFile, raw) {
	let spec = raw.trim().replace(/\{[^}]*\}\s*$/, '').replace(/#.*$/, '').trim();
	if (spec.startsWith('@/')) return join(DOCS, spec.slice(2));
	return resolve(dirname(fromFile), spec);
}

// Resolve a local Markdown link to the file(s) that would satisfy it; return the first that
// exists, or null if none do.
function resolveLocalLink(fromFile, target) {
	const clean = target.split('#')[0].split('?')[0];
	if (!clean) return fromFile; // pure in-page anchor
	const bases = [];
	if (clean.startsWith('/')) {
		bases.push(join(DOCS, clean));
		bases.push(join(DOCS, 'public', clean)); // VitePress public assets
	}
	else {
		bases.push(resolve(dirname(fromFile), clean));
	}
	for (const base of bases) {
		const candidates = base.endsWith('/')
			? [join(base, 'index.md')]
			: [base, `${base}.md`, join(base, 'index.md')];
		for (const c of candidates) if (existsSync(c) && statSync(c).isFile()) return c;
	}
	return null;
}

const externalUrls = new Map(); // url -> first file that references it

for (const file of mdFiles) {
	const rel = file.slice(ROOT.length + 1);
	const raw = readFileSync(file, 'utf8');

	// 1. includes + snippet imports (directives live outside code, scan the raw text)
	for (const m of raw.matchAll(/<!--\s*@include:\s*([^>]+?)\s*-->/g)) {
		const p = resolveIncludePath(file, m[1]);
		if (!existsSync(p)) fail(`${rel}: @include target not found: ${m[1].trim()}`);
	}
	for (const m of raw.matchAll(/^[ \t]*<<<\s+(\S+)/gm)) {
		const p = resolveIncludePath(file, m[1]);
		if (!existsSync(p)) fail(`${rel}: <<< snippet import target not found: ${m[1]}`);
	}

	// 2 + 3. links (ignore anything inside code)
	const prose = stripCode(raw);
	const links = [];
	for (const m of prose.matchAll(/\[[^\]]*\]\(\s*(<[^>]+>|[^)\s]+)[^)]*\)/g)) links.push(m[1].replace(/^<|>$/g, ''));
	for (const m of prose.matchAll(/<((?:https?:)\/\/[^>\s]+)>/g)) links.push(m[1]);

	for (const link of links) {
		if (/^(mailto:|tel:)/.test(link)) continue;
		if (/^https?:\/\//.test(link)) {
			if (isCheckableExternal(link) && !externalUrls.has(link)) externalUrls.set(link, rel);
			continue;
		}
		if (link.startsWith('#')) continue; // in-page anchor
		if (CROSS_SITE.some((p) => link.startsWith(p))) continue; // resolved as external elsewhere
		if (resolveLocalLink(file, link) === null) fail(`${rel}: local link does not resolve: ${link}`);
	}
}

// Placeholder / example hosts that appear in prose but are not meant to be real endpoints.
function isCheckableExternal(url) {
	let host;
	try { host = new URL(url).hostname; }
	catch { return false; }
	if (url.includes('${') || url.includes('{{')) return false;
	if (host === 'localhost' || host === '127.0.0.1' || /^(\d+\.){3}\d+$/.test(host)) return false;
	if (/(^|\.)(example|invalid|test|local)$/.test(host)) return false;
	if (/\.example\.(com|org|net)$/.test(host)) return false;
	const PLACEHOLDER = ['your-git-server.com', 'your-broker.example.com', 'some_url', 'foo.bar'];
	if (PLACEHOLDER.includes(host)) return false;
	return true;
}

// --- 3: external liveness (network) --------------------------------------------------------
async function checkExternal(url, ref) {
	const attempt = async (method) => {
		const ctrl = new AbortController();
		const t = setTimeout(() => ctrl.abort(), 15000);
		try {
			return await fetch(url, {
				method,
				redirect: 'follow',
				signal: ctrl.signal,
				headers: { 'User-Agent': 'Mozilla/5.0 (docs-link-check)', Accept: '*/*' },
			});
		}
		finally { clearTimeout(t); }
	};
	for (let i = 0; i < 3; i++) {
		try {
			let res = await attempt('HEAD');
			if (res.status === 405 || res.status === 501) res = await attempt('GET');
			if (res.status === 404 || res.status === 410) {
				fail(`${ref}: external link is dead (HTTP ${res.status}): ${url}`);
				return;
			}
			if (res.status === 429 || res.status >= 500) {
				if (i < 2) { await new Promise((r) => setTimeout(r, 1000 * (i + 1))); continue; }
				warn(`${ref}: could not verify (HTTP ${res.status}, tolerated): ${url}`);
				return;
			}
			if (!res.ok && res.status !== 403 && res.status !== 401)
				warn(`${ref}: unexpected status ${res.status} (tolerated): ${url}`);
			return;
		}
		catch (e) {
			if (i < 2) { await new Promise((r) => setTimeout(r, 1000 * (i + 1))); continue; }
			warn(`${ref}: could not reach (${e.cause?.code || e.name}, tolerated): ${url}`);
		}
	}
}

async function runExternal() {
	const entries = [...externalUrls.entries()];
	const CONCURRENCY = 6;
	let idx = 0;
	async function worker() {
		while (idx < entries.length) {
			const [url, ref] = entries[idx++];
			await checkExternal(url, ref);
		}
	}
	await Promise.all(Array.from({ length: Math.min(CONCURRENCY, entries.length) }, worker));
}

if (!process.env.SKIP_EXTERNAL) {
	console.log(`[check-docs] verifying ${externalUrls.size} external links …`);
	await runExternal();
}
else {
	console.log('[check-docs] SKIP_EXTERNAL set — skipping external link liveness.');
}

// --- report --------------------------------------------------------------------------------
for (const w of warnings) console.warn(`[check-docs] WARN  ${w}`);
if (problems.length) {
	for (const p of problems) console.error(`[check-docs] FAIL  ${p}`);
	console.error(`\n[check-docs] ${problems.length} problem(s) found.`);
	process.exit(1);
}
console.log(`[check-docs] OK — ${mdFiles.length} pages: all includes, snippets and local links resolve` +
	(process.env.SKIP_EXTERNAL ? '.' : ', external links live.'));
