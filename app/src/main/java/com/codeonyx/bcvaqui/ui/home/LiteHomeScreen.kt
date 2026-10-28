package com.codeonyx.bcvaqui.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.VeNumber
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.theme.BcvGold
import com.codeonyx.bcvaqui.ui.theme.BrandColors
import com.codeonyx.bcvaqui.ui.tools.copyText

/** Modo lite: solo el dólar BCV en grande y un convertidor simple. */
@Composable
fun LiteHomeScreen(viewModel: MainViewModel) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val rate = dashboard?.bcvRates?.get("USD")?.rateToVes ?: 0.0
    var input by remember { mutableStateOf("") }
    var dollarsToBs by remember { mutableStateOf(true) }
    val amount = VeNumber.parse(input)
    val result = when {
        rate <= 0 -> 0.0
        dollarsToBs -> FxCalculator.round2(amount * rate)
        else -> FxCalculator.round2(amount / rate)
    }
    val resultText = if (dollarsToBs) "${FxCalculator.format2(result)} Bs." else "${FxCalculator.format2(result)} $"
    val fromText = if (dollarsToBs) "${FxCalculator.format2(amount)} $" else "${FxCalculator.format2(amount)} Bs."
    val shareText = "$fromText = $resultText\nDólar BCV: ${FxCalculator.format2(rate)} Bs.\n— Codivisas"

    // Cabe sin scroll en un teléfono normal; en pantallas muy pequeñas hace scroll como respaldo.
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(BrandColors.heroGradient))) {
    val minH = maxHeight
    Column(
        Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .heightIn(min = minH)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("DÓLAR BCV HOY", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.refresh(forceNetwork = true) }, enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isLoading) "Actualizando…" else "Actualizar", color = Color.White, fontSize = 15.sp)
                }
            }
            Text(if (rate > 0) FxCalculator.format2(rate) else "—", color = BcvGold, fontSize = 60.sp,
                lineHeight = 64.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("Bs. por dólar · ${FxCalculator.formatUpdatedAt(dashboard?.bcvUpdatedAt)}", color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(10.dp))
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigToggle("$ → Bs.", dollarsToBs, Modifier.weight(1f)) { dollarsToBs = true }
                    BigToggle("Bs. → $", !dollarsToBs, Modifier.weight(1f)) { dollarsToBs = false }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = VeNumber.sanitize(it) },
                    label = { Text(if (dollarsToBs) "¿Cuántos dólares?" else "¿Cuántos bolívares?", fontSize = 16.sp) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    trailingIcon = if (input.isNotEmpty()) { { IconButton({ input = "" }) { Icon(Icons.Default.Close, "Borrar") } } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Son ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedContent(resultText, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "lite") {
                        Text(it, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary, maxLines = 1,
                            softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyText(context, FxCalculator.format2(result)) },
                        modifier = Modifier.weight(1f).height(50.dp)) {
                        Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copiar", fontSize = 17.sp)
                    }
                    Button(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                        runCatching { context.startActivity(Intent.createChooser(send, "Compartir")) }
                    }, modifier = Modifier.weight(1f).height(50.dp)) {
                        Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Enviar", fontSize = 17.sp)
                    }
                }
            }
        }
        TextButton(onClick = { viewModel.setLiteMode(false) }, modifier = Modifier.padding(top = 4.dp)) {
            Text("Volver al modo completo", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    }
}

@Composable
private fun BigToggle(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick, modifier.height(46.dp)) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    else OutlinedButton(onClick, modifier.height(46.dp)) { Text(label, fontSize = 18.sp) }
}
