package com.codeonyx.bcvaqui.ui.tools.agenda

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.data.model.PaymentType
import com.codeonyx.bcvaqui.data.model.VenezuelanBanks
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.SurfaceLight
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaPagosScreen(viewModel: MainViewModel = appViewModel()) {
    val preferences by viewModel.preferences.collectAsState()
    val contacts = preferences.paymentContacts
    val context = LocalContext.current

    var editing by remember { mutableStateOf<PaymentContact?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = SurfaceLight,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showEditor = true
                },
                containerColor = BcvBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Agenda vacía",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Guarda pagos móviles, cuentas VE o cuentas extranjeras para copiarlos rápido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    PaymentCard(
                        contact = contact,
                        onCopy = {
                            copyText(context, contact.formatForCopy())
                            Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                        },
                        onEdit = {
                            editing = contact
                            showEditor = true
                        },
                        onDelete = { deleteId = contact.id }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showEditor) {
        PaymentEditorSheet(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { contact ->
                viewModel.savePaymentContact(contact)
                showEditor = false
            }
        )
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Eliminar contacto") },
            text = { Text("¿Seguro que quieres borrar estos datos de pago?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePaymentContact(id)
                    deleteId = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PaymentCard(
    contact: PaymentContact,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.displayName.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = contact.typeLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = BcvBlue
                    )
                    Text(
                        text = contact.subtitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = BcvBlue)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentEditorSheet(
    initial: PaymentContact?,
    onDismiss: () -> Unit,
    onSave: (PaymentContact) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(initial?.type ?: PaymentType.PAGO_MOVIL) }
    var displayName by remember { mutableStateOf(initial?.displayName.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var accountNumber by remember { mutableStateOf(initial?.accountNumber.orEmpty()) }
    var documentId by remember { mutableStateOf(initial?.documentId.orEmpty()) }
    var bankCode by remember { mutableStateOf(initial?.bankCode.orEmpty()) }
    var bankName by remember {
        mutableStateOf(
            initial?.bankName?.ifBlank {
                VenezuelanBanks.byCode(initial.bankCode)?.name.orEmpty()
            }.orEmpty()
        )
    }
    var country by remember { mutableStateOf(initial?.country.orEmpty()) }
    var currency by remember { mutableStateOf(initial?.currency.orEmpty()) }
    var extraInfo by remember { mutableStateOf(initial?.extraInfo.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var bankMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initial == null) "Nuevo contacto" else "Editar contacto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == PaymentType.PAGO_MOVIL,
                    onClick = { type = PaymentType.PAGO_MOVIL },
                    label = { Text("Pago móvil") }
                )
                FilterChip(
                    selected = type == PaymentType.CUENTA_VE,
                    onClick = { type = PaymentType.CUENTA_VE },
                    label = { Text("Cuenta VE") }
                )
                FilterChip(
                    selected = type == PaymentType.CUENTA_EXTRANJERA,
                    onClick = { type = PaymentType.CUENTA_EXTRANJERA },
                    label = { Text("Extranjera") }
                )
            }

            Field(displayName, { displayName = it }, "Nombre de quien es")

            when (type) {
                PaymentType.PAGO_MOVIL -> {
                    Field(phone, { phone = it }, "Teléfono")
                    Field(documentId, { documentId = it }, "Cédula")
                    BankDropdown(
                        bankCode = bankCode,
                        bankName = bankName,
                        expanded = bankMenu,
                        onExpandedChange = { bankMenu = it },
                        onSelect = { code, name ->
                            bankCode = code
                            bankName = name
                            bankMenu = false
                        }
                    )
                }
                PaymentType.CUENTA_VE -> {
                    Field(accountNumber, { accountNumber = it }, "Número de cuenta")
                    Field(documentId, { documentId = it }, "Cédula")
                    BankDropdown(
                        bankCode = bankCode,
                        bankName = bankName,
                        expanded = bankMenu,
                        onExpandedChange = { bankMenu = it },
                        onSelect = { code, name ->
                            bankCode = code
                            bankName = name
                            bankMenu = false
                        }
                    )
                }
                PaymentType.CUENTA_EXTRANJERA -> {
                    Field(accountNumber, { accountNumber = it }, "Cuenta / IBAN")
                    Field(bankName, { bankName = it }, "Banco")
                    Field(country, { country = it }, "País")
                    Field(currency, { currency = it }, "Moneda (USD, EUR…)")
                    Field(documentId, { documentId = it }, "Documento (opcional)")
                    Field(extraInfo, { extraInfo = it }, "SWIFT / routing / extras")
                }
            }

            Field(notes, { notes = it }, "Nota privada (no se comparte)")

            Button(
                onClick = {
                    if (displayName.isBlank()) return@Button
                    onSave(
                        PaymentContact(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            type = type,
                            displayName = displayName.trim(),
                            phone = phone.trim(),
                            accountNumber = accountNumber.trim(),
                            documentId = documentId.trim(),
                            bankCode = if (type == PaymentType.CUENTA_EXTRANJERA) "" else bankCode,
                            bankName = bankName.trim(),
                            country = country.trim(),
                            currency = currency.trim(),
                            extraInfo = extraInfo.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = displayName.isNotBlank()
            ) {
                Text("Guardar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankDropdown(
    bankCode: String,
    bankName: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (code: String, name: String) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = if (bankCode.isBlank()) "" else "$bankCode · $bankName",
            onValueChange = {},
            readOnly = true,
            label = { Text("Banco") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BcvBlue,
                focusedLabelColor = BcvBlue
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            VenezuelanBanks.ALL.forEach { bank ->
                DropdownMenuItem(
                    text = { Text(bank.label) },
                    onClick = { onSelect(bank.code, bank.name) }
                )
            }
        }
    }
}

@Composable
private fun Field(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BcvBlue,
            focusedLabelColor = BcvBlue
        )
    )
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("pago", text))
}
