package com.rozkaam.app.ui.screens.poster

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.data.model.Transaction
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.ErrorRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.WalletViewModel

@Composable
fun PosterWalletScreen(
    onNavigateToAddMoney: () -> Unit,
    viewModel: WalletViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Poster Wallet & Escrow",
                canNavigateBack = false
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OldBurgundy)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Error Banner if needed
                    uiState.errorMessage?.let { error ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = ErrorRed.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = error,
                                color = ErrorRed,
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    // Balance & Escrow summary cards per SPEC.md Section A5
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Available Balance
                        RozkaamCard(
                            containerColor = Buttermilk,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Available Balance",
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.wallet.formattedAvailable,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = OldBurgundy
                            )
                        }

                        // Held in Escrow
                        RozkaamCard(
                            containerColor = PastelBlue,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Held in Escrow",
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                color = OldBurgundy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.wallet.formattedHeld,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = OldBurgundy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Add Money Pill Button
                    RozkaamPrimaryButton(
                        text = "Add Money",
                        icon = Icons.Outlined.AddCard,
                        onClick = onNavigateToAddMoney
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Ledger History",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OldBurgundy
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            RozkaamEmptyState(
                                title = "No Transactions Found",
                                message = "All wallet top-ups, escrow holds, and payment releases are maintained in a secure Firestore ledger. Your transactions will appear here.",
                                icon = Icons.Outlined.AccountBalanceWallet
                            )
                        }
                    }
                } else {
                    items(uiState.transactions, key = { it.id }) { transaction ->
                        TransactionItemCard(transaction = transaction)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItemCard(transaction: Transaction) {
    val icon = when (transaction.type) {
        "topup" -> Icons.Outlined.ArrowDownward
        "hold" -> Icons.Outlined.Lock
        "release" -> Icons.Outlined.CheckCircle
        "refund" -> Icons.Outlined.Refresh
        "withdrawal" -> Icons.Outlined.ArrowUpward
        else -> Icons.Outlined.AccountBalanceWallet
    }

    val iconBgColor = if (transaction.isCredit) {
        SuccessGreen.copy(alpha = 0.12f)
    } else {
        ErrorRed.copy(alpha = 0.12f)
    }

    val iconTint = if (transaction.isCredit) SuccessGreen else ErrorRed
    val amountColor = if (transaction.isCredit) SuccessGreen else OldBurgundy

    RozkaamCard(
        containerColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.displayTitle,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.displayTitle,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = OldBurgundy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.formattedDate,
                    fontFamily = NunitoFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (!transaction.razorpayPaymentId.isNullOrBlank()) {
                    Text(
                        text = "ID: ${transaction.razorpayPaymentId}",
                        fontFamily = NunitoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Amount
            Text(
                text = transaction.formattedAmount,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = amountColor
            )
        }
    }
}
