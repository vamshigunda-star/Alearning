package com.example.alearning.ui.roster

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.ui.components.RegisterAthleteSheet
import java.util.*

// Modern minimalist color palette
private val BackgroundGray = Color(0xFFF8F9FB)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)
private val BrandAccent = Color(0xFFF97D28) // Your orange accent
private val BorderLight = Color(0xFFE5E7EB)
private val AvatarBackground = Color(0xFFFFF4EC)

@Composable
fun RosterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAthleteReport: (String) -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RosterContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is RosterAction.OnNavigateBack -> onNavigateBack()
                is RosterAction.OnNavigateToAthleteReport -> onNavigateToAthleteReport(action.individualId)
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterContent(
    uiState: RosterUiState,
    onAction: (RosterAction) -> Unit
) {
    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Roster",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(RosterAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(RosterAction.OnShowRegisterAthleteDialog) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Register Athlete", tint = BrandAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite,
                    scrolledContainerColor = SurfaceWhite
                ),
                modifier = Modifier.border(width = 1.dp, color = BorderLight, shape = RoundedCornerShape(0.dp))
            )
        },
        floatingActionButton = {
            if (uiState.selectedAthleteIds.isEmpty()) {
                FloatingActionButton(
                    onClick = {
                        if (uiState.currentTab == RosterTab.ATHLETES) {
                            onAction(RosterAction.OnShowRegisterAthleteDialog)
                        } else {
                            onAction(RosterAction.OnShowAddGroupDialog)
                        }
                    },
                    containerColor = BrandAccent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RosterTabRow(
                currentTab = uiState.currentTab,
                onTabSelected = { onAction(RosterAction.OnTabSelected(it)) },
                athleteCount = uiState.allAthletes.size,
                groupCount = uiState.groups.size
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.errorMessage != null && uiState.allAthletes.isEmpty() -> ErrorState(
                        message = uiState.errorMessage,
                        onDismiss = { onAction(RosterAction.OnDismissError) }
                    )
                    uiState.currentTab == RosterTab.ATHLETES -> AthleteTabContent(uiState, onAction)
                    uiState.currentTab == RosterTab.GROUPS -> GroupsTabContent(uiState, onAction)
                }
                
                // Contextual Action Bar
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.selectedAthleteIds.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    ContextualActionBar(
                        selectedCount = uiState.selectedAthleteIds.size,
                        onAddToGroup = { onAction(RosterAction.OnShowAddToGroupDialog) }
                    )
                }
            }
        }
    }

    // Modern Bottom Sheets
    if (uiState.showAddGroupDialog) {
        AddGroupSheet(
            onDismiss = { onAction(RosterAction.OnDismissAddGroupDialog) },
            onConfirm = { name, location, cycle -> onAction(RosterAction.OnCreateGroup(name, location, cycle)) }
        )
    }

    if (uiState.showRegisterAthleteDialog) {
        RegisterAthleteSheet(
            onDismiss = { onAction(RosterAction.OnDismissRegisterAthleteDialog) },
            onConfirm = { first, last, dob, sex, email, med -> 
                onAction(RosterAction.OnRegisterAthlete(first, last, dob, sex, email, med)) 
            }
        )
    }

    if (uiState.showAddToGroupDialog) {
        AddToGroupSelectionSheet(
            groups = uiState.groups,
            onDismiss = { onAction(RosterAction.OnDismissAddToGroupDialog) },
            onGroupSelected = { onAction(RosterAction.OnAddSelectedToGroup(it)) },
            onCreateGroup = { onAction(RosterAction.OnShowAddGroupDialog) }
        )
    }

    uiState.showManageMembersDialog?.let { groupId ->
        val members = uiState.groupMembers[groupId] ?: emptyList()
        ManageGroupMembersSheet(
            allAthletes = uiState.allAthletes,
            currentAthleteIds = members.map { it.id }.toSet(),
            onDismiss = { onAction(RosterAction.OnDismissManageMembersDialog) },
            onAddAthlete = { id -> onAction(RosterAction.OnAddAthleteToGroup(groupId, id)) },
            onRemoveAthlete = { id -> onAction(RosterAction.OnRemoveAthleteFromGroup(groupId, id)) }
        )
    }

    // Confirmation Dialogs
    uiState.showDeleteAthleteConfirmation?.let { id ->
        val athlete = uiState.allAthletes.find { it.id == id }
        ConfirmationDialog(
            title = "Delete Athlete",
            message = "Are you sure you want to delete ${athlete?.fullName}? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = { onAction(RosterAction.OnConfirmDeleteAthlete(id)) },
            onDismiss = { onAction(RosterAction.OnDismissDeleteConfirmation) }
        )
    }

    uiState.showRemoveMemberConfirmation?.let { (groupId, individualId) ->
        val athlete = uiState.allAthletes.find { it.id == individualId }
        val group = uiState.groups.find { it.id == groupId }
        ConfirmationDialog(
            title = "Remove from Group",
            message = "Remove ${athlete?.fullName} from ${group?.name}?",
            confirmText = "Remove",
            isDestructive = true,
            onConfirm = { onAction(RosterAction.OnConfirmRemoveMember(groupId, individualId)) },
            onDismiss = { onAction(RosterAction.OnDismissRemoveMemberConfirmation) }
        )
    }

    // Floating error message
    if (uiState.errorMessage != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { onAction(RosterAction.OnDismissError) }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.errorMessage)
        }
    }
}

@Composable
private fun RosterTabRow(
    currentTab: RosterTab,
    onTabSelected: (RosterTab) -> Unit,
    athleteCount: Int,
    groupCount: Int
) {
    Surface(
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            TabItem(
                title = "Athletes",
                count = athleteCount,
                isSelected = currentTab == RosterTab.ATHLETES,
                onClick = { onTabSelected(RosterTab.ATHLETES) },
                modifier = Modifier.weight(1f)
            )
            TabItem(
                title = "Groups",
                count = groupCount,
                isSelected = currentTab == RosterTab.GROUPS,
                onClick = { onTabSelected(RosterTab.GROUPS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) BrandAccent else TextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = if (isSelected) AvatarBackground else BackgroundGray,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) BrandAccent else TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (isSelected) BrandAccent else Color.Transparent)
        )
    }
}

@Composable
private fun AthleteTabContent(
    uiState: RosterUiState,
    onAction: (RosterAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = uiState.athleteSearchQuery,
            onQueryChange = { onAction(RosterAction.OnAthleteSearchQueryChanged(it)) },
            placeholder = "Search athletes..."
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.filteredAthletes, key = { it.id }) { athlete ->
                val isSelected = uiState.selectedAthleteIds.contains(athlete.id)
                val groups = uiState.athleteGroups[athlete.id] ?: emptyList()
                
                SwipeableAthleteCard(
                    athlete = athlete,
                    groups = groups,
                    isSelected = isSelected,
                    onToggleSelection = { onAction(RosterAction.OnToggleAthleteSelection(athlete.id)) },
                    onDelete = { onAction(RosterAction.OnDeleteAthlete(athlete.id)) },
                    onClick = { onAction(RosterAction.OnNavigateToAthleteReport(athlete.id)) }
                )
            }
            item {
                Text(
                    "← Swipe left to delete",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAthleteCard(
    athlete: Individual,
    groups: List<Group>,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't dismiss yet, wait for confirmation dialog
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.errorContainer
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    ) {
        ModernAthleteCard(
            athlete = athlete,
            groups = groups,
            isSelected = isSelected,
            onToggleSelection = onToggleSelection,
            onClick = onClick
        )
    }
}

@Composable
private fun ModernAthleteCard(
    athlete: Individual,
    groups: List<Group>,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit
) {
    val initials = "${athlete.firstName.first()}${athlete.lastName.first()}".uppercase()
    val isRestricted = athlete.isRestricted || athlete.medicalAlert != null
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.5.dp, if (isSelected) BrandAccent else BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) BrandAccent else BackgroundGray)
                    .border(1.5.dp, if (isSelected) BrandAccent else BorderLight, RoundedCornerShape(6.dp))
                    .clickable { onToggleSelection() },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isRestricted) MaterialTheme.colorScheme.errorContainer else AvatarBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initials, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isRestricted) MaterialTheme.colorScheme.error else BrandAccent
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(athlete.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    if (isRestricted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Warning, contentDescription = "Medical Alert", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    "Age ${((System.currentTimeMillis() - athlete.dateOfBirth) / 31_557_600_000L).toInt()} • ${athlete.sex.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                if (groups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        groups.take(3).forEach { group ->
                            Surface(
                                color = AvatarBackground,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    group.name,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandAccent,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupsTabContent(
    uiState: RosterUiState,
    onAction: (RosterAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = uiState.groupSearchQuery,
            onQueryChange = { onAction(RosterAction.OnGroupSearchQueryChanged(it)) },
            placeholder = "Search groups..."
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.filteredGroups, key = { it.id }) { group ->
                val isExpanded = uiState.expandedGroupIds.contains(group.id)
                val members = uiState.groupMembers[group.id] ?: emptyList()
                
                ModernGroupCard(
                    group = group,
                    members = members,
                    isExpanded = isExpanded,
                    onToggleExpansion = { onAction(RosterAction.OnToggleGroupExpansion(group.id)) },
                    onRemoveMember = { athleteId -> onAction(RosterAction.OnRemoveAthleteFromGroup(group.id, athleteId)) },
                    onAddMember = { onAction(RosterAction.OnShowManageMembersDialog(group.id)) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ModernGroupCard(
    group: Group,
    members: List<Individual>,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onAddMember: () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 90f else 0f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.5.dp, if (isExpanded) BrandAccent else BorderLight)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = AvatarBackground
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🏃", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text("${members.size} athletes", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                if (!isExpanded) {
                    AvatarStack(members)
                }

                IconButton(onClick = onToggleExpansion) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation),
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                    members.forEach { member ->
                        MemberRow(member, onRemove = { onRemoveMember(member.id) })
                    }
                    
                    // Add Athlete button at the bottom of the list
                    TextButton(
                        onClick = onAddMember,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Athlete", color = BrandAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: Individual, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Menu, contentDescription = "Drag", tint = BorderLight, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(BackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            Text("${member.firstName.first()}${member.lastName.first()}".uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Age ${((System.currentTimeMillis() - member.dateOfBirth) / 31_557_600_000L).toInt()}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AvatarStack(members: List<Individual>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        members.take(4).forEach { member ->
            Surface(
                modifier = Modifier.size(28.dp).border(2.dp, SurfaceWhite, CircleShape),
                shape = CircleShape,
                color = BrandAccent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${member.firstName.first()}${member.lastName.first()}".uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        if (members.size > 4) {
            Surface(
                modifier = Modifier.size(28.dp).border(2.dp, SurfaceWhite, CircleShape),
                shape = CircleShape,
                color = TextSecondary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+${members.size - 4}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(placeholder, color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = BackgroundGray,
            focusedContainerColor = BackgroundGray,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = BrandAccent
        ),
        singleLine = true
    )
}

@Composable
private fun ContextualActionBar(selectedCount: Int, onAddToGroup: () -> Unit) {
    Surface(
        color = BrandAccent,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Group", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "$selectedCount selected",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onAddToGroup) {
                Text("CHOOSE", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToGroupSelectionSheet(
    groups: List<Group>,
    onDismiss: () -> Unit,
    onGroupSelected: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    // Basic implementation of group selection for "Add to Group"
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text("Select Group", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups) { group ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onGroupSelected(group.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = BackgroundGray
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(group.name, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    TextButton(onClick = onCreateGroup) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Create New Group")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) MaterialTheme.colorScheme.error else BrandAccent
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandAccent)
    }
}

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGroupSheet(onDismiss: () -> Unit, onConfirm: (String, String?, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Create New Group", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group Name *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = cycle,
                onValueChange = { cycle = it },
                label = { Text("Cycle/Term (e.g. Fall 2025)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { onConfirm(name, location.ifBlank { null }, cycle.ifBlank { null }) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
            ) {
                Text("Create Group", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageGroupMembersSheet(
    allAthletes: List<Individual>,
    currentAthleteIds: Set<String>,
    onDismiss: () -> Unit,
    onAddAthlete: (String) -> Unit,
    onRemoveAthlete: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredAthletes = if (searchQuery.isBlank()) allAthletes else {
        allAthletes.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f) // Spacious but not full screen
                .padding(horizontal = 24.dp)
        ) {
            Text("Manage Group Members", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search athletes...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAthletes) { athlete ->
                    val isInGroup = athlete.id in currentAthleteIds
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isInGroup) AvatarBackground else Color.Transparent)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isInGroup) BrandAccent else BorderLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                athlete.firstName.first().toString().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(athlete.fullName, fontWeight = FontWeight.SemiBold)
                            Text(athlete.sex.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        IconButton(
                            onClick = { 
                                if (isInGroup) onRemoveAthlete(athlete.id) 
                                else onAddAthlete(athlete.id) 
                            }
                        ) {
                            Icon(
                                if (isInGroup) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = if (isInGroup) MaterialTheme.colorScheme.error else BrandAccent
                            )
                        }
                    }
                }
            }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}
