package com.khangtm.pokepoke.business.domain.di

import com.khangtm.pokepoke.business.domain.favourite.ClearFavouritesUseCase
import com.khangtm.pokepoke.business.domain.favourite.ClearFavouritesUseCaseImpl
import com.khangtm.pokepoke.business.domain.favourite.ObserveFavouritePokemonUseCase
import com.khangtm.pokepoke.business.domain.favourite.ObserveFavouritePokemonUseCaseImpl
import com.khangtm.pokepoke.business.domain.favourite.ObserveIsFavouriteUseCase
import com.khangtm.pokepoke.business.domain.favourite.ObserveIsFavouriteUseCaseImpl
import com.khangtm.pokepoke.business.domain.favourite.RemoveFavouriteUseCase
import com.khangtm.pokepoke.business.domain.favourite.RemoveFavouriteUseCaseImpl
import com.khangtm.pokepoke.business.domain.favourite.ToggleFavouriteUseCase
import com.khangtm.pokepoke.business.domain.favourite.ToggleFavouriteUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FavouriteDomainModule {
    @Binds
    abstract fun bindObserveFavouritePokemon(impl: ObserveFavouritePokemonUseCaseImpl): ObserveFavouritePokemonUseCase

    @Binds
    abstract fun bindObserveIsFavourite(impl: ObserveIsFavouriteUseCaseImpl): ObserveIsFavouriteUseCase

    @Binds
    abstract fun bindToggleFavourite(impl: ToggleFavouriteUseCaseImpl): ToggleFavouriteUseCase

    @Binds
    abstract fun bindRemoveFavourite(impl: RemoveFavouriteUseCaseImpl): RemoveFavouriteUseCase

    @Binds
    abstract fun bindClearFavourites(impl: ClearFavouritesUseCaseImpl): ClearFavouritesUseCase
}
