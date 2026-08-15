---
name: compose-validate
description: "Scan a Kotlin / Compose / Android codebase for anti-patterns: findViewById, GlobalScope, LiveData, Material 2 imports, lowercase composables, LazyColumn without key, kapt, kotlinCompilerExtensionVersion, force-unwrap !!, Log.d, string nav routes, hardcoded dependency versions."
---

# Validate Compose / Kotlin Codebase

## When to Use

When auditing AI-generated Android code, reviewing a migration to Compose, or preparing a codebase for shipping.

## Instructions

Run each grep against the project root. Each hit is a candidate; review case by case.

### View-system leak

```bash
# findViewById in a Compose-based module
grep -rn 'findViewById' --include='*.kt' .

# Activity using setContentView(R.layout
grep -rn 'setContentView(R\.layout' --include='*.kt' .

# Android Views imports leaking into Compose code
grep -rnE 'import android\.widget\.(TextView|Button|ImageView|EditText)' --include='*.kt' .

# XML layout files for new screens (audit each: is this in active use?)
find . -name '*.xml' -path '*/res/layout/*'
```

### Material 2 leak

```bash
# Material 2 imports in a Material 3 project
grep -rnE 'import androidx\.compose\.material\.[A-Z]' --include='*.kt' .

# Material 2 dependency in build files
grep -rn 'androidx.compose.material:material' --include='*.gradle*' .
```

### Coroutine misuse

```bash
grep -rn 'GlobalScope\.' --include='*.kt' .
grep -rnE 'runBlocking\s*\{' --include='*.kt' .
```

### Null safety

```bash
# Force-unwrap operator (audit every match)
grep -rn '!!' --include='*.kt' .
```

`!!` has legitimate uses but they are rare. Most matches should be `?:` / `requireNotNull` / scoped `?.let`.

### State / Compose hygiene

```bash
# LaunchedEffect with sentinel key
grep -rnE 'LaunchedEffect\(\s*(true|Unit)\s*\)' --include='*.kt' .

# Public MutableStateFlow (no leading _)
grep -rnE '(public\s+|^\s*)val\s+\w+\s*:\s*MutableStateFlow' --include='*.kt' .
grep -rnE '(public\s+|^\s*)val\s+\w+\s*=\s*MutableStateFlow' --include='*.kt' .

# LazyColumn / LazyRow without key
grep -rnE 'items\(\s*\w+\s*\)\s*\{' --include='*.kt' .

# collectAsState without WithLifecycle
grep -rn 'collectAsState(' --include='*.kt' . | grep -v 'WithLifecycle'

# Composables named lowercase
grep -rnE '@Composable\s*$' --include='*.kt' . -A 1 | grep -E 'fun\s+[a-z]'
```

### Legacy reactive

```bash
grep -rnE 'LiveData|MutableLiveData|observeAsState' --include='*.kt' .
grep -rnE 'observe\(this\)\s*\{' --include='*.kt' .
```

### DI / architecture

```bash
# Hand-rolled singleton
grep -rnE 'object\s+\w*(Singleton|Locator|Manager)' --include='*.kt' .

# ViewModel constructed directly instead of injected
grep -rnE '=\s*[A-Z]\w*ViewModel\(' --include='*.kt' .

# kapt instead of ksp
grep -rn '\bkapt(' --include='*.kts' --include='*.gradle' .
```

### Build config

```bash
# Obsolete composeOptions block
grep -rn 'kotlinCompilerExtensionVersion' .

# compileSdk / targetSdk lower than 36 (Play Store requires 36 by Aug 2026)
grep -rnE '(compileSdk|targetSdk)\s*=\s*([0-9]|[12][0-9]|3[0-5])\b' --include='*.kts' .

# Hardcoded versions in module gradle files
grep -rnE '"\w+:.+:[0-9]+\.[0-9]+\.[0-9]+"' --include='*.kts' . | grep -v 'libs.versions.toml\|version.ref'
```

### Navigation

```bash
# String routes (should be @Serializable)
grep -rn 'composable("' --include='*.kt' .

# navigate with string format
grep -rnE 'navigate\("\w+/\$' --include='*.kt' .
```

### Logging

```bash
grep -rnE 'Log\.[dvwie]\(' --include='*.kt' .
```

### Java idioms in Kotlin

```bash
# Optional<T>
grep -rn 'Optional<' --include='*.kt' .

# Java streams
grep -rn 'java.util.stream\|\.stream()' --include='*.kt' .

# getX() / setX() on Kotlin classes
grep -rnE 'fun (get|set)[A-Z]\w*\s*\(' --include='*.kt' .
```

### Modifier order bug

```bash
# padding before clickable creates a dead zone
grep -rnE 'Modifier\.padding\([^)]*\)\.clickable' --include='*.kt' .
```

## Output Format

```
=== Critical ===
app/build.gradle.kts:18 - kotlinCompilerExtensionVersion (removed in Kotlin 2.0); apply kotlin-compose plugin
feature/profile/.../ProfileViewModel.kt:34 - GlobalScope.launch leaks
feature/home/.../HomeScreen.kt:45 - LaunchedEffect(true) - key on the changing input

=== Warnings ===
core/data/.../UserRepository.kt:12 - LiveData<List<User>>; convert to StateFlow
feature/home/.../HomeScreen.kt:67 - LazyColumn items() without key; pass key = { it.id }
build.gradle.kts:18 - kapt("...") instead of ksp("...")

=== Suggestions ===
core/ui/.../UserCard.kt:23 - data class wrapping List<Tag>; consider @Immutable
```
