package com.lyf.small.data.login.di

import com.lyf.small.data.login.repository.LoginRepository
import com.lyf.small.data.login.repository.MockLoginRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Mock 阶段绑定假实现；后端就绪后改为绑定真实仓库即可，接口与 ViewModel 不动。 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoginDataModule {

    @Binds
    @Singleton
    abstract fun bindLoginRepository(
        repository: MockLoginRepository,
    ): LoginRepository
}
