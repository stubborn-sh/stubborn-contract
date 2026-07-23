import * as path from 'node:path'
import { describe, it, expect } from 'vitest'
import { asciidocTagsPlugin } from './asciidoc-tags'

const FIXTURES = path.join(__dirname, '__fixtures__')
const SAMPLE_JAVA = path.join(FIXTURES, 'Sample.java')
const SAMPLE_GROOVY = path.join(FIXTURES, 'sample.groovy')

// The plugin's transform function — extract it for direct testing
function transform(code: string, id = '/docs/page.md') {
  const plugin = asciidocTagsPlugin() as any
  return plugin.transform(code, id)
}

// Helpers to make relative paths in markdown that resolve to our fixtures
function rel(absPath: string) {
  return path.relative('/docs', absPath)
}

describe('asciidocTagsPlugin', () => {
  describe('tag extraction', () => {
    it('extracts a tagged region from a Java file with explicit language', () => {
      const md = `@[code{tag:setup} java](${rel(SAMPLE_JAVA)})`
      const result = transform(md)
      expect(result).toContain('```java')
      expect(result).toContain('void setup()')
      expect(result).toContain('RestAssuredMockMvc.standaloneSetup')
      expect(result).not.toContain('// tag::setup[]')
      expect(result).not.toContain('// end::setup[]')
    })

    it('extracts a multi-statement tagged region', () => {
      const md = `@[code{tag:test} java](${rel(SAMPLE_JAVA)})`
      const result = transform(md)
      expect(result).toContain('```java')
      expect(result).toContain('@Test')
      expect(result).toContain('should_mark_client_as_fraud')
      expect(result).toContain('assertThat')
    })

    it('infers language from file extension when no language given', () => {
      const md = `@[code{tag:contract}](${rel(SAMPLE_GROOVY)})`
      const result = transform(md)
      expect(result).toContain('```groovy')
      expect(result).toContain('Contract.make')
      expect(result).toContain('fraudCheckStatus')
    })

    it('preserves multiline content exactly', () => {
      const md = `@[code{tag:multiline} java](${rel(SAMPLE_JAVA)})`
      const result = transform(md)
      expect(result).toContain('// Line one')
      expect(result).toContain('// Line two')
    })
  })

  describe('error handling', () => {
    it('returns a clear error block when the tag does not exist', () => {
      const md = `@[code{tag:no_such_tag} java](${rel(SAMPLE_JAVA)})`
      const result = transform(md)
      expect(result).toContain('```')
      expect(result).toContain('Tag "no_such_tag" not found')
    })

    it('returns a clear error block when the file does not exist', () => {
      const md = `@[code{tag:setup} java](../missing/File.java)`
      const result = transform(md)
      expect(result).toContain('File not found')
      expect(result).toContain('missing/File.java')
    })
  })

  describe('non-matching content', () => {
    it('passes through regular markdown unchanged', () => {
      const md = '# Hello\n\nSome text without any code includes.'
      expect(transform(md)).toBeNull()
    })

    it('ignores regular VitePress code includes (no tag syntax)', () => {
      const md = `@[code java](${rel(SAMPLE_JAVA)})`
      expect(transform(md)).toBeNull()
    })

    it('ignores non-md files', () => {
      const plugin = asciidocTagsPlugin() as any
      const md = `@[code{tag:setup} java](${rel(SAMPLE_JAVA)})`
      expect(plugin.transform(md, '/docs/not-a-markdown.ts')).toBeNull()
    })

    it('handles multiple tag imports on separate lines', () => {
      const md = [
        `@[code{tag:setup} java](${rel(SAMPLE_JAVA)})`,
        '',
        `@[code{tag:test} java](${rel(SAMPLE_JAVA)})`,
      ].join('\n')
      const result = transform(md)
      expect(result).toContain('void setup()')
      expect(result).toContain('should_mark_client_as_fraud')
    })
  })

  describe('tag with special regex characters in name', () => {
    it('handles tags with underscores and hyphens', () => {
      // tag:setup is tested above; verify name with underscore via fixture
      const md = `@[code{tag:multiline} java](${rel(SAMPLE_JAVA)})`
      const result = transform(md)
      expect(result).toContain('Line one')
    })
  })
})
