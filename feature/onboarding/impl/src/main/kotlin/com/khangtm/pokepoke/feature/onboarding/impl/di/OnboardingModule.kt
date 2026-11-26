package com.khangtm.pokepoke.feature.onboarding.impl.di

import com.khangtm.pokepoke.core.navigation.EntryProviderInstaller
import com.khangtm.pokepoke.core.navigation.NavigationInterceptor
import com.khangtm.pokepoke.feature.onboarding.impl.data.OnboardingRepository
import com.khangtm.pokepoke.feature.onboarding.impl.data.OnboardingRepositoryImpl
import com.khangtm.pokepoke.feature.onboarding.impl.domain.CompleteOnboardingUseCase
import com.khangtm.pokepoke.feature.onboarding.impl.domain.CompleteOnboardingUseCaseImpl
import com.khangtm.pokepoke.feature.onboarding.impl.domain.IsOnboardingCompletedUseCase
import com.khangtm.pokepoke.feature.onboarding.impl.domain.IsOnboardingCompletedUseCaseImpl
import com.khangtm.pokepoke.feature.onboarding.impl.navigation.OnboardingEntryInstaller
import com.khangtm.pokepoke.feature.onboarding.impl.navigation.OnboardingInterceptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OnboardingBindingsModule {

    @Binds
    abstract fun bindRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    abstract fun bindIsOnboardingCompleted(impl: IsOnboardingCompletedUseCaseImpl): IsOnboardingCompletedUseCase

    @Binds
    abstract fun bindCompleteOnboarding(impl: CompleteOnboardingUseCaseImpl): CompleteOnboardingUseCase

    @Binds
    @IntoSet
    abstract fun bindInterceptor(impl: OnboardingInterceptor): NavigationInterceptor
}

@Module
@InstallIn(SingletonComponent::class)
internal object OnboardingEntryModule {

    @Provides
    @IntoSet
    fun provideEntryInstaller(): EntryProviderInstaller = OnboardingEntryInstaller
}
