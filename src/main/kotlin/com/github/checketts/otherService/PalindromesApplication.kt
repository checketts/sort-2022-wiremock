package com.github.checketts.otherService

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException.NotFound
import java.lang.IllegalStateException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.http.HttpServletResponse

@SpringBootApplication
private class PalindromesApplication{
	@Bean
	fun userDetailService(): UserDetailsService {
		return UserDetailsService { username ->
			if (username.equals("user", ignoreCase = true)) {
				User(username, "{noop}password", true, true, true, true,
						AuthorityUtils.createAuthorityList("USER"))
			} else {
				throw UsernameNotFoundException("could not find the user '$username'")
			}
		}
	}

	@Bean
	fun externalSecurity(http: HttpSecurity): SecurityFilterChain {
		return http
//				.userDetailsService
				.requestMatchers()
				.antMatchers("/palindrome/**")
				.and().authorizeRequests()
				.anyRequest().hasAuthority("USER").and()
				.httpBasic().authenticationEntryPoint(BasicAuthenticationEntryPoint().apply { realmName = "demo realm" })
				.and().csrf().disable()
				.build()
	}
}

fun main(args: Array<String>) {
	runApplication<PalindromesApplication>(*args)
}


data class Submitter(
		val name: String,
		val id: String,
)

data class PalindromeDetails(
		val text: String,
		val normalized: String,
		val createdAt: Instant,
		val submittedById: String,
		val votes: AtomicLong = AtomicLong(0)
		)

@RestController
@RequestMapping()
class RandomPalindromeController {

	val alice = Submitter("Alice", UUID.randomUUID().toString())
	val bob = Submitter("Bob", UUID.randomUUID().toString())
	val carl = Submitter("Carl", UUID.randomUUID().toString())

	val submitters = mutableListOf(alice, bob, carl)

	val palindromeList = mutableListOf(
			PalindromeDetails("racecar", "racecar", Instant.now().minus(10, ChronoUnit.DAYS), alice.id),
			PalindromeDetails("Step on no pets", "steponnopets", Instant.now().minus(10, ChronoUnit.DAYS), alice.id),
			PalindromeDetails("Mom", "mom", Instant.now().minus(10, ChronoUnit.DAYS), bob.id),
			PalindromeDetails("Dad", "dad", Instant.now().minus(10, ChronoUnit.DAYS), carl.id),
	)

	// usage

	@GetMapping
	fun redirectRoot(response: HttpServletResponse) {
		response.sendRedirect("/palindrome/random")
	}

	@GetMapping("palindrome","palindrome/random")
	fun getRandom(): PalindromeDetails {
		return palindromeList.random()
	}

	@GetMapping("palindrome/{id}")
	fun getById(@PathVariable id: String): PalindromeDetails {
		return palindromeList.firstOrNull { it.normalized == id.lowercase() }
				?: throw NotFoundResponse("Palindrome with id of $id")
	}

	class NotFoundResponse(msg: String): RuntimeException(msg)

	@ResponseStatus(value= NOT_FOUND, reason="Not Found")
	@ExceptionHandler(NotFoundResponse::class)
	fun notFoundHandler() {}

	@GetMapping("palindrome/popular")
	fun getMostPopular(): List<PalindromeDetails> {
		return palindromeList.sortedByDescending { it.votes.get() }.take(5)
	}


	@GetMapping("palindrome/submitted-by/{id}")
	fun getBySubmittedId(@PathVariable id: String): List<PalindromeDetails> {
		return palindromeList.filter { it.submittedById == id }
	}

	@PostMapping("palindrome/vote/{id}")
	fun postVote(@PathVariable id: String): Long {
		return palindromeList.firstOrNull { it.normalized == id }?.votes?.incrementAndGet() ?: 0
	}

	data class PalindromeRequest(
			val text: String,
			val submitter: String,
	)

	@PostMapping("palindrome")
	fun postNewPalindrome(@RequestBody palindromeRequest: PalindromeRequest): PalindromeDetails {
		val normalized = palindromeRequest.text.replace("[^a-zA-Z\\d]*", "")

		val alreadyExists = palindromeList.firstOrNull { it.normalized == normalized } != null
		if(alreadyExists) {
			throw IllegalStateException("A palindrome matching '$normalized' already exists")
		}
		val submitter = findSubmitter(palindromeRequest.submitter)
		val newPal = PalindromeDetails(palindromeRequest.text, normalized, Instant.now(), submitter.id)
		palindromeList.add(newPal)

		return newPal
	}

	private fun findSubmitter(submitterName: String): Submitter {
		val submitter = submitters.firstOrNull { it.name == submitterName }
		if(submitter != null) {
			return submitter
		}

		val newSubmitter = Submitter(submitterName, UUID.randomUUID().toString())
		submitters.add(newSubmitter)
		return newSubmitter
	}


}
