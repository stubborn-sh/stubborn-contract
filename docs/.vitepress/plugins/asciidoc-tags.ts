/**
 * VitePress plugin that adds Asciidoctor-compatible tagged-region code imports.
 *
 * Usage in Markdown:
 *   @[code{tag:my_tag} java](../../path/to/Source.java)
 *   @[code{tag:my_tag}](../../path/to/contract.groovy)
 *
 * Source files mark regions with:
 *   // tag::my_tag[]
 *   ... code ...
 *   // end::my_tag[]
 *
 * Also supports hash-comment markers (YAML, Python, shell):
 *   # tag::my_tag[]  and  # end::my_tag[]
 */

import * as fs from 'node:fs'
import * as path from 'node:path'
import type { Plugin } from 'vite'

const INCLUDE_RE = /^@\[code\{tag:([^}]+)\}(?:\s+(\w+))?\]\(([^)]+)\)/gm

export function asciidocTagsPlugin(): Plugin {
  return {
    name: 'vitepress-asciidoc-tags',
    enforce: 'pre',
    transform(code: string, id: string) {
      if (!id.endsWith('.md')) return null
      const dir = path.dirname(id)
      let changed = false
      const result = code.replace(
        INCLUDE_RE,
        (_match, tag: string, lang: string | undefined, filePath: string) => {
          changed = true
          const abs = path.resolve(dir, filePath)
          if (!fs.existsSync(abs)) {
            return `\`\`\`\n[File not found: ${filePath}]\n\`\`\``
          }
          const src = fs.readFileSync(abs, 'utf-8')
          const tagStartRe = new RegExp(
            `(?:^|\\n)[ \\t]*(?://|#)[ \\t]*tag::${escapeRe(tag)}\\[\\]`
          )
          const tagEndRe = new RegExp(
            `(?:^|\\n)[ \\t]*(?://|#)[ \\t]*end::${escapeRe(tag)}\\[\\]`
          )
          const startMatch = tagStartRe.exec(src)
          const endMatch = tagEndRe.exec(src)
          if (!startMatch || !endMatch) {
            return `\`\`\`\n[Tag "${tag}" not found in ${filePath}]\n\`\`\``
          }
          // startMatch[0] ends with `[]`; advance past that line to reach content
          const tagLineEnd = startMatch.index + startMatch[0].length
          const firstNewline = src.indexOf('\n', tagLineEnd)
          const contentStart = firstNewline === -1 ? tagLineEnd : firstNewline + 1
          const contentEnd = endMatch.index
          const snippet = src.slice(contentStart, contentEnd).trimEnd()
          const language = lang ?? path.extname(abs).slice(1) ?? ''
          return `\`\`\`${language}\n${snippet}\n\`\`\``
        }
      )
      return changed ? result : null
    }
  }
}

function escapeRe(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
