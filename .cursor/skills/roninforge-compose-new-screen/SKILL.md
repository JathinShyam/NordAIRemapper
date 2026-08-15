---
name: compose-new-screen
description: "Scaffold a new Jetpack Compose screen with stateless Screen + stateful Route, ViewModel + StateFlow + collectAsStateWithLifecycle, sealed UI state, type-safe Navigation Compose route, Hilt injection, Compose previews."
---

# Scaffold Jetpack Compose Screen

## When to Use

When creating a new screen (e.g. Profile, Settings, OrderDetail) in a Compose-based Android app.

## Instructions

1. Choose the route name and arguments. Define the route as a `@Serializable` data class or `data object`:

   ```kotlin
   @Serializable data class Profile(val userId: String)
   ```

2. Define the UI state as a sealed interface:

   ```kotlin
   sealed interface ProfileUiState {
       data object Loading : ProfileUiState
       data class Success(val user: User) : ProfileUiState
       data class Error(val message: String) : ProfileUiState
   }
   ```

3. Define intents (events from UI to ViewModel):

   ```kotlin
   sealed interface ProfileIntent {
       data object Refresh : ProfileIntent
   }
   ```

4. Create the ViewModel:

   ```kotlin
   @HiltViewModel
   class ProfileViewModel @Inject constructor(
       private val repo: ProfileRepository,
       savedState: SavedStateHandle,
   ) : ViewModel() {

       private val args = savedState.toRoute<Profile>()

       val state: StateFlow<ProfileUiState> = repo.observeUser(args.userId)
           .map { ProfileUiState.Success(it) as ProfileUiState }
           .catch { emit(ProfileUiState.Error(it.message ?: "Unknown")) }
           .stateIn(
               scope = viewModelScope,
               started = SharingStarted.WhileSubscribed(5_000),
               initialValue = ProfileUiState.Loading,
           )

       fun onIntent(intent: ProfileIntent) {
           when (intent) {
               ProfileIntent.Refresh -> viewModelScope.launch { repo.refresh(args.userId) }
           }
       }
   }
   ```

5. Create the stateful Route composable (requests the VM, collects state, delegates to Screen):

   ```kotlin
   @Composable
   fun ProfileRoute(vm: ProfileViewModel = hiltViewModel()) {
       val state by vm.state.collectAsStateWithLifecycle()
       ProfileScreen(state, onIntent = vm::onIntent)
   }
   ```

6. Create the stateless Screen composable:

   ```kotlin
   @Composable
   fun ProfileScreen(
       state: ProfileUiState,
       onIntent: (ProfileIntent) -> Unit,
   ) {
       when (state) {
           ProfileUiState.Loading -> CircularProgressIndicator()
           is ProfileUiState.Success -> Column(Modifier.padding(16.dp)) {
               Text(state.user.name, style = MaterialTheme.typography.titleLarge)
               Text(state.user.email, style = MaterialTheme.typography.bodyMedium)
               Button(onClick = { onIntent(ProfileIntent.Refresh) }) { Text("Refresh") }
           }
           is ProfileUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
       }
   }
   ```

7. Wire the route into the nav graph:

   ```kotlin
   composable<Profile> {
       // The ViewModel reads Profile args via SavedStateHandle.toRoute<Profile>().
       ProfileRoute()
   }
   ```

8. Add previews for each state:

   ```kotlin
   @ThemePreviews
   @Composable
   fun ProfileScreenPreview_Success() {
       AppTheme {
           ProfileScreen(
               ProfileUiState.Success(User("alice", "Alice", "alice@example.com")),
               onIntent = {}
           )
       }
   }

   @ThemePreviews
   @Composable
   fun ProfileScreenPreview_Loading() {
       AppTheme { ProfileScreen(ProfileUiState.Loading, onIntent = {}) }
   }
   ```

9. Add a Compose UI test for the stateless Screen:

   ```kotlin
   @get:Rule val rule = createComposeRule()

   @Test fun shows_user_name() {
       rule.setContent {
           AppTheme {
               ProfileScreen(ProfileUiState.Success(User("alice", "Alice", "a@x.com")), onIntent = {})
           }
       }
       rule.onNodeWithText("Alice").assertIsDisplayed()
   }
   ```

## Anti-patterns to avoid

- Never put state in the Screen composable; hoist to the ViewModel.
- Never use `LaunchedEffect(true)` to fetch data; key on the args that change.
- Never expose `MutableStateFlow` publicly; use a private backing field.
- Never `findViewById` or `setContentView(R.layout...)`.
- Never use Material 2 imports (`androidx.compose.material.*`).
- Never name composables lowercase.
