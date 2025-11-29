package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.legacy.IvyWalletPreview
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BoxWithConstraintsScope.ReconcileConfirmationModal(
    visible: Boolean,
    dismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    IvyModal(
        id = null,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = {
            IvyButton(
                text = "Mark as Reconciled",
                iconStart = com.ivy.ui.R.drawable.ic_check
            ) {
                onConfirm()
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalTitle(text = "Reconcile Account")

            Spacer(Modifier.weight(1f))

            Spacer(Modifier.width(32.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Are you sure you want to mark this account as reconciled? This will update the reconciliation date to today.",
            style = UI.typo.b2.style(
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Preview
@Composable
private fun PreviewReconcileConfirmationModal() {
    IvyWalletPreview {
        ReconcileConfirmationModal(
            visible = true,
            dismiss = {},
            onConfirm = {}
        )
    }
}
