// Generates reference tables in docs/reference/_generated/ from the real Java source, so the
// documentation of the YAML contract schema, matcher enums, predefined regexes and test
// frameworks can never drift from the code.
//
// Design: the *names* (enum constants, model field names) are extracted from the Java source and
// are authoritative. The *descriptions* live in the curated DESCRIPTIONS map below. The generator
// FAILS if the two sets disagree (a field added/removed/renamed in code with no matching
// description, or vice-versa), so a schema change forces a docs change. Run:
//   npm run docs:gen          # (re)write the partials
//   npm run docs:gen:check    # write + fail if anything changed (CI gate)
import { readFileSync, writeFileSync, mkdirSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = join(ROOT, 'docs', 'reference', '_generated');
const YAML_CONTRACT = 'stubborn-contract-verifier/src/main/java/sh/stubborn/contract/verifier/converter/YamlContract.java';
const TEST_FRAMEWORK = 'stubborn-contract-verifier/src/main/java/sh/stubborn/contract/verifier/config/TestFramework.java';

const read = (p) => readFileSync(join(ROOT, p), 'utf8');

// --- Java source parsing -------------------------------------------------------------------

// Body of `... <keyword> <name> {` matched by brace depth.
function blockBody(src, header) {
	const start = src.indexOf(header);
	if (start < 0) throw new Error(`not found: ${header}`);
	let i = src.indexOf('{', start), depth = 0, begin = i;
	for (; i < src.length; i++) {
		if (src[i] === '{') depth++;
		else if (src[i] === '}') { depth--; if (depth === 0) return src.slice(begin + 1, i); }
	}
	throw new Error(`unbalanced: ${header}`);
}

function enumConstants(src, name) {
	let body = blockBody(src, `enum ${name} {`);
	body = body.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, ''); // strip comments
	const head = body.split(';')[0]; // constant list ends at the first ';' (before fields/ctor)
	// split on top-level commas only (constructor args may contain their own commas)
	const parts = [];
	let depth = 0, cur = '';
	for (const ch of head) {
		if (ch === '(' || ch === '<') depth++;
		else if (ch === ')' || ch === '>') depth--;
		if (ch === ',' && depth === 0) { parts.push(cur); cur = ''; }
		else cur += ch;
	}
	parts.push(cur);
	return parts.map((s) => (s.trim().match(/^([A-Za-z_]\w*)/) || [])[1]).filter(Boolean);
}

// Public instance fields declared directly in a class body (excludes methods, static constants,
// and anything inside a nested type). For the outer YamlContract, pass stopAtNested=true to stop
// at the first nested class/enum.
function classFields(src, header, stopAtNested = false) {
	let body = blockBody(src, header);
	if (stopAtNested) {
		const m = body.search(/\n\s*(public|private|protected)?\s*(static\s+)?(class|enum)\s/);
		if (m >= 0) body = body.slice(0, m);
	}
	const fields = [];
	for (const line of body.split('\n')) {
		if (/\bstatic\b/.test(line)) continue;
		// public [@Nullable] <type> <name> (= ... | ;)   — methods have '(' after the name, so excluded
		const m = line.match(/^\s*public\s+(?:@Nullable\s+)?.*?\b(\w+)\s*(?:=[^=]|;)/);
		if (m) fields.push(m[1]);
	}
	return fields;
}

// --- description registry ------------------------------------------------------------------

const DESCRIPTIONS = {
	topLevel: {
		description: 'Human-readable description of the scenario.',
		name: 'Name for the generated test method (auto-generated from the file name if absent).',
		priority: 'Stub priority — a lower number wins over a higher one.',
		ignored: 'Exclude from test generation; the stub is still generated.',
		inProgress: 'Mark as in-progress; generates a `@Disabled`/skipped test, the stub is still usable.',
		label: 'Identifier used to trigger a messaging contract from `StubTrigger`.',
		metadata: 'Free-form metadata map passed through to converters and tooling.',
		request: 'Incoming HTTP request definition (HTTP contracts).',
		response: 'HTTP response to return (HTTP contracts).',
		input: 'Input message definition (messaging contracts).',
		outputMessage: 'Output message definition (messaging contracts).',
	},
	request: {
		method: 'HTTP method: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, `OPTIONS`, `TRACE`.',
		url: 'Exact URL (including query string). For a regex URL use `matchers.url`.',
		urlPath: 'Exact path without the query string.',
		queryParameters: 'Query-parameter matchers.',
		headers: 'Request header matchers.',
		cookies: 'Cookie matchers.',
		body: 'Expected request body (JSON, XML or plain string).',
		bodyFromFile: 'Read the request body from this classpath file.',
		bodyFromFileAsBytes: 'Read the request body as raw bytes from this classpath file.',
		matchers: 'Per-element matchers (`url`, `body`, `headers`, `queryParameters`, `cookies`, `multipart`) — see the matcher entry below.',
		multipart: 'Multipart request definition.',
	},
	response: {
		status: 'HTTP status code.',
		headers: 'Response headers.',
		cookies: 'Response cookies.',
		body: 'Response body.',
		bodyFromFile: 'Read the body from this classpath file.',
		bodyFromFileAsBytes: 'Read the body as raw bytes from this classpath file.',
		matchers: 'Per-element matchers asserted in the generated consumer test — see the matcher entry below.',
		async: 'Respond asynchronously.',
		fixedDelayMilliseconds: 'Artificial delay for the WireMock stub.',
	},
	input: {
		triggeredBy: 'Name of a method on the test base class that triggers the message.',
		assertThat: 'Name of a method called after the message is processed (for assertions).',
	},
	outputMessage: {
		sentTo: 'Destination (topic/queue/binding) the producer writes to.',
		body: 'Expected message payload.',
		bodyFromFile: 'Read the payload from this classpath file.',
		bodyFromFileAsBytes: 'Read the payload as raw bytes from this classpath file.',
		assertThat: 'Name of a method called after the message is sent (for assertions).',
		headers: 'Expected message headers.',
		matchers: 'Per-element matchers for the message body/headers — see the matcher entry below.',
	},
	matcherEntry: {
		path: 'JSONPath expression the matcher applies to (e.g. `$.items[0].id`).',
		type: 'Matcher type (see the matcher-type tables).',
		value: 'Value for the matcher (required for `by_regex`, `by_equality`, `by_command`).',
		predefined: 'One of the predefined regex shortcuts (see below) instead of a raw `value`.',
		regexType: 'Coerce the matched value to this type (e.g. `as_integer`, `as_double`).',
		minOccurrence: 'For array paths: minimum number of matching elements.',
		maxOccurrence: 'For array paths: maximum number of matching elements.',
	},
	predefinedRegex: {
		only_alpha_unicode: 'Unicode letters only.',
		number: 'Integer or decimal number.',
		any_double: 'Decimal number.',
		any_boolean: '`true` or `false`.',
		ip_address: 'IPv4 address.',
		hostname: 'Host name.',
		email: 'Email address.',
		url: 'URL.',
		uuid: 'UUID.',
		iso_date: 'ISO-8601 date (`yyyy-MM-dd`).',
		iso_date_time: 'ISO-8601 date-time.',
		iso_time: 'ISO-8601 time.',
		iso_8601_with_offset: 'ISO-8601 date-time with a zone offset.',
		non_empty: 'Any non-empty string.',
		non_blank: 'Any non-blank string.',
	},
	stubMatcherType: {
		by_equality: 'Exact value match.',
		by_type: 'Same JSON type (string, number, boolean, object, array).',
		by_regex: 'Value matches the `value` regex (or a `predefined` shortcut).',
		by_date: 'ISO-8601 date.',
		by_time: 'ISO-8601 time.',
		by_timestamp: 'ISO-8601 date-time.',
		by_null: 'Value is null.',
	},
	testMatcherType: {
		by_equality: 'Exact value match.',
		by_type: 'Same JSON type.',
		by_regex: 'Value matches the `value` regex (or a `predefined` shortcut).',
		by_date: 'ISO-8601 date.',
		by_time: 'ISO-8601 time.',
		by_timestamp: 'ISO-8601 date-time.',
		by_command: 'Call `value` as a Java method to assert the value.',
		by_null: 'Value is null.',
	},
	testFramework: {
		JUNIT5: 'JUnit 5 (Jupiter). **The default.**',
		SPOCK: 'Spock.',
		TESTNG: 'TestNG.',
		CUSTOM: 'A custom framework supplied via configuration.',
	},
};

// --- table rendering + validation ----------------------------------------------------------

const WARNING = '<!-- GENERATED by docs/.vitepress/reference-gen.mjs from the Java source — do not edit by hand. Run `npm run docs:gen`. -->\n';

function table(names, descs, label, header = ['Field', 'Description']) {
	const codeSet = new Set(names), descSet = new Set(Object.keys(descs));
	const missing = names.filter((n) => !descSet.has(n));
	const extra = Object.keys(descs).filter((n) => !codeSet.has(n));
	if (missing.length || extra.length) {
		console.error(`\n[reference-gen] ${label} is out of sync with the Java source:`);
		if (missing.length) console.error(`  in code but NOT described (add to reference-gen.mjs): ${missing.join(', ')}`);
		if (extra.length) console.error(`  described but NOT in code (remove/rename): ${extra.join(', ')}`);
		process.exitCode = 2;
	}
	const rows = names.map((n) => `| \`${n}\` | ${descs[n] ?? '—'} |`);
	return `${WARNING}| ${header[0]} | ${header[1]} |\n|---|---|\n${rows.join('\n')}\n`;
}

// --- generate ------------------------------------------------------------------------------

const yc = read(YAML_CONTRACT);
const tf = read(TEST_FRAMEWORK);

const outputs = {
	'yaml-top-level.md': table(classFields(yc, 'class YamlContract {', true), DESCRIPTIONS.topLevel, 'YAML top-level fields'),
	'yaml-request.md': table(classFields(yc, 'class Request {'), DESCRIPTIONS.request, 'YAML request fields'),
	'yaml-response.md': table(classFields(yc, 'class Response {'), DESCRIPTIONS.response, 'YAML response fields'),
	'yaml-input.md': table(classFields(yc, 'class Input {'), DESCRIPTIONS.input, 'YAML input fields'),
	'yaml-output-message.md': table(classFields(yc, 'class OutputMessage {'), DESCRIPTIONS.outputMessage, 'YAML outputMessage fields'),
	'yaml-matcher-entry.md': table(classFields(yc, 'class BodyStubMatcher {'), DESCRIPTIONS.matcherEntry, 'YAML matcher entry fields'),
	'yaml-stub-matcher-types.md': table(enumConstants(yc, 'StubMatcherType'), DESCRIPTIONS.stubMatcherType, 'StubMatcherType', ['`type` (request/stub side)', 'Description']),
	'yaml-test-matcher-types.md': table(enumConstants(yc, 'TestMatcherType'), DESCRIPTIONS.testMatcherType, 'TestMatcherType', ['`type` (response/test side)', 'Description']),
	'yaml-predefined-regex.md': table(enumConstants(yc, 'PredefinedRegex'), DESCRIPTIONS.predefinedRegex, 'PredefinedRegex', ['`predefined`', 'Description']),
	'test-frameworks.md': table(enumConstants(tf, 'TestFramework'), DESCRIPTIONS.testFramework, 'TestFramework', ['Value', 'Description']),
};

mkdirSync(OUT, { recursive: true });
for (const [file, content] of Object.entries(outputs)) writeFileSync(join(OUT, file), content);

// --- drift guard: reference docs and example fixtures must not use schema names that don't
// exist in the model (the exact drift this harness was built to kill). ------------------------
const FORBIDDEN = ['predefinedRegex', 'messageFrom', 'messageBody', 'messageHeaders', 'urlPattern',
	'urlPathPattern', 'by_empty', 'iso_date_time_with_millis'];
function scanForbidden(dir, exts) {
	for (const entry of readdirSync(join(ROOT, dir), { withFileTypes: true })) {
		if (entry.isDirectory() || !exts.some((e) => entry.name.endsWith(e))) continue;
		if (dir.endsWith('_generated')) continue;
		const text = readFileSync(join(ROOT, dir, entry.name), 'utf8');
		for (const tok of FORBIDDEN) {
			if (new RegExp(`\\b${tok}\\b`).test(text)) {
				console.error(`[reference-gen] ${dir}/${entry.name} uses '${tok}', which is not a real schema name.`);
				process.exitCode = 2;
			}
		}
	}
}
scanForbidden('docs/reference', ['.md']);
scanForbidden('docs/examples', ['.yml', '.yaml']);

const n = Object.keys(outputs).length;
if (process.exitCode) {
	console.error(`\n[reference-gen] FAILED — reconcile the descriptions above with the Java source.\n`);
} else {
	console.log(`[reference-gen] wrote ${n} reference partials to docs/reference/_generated/`);
}
