# Plugin Building, Development and Verification Guide
## Default Publish Guide
### Build the Plugin (clean)
```bash
./gradlew clean buildPlugin
```
### Upload
Log into the JetBrains Marketplace and upload the plugin zip file located in `build/distributions/`.

## Check for compatibility issues
```bash
./gradlew verifyPlugin
```

## Run all verification tasks
```bash
./gradlew check
```

## Build and verify everything
```bash
./gradlew buildPlugin verifyPlugin
```

## Check against specific IDE versions
```bash
./gradlew verifyPlugin --ide-versions="2024.1,2025.1"
```


## Complete Verification
### Clean and verify
```bash
./gradlew clean
```

### Check code quality
```bash
./gradlew check
```

### Verify plugin compatibility
```bash
./gradlew verifyPlugin
```

### Build the plugin
```bash
./gradlew buildPlugin
```

### Optional: Test in sandbox
```bash
./gradlew runIde
```

## CI/CD Integration (not implemented - yet)
```yaml
- name: Verify Plugin
  run: ./gradlew verifyPlugin

- name: Check for Deprecations
  run: ./gradlew check --warning-mode all
```

