package com.khangtm.pokepoke.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

object PokeIcons {
    val Home: ImageVector = Icons.Outlined.Home
    val HomeSelected: ImageVector = Icons.Filled.Home
    val Favourite: ImageVector = Icons.Outlined.FavoriteBorder
    val FavouriteSelected: ImageVector = Icons.Filled.Favorite
    val Profile: ImageVector = Icons.Outlined.Person
    val ProfileSelected: ImageVector = Icons.Filled.Person
    val Search: ImageVector = Icons.Filled.Search
    val Clear: ImageVector = Icons.Filled.Clear
    val Close: ImageVector = Icons.Filled.Close
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Delete: ImageVector = Icons.Outlined.Delete

    val Language: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_language)
    val CloudOff: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_cloud_off)
    val Visibility: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_visibility)
    val VisibilityOff: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_visibility_off)
    val Pokeball: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_catching_pokemon)
}
