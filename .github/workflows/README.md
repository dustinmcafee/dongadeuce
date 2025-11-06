# GitHub Actions Workflows

This directory contains GitHub Actions workflows for building dongadeuce across different platforms.

## Workflows

### 1. Build Windows Release (`build-windows.yml`)
**Triggers:**
- Push to `master` branch
- Pull requests to `master`
- Git tags starting with `v*` (e.g., `v2.17.0`)
- Manual trigger via GitHub UI

**What it does:**
- Builds the project on Windows
- Creates a Windows MSI installer
- Creates a cross-platform JAR file
- Uploads artifacts for download
- Creates a GitHub release (when triggered by a tag)

**Manual trigger:**
1. Go to: Actions → Build Windows Release → Run workflow
2. Select branch and click "Run workflow"

### 2. Build All Platforms (`build-all-platforms.yml`)
**Triggers:**
- Git tags starting with `v*`
- Manual trigger via GitHub UI

**What it does:**
- Builds Windows MSI installer and JAR
- Builds macOS DMG installer
- Builds Linux DEB package
- Creates a comprehensive GitHub release with all platform builds

**Manual trigger:**
1. Go to: Actions → Build All Platforms → Run workflow
2. Select branch and click "Run workflow"

## Creating a Release

### Option 1: Automatic release with tag
```bash
git tag v2.18.0
git push origin v2.18.0
```

This will automatically trigger the workflows and create a GitHub release with all platform builds.

### Option 2: Manual trigger
1. Go to GitHub → Actions
2. Select the desired workflow
3. Click "Run workflow"
4. Choose the branch
5. Click "Run workflow" button

## Downloading Built Artifacts

### From workflow runs:
1. Go to Actions tab
2. Click on the specific workflow run
3. Scroll to "Artifacts" section
4. Download the desired artifact

### From releases:
1. Go to Releases tab
2. Find the desired release
3. Download from "Assets" section

## Requirements

- Repository must be hosted on GitHub
- GitHub Actions must be enabled
- No additional secrets required (uses `GITHUB_TOKEN` automatically)

## Artifacts Produced

- **Windows MSI**: Native installer for Windows, includes bundled JRE
- **Windows JAR**: Cross-platform JAR (requires Java 17+)
- **macOS DMG**: Native installer for macOS
- **Linux DEB**: Debian/Ubuntu package

## Troubleshooting

If a workflow fails:
1. Check the workflow logs in the Actions tab
2. Look for error messages in the failed step
3. Verify that all dependencies are properly configured
4. Ensure the Gradle build works locally first

## Build Times

Approximate build times:
- Windows build: 5-10 minutes
- macOS build: 5-10 minutes
- Linux build: 3-7 minutes
- All platforms: 15-25 minutes total
