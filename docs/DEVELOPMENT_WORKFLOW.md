# Development Workflow

## Table of Contents

- [Development Workflow](#development-workflow)
  - [Table of Contents](#table-of-contents)
  - [Branching Strategy](#branching-strategy)
    - [Feature Branches](#feature-branches)
    - [Pull Requests](#pull-requests)
    - [Merging](#merging)
  - [Commit Strategy](#commit-strategy)
    - [Format](#format)
      - [1. Type](#1-type)
      - [2. Optional Scope](#2-optional-scope)
      - [3. Description](#3-description)
  - [Versioning](#versioning)

## Branching Strategy

The **Kavun** project will use the feature-based branching strategy. This strategy involves creating a new branch for each feature or bug fix. This allows developers to work on features or bug fixes in isolation without affecting the main codebase. Once a feature or bug fix is complete, it can be merged back into the main codebase.

The main branches in the **Kavun** project are:

- `master`: The main branch of the project. This branch contains the latest stable version of the codebase and is used for production deployments.
- `develop`: The development branch of the project. This branch contains the latest development version of the codebase and is used for testing and integration before being merged into the `master` branch.
- `feature/*`: Feature branches are created from the `develop` branch and are used for developing new features or bug fixes. Once a feature or bug fix is complete, the branch is merged back into the `develop` branch.
- `hotfix/*`: Hotfix branches are created from the `master` branch and are used for fixing critical bugs in the production environment. Once a hotfix is complete, the branch is merged back into the `master` branch.
- `release/*`: Release branches are created from the `develop` branch and are used for preparing a new release of the project. Once a release is complete, the branch is merged back into the `master` branch.
- `bugfix/*`: Bugfix branches are created from the `develop` branch and are used for fixing non-critical bugs in the development environment. Once a bugfix is complete, the branch is merged back into the `develop` branch.
- `refactor/*`: Refactor branches are created from the `develop` branch and are used for refactoring code without changing the functionality. Once a refactor is complete, the branch is merged back into the `develop` branch.

### Feature Branches

When working on a new feature or bug fix, create a new branch from the `master` branch. The branch name should be descriptive of the feature or bug fix being worked on. For example, if you are working on a feature to add a new section to the website, you could name the branch `feature/add-new-section`.

### Pull Requests

Once a feature or bug fix is complete, create a pull request to merge the feature branch into the `master` branch. The pull request should include a description of the changes made, any relevant screenshots or links, and any additional information that may be helpful for reviewers. The pull request should be reviewed by at least one other team member before being merged.

### Merging

Once a pull request has been reviewed and approved, it can be merged into the `master` branch. The feature branch can then be deleted. If there are any conflicts during the merge, they should be resolved before merging the pull request. Once the pull request has been merged, the changes will be deployed to the production environment.

## Commit Strategy

Throughout the **Kavun** project, we will use the feature-based branching strategy. With this strategy, we will see the following benefits:

- **Easier collaboration**: Each feature has its own private branch, allowing multiple team members to work independently on different features at the same time.
- **Less merge conflicts**: Ephemeral feature branches contain code for a specific feature, reducing the chance of merge conflicts when merging into the main codebase.
- **Faster deployments**: Because each feature is developed and tested in its own private branch before being merged into the main codebase, features can be deployed as they are completed.
- **Better visibility**: Improves project planning and resource allocation by allowing team members to easily see which features are being developed and the progress of each feature.
- **Easier rollbacks**: If a feature is not ready for deployment, it can be easily removed from the main codebase by deleting the feature branch.

This website follows [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)

Commit message will be checked using [husky and commit lint](https://theodorusclarence.com/library/husky-commitlint-prettier), you can't commit if not using the proper convention below.

### Format

`<type>(optional scope): <description>`
Example: `feat(pre-event): add speakers section`

#### 1. Type

Available types are:

- **feat** → Changes about addition or removal of a feature. Ex: `feat: add table on landing page`, `feat: remove table from landing page`
- **fix** → Bug fixing, followed by the bug. Ex: `fix: illustration overflows in mobile view`
- **docs** → Update documentation (README.md)
- **style** → Updating style, and not changing any logic in the code (reorder imports, fix whitespace, remove comments)
- **chore** → Installing new dependencies, or bumping deps
- **refactor** → Changes in code, same output, but different approach
- **ci** → Update github workflows, husky
- **test** → Update testing suite, cypress files
- **revert** → when reverting commits
- **perf** → Fixing something regarding performance (deriving state, using memo, callback)
- vercel → Blank commit to trigger vercel deployment. Ex: `vercel: trigger deployment`

#### 2. Optional Scope

Labels per page Ex: `feat(pre-event): add date label`

\*If there is no scope needed, you don't need to write it

#### 3. Description

Description must fully explain what is being done.

Add BREAKING CHANGE in the description if there is a significant change. **If there are multiple changes, then commit one by one**

- After colon, there are a single space Ex: `feat: add something`
- When using `fix` type, state the issue Ex: `fix: file size limiter not working`
- Use imperative, and present tense: "change" not "changed" or "changes"
- Don't use capitals in front of the sentence
- Don't add full stop (.) at the end of the sentence

## Versioning

x.y.z

- **x** - major release
- **y** - minor release
- **z** - build number

Where:

- **x** = major version number, 1-~.
- **y** = feature number, 0-9. Increase this number if the change includes new features with or without bug fixes.
- **z** = fix number, 0-~. Increase this number if the change contains only bug fixes.
Example:

The version number for the new app starts with 1.0.0.0.
If the new version contains only bug fixes, increment the hotfix number so that the version number is 1.0.1.
If the new version includes new features with or without bug fixes, increment the feature number and reset the hotfix number so that the version number is 1.1.0. If the number of features reaches 9, increment the major version number and reset the feature and hotfix number to zero (2.0.0, etc.).
