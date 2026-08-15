---
name: compose-migrate-views-to-compose
description: "Migrate a screen from the View system (XML layouts + Activity / Fragment + findViewById) to Compose: setContent, ViewModel + StateFlow, hilt, type-safe nav, Material 3."
---

# Migrate View-system Screen to Compose

## When to Use

When inheriting a legacy XML-based screen and converting it to Compose, or when an AI-generated codebase mixes View-system Activities with new Compose features.

## Instructions

Apply per screen. Plan the migration screen-by-screen, not all at once.

### Step 1: Inventory the screen

Identify:
- The Activity / Fragment + its XML layout.
- All `findViewById` references and what each view does.
- ViewModel (if any) and its state surface (LiveData / Flow / lateinit).
- Navigation entry points (which other screens navigate here, what arguments).

### Step 2: Modernise the ViewModel

If the VM uses LiveData, convert to StateFlow:

```kotlin
// BEFORE
val users: LiveData<List<User>> = repo.observeUsers().asLiveData()

// AFTER
val users: StateFlow<List<User>> = repo.observeUsers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

If the VM does not exist yet, create one with a sealed `UiState`.

### Step 3: Create the Screen composable

Define the stateless Screen taking the UI state and event lambdas:

```kotlin
@Composable
fun UserListScreen(
    state: UserListUiState,
    onUserClicked: (User) -> Unit,
    onRefresh: () -> Unit,
) {
    when (state) {
        UserListUiState.Loading -> CircularProgressIndicator()
        is UserListUiState.Success -> LazyColumn {
            items(state.users, key = { it.id }) { user ->
                UserRow(user, onClick = { onUserClicked(user) })
            }
        }
        is UserListUiState.Error -> Text(state.message)
    }
}
```

### Step 4: Create the Route composable

```kotlin
@Composable
fun UserListRoute(
    vm: UserListViewModel = hiltViewModel(),
    onUserClicked: (User) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    UserListScreen(state, onUserClicked, onRefresh = vm::refresh)
}
```

### Step 5: Replace setContentView with setContent

Make the Activity a `ComponentActivity` (not `AppCompatActivity` unless you genuinely need Material Components / AppCompat):

```kotlin
// BEFORE
class UserListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserListBinding
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // findViewById / view binding code...
    }
}

// AFTER
@AndroidEntryPoint
class UserListActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent {
            AppTheme {
                UserListRoute(onUserClicked = { startProfileActivity(it.id) })
            }
        }
    }
}
```

### Step 6: Delete the XML layout

Once the Activity is purely `setContent`, delete `res/layout/activity_user_list.xml` and any `view binding` references.

### Step 7: Migrate the navigation

If the project uses Jetpack Navigation, convert the destination to Compose:

Define routes as `@Serializable` data classes / data objects:

```kotlin
@Serializable data object UserList
@Serializable data class Profile(val userId: String)

NavHost(navController, startDestination = UserList) {
    composable<UserList> {
        UserListRoute(onUserClicked = { navController.navigate(Profile(it.id)) })
    }
    composable<Profile> {
        // ProfileViewModel reads args via SavedStateHandle.toRoute<Profile>().
        ProfileRoute()
    }
}
```

### Step 8: Move dependencies to Compose equivalents

| Legacy | Compose equivalent |
|--------|--------------------|
| `RecyclerView` + `Adapter` | `LazyColumn` / `LazyRow` |
| `ViewPager2` | `HorizontalPager` |
| `BottomNavigationView` | `NavigationBar` (Material 3) or `NavigationSuiteScaffold` |
| `MaterialAlertDialogBuilder` | `AlertDialog` (Material 3) |
| `Snackbar` | `SnackbarHost` |
| `SwipeRefreshLayout` | `PullToRefreshContainer` (Material 3) |
| `Glide` / `Coil` ImageView | `coil-compose` `AsyncImage` |

### Step 9: Test the migration

Add Compose UI tests for the new Screen. Run the existing instrumentation tests against the migrated Activity - if they assert on specific view IDs, they will need updating to semantic matchers.

### Step 10: Repeat for the next screen

Migrate one screen at a time. Mixed View / Compose in the same Activity (via `ComposeView`) is supported but adds complexity - aim to convert whole screens.

## Anti-patterns to avoid

- Do not migrate every screen in one PR. Per-screen PRs are reviewable.
- Do not delete the XML before the new Composable works end-to-end.
- Do not keep `lateinit` view references "for safety". They cannot survive the migration.
- Do not introduce a new ViewModel architecture mid-migration. Modernise the existing VM first.
