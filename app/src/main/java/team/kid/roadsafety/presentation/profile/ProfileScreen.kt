package team.kid.roadsafety.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.domain.aggregates.family.FamilyMemberEntity
import team.kid.roadsafety.domain.aggregates.user.UserRole
import team.kid.roadsafety.presentation.theme.ButtonGreen

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        if (state.isLoading && state.user == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.user?.let { user ->
                    item {
                        ProfileHeader(user = user, onLogout = onLogout)
                    }

                    item {
                        PersonalInfoSection(user = user, members = state.members)
                    }

                    val children = state.members.filter { it.role == UserRole.CHILD }
                    item {
                        MembersSection(
                            title = "Дети",
                            members = children,
                            onAddClick = { viewModel.generateInviteCode(UserRole.CHILD) }
                        )
                    }

                    val parents = state.members.filter { it.role == UserRole.PARENT }
                    item {
                        MembersSection(
                            title = "Все родители",
                            members = parents,
                            onAddClick = { viewModel.generateInviteCode(UserRole.PARENT) }
                        )
                    }
                }
            }
        }

        if (state.inviteCode != null) {
            InviteCodeDialog(
                code = state.inviteCode!!,
                onDismiss = { viewModel.clearInviteCode() }
            )
        }

        if (state.error != null) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = { /* Handle retry or dismiss */ }) {
                        Text("ОК", color = Color.White)
                    }
                }
            ) {
                Text(state.error!!)
            }
        }
    }
}

@Composable
fun ProfileHeader(user: UserResponseDto, onLogout: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ButtonGreen,
        shape = RoundedCornerShape(25.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = Color.White
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.firstName ?: ""} ${user.lastName ?: ""}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = user.email ?: user.phoneNumber ?: "",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
            }
        }
    }
}

@Composable
fun PersonalInfoSection(user: UserResponseDto, members: List<FamilyMemberEntity>) {
    val currentUserRole = members.find { it.userId == user.id }?.role

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Gray.copy(alpha = 0.8f),
        shape = RoundedCornerShape(25.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoRow(label = "Фамилия:", value = user.lastName ?: "")
            InfoRow(label = "Имя:", value = user.firstName ?: "")
            InfoRow(label = "Отчество:", value = user.patronymic ?: "")
            
            if (user.email != null) {
                InfoRow(label = "Email:", value = user.email)
            }
            if (user.phoneNumber != null) {
                InfoRow(label = "Телефон:", value = user.phoneNumber)
            }
            
            if (currentUserRole != null) {
                InfoRow(
                    label = "Роль:", 
                    value = if (currentUserRole == UserRole.PARENT) "Родитель" else "Ребенок"
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = ButtonGreen,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.width(100.dp).height(40.dp)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                Text(
                    label, 
                    modifier = Modifier.padding(horizontal = 12.dp), 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                Text(
                    value, 
                    modifier = Modifier.padding(horizontal = 12.dp), 
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MembersSection(title: String, members: List<FamilyMemberEntity>, onAddClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Gray.copy(alpha = 0.8f),
        shape = RoundedCornerShape(25.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )
            members.forEach { member ->
                MemberItem(member)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            IconButton(
                onClick = onAddClick, 
                modifier = Modifier.align(Alignment.CenterHorizontally).background(Color.White, CircleShape).size(30.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun MemberItem(member: FamilyMemberEntity) {
    val color = if (member.role == UserRole.CHILD) Color(0xFF1E88E5) else Color(0xFFD81B60)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color,
        shape = RoundedCornerShape(25.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color.White) {}
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "User ${member.userId}", color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = if (member.role == UserRole.PARENT) "Родитель" else "Ребенок", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun InviteCodeDialog(code: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Код приглашения",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    code,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ButtonGreen,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}
