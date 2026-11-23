package com.khangtm.pokepoke.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark

// Sample content shared by the @Preview functions below and by the screenshot tests.
// Texts here are preview data only, not app UI.

@Composable
internal fun PokeSampleSurface(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(PokeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PokeSpacing.md),
        ) { content() }
    }
}

@Composable
internal fun PokeTopAppBarSample() {
    PokeSampleSurface {
        PokeTopAppBar(title = "Pikachu")
        PokeTopAppBar(title = "Pikachu", onBack = {}, actions = { PokeTextButton("Action", onClick = {}) })
    }
}

@Composable
internal fun PokeButtonsSample() {
    PokeSampleSurface {
        PokePrimaryButton(text = "Primary", onClick = {})
        PokePrimaryButton(text = "Disabled", onClick = {}, enabled = false)
        PokePrimaryButton(text = "Loading", onClick = {}, loading = true)
        PokeSecondaryButton(text = "Secondary", onClick = {})
        PokeSecondaryButton(text = "Disabled", onClick = {}, enabled = false)
        PokeTextButton(text = "Text button", onClick = {})
    }
}

@Composable
internal fun PokeTextFieldsSample() {
    PokeSampleSurface {
        PokeTextField(value = "", onValueChange = {}, label = "Username")
        PokeTextField(value = "ash", onValueChange = {}, label = "Username")
        PokeTextField(value = "secret", onValueChange = {}, label = "Password", isPassword = true)
        PokeTextField(
            value = "x",
            onValueChange = {},
            label = "Username",
            isError = true,
            supportingText = "Invalid value",
        )
    }
}

@Composable
internal fun PokeSearchFieldSample() {
    PokeSampleSurface {
        PokeSearchField(
            query = "",
            onQueryChange = {},
            onSearch = {},
            onClear = {},
            placeholder = "Search Pokémon",
        )
        PokeSearchField(
            query = "pikachu",
            onQueryChange = {},
            onSearch = {},
            onClear = {},
            placeholder = "Search Pokémon",
        )
    }
}

@Composable
internal fun PokeCardAndChipSample() {
    PokeSampleSurface {
        PokeCard(onClick = null) {
            Text("Static card", modifier = Modifier.padding(PokeSpacing.lg))
        }
        PokeCard(onClick = {}) {
            Text("Clickable card", modifier = Modifier.padding(PokeSpacing.lg))
        }
        PokeChip(
            text = "Electric",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
internal fun PokeListItemSample() {
    PokeSampleSurface {
        PokeListItem(headline = "Headline only")
        PokeListItem(headline = "Language", supporting = "English", leadingIcon = PokeIcons.Language, onClick = {})
        PokeListItem(headline = "Logout", leadingIcon = PokeIcons.Delete, trailing = { Text("›") })
    }
}

@Composable
internal fun PokeDialogSample() {
    PokeDialog(
        title = "Log out?",
        text = "You will need to sign in again.",
        confirmText = "Log out",
        onConfirm = {},
        dismissText = "Cancel",
        onDismiss = {},
    )
}

@Composable
internal fun PokeRadioDialogSample() {
    PokeRadioDialog(
        title = "Language",
        options = listOf("System", "English", "Tiếng Việt"),
        selected = "English",
        label = { it },
        onSelect = {},
        onDismiss = {},
    )
}

@PreviewLightDark
@Composable
private fun PokeTopAppBarPreview() = PokeTheme { PokeTopAppBarSample() }

@PreviewLightDark
@Composable
private fun PokeButtonsPreview() = PokeTheme { PokeButtonsSample() }

@PreviewLightDark
@Composable
private fun PokeTextFieldsPreview() = PokeTheme { PokeTextFieldsSample() }

@PreviewLightDark
@Composable
private fun PokeSearchFieldPreview() = PokeTheme { PokeSearchFieldSample() }

@PreviewLightDark
@Composable
private fun PokeCardAndChipPreview() = PokeTheme { PokeCardAndChipSample() }

@PreviewLightDark
@Composable
private fun PokeListItemPreview() = PokeTheme { PokeListItemSample() }

@PreviewLightDark
@Composable
private fun PokeDialogPreview() = PokeTheme { PokeDialogSample() }

@PreviewLightDark
@Composable
private fun PokeRadioDialogPreview() = PokeTheme { PokeRadioDialogSample() }
