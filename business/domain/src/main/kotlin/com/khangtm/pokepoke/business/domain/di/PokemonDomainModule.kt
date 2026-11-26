package com.khangtm.pokepoke.business.domain.di

import com.khangtm.pokepoke.business.domain.pokemon.LoadPokemonPageUseCase
import com.khangtm.pokepoke.business.domain.pokemon.LoadPokemonPageUseCaseImpl
import com.khangtm.pokepoke.business.domain.pokemon.ObservePokemonDetailUseCase
import com.khangtm.pokepoke.business.domain.pokemon.ObservePokemonDetailUseCaseImpl
import com.khangtm.pokepoke.business.domain.pokemon.ObservePokemonListUseCase
import com.khangtm.pokepoke.business.domain.pokemon.ObservePokemonListUseCaseImpl
import com.khangtm.pokepoke.business.domain.pokemon.SearchPokemonUseCase
import com.khangtm.pokepoke.business.domain.pokemon.SearchPokemonUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PokemonDomainModule {
    @Binds
    abstract fun bindObservePokemonList(impl: ObservePokemonListUseCaseImpl): ObservePokemonListUseCase

    @Binds
    abstract fun bindLoadPokemonPage(impl: LoadPokemonPageUseCaseImpl): LoadPokemonPageUseCase

    @Binds
    abstract fun bindSearchPokemon(impl: SearchPokemonUseCaseImpl): SearchPokemonUseCase

    @Binds
    abstract fun bindObservePokemonDetail(impl: ObservePokemonDetailUseCaseImpl): ObservePokemonDetailUseCase
}
