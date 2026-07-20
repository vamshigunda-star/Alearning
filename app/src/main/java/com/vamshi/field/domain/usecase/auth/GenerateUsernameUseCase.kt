package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Derives a unique login handle from a coach's display name.
 *
 * The new onboarding flow only collects "Coach Name" + password (+ optional email) —
 * there is no username field for the coach to fill in. This use case slugifies the
 * name into a candidate that satisfies [ValidateUsernameUseCase.USERNAME_REGEX], then
 * appends an incrementing numeric suffix until it finds one that isn't already taken.
 *
 * Slugify rules (documented here as the single source of truth):
 *  1. Lowercase the input.
 *  2. Replace every run of characters outside `[a-z0-9]` with a single `.`.
 *  3. Trim leading/trailing `.`.
 *  4. If the result is blank, fall back to `"coach"`.
 *  5. If shorter than the 3-character minimum (e.g. "Jo" -> "jo"), pad with
 *     trailing zeros so the regex's `{3,30}` lower bound is always satisfied.
 *  6. Truncate to 24 characters, leaving room for a numeric suffix within the
 *     30-character username limit.
 */
class GenerateUsernameUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    companion object {
        private val NON_SLUG_CHARS = Regex("[^a-z0-9]+")
        private const val MAX_BASE_LENGTH = 24
        private const val MIN_BASE_LENGTH = 3
    }

    suspend operator fun invoke(coachName: String): String {
        val base = slugify(coachName)

        var candidate = base
        var suffix = 1
        while (repository.isUsernameTaken(candidate)) {
            suffix += 1
            candidate = "$base$suffix"
        }
        return candidate
    }

    private fun slugify(name: String): String {
        val slug = name.lowercase()
            .replace(NON_SLUG_CHARS, ".")
            .trim('.')
        return slug.ifBlank { "coach" }
            .take(MAX_BASE_LENGTH)
            .padEnd(MIN_BASE_LENGTH, '0')
    }
}
