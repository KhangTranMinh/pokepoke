package com.khangtm.pokepoke.business.data.pokemon

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** PokeAPI endpoints used by [PokemonRepositoryImpl]. */
internal interface PokemonApi {
    @GET("pokemon")
    suspend fun list(@Query("offset") offset: Int, @Query("limit") limit: Int): PokemonListDto

    @GET("pokemon/{idOrName}")
    suspend fun pokemonBasic(@Path("idOrName") idOrName: String): PokemonBasicDto

    @GET("pokemon/{id}")
    suspend fun pokemonDetail(@Path("id") id: Int): PokemonDto

    @GET("pokemon-species/{id}")
    suspend fun species(@Path("id") id: Int): SpeciesDto

    @GET("evolution-chain/{id}")
    suspend fun evolutionChain(@Path("id") id: Int): EvolutionChainDto
}
