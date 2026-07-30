# Releasing valid4j

Published to Maven Central via the [Central Portal](https://central.sonatype.com), using the
[nmcp](https://gradleup.com/nmcp/) Gradle plugin. There is no official Sonatype Gradle plugin.

> **Central publications are immutable.** A published version can never be deleted or edited — the
> only remedy is publishing a new version. `publishingType` is set to `USER_MANAGED` in
> `settings.gradle.kts` so a pushed tag *stages* a deployment rather than releasing it; nothing
> becomes public until step 8 below.

---

## One-time setup

### 1. Verify the namespace

Sign in to [central.sonatype.com](https://central.sonatype.com) **with the `jpalmerr` GitHub
account**. Signing in via GitHub auto-verifies `io.github.<username>`, so `io.github.jpalmerr`
requires no verification repository.

### 2. Generate a signing key

Central rejects unsigned components.

```bash
gpg --full-generate-key                          # RSA 4096
gpg --list-secret-keys --keyid-format=long       # note the key id
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
gpg --armor --export-secret-keys <KEY_ID>        # this whole output is SIGNING_KEY
```

The public key **must** reach a keyserver or validation fails. Export the private key including its
`BEGIN`/`END` lines.

### 3. Create a Portal token

Portal → your profile → **Generate User Token**. Produces a username/password pair distinct from
your login.

### 4. Add the repository secrets

Settings → Secrets and variables → Actions:

| Secret | Value |
|---|---|
| `CENTRAL_PORTAL_USERNAME` | token username from step 3 |
| `CENTRAL_PORTAL_PASSWORD` | token password from step 3 |
| `SIGNING_KEY` | ASCII-armored private key from step 2 |
| `SIGNING_PASSPHRASE` | passphrase for that key |

`release.yml` fails fast if any is missing, because signing is opt-in in `build.gradle.kts` and an
absent key would otherwise produce unsigned artifacts.

---

## Cutting a release

1. Set `version` in `build.gradle.kts`.
2. Match the coordinates in the `README.md` Quick Start — both the Maven XML and the Gradle block.
3. **First release only:** delete the `> **Not yet on Maven Central.**` blockquote from `README.md`.
   It becomes false the moment step 8 completes.
4. `CHANGELOG.md`: rename `## Unreleased` to `## <version> — <YYYY-MM-DD>` and delete the
   "Not yet published" paragraph.
5. Build locally — Gradle itself needs a JDK it supports (8–23), independent of the 21 toolchain:
   ```bash
   JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew spotlessCheck build
   ```
6. Commit the above.
7. Tag and push. The tag must match `version` exactly or the workflow refuses to publish:
   ```bash
   git tag v<version>
   git push origin v<version>
   ```
8. `release.yml` stages the deployment. **It is not public yet.** Go to
   [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments),
   review the artifacts and POM, then click **Publish**. You can still *drop* the deployment up to
   this point; after it, never.
9. Confirm it landed at
   `https://repo1.maven.org/maven2/io/github/jpalmerr/valid4j/`. Allow ~15–30 minutes for the
   repository, longer for search indexing.

---

## Notes

- Signing is skipped when `SIGNING_KEY` is unset, so `./gradlew publishToMavenLocal` works for
  anyone cloning the repo without the private key.
- To publish automatically without the manual gate in step 8, change `publishingType` to
  `"AUTOMATIC"` in `settings.gradle.kts`. Given immutability, keeping the gate is recommended.
- Snapshots are available via `publishAllPublicationsToCentralSnapshots` if ever needed; the release
  workflow does not use them.
