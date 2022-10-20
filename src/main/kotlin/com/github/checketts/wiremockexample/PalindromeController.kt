package com.github.checketts.wiremockexample

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/palindromes")
class PalindromeController(
        private val palindromeClient: PalindromeClient,
) {

    @GetMapping
    fun getAPalindrome(
            model: Model,
    ): String {
        model["p"] = palindromeClient.fetchAPalindrome()
        return "palindromes"
    }

    @GetMapping("top")
    fun getTopPalindromes(
            model: Model,
    ): String {
        model["topPalindromes"] = palindromeClient.fetchTopPalindromes()
        return "palindromes :: topPalindromes"
    }

    @GetMapping("random")
    fun getARandomPalindrome(
            model: Model,
    ): String {
        model["p"] = palindromeClient.fetchAPalindrome()
        return "palindromes :: randomPalindrome"
    }

    data class NewPalindromeRequest(val palindromeText: String, val submitter: String)

    @GetMapping("{palindromeId}")
    fun getAPalindromeById(
            @PathVariable palindromeId: String,
            model: Model,
    ): String {
        model["p"] = palindromeClient.getAPalindromeById(palindromeId)
        return "palindromes :: randomPalindrome"
    }


    @PostMapping("vote/{palindromeId}")
    fun addVote(
            @PathVariable palindromeId: String,
            model: Model,
    ): String {
        palindromeClient.recordVote(palindromeId)

        return getAPalindromeById(palindromeId, model)
    }


    @PostMapping
    fun postNewPalindrome(
            @ModelAttribute form: NewPalindromeRequest,
            bindingResult: BindingResult,
            model: Model,
    ): String {
        println("The form: $form and $bindingResult")
        palindromeClient.addNewPalindrome(form.palindromeText, form.submitter)

        model["addMessage"] = "New palindrome ${form.palindromeText} has been added!"

        return "palindromes :: addForm"
    }


}
