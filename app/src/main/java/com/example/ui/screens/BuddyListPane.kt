package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.LocalAppStrings
import com.example.model.BuddyDirectoryEntry
import com.example.model.ChatThread
import com.example.model.MsnStatus
import com.example.model.UserContact
import com.example.ui.components.MsnAvatarFrame
import com.example.ui.components.StatusDropdownChip
import com.example.ui.components.StyledBuddyName
import com.example.ui.components.statusLabel
import com.example.ui.theme.MsnAzurePrimary
import com.example.ui.theme.MsnGreenOnline
import com.example.ui.theme.MsnHeaderBottom
import com.example.ui.theme.MsnHeaderTop
import com.example.ui.theme.MsnIceBlue

@Composable
fun BuddyListPane(
    currentUserId: String,
    myDirectory: BuddyDirectoryEntry,
    contacts: List<UserContact>,
    chats: List<ChatThread>,
    buddyDirectoryMap: Map<String, BuddyDirectoryEntry>,
    selectedPartnerUid: String?,
    onSelectPartner: (String) -> Unit,
    onStatusChange: (MsnStatus) -> Unit,
    onPersonalMessageSave: (String) -> Unit,
    onAddContact: (String) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
    onRemoveContact: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCopiedNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var addHandleInput by remember { mutableStateOf("") }
    var editingPersonalMsg by remember { mutableStateOf(false) }
    var personalMsgDraft by remember(myDirectory.personalMessage) { mutableStateOf(myDirectory.personalMessage) }
    var onlineExpanded by remember { mutableStateOf(true) }
    var offlineExpanded by remember { mutableStateOf(true) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addContact) },
            text = {
                OutlinedTextField(
                    value = addHandleInput,
                    onValueChange = { addHandleInput = it },
                    label = { Text(strings.addContactHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_handle_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = addHandleInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddContact(trimmed)
                            addHandleInput = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_contact_button")
                ) {
                    Text(strings.addBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(strings.cancelBtn)
                }
            }
        )
    }

    // Merge explicit contacts + active chat partners so incoming chats are always visible
    val chatPartnerUids = remember(chats, currentUserId) {
        chats.flatMap { it.memberUids }.filter { it != currentUserId }.distinct()
    }
    val knownContactMap = remember(contacts) { contacts.associateBy { it.contactUid } }
    val allBuddyUids = remember(contacts, chatPartnerUids, currentUserId) {
        (contacts.map { it.contactUid } + chatPartnerUids)
            .filter { it != currentUserId }
            .distinct()
    }

    val filteredUids = remember(allBuddyUids, buddyDirectoryMap, knownContactMap, searchQuery) {
        val q = searchQuery.trim().lowercase()
        allBuddyUids.filter { uid ->
            if (q.isEmpty()) true
            else {
                val dir = buddyDirectoryMap[uid]
                val contact = knownContactMap[uid]
                (dir?.displayName?.lowercase()?.contains(q) == true) ||
                    (dir?.msnHandle?.lowercase()?.contains(q) == true) ||
                    (contact?.msnHandle?.lowercase()?.contains(q) == true)
            }
        }
    }

    val onlineUids = filteredUids.filter { uid ->
        val st = buddyDirectoryMap[uid]?.statusEnum() ?: MsnStatus.OFFLINE
        st != MsnStatus.OFFLINE
    }
    val offlineUids = filteredUids.filter { uid ->
        val st = buddyDirectoryMap[uid]?.statusEnum() ?: MsnStatus.OFFLINE
        st == MsnStatus.OFFLINE
    }

    val threadByPartner = remember(chats, currentUserId) {
        chats.associateBy { thread ->
            thread.memberUids.firstOrNull { it != currentUserId } ?: currentUserId
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MsnAzurePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_contact")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = strings.addContact)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Classic Glossy Windows XP MSN Header Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(MsnHeaderTop, MsnHeaderBottom)))
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MsnAvatarFrame(
                        avatarPreset = myDirectory.avatarPresetEnum(),
                        avatarBase64 = myDirectory.avatarBase64,
                        status = myDirectory.statusEnum(),
                        size = 58.dp,
                        modifier = Modifier.testTag("my_avatar_button"),
                        onClick = onOpenSettings
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        // User's colored, styled display name
                        Surface(
                            color = Color.White.copy(alpha = 0.90f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            StyledBuddyName(
                                entry = myDirectory,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        // Status Dropdown right below the name!
                        StatusDropdownChip(
                            currentStatus = myDirectory.statusEnum(),
                            onStatusSelected = onStatusChange,
                            textColor = Color.White
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.profileSettingsTitle,
                            tint = Color.White
                        )
                    }
                }

                // Inline editable Personal Message
                if (editingPersonalMsg) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = personalMsgDraft,
                            onValueChange = { personalMsgDraft = it },
                            placeholder = { Text(strings.personalMessageHint, color = Color.White.copy(0.7f)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                        )
                        IconButton(
                            onClick = {
                                editingPersonalMsg = false
                                onPersonalMessageSave(personalMsgDraft)
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = strings.saveBtn, tint = Color.White)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingPersonalMsg = true }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFFD6EAFF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = myDirectory.personalMessage.ifBlank { strings.personalMessageHint },
                            color = Color(0xFFEAF4FF),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Own MSN Handle + Copy + Share Invite
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.myMsnIdLabel} ${myDirectory.msnHandle}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(myDirectory.msnHandle))
                                    onCopiedNotice(strings.copiedMsg)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Handle", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "MSN Messenger: ${myDirectory.msnHandle}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, strings.inviteFriends))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = strings.inviteFriends, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Search bar + Solo Test & Games Quick Card
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchContactsHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("search_contacts_input")
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedPartnerUid == currentUserId) MsnIceBlue else MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPartner(currentUserId) }
                        .testTag("self_test_chat_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MsnAzurePrimary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.selfTestChat, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${strings.gameGomokuTitle} • ${strings.gameMemoryTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("→", color = MsnAzurePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Buddy Groups List
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Online Group Header
                item(key = "header_online") {
                    GroupHeaderRow(
                        title = "${strings.onlineGroup} (${onlineUids.size})",
                        expanded = onlineExpanded,
                        onToggle = { onlineExpanded = !onlineExpanded }
                    )
                }

                if (onlineExpanded) {
                    items(onlineUids, key = { "on_$it" }) { uid ->
                        BuddyListRow(
                            uid = uid,
                            entry = buddyDirectoryMap[uid],
                            contact = knownContactMap[uid],
                            thread = threadByPartner[uid],
                            isSelected = selectedPartnerUid == uid,
                            onClick = { onSelectPartner(uid) },
                            onToggleBlock = { blocked -> onToggleBlock(uid, blocked) },
                            onRemove = { onRemoveContact(uid) }
                        )
                    }
                }

                // Offline Group Header
                item(key = "header_offline") {
                    GroupHeaderRow(
                        title = "${strings.offlineGroup} (${offlineUids.size})",
                        expanded = offlineExpanded,
                        onToggle = { offlineExpanded = !offlineExpanded }
                    )
                }

                if (offlineExpanded) {
                    items(offlineUids, key = { "off_$it" }) { uid ->
                        BuddyListRow(
                            uid = uid,
                            entry = buddyDirectoryMap[uid],
                            contact = knownContactMap[uid],
                            thread = threadByPartner[uid],
                            isSelected = selectedPartnerUid == uid,
                            onClick = { onSelectPartner(uid) },
                            onToggleBlock = { blocked -> onToggleBlock(uid, blocked) },
                            onRemove = { onRemoveContact(uid) }
                        )
                    }
                }

                if (filteredUids.isEmpty()) {
                    item(key = "empty_contacts") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = strings.noContactsYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text(strings.addContact)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun GroupHeaderRow(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BuddyListRow(
    uid: String,
    entry: BuddyDirectoryEntry?,
    contact: UserContact?,
    thread: ChatThread?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleBlock: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalAppStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val resolved = entry ?: BuddyDirectoryEntry(
        userId = uid,
        msnHandle = contact?.msnHandle ?: uid.take(10),
        displayName = contact?.msnHandle ?: uid.take(10),
        status = MsnStatus.OFFLINE.code
    )
    val isBlocked = contact?.blocked == true

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MsnIceBlue else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("buddy_row_$uid")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MsnAvatarFrame(
                avatarPreset = resolved.avatarPresetEnum(),
                avatarBase64 = resolved.avatarBase64,
                status = resolved.statusEnum(),
                size = 44.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StyledBuddyName(entry = resolved, fontSize = 15.sp, modifier = Modifier.weight(1f, fill = false))
                    Text(
                        text = "(${statusLabel(resolved.statusEnum(), strings)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (resolved.statusEnum() == MsnStatus.ONLINE) MsnGreenOnline else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isBlocked) {
                        Icon(Icons.Default.Block, contentDescription = "Blocked", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    }
                }
                val subtitle = thread?.lastMessageText?.takeIf { it.isNotBlank() }
                    ?: resolved.personalMessage.takeIf { it.isNotBlank() }
                    ?: resolved.msnHandle
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (contact != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Contact options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (isBlocked) strings.unblockLabel else strings.blockLabel) },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onToggleBlock(!isBlocked)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.removeLabel) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    }
}
