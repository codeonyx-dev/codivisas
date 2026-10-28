package com.codeonyx.bcvaqui.ui.tools.agenda

import com.codeonyx.bcvaqui.ui.theme.BcvGold
import com.codeonyx.bcvaqui.data.model.ColombianBanks
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.codeonyx.bcvaqui.R
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.data.model.PaymentType
import com.codeonyx.bcvaqui.data.model.VenezuelanBanks
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.tools.ToolChoiceFlow
import com.codeonyx.bcvaqui.ui.tools.toolChipColors
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.SurfaceLight
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaPagosScreen(viewModel: MainViewModel = appViewModel()) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val contacts = preferences.paymentContacts
    val context = LocalContext.current

    var editing by remember { mutableStateOf<PaymentContact?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    text = stringResource(R.string.pay_empty_hint),
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
                        onSetDefault = { viewModel.setDefaultPaymentContact(contact.id) },
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
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = contact.subtitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSetDefault) {
                    Icon(
                        if (contact.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(R.string.pay_default),
                        tint = if (contact.isDefault) BcvGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.primary)
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
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var userHandle by remember { mutableStateOf(initial?.userHandle.orEmpty()) }
    var accountType by remember { mutableStateOf(initial?.accountType.orEmpty()) }
    var isDefault by remember { mutableStateOf(initial?.isDefault ?: false) }

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

            val typeOrder = listOf(
                PaymentType.PAGO_MOVIL, PaymentType.CUENTA_VE, PaymentType.BINANCE, PaymentType.WALLY,
                PaymentType.ZINLI, PaymentType.CUENTA_CO, PaymentType.ZELLE, PaymentType.PAYPAL, PaymentType.CUENTA_EXTRANJERA
            )
            val typeLabels = typeOrder.map { stringResource(paymentTypeLabel(it)) }
            OptionDropdown(
                label = stringResource(R.string.pay_method_type),
                selected = stringResource(paymentTypeLabel(type)),
                options = typeLabels,
                onSelect = { i ->
                    val newType = typeOrder[i]
                    if (newType != type) {
                        if (newType == PaymentType.CUENTA_CO || type == PaymentType.CUENTA_CO) { bankCode = ""; bankName = "" }
                        type = newType
                    }
                }
            )

            Field(
                displayName, { displayName = it },
                stringResource(if (type == PaymentType.CUENTA_CO) R.string.pay_holder else R.string.pay_name)
            )

            when (type) {
                PaymentType.PAGO_MOVIL -> {
                    Field(phone, { phone = it }, "Teléfono", KeyboardType.Phone)
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
                    Field(accountNumber, { accountNumber = it }, "Número de cuenta", KeyboardType.Number)
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
                PaymentType.BINANCE -> {
                    Field(accountNumber, { accountNumber = it }, stringResource(R.string.pay_binance_id), KeyboardType.Number)
                    Field(email, { email = it }, stringResource(R.string.pay_email), KeyboardType.Email)
                    Field(userHandle, { userHandle = it }, stringResource(R.string.pay_user))
                }
                PaymentType.WALLY -> {
                    Field(phone, { phone = it }, stringResource(R.string.pay_phone), KeyboardType.Phone)
                    Field(email, { email = it }, stringResource(R.string.pay_email), KeyboardType.Email)
                    Field(userHandle, { userHandle = it }, stringResource(R.string.pay_user))
                }
                PaymentType.ZINLI, PaymentType.ZELLE -> {
                    Field(email, { email = it }, stringResource(R.string.pay_email), KeyboardType.Email)
                    Field(phone, { phone = it }, stringResource(R.string.pay_phone), KeyboardType.Phone)
                }
                PaymentType.PAYPAL -> {
                    Field(email, { email = it }, stringResource(R.string.pay_email), KeyboardType.Email)
                    Field(userHandle, { userHandle = it }, stringResource(R.string.pay_paypal_me))
                }
                PaymentType.CUENTA_CO -> {
                    OptionDropdown(
                        label = stringResource(R.string.pay_bank),
                        selected = bankName,
                        options = ColombianBanks.ALL,
                        onSelect = { bankName = ColombianBanks.ALL[it] }
                    )
                    if (ColombianBanks.isWallet(bankName)) {
                        Field(phone, { phone = it }, stringResource(R.string.pay_phone), KeyboardType.Phone)
                    } else {
                        OptionDropdown(
                            label = stringResource(R.string.pay_account_type),
                            selected = accountType,
                            options = ColombianBanks.ACCOUNT_TYPES,
                            onSelect = { accountType = ColombianBanks.ACCOUNT_TYPES[it] }
                        )
                        Field(accountNumber, { accountNumber = it }, stringResource(R.string.pay_account_number), KeyboardType.Number)
                    }
                    Field(documentId, { documentId = it }, stringResource(R.string.pay_cc_nit))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pay_default), modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = isDefault, onCheckedChange = { isDefault = it })
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
                            bankCode = if (type == PaymentType.PAGO_MOVIL || type == PaymentType.CUENTA_VE) bankCode else "",
                            bankName = bankName.trim(),
                            country = country.trim(),
                            currency = currency.trim(),
                            extraInfo = extraInfo.trim(),
                            notes = notes.trim(),
                            email = email.trim(),
                            userHandle = userHandle.trim(),
                            accountType = accountType,
                            isDefault = isDefault
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
            value = if (bankCode.isBlank()) "" else "$bankCode - $bankName",
            onValueChange = {},
            readOnly = true,
            label = { Text("Banco") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            VenezuelanBanks.ALL.forEach { bank ->
                DropdownMenuItem(
                    text = { Text("${bank.code} - ${bank.name}") },
                    onClick = { onSelect(bank.code, bank.name) }
                )
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("pago", text))
}

@androidx.annotation.StringRes
private fun paymentTypeLabel(type: PaymentType): Int = when (type) {
    PaymentType.PAGO_MOVIL -> R.string.pay_type_pago_movil
    PaymentType.CUENTA_VE -> R.string.pay_type_cuenta_ve
    PaymentType.CUENTA_EXTRANJERA -> R.string.pay_type_extranjera
    PaymentType.BINANCE -> R.string.pay_type_binance
    PaymentType.WALLY -> R.string.pay_type_wally
    PaymentType.ZINLI -> R.string.pay_type_zinli
    PaymentType.CUENTA_CO -> R.string.pay_type_co
    PaymentType.ZELLE -> R.string.pay_type_zelle
    PaymentType.PAYPAL -> R.string.pay_type_paypal
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(i)
                        expanded = false
                    }
                )
            }
        }
    }
}
