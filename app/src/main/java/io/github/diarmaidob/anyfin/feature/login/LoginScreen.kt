package io.github.diarmaidob.anyfin.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diarmaidob.anyfin.core.ui.components.AnyfinIcon
import io.github.diarmaidob.anyfin.core.ui.components.AsyncContentLayout

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiModel by viewModel.uiModel.collectAsStateWithLifecycle()

    AsyncContentLayout(
        uiModel = uiModel.screenStateUiModel,
        onRefresh = { },
        onEventConsumed = viewModel::onEventConsumed,
        onEvent = { event ->
            when (event) {
                LoginEvent.NavigateToDashboard -> onLoginSuccess()
            }
        },
        loadingContent = { }
    ) {
        LoginContent(
            form = uiModel.loginFormUiModel,
            isSyncing = uiModel.screenStateUiModel.isSyncing,
            canInteract = uiModel.canInteract,
            onHostChange = viewModel::onHostChange,
            onPortChange = viewModel::onPortChange,
            onUserChange = viewModel::onUserChange,
            onPasswordChange = viewModel::onPasswordChange,
            onProtocolToggle = viewModel::onProtocolToggle,
            onTogglePassVisibility = viewModel::onTogglePasswordVisibility,
            onLoginClick = viewModel::onLoginClick
        )
    }
}

@Composable
private fun LoginContent(
    form: LoginFormUiModel,
    canInteract: Boolean,
    isSyncing: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onProtocolToggle: () -> Unit,
    onTogglePassVisibility: () -> Unit,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LoginHeader()

            LoginForm(
                form = form,
                canInteract = canInteract,
                isLoading = isSyncing,
                onHostChange = onHostChange,
                onPortChange = onPortChange,
                onUserChange = onUserChange,
                onPasswordChange = onPasswordChange,
                onProtocolToggle = onProtocolToggle,
                onTogglePassVisibility = onTogglePassVisibility,
                onLoginClick = onLoginClick
            )
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            AnyfinIcon()
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Anyfin",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun LoginForm(
    form: LoginFormUiModel,
    canInteract: Boolean,
    isLoading: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onProtocolToggle: () -> Unit,
    onTogglePassVisibility: () -> Unit,
    onLoginClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = form.host,
                onValueChange = onHostChange,
                modifier = Modifier.weight(1f),
                label = { Text("Server Address") },
                singleLine = true,
                isError = form.hostError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                enabled = canInteract,
                shape = MaterialTheme.shapes.medium,
                prefix = {
                    Row(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(enabled = canInteract) { onProtocolToggle() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${form.protocolString}://",
                            color = if (canInteract) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            )

            OutlinedTextField(
                value = form.port,
                onValueChange = onPortChange,
                modifier = Modifier.width(100.dp),
                label = { Text("Port") },
                singleLine = true,
                isError = form.portError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                enabled = canInteract,
                shape = MaterialTheme.shapes.medium,
                prefix = {
                    Text(":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }

        OutlinedTextField(
            value = form.username,
            onValueChange = onUserChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            enabled = canInteract,
            shape = MaterialTheme.shapes.medium
        )

        OutlinedTextField(
            value = form.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onTogglePassVisibility) {
                    Icon(
                        if (form.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password"
                    )
                }
            },
            visualTransformation = if (form.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = {
                focusManager.clearFocus()
                onLoginClick()
            }),
            enabled = canInteract,
            shape = MaterialTheme.shapes.medium
        )

        if (form.formError != null) {
            Text(
                text = form.formError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Button(
            onClick = {
                focusManager.clearFocus()
                onLoginClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = canInteract,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Connect", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}