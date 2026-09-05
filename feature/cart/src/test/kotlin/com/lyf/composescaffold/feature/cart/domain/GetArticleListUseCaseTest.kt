package com.lyf.composescaffold.feature.cart.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetArticleListUseCaseTest {

    @Test
    fun invokeDelegatesToRepository() = runTest {
        val expectedArticles = listOf(
            Article(1, "Title 1", "Author 1", "Chapter 1", "https://example.com/1", "1h ago"),
        )
        val expectedPage = ArticlePage(items = expectedArticles, hasMore = true)
        val fakeRepository = object : ArticleRepository {
            var queriedPage: Int? = null
            override suspend fun loadPage(page: Int): Result<ArticlePage> {
                queriedPage = page
                return Result.success(expectedPage)
            }
        }

        val useCase = GetArticleListUseCase(fakeRepository)
        val result = useCase(2)

        assertThat(fakeRepository.queriedPage).isEqualTo(2)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedPage)
    }

    @Test
    fun invokePropagatesRepositoryFailure() = runTest {
        val expectedException = IllegalStateException("网络异常")
        val fakeRepository = object : ArticleRepository {
            override suspend fun loadPage(page: Int): Result<ArticlePage> =
                Result.failure(expectedException)
        }

        val useCase = GetArticleListUseCase(fakeRepository)
        val result = useCase(1)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(expectedException)
    }
}
