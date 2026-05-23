# Contributing to Kavun

## Table of Contents

- [Contributing to Kavun](#contributing-to-kavun)
  - [Table of Contents](#table-of-contents)
  - [Branching Strategy](#branching-strategy)
    - [Branch Types](#branch-types)
    - [Feature Branches \& Bugfixes](#feature-branches--bugfixes)
    - [Pull Requests \& Merging](#pull-requests--merging)
  - [Commit Strategy](#commit-strategy)
    - [Format](#format)
      - [1. Available Types](#1-available-types)
      - [2. Rules for Description](#2-rules-for-description)
  - [Versioning](#versioning)

## Branching Strategy

The **Kavun** project uses a feature-based branching strategy (GitFlow workflow) to ensure codebase stability, seamless collaboration, and isolated development.

### Branch Types

- `master`: Contains the latest stable, production-ready version of the codebase.
- `develop`: The main integration branch for development. Contains the latest features ready for testing.
- `feature/*`: Created from `develop`. Used for new features. Merged back into `develop`.
- `bugfix/*`: Created from `develop`. Used for non-critical development bugs. Merged back into `develop`.
- `refactor/*`: Created from `develop`. Used for code refactoring. Merged back into `develop`.
- `release/*`: Created from `develop` when preparing a production release. Merged into both `master` and `develop`.
- `hotfix/*`: Created from `master`. Used for critical production bugs. Merged into both `master` and `develop`.

### Feature Branches & Bugfixes

When starting a new task, create a branch from the **`develop`** branch. The name must be descriptive:

- Example: `feature/add-dynamic-rbac` or `bugfix/fix-logging-output`

### Pull Requests & Merging

Once your work is complete, open a Pull Request (PR) to merge your branch into the **`develop`** branch.

- Provide a clear description of changes and any relevant logs or screenshots.
- The PR must be reviewed and approved by at least one team member before merging.
- Once merged, delete the source feature/bugfix branch.

---

## Commit Strategy

We follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification. Commit messages are linted automatically via `husky` and `commitlint`.

### Format

`<type>(optional scope): <description>`
*Example:* `feat(security): add clamav virus scanning logic`

#### 1. Available Types

- **feat** → Introduction of a new feature (or removal of one).
- **fix** → A bug fix.
- **docs** → Documentation changes only (e.g., README.md).
- **style** → Code style updates (formatting, missing semi-colons, whitespace, reordering imports) with no logic changes.
- **refactor** → Code changes that neither fix a bug nor add a feature (restructuring).
- **perf** → Code changes that improve performance.
- **test** → Adding missing tests or correcting existing tests.
- **chore** → Updating build tasks, package dependencies, or configuration files.
- **ci** → CI/CD configuration changes (e.g., GitHub Actions, Husky setups).
- **revert** → Reverting a previous commit.

#### 2. Rules for Description

- Use the imperative, present tense: "change" not "changed" nor "changes".
- Do not capitalize the first letter.
- Do not end the commit message with a period (`.`).
- Keep a single space after the colon. *Example:* `fix: resolve null pointer in token validation`

---

## Versioning

We use Semantic Versioning (**MAJOR.MINOR.PATCH**):

- **MAJOR (x)**: Incremented for incompatible API/architectural changes.
- **MINOR (y)**: Incremented for adding functionality in a backwards-compatible manner.
- **PATCH (z)**: Incremented for backwards-compatible bug fixes.

*Example Flow:* `1.0.0` (Initial) -> `1.0.1` (Bugfix) -> `1.1.0` (New feature added)
