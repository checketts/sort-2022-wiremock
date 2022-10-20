package com.github.checketts.wiremockexample

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@WireMockTest
internal class PalindromeClientTest {

    lateinit var sut: PalindromeClient

    @BeforeEach
    fun setup(wmConfig: WireMockRuntimeInfo) {
        println("WM: ${wmConfig.httpPort}")
        sut = PalindromeClient(WebClient.builder(), PalindromeConfig("http://localhost:${wmConfig.httpPort}"))
    }

    @Test
    fun aTest(wmConfig: WireMockRuntimeInfo) {
        stubFor(get("/palindrome/random").willReturn(aResponse()
                .withHeader("Content-Type", "application/json").withStatus(200).withBody("""{
            "text": "Mom", "normalized": "mom"
            }""")))

        val p1 = sut.fetchAPalindrome()

        p1.text shouldBe "Mom"
    }

    @Test
    fun forceRetry(wmConfig: WireMockRuntimeInfo) {
        stubFor(get(urlEqualTo("/palindrome/random")).inScenario("palindrome-random")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500)).willSetStateTo("error"))

        stubFor(get(urlEqualTo("/palindrome/random")).inScenario("palindrome-random")
                .whenScenarioStateIs("error")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json").withStatus(200).withBody("""{
                "text": "Mom", 
                "normalized": "mom",
                "createdAt":"2022-09-26T04:07:35.755362Z",
                "submittedById":"63d39ac0-323a-4ca8-9601-8c73c099bd7d",
                "votes":0
            }""")).willSetStateTo(STARTED))


        val p1 = sut.fetchAPalindrome()

        p1.text shouldBe "Mom"
    }

}
