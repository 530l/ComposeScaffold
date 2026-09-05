package com.lyf.composescaffold.core.data.di

import com.lyf.composescaffold.core.data.api.CartApi
import com.lyf.composescaffold.core.data.repository.CartRepository
import com.lyf.composescaffold.core.data.repository.DefaultCartRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CartDataModule {

    @Binds
    @Singleton
    internal abstract fun bindCartRepository(impl: DefaultCartRepository): CartRepository

    companion object {
        @Provides
        @Singleton
        fun provideCartApi(retrofit: Retrofit): CartApi =
            retrofit.create(CartApi::class.java)
    }
}
