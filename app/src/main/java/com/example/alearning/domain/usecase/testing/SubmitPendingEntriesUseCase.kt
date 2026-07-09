package com.example.alearning.domain.usecase.testing

import android.util.Log
import com.example.alearning.domain.repository.PendingTestEntryRepository
import com.example.alearning.domain.repository.PeopleRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads all pending entries for an event, persists each as a real TestResult via
 * RecordTestResultUseCase (which computes percentile snapshots), then clears the pending set.
 *
 * If recording any single entry fails the whole submit is aborted — already-persisted entries
 * are intentionally NOT rolled back (they are valid test results), and the corresponding pending
 * entries are cleared as they succeed so a retry only re-attempts the unprocessed remainder.
 *
 * Wrapped in NonCancellable so an in-flight flush survives the ViewModel being cleared
 * (e.g. coach navigates away while isSubmitting=true). Without this, a partial flush
 * could leave some rows in test_results and others stranded in pending_test_entries.
 */
class SubmitPendingEntriesUseCase @Inject constructor(
    private val pendingRepository: PendingTestEntryRepository,
    private val peopleRepository: PeopleRepository,
    private val recordTestResult: RecordTestResultUseCase
) {
    suspend operator fun invoke(eventId: String): Result<Int> = withContext(NonCancellable) {
        try {
            val pending = pendingRepository.getPendingForEvent(eventId)
            Log.d("SubmitPending", "begin flush eventId=$eventId pending=${pending.size}")
            var written = 0
            var skippedMissingAthlete = 0
            for (entry in pending) {
                val athlete = peopleRepository.getIndividualById(entry.individualId)
                if (athlete == null) {
                    // Pending row references an athlete that no longer exists. The
                    // pending entity has no FK to individuals (only to events), so this
                    // is reachable. Drop the orphan and warn — silently leaving it
                    // means the grid permanently shows a stale pending cell.
                    Log.w(
                        "SubmitPending",
                        "Dropping orphan pending entry: individualId=${entry.individualId} testId=${entry.testId} (athlete not found)"
                    )
                    pendingRepository.delete(eventId, entry.individualId, entry.testId)
                    skippedMissingAthlete++
                    continue
                }
                val ageMillis = System.currentTimeMillis() - athlete.dateOfBirth
                val ageYears = (ageMillis / (365.25 * 24 * 60 * 60 * 1000)).toFloat()
                recordTestResult(
                    eventId = eventId,
                    individualId = entry.individualId,
                    testId = entry.testId,
                    rawScore = entry.rawScore,
                    ageAtTime = ageYears,
                    sex = athlete.sex
                )
                pendingRepository.delete(eventId, entry.individualId, entry.testId)
                written++
            }
            Log.d(
                "SubmitPending",
                "flush complete eventId=$eventId written=$written droppedOrphans=$skippedMissingAthlete"
            )
            Result.success(written)
        } catch (e: Exception) {
            Log.e("SubmitPending", "flush FAILED eventId=$eventId", e)
            Result.failure(e)
        }
    }
}
