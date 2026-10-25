package com.eimsound.eimusic.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.eimsound.eimusic.route.Route
import com.eimsound.eimusic.route.TopLevelRoute
import com.eimsound.eimusic.route.WelcomeRoute
import com.eimsound.eimusic.route.localizedRouteName
import com.eimsound.eimusic.db.DbSessionRepository
import com.eimsound.eimusic.route.routes
import com.eimsound.eimusic.theme.LocalPalette
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.chevron_left_24px
import eimusic.composeapp.generated.resources.chevron_right_24px
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

private val RailExpandedWidth = 236.dp
private val RailCollapsedWidth = 90.dp
private val RailItemHeight = 48.dp
private val RailIconSlotWidth = 24.dp
private val RailShape = RoundedCornerShape(16.dp)
private val RailItemShape = RoundedCornerShape(16.dp)

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    backStack: MutableList<Route>,
    navigationLayoutType: NavigationSuiteType,
    isDrawerExpanded: Boolean = true,
    onToggleDrawer: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    var selected by rememberSerializable { mutableStateOf<TopLevelRoute>(WelcomeRoute) }
    val sessionRepository = koinInject<DbSessionRepository>()

    LaunchedEffect(backStack.lastOrNull()) {
        val currentRoute = backStack.lastOrNull()
        if (currentRoute is TopLevelRoute) {
            selected = currentRoute
            val index = routes.indexOf(currentRoute)
            if (index >= 0) sessionRepository.saveTab(index)
        }
    }

    if (navigationLayoutType == NavigationSuiteType.NavigationBar) {
        Column(modifier = modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().weight(1f)) { content() }
            CompactBottomNavigation(
                selected = selected,
                onSelect = { route ->
                    selected = route
                    if (backStack.lastOrNull() != route) {
                        backStack.add(route)
                    }
                }
            )
        }
    } else {
        Row(modifier = modifier.fillMaxSize()) {
            SideRail(
                selected = selected,
                isExpanded = isDrawerExpanded,
                onToggle = onToggleDrawer,
                onSelect = { route ->
                    selected = route
                    if (backStack.lastOrNull() != route) {
                        backStack.add(route)
                    }
                }
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) { content() }
        }
    }
}

@Composable
private fun SideRail(
    selected: TopLevelRoute,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (TopLevelRoute) -> Unit,
) {
    val palette = LocalPalette.current
    val animatedRailWidth by animateDpAsState(
        targetValue = if (isExpanded) RailExpandedWidth else RailCollapsedWidth,
        label = "rail-width"
    )

    val railBrush = Brush.verticalGradient(
        colors = listOf(
            palette.surfaceRaised.copy(alpha = 0.94f),
            palette.surfaceGlass.copy(alpha = 0.8f),
            palette.surfaceBase.copy(alpha = 0.9f)
        )
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .width(animatedRailWidth)
            .fillMaxHeight()
            .clip(RailShape)
            .background(railBrush)
            .border(
                width = 1.dp,
                color = palette.borderSoft.copy(alpha = 0.55f),
                shape = RailShape
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RailHeader(
            isExpanded = isExpanded,
            onToggle = onToggle,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            routes.forEach { route ->
                RailItem(
                    route = route,
                    selected = route == selected,
                    isExpanded = isExpanded,
                    onClick = { onSelect(route) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RailHeader(
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val palette = LocalPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surfaceRaised.copy(alpha = 0.56f))
            .border(1.dp, palette.borderSoft.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "EIM",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "EIMusic",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = "Navigation",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surfaceBase.copy(alpha = 0.65f))
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = if (isExpanded) painterResource(Res.drawable.chevron_left_24px) else painterResource(Res.drawable.chevron_right_24px),
                contentDescription = "Toggle navigation drawer",
                tint = palette.textSecondary
            )
        }
    }
}

@Composable
private fun RailItem(
    route: TopLevelRoute,
    selected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val label = route.localizedRouteName()
    val interactionSource = remember(route) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val itemBackground by animateColorAsState(
        targetValue = when {
            selected -> palette.accent.copy(alpha = 0.2f)
            hovered -> palette.surfaceRaised.copy(alpha = 0.62f)
            else -> Color.Transparent
        },
        label = "item-bg"
    )
    val itemBorder by animateColorAsState(
        targetValue = when {
            selected -> palette.accent.copy(alpha = 0.45f)
            hovered -> palette.borderSoft.copy(alpha = 0.55f)
            else -> Color.Transparent
        },
        label = "item-border"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) palette.accent else if (hovered) palette.textPrimary else palette.textSecondary,
        label = "item-icon"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) palette.textPrimary else if (hovered) palette.textPrimary else palette.textSecondary,
        label = "item-text"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RailItemHeight)
            .hoverable(interactionSource)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RailItemShape)
                .background(itemBackground)
                .border(1.dp, itemBorder, RailItemShape)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.spacedBy(12.dp) else Arrangement.Center
        ) {
            Box(
                modifier = Modifier.width(RailIconSlotWidth),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(route.icon),
                    contentDescription = label,
                    tint = iconColor
                )
            }

            if (isExpanded) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!isExpanded && hovered) {
            RailTooltip(text = label)
        }
    }
}

@Composable
private fun RailTooltip(text: String) {
    val palette = LocalPalette.current
    Popup(offset = IntOffset(102, 6)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.surfaceRaised,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = palette.borderSoft.copy(alpha = 0.7f)
            ),
            shadowElevation = 6.dp,
            tonalElevation = 0.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun CompactBottomNavigation(
    selected: TopLevelRoute,
    onSelect: (TopLevelRoute) -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        routes.forEach { route ->
            val isSelected = route == selected
            val chipColor by animateColorAsState(
                targetValue = if (isSelected) palette.accent.copy(alpha = 0.2f) else Color.Transparent,
                label = "compact-chip"
            )
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) palette.accent else palette.textSecondary,
                label = "compact-icon"
            )

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipColor)
                    .clickable { onSelect(route) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(route.icon),
                    contentDescription = route.localizedRouteName(),
                    tint = iconColor,
                    modifier = Modifier.width(16.dp)
                )
                Text(
                    text = route.localizedRouteName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) palette.textPrimary else palette.textSecondary,
                )
            }
        }
    }
}

@Composable
expect fun navigationLayoutType(): NavigationSuiteType
