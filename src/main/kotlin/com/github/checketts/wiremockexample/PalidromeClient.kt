package com.github.checketts.wiremockexample

import com.github.checketts.otherService.PalindromeDetails
import com.github.checketts.otherService.RandomPalindromeController
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.util.retry.RetryBackoffSpec
import java.time.Duration
import java.time.Instant

@ConstructorBinding
@ConfigurationProperties("palindrome")
data class PalindromeConfig(
        val url: String = "",
        val username: String = "",
        val password: String = "",
)

@Component
class PalindromeClient(
        webClientBuilder: WebClient.Builder,
        config: PalindromeConfig,
) {
    private val logger = LoggerFactory.getLogger(PalindromeClient::class.java)
    private val webClient = webClientBuilder
            .baseUrl(config.url)
            .defaultHeaders { it.setBasicAuth(config.username, config.password) }
            .build()


    fun fetchAPalindrome(): PalindromeDetails {
        return webClient.get()
                .uri("/palindrome/random")
                .retrieve()
                .toEntity(PalindromeDetails::class.java)
                .doOnError { e -> logger.error("Error making request", e)  }
                .retryWhen(RetryBackoffSpec.backoff(2, Duration.ofSeconds(3)))
                .block(Duration.ofSeconds(30))?.body ?: error("Error fetching palindrome")
    }

    fun addNewPalindrome(palindromeText: String, submitter: String) {
        webClient.post().uri("/palindrome").bodyValue(
                RandomPalindromeController.PalindromeRequest(palindromeText, submitter)
        ).retrieve().toBodilessEntity()
                .doOnError { e -> logger.error("Error making request", e)  }
                .block(Duration.ofSeconds(30))
    }

    fun getAPalindromeById(palindromeId: String): PalindromeDetails {
        return webClient.get()
                .uri("/palindrome/{id}", palindromeId)
                .retrieve()
                .toEntity(PalindromeDetails::class.java)
                .doOnError { e -> logger.error("Error making request", e)  }
                .retryWhen(RetryBackoffSpec.backoff(2, Duration.ofSeconds(3)))
                .block(Duration.ofSeconds(30))?.body ?: error("Error fetching palindrome")
    }

    fun recordVote(palindromeId: String) {
        webClient.post().uri("/palindrome/vote/{id}", palindromeId).retrieve().toBodilessEntity()
                .doOnError { e -> logger.error("Error making request", e)  }
                .block(Duration.ofSeconds(30))
    }

    fun fetchTopPalindromes(): List<PalindromeDetails> {
        return webClient.get()
                .uri("/palindrome/popular")
                .retrieve()
                .bodyToMono<List<PalindromeDetails>>()
                .doOnError { e -> logger.error("Error making request", e)  }
                .retryWhen(RetryBackoffSpec.backoff(2, Duration.ofSeconds(3)))
                .block(Duration.ofSeconds(30)) ?: error("Error fetching top palindrome")
    }

}
