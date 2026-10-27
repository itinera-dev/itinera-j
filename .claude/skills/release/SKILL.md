---
name: release
description: How Claude prepares a release of the itinera-j artifacts on Maven Central, and carries it into the book and the site. Use when the maintainer asks for a release, when writing a version's changelog section, and as soon as a release reaches Maven Central.
---

# Releasing

The "Releasing" section of [docs/contributing.md](../../../docs/contributing.md) says how a release runs: the tag, the release gate, the release workflow and its four jobs, the `maven-central` environment and its secrets, and why a version on Maven Central can never be replaced. This skill adds only the part Claude does.

Claude never pushes a tag and never publishes to Maven Central. The maintainer pushes the tag, and the release workflow publishes.

## The release pull request

One pull request, titled "Release <version>" with a release commit `chore(release): release <version>`, sets the version and writes its changelog section. It names its issue as AGENTS.md says, normally `Refs #1`, the tier 1 tracking issue.

1. Branch from the latest `main`.
2. After `git fetch --tags`, list what changed since the last release with `git log v<last version>..origin/main`, and read each commit's message, its changelog trailers included.
3. Raise the version:
   - `version` in `gradle.properties`, which every artifact, the bill of materials included, takes;
   - the status paragraph of `README.md`, with the cases it passes.
4. Check that both `conformance.json` and `conformance-java.json` list every tier 1 proposal of the cases they pin, except one whose every case needs a capability the manifest does not claim, which the release gate in docs/contributing.md "Releasing" requires, and that `./gradlew checkManifests` passes, since a release ships both languages or neither. If either fails, stop and tell the maintainer, since the tag would be refused.
5. Write the version's section at the top of `CHANGELOG.md`, above the last release's, in the form of the sections below it:
   - `git cliff --unreleased --tag v<version> --strip all` prints the heading `## [<version>]` and the entries that commits since the last release carry as trailers, under `### Added`, `### Changed`, `### Fixed` and `### Removed`, each only when it has entries; `docs/contributing.md` "Commit messages and the changelog" says how;
   - after the heading, one paragraph naming which release it is, whether it breaks code written for the last one, the specification version, the tier, the capabilities each language claims, the conformance cases of the manifests, the oldest JDK supported and the Kotlin standard library the artifacts need, and saying "What a journey does is unchanged" when that is so;
   - the heading's link at the bottom of the file, to `releases/tag/v<version>`.
6. Reconcile the entries, since a commit's entries cannot change once it merges. Each entry follows AGENTS.md, and the section as a whole says what an application meets, compared with the last release:
   - entries that a later commit made untrue, such as an addition renamed before the release, become one entry saying what the release holds;
   - entries that say the same thing merge;
   - nothing that was already true in the last release, which `git show v<last version>:<file>` settles;
   - nothing about how the documentation spells something the language already allowed, such as a type the language lets code leave out;
   - a commit since the last release that changes what an application meets but carries no entry gets its entry here; changes to CI, the release workflow or the documentation get one only when they change what is published or how, as docs/contributing.md "Commit messages and the changelog" says.
7. Check the change against every condition of the release gate. Run the checks of AGENTS.md "Running the checks", and have the change reviewed as the `review-change` skill says. Ask the reviewer to check every changelog entry against the code at both versions, and the section against the entries git-cliff printed.
8. Open the pull request, in a cloud session with the REST form the `stack` skill gives, and report it as AGENTS.md "Working with the maintainer" says.

## After the merge

1. Give the maintainer the commands that make the tag docs/contributing.md "Releasing" describes, at `main`'s merge commit:
   1. `git fetch origin`
   2. `git switch --detach <commit>`
   3. `./gradlew check dokkaGenerate publishAllPublicationsToPackageRepository`
   4. `git tag -a v<version> <commit> -m "itinera-j <version>"`
   5. `git push origin v<version>`

   Once it is pushed, check with `git ls-remote origin 'refs/tags/v<version>*'` that it shows a tag object, and a `^{}` line naming the commit.
2. Once the release workflow has finished, check that:
   - every artifact, `itinera`, `itinera-kotlin`, `itinera-java`, `itinera-testing`, `itinera-checker` and `itinera-bom`, is on Maven Central at the version, under `https://repo.maven.apache.org/maven2/org/itinera-dev/`, which can take an hour after the deployment is released;
   - javadoc.io serves each artifact's reference at the version;
   - the GitHub release exists, marked as a pre-release when the version has a pre-release part such as `-beta.1`, with the changelog section and both conformance reports.

   If publishing failed, tell the maintainer, pointing at docs/contributing.md "Releasing".

## Once the artifacts are published

As soon as every artifact is on Maven Central at the version, Claude carries the release into the book and the site without waiting to be asked. The maintainer's standing preference is that both changes are pushed straight to each repository's `main`, with no pull request, once the checks its own manual asks for pass and a separate reviewer has passed them. When this session cannot reach a repository, Claude asks the maintainer for a thread working on it, and passes that preference on.

1. **The book**, itinera-dev/book, follows the release as its BOOK.md says, once the Kotlin edition exists: `next` merges into `main`, whose `kotlin` directory then names the released version, and the prose naming the release is raised to it. Once that commit is on the book's `main`, give the maintainer the commands that tag it:
   1. `git fetch origin`
   2. `git tag -a kotlin-<version> <commit> -m "The Kotlin book for itinera <version>"`
   3. `git push origin kotlin-<version>`

   The tag freezes its commit as the version's edition, so it goes only on the commit whose prose names the new version.
2. **The site**, itinera-dev/itinera-dev.github.io, follows each language's release, as its own manual says. For the JVM, two entries:
   - in `src/data/languages.json`, the Kotlin entry and the Java entry, each with `state` `released`, and its `version`, `install`, `tier` and `executors` matching the release and its own manifest, with the other fields a released entry has, and links to javadoc.io;
   - in the `facts` of `site.config.mjs`, `conformance-ref` matches the cases the manifests pin;
   - a post under `content/news/` announces the release.
