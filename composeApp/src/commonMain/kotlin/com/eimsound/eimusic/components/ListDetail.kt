package com.eimsound.eimusic.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T> ListDetail(
    list: List<T>,
    onItemClick: ((T) -> Unit)? = null,
    leftContent: @Composable (T) -> Unit,
    content: @Composable (T) -> Unit
) {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<T>()
    val scope = rememberCoroutineScope()
    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        listPane = {
            AnimatedPane {
                Card {
                    ColumnList(list = list) {
                        ListItem(
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        scaffoldNavigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            it
                                        )
                                    }
                                    onItemClick?.invoke(it)
                                },
                            headlineContent = {
                                leftContent(it)
                            },
                        )
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                scaffoldNavigator.currentDestination?.contentKey?.let {
                    content(it)
                }
            }
        },
    )
}
