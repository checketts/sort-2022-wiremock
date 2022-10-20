package com.github.checketts.wiremockexample

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault

/**
 * Use http://localhost:8078/__admin/recorder/ to enable recording
 */

fun main(args: Array<String>) {
    println("Starting wiremock")
    val wireMockServer: WireMockServer = WireMockServer(
            WireMockConfiguration.options()
                    .port(8078)
    ).apply {
        this.stubFor(any(urlMatching(".*"))
                .atPriority(10)
                .willReturn(aResponse().proxiedFrom("http://localhost:8081")
                        .withFixedDelay(1000)
                )
        )

//        stubFor(get(urlMatching("/palindrome/mom")
//        ).willReturn(aResponse().withFixedDelay(5000)))
    }

    wireMockServer.start()

    println("Port: ${wireMockServer.port()}")
}

