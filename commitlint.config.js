/**
 * Commitlint Configuration for Kavun
 *
 * Commit Message Format:
 * <type>(<scope>): <subject>
 * [optional body]
 * [optional footer]
 *
 * Types:
 * - feat: A new feature
 * - fix: A bug fix
 * - docs: Documentation only changes
 * - style: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
 * - refactor: A code change that neither fixes a bug nor adds a feature
 * - perf: A code change that improves performance
 * - test: Adding missing tests or correcting existing tests
 *
 * References:
 * - https://www.conventionalcommits.org/
 * - https://github.com/conventional-changelog/commitlint
 * - https://gist.github.com/YunusEmreAlps/f87ecdee88c05b76ab7518c2dca53f9b (Turkish)
 */

module.exports = {
  extends: ["@commitlint/config-conventional"],

  rules: {
    "body-leading-blank": [1, "always"],
    "body-max-line-length": [2, "always", 100],
    "footer-leading-blank": [1, "always"],
    "footer-max-line-length": [2, "always", 100],
    "header-max-length": [2, "always", 100],
    "scope-case": [2, "always", "lower-case"],
    "scope-enum": [
      2,
      "always",
      [
        "user",
        "security",
        "permission",
        "mail",
        "sms",
        "storage",
        "i18n",
        "audit",
        "logging",
        "siem",
        "upload",
        "web",
        "api",
        "config",
        "database",
        "docker",
        "gradle",
        "build",
        "ci",
        "docs",
        "readme",
        "contributing",
        "deps",
        "scripts",
        "tools",
        "testing",
        "global",
        "all",
      ],
    ],
    "subject-case": [
      2,
      "never",
      ["sentence-case", "start-case", "pascal-case", "upper-case"],
    ],
    "subject-empty": [2, "never"],
    "subject-full-stop": [2, "never", "."],
    "type-case": [2, "always", "lower-case"],
    "type-empty": [2, "never"],
    "type-enum": [
      2,
      "always",
      [
        "feat",         // New feature
        "fix",          // Bug fix
        "refactor",     // Code refactoring
        "perf",         // Performance improvements
        "docs",         // Documentation changes
        "test",         // Test additions/updates
        "build",        // Build system changes
        "ci",           // CI configuration changes
        "chore",        // Maintenance tasks
        "style",        // Code style/formatting
        "revert",       // Revert previous commit
        "security",     // Security fixes
        "translation",  // i18n/l10n changes
        "changeset",    // Versioning changes
        "breaking",     // Breaking API changes
      ],
    ],
    "references-empty": [1, "never"],
  },
};
