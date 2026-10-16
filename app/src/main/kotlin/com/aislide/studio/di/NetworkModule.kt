package com.aislide.studio.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import com.aislide.studio.data.local.database.AppJson
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
                callTimeout(120, TimeUnit.SECONDS)  // Rule of android skill: set callTimeout explicitly
                retryOnConnectionFailure(true)
            }
        }

        install(ContentNegotiation) {
            json(AppJson.default)
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }

        install(Logging) {
            level = LogLevel.NONE  // set to HEADERS/INFO in debug if needed
        }
    }
}
