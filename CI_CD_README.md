# CI/CD and Version Management

This document describes the automated build and version management features imported from the CleverFerret repository.

## Overview

Spacemaker now includes automated CI/CD workflows for building, testing, and releasing the Android app, along with automated version code management for pull requests.

## Features

### 1. Main CI/CD Pipeline (`.github/workflows/main.yml`)

The main workflow automatically runs on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches
- Git tags matching `v*` pattern
- Manual workflow dispatch

#### Jobs

**Test Job:**
- Compiles the project with detailed error reporting
- Runs unit tests
- Performs lint checks
- Uploads test reports and compilation logs on failure

**Build Job:**
- Builds debug and release APKs
- Extracts version information
- Signs release APKs (when keystore secrets are available)
- Generates SHA256 checksums for all APKs
- Renames APKs with version names (e.g., `Spacemaker-v1.0-debug.apk`)
- Uploads APK artifacts

**Release Job:**
- Creates GitHub releases for version tags
- Generates detailed release notes with change history
- Uploads APKs and checksums to the release
- Includes system requirements and installation instructions

### 2. Auto-Bump Version Code (`.github/workflows/auto-bump-version-code.yml`)

Automatically manages version codes for pull requests:
- Runs on PR open, reopen, synchronize, or ready for review
- Compares PR version code against base branch
- Automatically increments version code if it's not greater than base
- Commits the change back to the PR branch

This ensures every PR has a unique, incrementing version code without manual intervention.

### 3. Version Management Scripts

#### `scripts/bump_version_code.py`

Python script for managing version codes in `app/build.gradle`:

```bash
# Read current version code
python3 scripts/bump_version_code.py --file app/build.gradle

# Set specific version code
python3 scripts/bump_version_code.py --file app/build.gradle --set 42

# Increment version code by 1
python3 scripts/bump_version_code.py --file app/build.gradle --bump 1
```

#### `app/version.gradle`

Gradle tasks for version extraction and manipulation:

```bash
# Print version name (e.g., "1.0")
./gradlew printVersionName -q

# Print version code (e.g., "1")
./gradlew printVersionCode -q

# Print all version information including git commit
./gradlew printVersionInfo

# Increment version code
./gradlew incrementVersionCode

# Update version name
./gradlew updateVersionName -PnewVersionName=2.0
```

## GitHub Secrets Configuration

For full functionality, configure these secrets in your repository settings:

### Required for Release Signing

- `KEYSTORE_BASE64`: Base64-encoded release keystore file
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias in the keystore
- `KEY_PASSWORD`: Key password

### How to Generate Keystore Secrets

```bash
# Create a release keystore (if you don't have one)
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release

# Encode keystore to base64
base64 release-keystore.jks > keystore.base64.txt

# Add the contents of keystore.base64.txt to KEYSTORE_BASE64 secret
```

## Workflow Triggers

### Push to Main/Develop
- Runs tests and builds
- Creates APK artifacts
- Signs release APK if secrets are available

### Pull Request
- Runs tests and builds
- Auto-bumps version code if needed
- Creates APK artifacts for testing

### Tag Push (v*)
- Runs full pipeline
- Creates GitHub release with APKs
- Generates release notes from commit history

### Manual Dispatch
- Allows manual workflow execution from GitHub Actions UI

## Artifact Access

After workflow completion:
1. Go to GitHub Actions tab
2. Click on the workflow run
3. Scroll to "Artifacts" section
4. Download `debug-apk` or `release-apk`

## Release Process

To create a new release:

1. Update version name in `app/build.gradle`:
   ```gradle
   versionName "1.1"
   ```

2. Commit and push to main:
   ```bash
   git commit -am "Release v1.1"
   git push origin main
   ```

3. Create and push a version tag:
   ```bash
   git tag v1.1
   git push origin v1.1
   ```

4. The workflow will automatically:
   - Build release APKs
   - Create a GitHub release
   - Upload APKs and checksums
   - Generate release notes

## Troubleshooting

### Build Failures

Check the workflow logs for detailed error messages:
- Compilation errors show in the "Compilation Errors" group
- Build errors show in the "Build Errors" group
- Error summaries are extracted and displayed separately

### Version Code Conflicts

If auto-bump doesn't work:
1. Check PR branch is in the same repository (not a fork)
2. Ensure the workflow has write permissions
3. Manually run: `python3 scripts/bump_version_code.py --file app/build.gradle --bump 1`

### Signing Issues

If APK signing fails:
- Verify all four secrets are set correctly
- Check keystore password and key password are correct
- Ensure key alias matches the one in your keystore

## Local Testing

Test the build locally before pushing:

```bash
# Run tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## Migration from CleverFerret

This feature was imported from the [CleverFerret repository](https://github.com/kaleaon/cleverferret) and adapted for Spacemaker's Gradle structure:

- Converted from Kotlin DSL to Groovy syntax
- Adapted file paths (`CleverFerret/` → `app/`)
- Updated app name and branding
- Adjusted Android SDK versions to match Spacemaker's requirements
- Modified version.gradle regex patterns for Groovy syntax

## Future Enhancements

Potential additions:
- Code coverage reporting
- Static analysis integration (SonarQube, Detekt)
- Automated dependency updates
- Multi-device testing
- Beta distribution (Firebase App Distribution, Google Play Internal Testing)
