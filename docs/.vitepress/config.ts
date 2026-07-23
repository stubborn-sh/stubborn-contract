import { defineConfig } from 'vitepress'
import { asciidocTagsPlugin } from './plugins/asciidoc-tags'

export default defineConfig({
  lang: 'en-US',
  title: 'Stubborn Contract',
  description: 'Consumer-driven contract testing for the JVM. Brings TDD to architectural level.',
  base: '/stubborn-contract/',

  head: [
    ['link', { rel: 'icon', href: '/stubborn-contract/favicon.ico' }],
  ],

  vite: {
    plugins: [asciidocTagsPlugin()],
  },

  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'Stubborn Contract',

    nav: [
      { text: 'Getting Started', link: '/getting-started/' },
      { text: 'Reference', link: '/reference/maven-plugin' },
      { text: 'How-to Guides', link: '/howto/' },
      {
        text: 'Migration',
        items: [
          { text: 'From Spring Cloud Contract', link: '/migration/from-spring-cloud-contract' },
        ],
      },
      {
        text: 'GitHub',
        link: 'https://github.com/stubborn-sh/stubborn-contract',
      },
    ],

    sidebar: {
      '/getting-started/': [
        {
          text: 'Getting Started',
          items: [
            { text: 'Introduction', link: '/getting-started/' },
            { text: 'Quick Start (3 min)', link: '/getting-started/quick-start' },
            { text: 'First Application', link: '/getting-started/first-application' },
            { text: 'Consumer-Driven CDC', link: '/getting-started/cdc' },
          ],
        },
      ],
      '/reference/': [
        {
          text: 'Build Plugins',
          items: [
            { text: 'Maven Plugin', link: '/reference/maven-plugin' },
            { text: 'Gradle Plugin', link: '/reference/gradle-plugin' },
          ],
        },
        {
          text: 'Writing Contracts',
          items: [
            { text: 'Contract DSL', link: '/reference/contract-dsl' },
            { text: 'HTTP Contracts', link: '/reference/http-contracts' },
            { text: 'Messaging Contracts', link: '/reference/messaging-contracts' },
            { text: 'YAML Contracts', link: '/reference/yaml-contracts' },
          ],
        },
        {
          text: 'Stub Runner',
          items: [
            { text: 'Overview', link: '/reference/stub-runner' },
            { text: 'Spring Boot', link: '/reference/stub-runner-spring-boot' },
            { text: 'JUnit 5', link: '/reference/stub-runner-junit5' },
            { text: 'Git Storage', link: '/reference/stub-runner-git' },
          ],
        },
        {
          text: 'Advanced',
          items: [
            { text: 'Docker Integration', link: '/reference/docker' },
            { text: 'Customization', link: '/reference/customization' },
            { text: 'Configuration Reference', link: '/reference/configuration' },
          ],
        },
      ],
      '/howto/': [
        {
          text: 'How-to Guides',
          items: [
            { text: 'Overview', link: '/howto/' },
            { text: 'Why Stubborn Contract?', link: '/howto/why-stubborn-contract' },
            { text: 'Dynamic Values', link: '/howto/dynamic-values' },
            { text: 'Debug WireMock', link: '/howto/debug-wiremock' },
            { text: 'Git as Contract Storage', link: '/howto/git-storage' },
            { text: 'Common Contract Repo', link: '/howto/common-repo' },
            { text: 'Non-JVM Providers', link: '/howto/non-jvm' },
            { text: 'Stubs Versioning', link: '/howto/stubs-versioning' },
            { text: 'Mark Contract In-Progress', link: '/howto/mark-in-progress' },
            { text: 'Transitive Dependencies', link: '/howto/transitive' },
          ],
        },
      ],
      '/migration/': [
        {
          text: 'Migration',
          items: [
            { text: 'From Spring Cloud Contract', link: '/migration/from-spring-cloud-contract' },
          ],
        },
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/stubborn-sh/stubborn-contract' },
    ],

    editLink: {
      pattern: 'https://github.com/stubborn-sh/stubborn-contract/edit/main/docs/:path',
      text: 'Edit this page on GitHub',
    },

    footer: {
      message: 'Released under the Apache 2.0 License.',
      copyright: 'Copyright © 2012-present Marcin Grzejszczak and contributors.',
    },

    search: {
      provider: 'local',
    },
  },
})
