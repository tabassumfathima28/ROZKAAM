package com.rozkaam.app.ui.screens.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.WorkerWithdrawViewModel

@Composable
fun WorkerWithdrawScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkerWithdrawViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Withdraw Earnings",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (uiState.isLoading) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator(color = OldBurgundy)
            }
        } else if (uiState.wallet.available <= 0L && uiState.withdrawals.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                RozkaamEmptyState(
                    title = "Zero Available Balance",
                    message = "Complete tasks to earn a balance before requesting a UPI withdrawal.",
                    icon = Icons.Outlined.AccountBalance,
                    actionText = "Back to Earnings",
                    onActionClick = onNavigateBack
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RozkaamCard(containerColor = androidx.compose.ui.graphics.Color.White) {
                    Text(
                        text = "Available balance",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.wallet.formattedAvailable,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = OldBurgundy
                    )
                }

                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = viewModel::setAmount,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OldBurgundy,
                        unfocusedBorderColor = OldBurgundy.copy(alpha = 0.35f)
                    )
                )
                OutlinedTextField(
                    value = uiState.upiId,
                    onValueChange = viewModel::setUpiId,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("UPI ID") },
                    placeholder = { Text("name@bank") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OldBurgundy,
                        unfocusedBorderColor = OldBurgundy.copy(alpha = 0.35f)
                    )
                )

                Text(
                    text = "Test mode only: no real bank transfer is initiated. In the local emulator, the request is marked processed for demonstration.",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                uiState.errorMessage?.let { message ->
                    Text(message, color = MutedRed, fontFamily = NunitoFontFamily)
                }
                uiState.withdrawalId?.let {
                    Text(
                        text = "Test withdrawal ${uiState.withdrawalStatus ?: "pending"}. No bank transfer was made. Reference: $it",
                        color = SuccessGreen,
                        fontFamily = NunitoFontFamily
                    )
                }
                if (uiState.withdrawals.isNotEmpty()) {
                    Text(
                        text = "Withdrawal status history",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = OldBurgundy
                    )
                    uiState.withdrawals.forEach { request ->
                        RozkaamCard(containerColor = androidx.compose.ui.graphics.Color.White) {
                            Text(
                                text = "${request.formattedAmount} • ${request.status.replaceFirstChar { it.uppercase() }}",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = OldBurgundy
                            )
                            Text(
                                text = "Test-mode request ${request.id}. No bank transfer was made.",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                RozkaamPrimaryButton(
                    text = "Request Withdrawal",
                    onClick = viewModel::requestWithdrawal,
                    enabled = !uiState.isSubmitting && uiState.withdrawalId == null && uiState.wallet.available > 0L,
                    isLoading = uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
