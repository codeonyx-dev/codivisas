package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import com.codeonyx.bcvaqui.ui.tour.tourTarget
import com.codeonyx.bcvaqui.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.lerp
import com.codeonyx.bcvaqui.ui.theme.LocalDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codeonyx.bcvaqui.ui.theme.SurfaceLight

@Composable
fun ToolsHubScreen(onOpenTool: (String) -> Unit) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    // Orden plano de items (igual que abajo) para que la guía haga scroll hasta cada tarjeta.
    val flatKeys = remember {
        buildList {
            add("header_finanzas"); featuredTools.forEach { add(it.route) }
            ToolCategory.entries.forEach { c ->
                val t = toolCatalog.filter { it.category == c }
                if (t.isNotEmpty()) { add("header_${c.name}"); t.forEach { add(it.route) } }
            }
        }
    }
    val focus = com.codeonyx.bcvaqui.ui.tour.LocalTourFocus.current
    LaunchedEffect(focus) {
        val route = focus?.removePrefix("tool_") ?: return@LaunchedEffect
        val i = flatKeys.indexOf(route)
        if (i >= 0) gridState.animateScrollToItem((i - 1).coerceAtLeast(0))
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item(key = "header_finanzas", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.tool_cat_finanzas),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }
        items(featuredTools, key = { it.route }, span = { GridItemSpan(maxLineSpan) }) { tool ->
            Box(Modifier.tourTarget("tool_${tool.route}")) { FeaturedToolCard(tool = tool, onClick = { onOpenTool(tool.route) }) }
        }
        ToolCategory.entries.forEach { category ->
            val tools = toolCatalog.filter { it.category == category }
            if (tools.isEmpty()) return@forEach
            item(key = "header_${category.name}", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(category.label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                )
            }
            items(tools, key = { it.route }) { tool ->
                Box(Modifier.tourTarget("tool_${tool.route}")) { ToolCard(tool = tool, onClick = { onOpenTool(tool.route) }) }
            }
        }
    }
}

@Composable
private fun ToolCard(tool: ToolItem, onClick: () -> Unit) {
    val dark = LocalDarkTheme.current
    val title = stringResource(tool.title)
    val tint = if (dark) lerp(tool.tint, Color.White, 0.35f) else tool.tint
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClickLabel = stringResource(R.string.tool_open_a11y, title), onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (dark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(tool.subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Tarjeta destacada navy/dorado a todo el ancho. */
@Composable
private fun FeaturedToolCard(tool: ToolItem, onClick: () -> Unit) {
    val title = stringResource(tool.title)
    val navy = com.codeonyx.bcvaqui.ui.theme.BcvBlue
    val navyDark = com.codeonyx.bcvaqui.ui.theme.BcvBlueDark
    val gold = com.codeonyx.bcvaqui.ui.theme.BcvGold
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(navyDark, navy, com.codeonyx.bcvaqui.ui.theme.BcvIndigo)))
            .clickable(onClickLabel = stringResource(R.string.tool_open_a11y, title), onClick = onClick)
    ) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            drawCircle(gold.copy(alpha = 0.18f), radius = size.height * 0.9f, center = androidx.compose.ui.geometry.Offset(size.width, 0f))
            drawCircle(Color.White.copy(alpha = 0.06f), radius = size.height * 0.6f, center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 1.2f))
        }
        Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(gold.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) { Icon(tool.icon, contentDescription = null, tint = gold, modifier = Modifier.size(30.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text(stringResource(tool.subtitle), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = gold)
        }
    }
}
