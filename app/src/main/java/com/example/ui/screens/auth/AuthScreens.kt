package com.example.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AlokBrandLogo
import com.example.ui.components.AlokCreditFooter
import com.example.ui.viewmodel.AuthStep
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun AuthFlowScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_splash_pattern_1795369076698),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.35f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            AlokBrandLogo(size = 72.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "ALOK CHAT",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Connect securely with your email account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.step == AuthStep.EMAIL_LOGIN || state.step == AuthStep.EMAIL_SIGNUP) {
                        val isSignUp = state.step == AuthStep.EMAIL_SIGNUP

                        TabRow(
                            selectedTabIndex = if (isSignUp) 1 else 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = !isSignUp,
                                onClick = { viewModel.switchStep(AuthStep.EMAIL_LOGIN) },
                                text = { Text("Sign In", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("tab_sign_in")
                            )
                            Tab(
                                selected = isSignUp,
                                onClick = { viewModel.switchStep(AuthStep.EMAIL_SIGNUP) },
                                text = { Text("Create Account", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("tab_sign_up")
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = if (isSignUp) "Create your ALOK CHAT account" else "Welcome back",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isSignUp) {
                                "Enter your email and a password (6+ characters)."
                            } else {
                                "Sign in with your registered email and password."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(18.dp))

                        OutlinedTextField(
                            value = state.emailInput,
                            onValueChange = viewModel::updateEmailInput,
                            label = { Text("Email address") },
                            placeholder = { Text("you@example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = state.passwordInput,
                            onValueChange = viewModel::updatePasswordInput,
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(
                                        imageVector = if (state.passwordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (state.passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (state.passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        if (isSignUp) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.confirmPasswordInput,
                                onValueChange = viewModel::updateConfirmPasswordInput,
                                label = { Text("Confirm password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                visualTransformation = if (state.passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_password_input")
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = viewModel::sendPasswordReset,
                                    enabled = !state.isLoading,
                                    modifier = Modifier.testTag("forgot_password_button")
                                ) {
                                    Text("Forgot Password?")
                                }
                            }
                        }

                        state.errorMessage?.let { err ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }

                        state.infoMessage?.let { info ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = info,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isSignUp) viewModel.signUpWithEmail() else viewModel.signInWithEmail()
                            },
                            enabled = !state.isLoading &&
                                state.emailInput.isNotBlank() &&
                                state.passwordInput.isNotBlank() &&
                                (!isSignUp || state.confirmPasswordInput.isNotBlank()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("email_auth_submit_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignUp) "Create Account" else "Sign In",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Set up your profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Add your display name and unique @username so friends can find you.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = state.displayNameInput,
                            onValueChange = viewModel::updateDisplayName,
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_display_name_input")
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.usernameInput,
                            onValueChange = viewModel::updateUsername,
                            label = { Text("Username") },
                            prefix = { Text("@") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_username_input")
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.aboutInput,
                            onValueChange = viewModel::updateAbout,
                            label = { Text("About") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        state.errorMessage?.let { err ->
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { viewModel.completeProfileSetup(onAuthenticated) },
                            enabled = !state.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("complete_profile_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Start Messaging")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))
            AlokCreditFooter()
        }
    }
}
