package com.example.homebrewhelper.ui.screens.recipe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homebrewhelper.data.model.BeverageType
import com.example.homebrewhelper.ui.components.BeverageTypeGrid
import com.example.homebrewhelper.ui.components.RecipeCard
import com.example.homebrewhelper.viewmodel.RecipeListViewModel

/**
 * Main screen displaying list of recipes with filtering and search capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    onNavigateToRecipe: (String) -> Unit,
    onNavigateToNewRecipe: () -> Unit,
    onNavigateToEditRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val recentRecipes by viewModel.recentRecipes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedBeverageType by viewModel.selectedBeverageType.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val ingredientStats by viewModel.ingredientStats.collectAsStateWithLifecycle()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDebugInfo by remember { mutableStateOf(false) }
    
    // Handle navigation from ViewModel
    LaunchedEffect(uiState.navigationTarget) {
        uiState.navigationTarget?.let { recipeId ->
            onNavigateToRecipe(recipeId)
            viewModel.clearNavigationTarget()
        }
    }
    
    // Show success messages
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // TODO: Show snackbar with success message
            viewModel.clearSuccessMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HomeBrewHelper",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Ingredient status icon (shows if ingredients are loaded)
                    IconButton(
                        onClick = { showDebugInfo = !showDebugInfo }
                    ) {
                        Icon(
                            imageVector = if (uiState.hasIngredients) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = "Ingredient status",
                            tint = if (uiState.hasIngredients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    // Refresh ingredients button (for troubleshooting)
                    IconButton(
                        onClick = { viewModel.forceInitialization() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh ingredients"
                        )
                    }
                    
                    // Favorites filter toggle
                    IconButton(
                        onClick = { viewModel.toggleFavoritesFilter() }
                    ) {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (showFavoritesOnly) "Show all recipes" else "Show favorites only",
                            tint = if (showFavoritesOnly) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }
                    
                    // Filter button
                    IconButton(
                        onClick = { showFilterSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter recipes"
                        )
                    }
                    
                    // Search button (TODO: implement search bar)
                    IconButton(
                        onClick = { /* TODO: Show search bar */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search recipes"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewRecipe,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new recipe"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Initialization status card
            if (uiState.initializationMessage != null || !uiState.hasIngredients || showDebugInfo) {
                InitializationStatusCard(
                    uiState = uiState,
                    ingredientStats = ingredientStats,
                    onRefreshIngredients = { viewModel.forceInitialization() },
                    onCheckStatus = { viewModel.checkIngredientStatus() },
                    showDebugInfo = showDebugInfo,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Search bar (if search query is not empty)
            if (searchQuery.isNotBlank()) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onClearQuery = { viewModel.updateSearchQuery("") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Active filters display
            if (selectedBeverageType != null || showFavoritesOnly) {
                ActiveFiltersRow(
                    selectedBeverageType = selectedBeverageType,
                    showFavoritesOnly = showFavoritesOnly,
                    onClearBeverageFilter = { viewModel.selectBeverageType(null) },
                    onClearFavoritesFilter = { viewModel.toggleFavoritesFilter() },
                    onClearAllFilters = { viewModel.clearFilters() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Recipe statistics (if available)
            uiState.recipeStats?.let { stats ->
                RecipeStatsCard(
                    stats = stats,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Recipes list or empty state
            if (uiState.isLoading) {
                LoadingState(
                    message = uiState.initializationMessage ?: "Loading...",
                    modifier = Modifier.fillMaxSize()
                )
            } else if (recipes.isEmpty() && searchQuery.isBlank() && selectedBeverageType == null && !showFavoritesOnly) {
                // Empty state for new users
                EmptyRecipeState(
                    onCreateRecipe = onNavigateToNewRecipe,
                    hasIngredients = uiState.hasIngredients,
                    onRefreshIngredients = { viewModel.forceInitialization() },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (recipes.isEmpty()) {
                // No results for current filters
                NoResultsState(
                    searchQuery = searchQuery,
                    selectedBeverageType = selectedBeverageType,
                    showFavoritesOnly = showFavoritesOnly,
                    onClearFilters = { viewModel.clearFilters() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Recipes list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Recent recipes section (if showing all recipes and no filters)
                    if (searchQuery.isBlank() && selectedBeverageType == null && !showFavoritesOnly && recentRecipes.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Recipes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(
                            items = recentRecipes.take(3),
                            key = { it.id }
                        ) { recipe ->
                            RecipeCard(
                                recipe = recipe,
                                onClick = { onNavigateToRecipe(recipe.id) },
                                onFavoriteClick = { viewModel.toggleFavorite(recipe.id) },
                                onMenuClick = { onNavigateToEditRecipe(recipe.id) }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "All Recipes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    
                    // All recipes
                    items(
                        items = recipes,
                        key = { it.id }
                    ) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onNavigateToRecipe(recipe.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(recipe.id) },
                            onMenuClick = { onNavigateToEditRecipe(recipe.id) }
                        )
                    }
                }
            }
        }
    }
    
    // Filter bottom sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            selectedBeverageType = selectedBeverageType,
            onBeverageTypeSelected = viewModel::selectBeverageType,
            onDismiss = { showFilterSheet = false }
        )
    }
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show error snackbar
            viewModel.clearError()
        }
    }
}

@Composable
private fun InitializationStatusCard(
    uiState: com.example.homebrewhelper.viewmodel.RecipeListUiState,
    ingredientStats: com.example.homebrewhelper.data.repository.IngredientRepository.IngredientStats?,
    onRefreshIngredients: () -> Unit,
    onCheckStatus: () -> Unit,
    showDebugInfo: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                uiState.error != null -> MaterialTheme.colorScheme.errorContainer
                uiState.initializationMessage != null -> MaterialTheme.colorScheme.secondaryContainer
                uiState.hasIngredients -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        uiState.error != null -> "Ingredient Loading Error"
                        uiState.initializationMessage != null -> "Loading Ingredients"
                        uiState.hasIngredients -> "Mead Brewing Database"
                        else -> "No Ingredients Loaded"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(
                        onClick = onCheckStatus,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Check status",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onRefreshIngredients,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh ingredients",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when {
                uiState.error != null -> {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                uiState.initializationMessage != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.initializationMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                uiState.hasIngredients -> {
                    Text(
                        text = "✓ ${uiState.ingredientCount} ingredients loaded for mead brewing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                else -> {
                    Text(
                        text = "No ingredients available. Tap refresh to load mead brewing ingredients.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Debug information
            if (showDebugInfo && ingredientStats != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Debug Information",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = buildString {
                        appendLine("Total: ${ingredientStats.totalIngredients}")
                        appendLine("Custom: ${ingredientStats.customIngredients}")
                        appendLine("Grains: ${ingredientStats.grainCount}")
                        appendLine("Hops: ${ingredientStats.hopCount}")
                        appendLine("Yeast: ${ingredientStats.yeastCount}")
                        append("Avg Cost: $${ingredientStats.averageCost?.let { "%.2f".format(it) } ?: "N/A"}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun LoadingState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search recipes...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        } else null,
        singleLine = true
    )
}

@Composable
private fun ActiveFiltersRow(
    selectedBeverageType: BeverageType?,
    showFavoritesOnly: Boolean,
    onClearBeverageFilter: () -> Unit,
    onClearFavoritesFilter: () -> Unit,
    onClearAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filters:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (selectedBeverageType != null) {
            FilterChip(
                onClick = onClearBeverageFilter,
                label = { Text(selectedBeverageType.displayName) },
                selected = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        if (showFavoritesOnly) {
            FilterChip(
                onClick = onClearFavoritesFilter,
                label = { Text("Favorites") },
                selected = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        TextButton(onClick = onClearAllFilters) {
            Text("Clear All")
        }
    }
}

@Composable
private fun RecipeStatsCard(
    stats: com.example.homebrewhelper.data.repository.RecipeRepository.RecipeStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = stats.totalRecipes.toString(),
                label = "Total"
            )
            StatItem(
                value = stats.favoriteRecipes.toString(),
                label = "Favorites"
            )
            StatItem(
                value = stats.mostPopularType.displayName,
                label = "Most Popular"
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyRecipeState(
    onCreateRecipe: () -> Unit,
    hasIngredients: Boolean,
    onRefreshIngredients: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalBar,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome to HomeBrewHelper!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (hasIngredients) {
                "Start by creating your first mead recipe"
            } else {
                "First, let's load the mead brewing ingredients"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (hasIngredients) {
            Button(
                onClick = onCreateRecipe,
                modifier = Modifier.size(width = 200.dp, height = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Recipe")
            }
        } else {
            Button(
                onClick = onRefreshIngredients,
                modifier = Modifier.size(width = 220.dp, height = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Ingredients")
            }
        }
    }
}

@Composable
private fun NoResultsState(
    searchQuery: String,
    selectedBeverageType: BeverageType?,
    showFavoritesOnly: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No recipes found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val filterText = buildString {
            if (searchQuery.isNotBlank()) append("for \"$searchQuery\"")
            if (selectedBeverageType != null) {
                if (isNotEmpty()) append(" ")
                append("in ${selectedBeverageType.displayName}")
            }
            if (showFavoritesOnly) {
                if (isNotEmpty()) append(" ")
                append("favorites")
            }
        }
        
        Text(
            text = "Try adjusting your search $filterText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(onClick = onClearFilters) {
            Text("Clear Filters")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    selectedBeverageType: BeverageType?,
    onBeverageTypeSelected: (BeverageType?) -> Unit,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Filter Recipes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Beverage Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            BeverageTypeGrid(
                selectedType = selectedBeverageType,
                onTypeSelected = { type ->
                    onBeverageTypeSelected(type)
                    onDismiss()
                },
                showClearAll = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}