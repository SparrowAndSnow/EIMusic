package com.eimsound.eimusic.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T> ListDetail(
    list: List<T>,
    onItemClick: ((T) -> Unit)? = null,
) {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<T>()
    val scope = rememberCoroutineScope()
    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        listPane = {
            AnimatedPane {
                List(
                    list = list,
                    onItemClick = { item ->
                        scope.launch {
                            scaffoldNavigator.navigateTo(
                                ListDetailPaneScaffoldRole.Detail,
                                item
                            )
                        }
                        onItemClick?.invoke(item)
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                scaffoldNavigator.currentDestination?.contentKey?.let {
                    Details(it, topBar = {
                        Row {
                            Button({
                                scope.launch {
                                    scaffoldNavigator.navigateBack(backNavigationBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange)
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                            }
                            Text(text = "${it}")
                        }
                    })
                }
            }
        },
    )
}


@Composable
private fun <T> List(
    list: List<T>,
    onItemClick: (T) -> Unit,
) {
    Card {
        ColumnList(list = list) {
            ListItem(
                modifier = Modifier
                    .clickable {
                        onItemClick(it)
                    },
                headlineContent = {
                    Text(
                        text = it.toString(),
                    )
                },
            )
        }
//        LazyColumn {
//            list.forEachIndexed { index, item ->
//                item {
//
//                }
//            }
//        }
    }
}

@Composable
fun <T> Details(item: T, topBar: @Composable () -> Unit) {
    Card {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            topBar.invoke()
            Text(
                text = "Details page for $item",
                fontSize = 24.sp,
            )
            Spacer(Modifier.size(16.dp))
            Text(
                text = "TODO: Add great details here"
            )
        }
    }
}

