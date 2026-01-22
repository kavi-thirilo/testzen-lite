package com.testzen.core.stability

import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * Resilient retry mechanism with exponential backoff.
 *
 * Features:
 * - Exponential backoff with jitter
 * - Configurable retry conditions
 * - Transient vs permanent failure classification
 * - Stale element automatic recovery
 * - Operation result tracking
 */
class RetryWithBackoff(
    private val config: StabilityConfig = StabilityConfig.default()
) {
    private val logger = LoggerFactory.getLogger(RetryWithBackoff::class.java)

    /**
     * Retry result with detailed information.
     */
    sealed class RetryResult<T> {
        data class Success<T>(
            val value: T,
            val attempts: Int,
            val totalTimeMs: Long,
            val retriedExceptions: List<String>
        ) : RetryResult<T>()

        data class Failure<T>(
            val lastException: Exception,
            val attempts: Int,
            val totalTimeMs: Long,
            val allExceptions: List<Exception>
        ) : RetryResult<T>()
    }

    /**
     * Retry context for tracking state across attempts.
     */
    data class RetryContext(
        val attemptNumber: Int,
        val totalAttempts: Int,
        val lastException: Exception?,
        val elapsedTimeMs: Long,
        val remainingTimeMs: Long
    )

    /**
     * Execute operation with retry and exponential backoff.
     *
     * @param operation The operation to execute
     * @param maxAttempts Maximum number of attempts
     * @param timeoutMs Total timeout for all attempts
     * @param retryOn Exception types that should trigger retry
     * @param onRetry Callback before each retry
     * @return RetryResult with success value or failure details
     */
    fun <T> execute(
        operation: (RetryContext) -> T,
        maxAttempts: Int = config.maxTransientRetries + 1,
        timeoutMs: Long = config.getActionTimeout("DEFAULT"),
        retryOn: Set<KClass<out Exception>> = TRANSIENT_EXCEPTIONS,
        onRetry: ((RetryContext, Exception) -> Unit)? = null
    ): RetryResult<T> {
        val startTime = System.currentTimeMillis()
        val exceptions = mutableListOf<Exception>()
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            val elapsedTime = System.currentTimeMillis() - startTime
            val remainingTime = timeoutMs - elapsedTime

            if (remainingTime <= 0 && attempt > 1) {
                logger.debug("Retry timeout reached after $attempt attempts")
                break
            }

            val context = RetryContext(
                attemptNumber = attempt,
                totalAttempts = maxAttempts,
                lastException = lastException,
                elapsedTimeMs = elapsedTime,
                remainingTimeMs = remainingTime.coerceAtLeast(0)
            )

            try {
                val result = operation(context)
                val totalTime = System.currentTimeMillis() - startTime

                if (attempt > 1) {
                    logger.debug("Operation succeeded on attempt $attempt after ${totalTime}ms")
                }

                return RetryResult.Success(
                    value = result,
                    attempts = attempt,
                    totalTimeMs = totalTime,
                    retriedExceptions = exceptions.map { it.javaClass.simpleName }
                )

            } catch (e: Exception) {
                lastException = e
                exceptions.add(e)

                val shouldRetry = shouldRetry(e, retryOn, attempt, maxAttempts, remainingTime)

                if (!shouldRetry) {
                    val totalTime = System.currentTimeMillis() - startTime
                    logger.debug("Operation failed permanently: ${e.javaClass.simpleName} - ${e.message}")
                    return RetryResult.Failure(
                        lastException = e,
                        attempts = attempt,
                        totalTimeMs = totalTime,
                        allExceptions = exceptions.toList()
                    )
                }

                // Calculate backoff delay
                val delay = config.calculateRetryDelay(attempt)
                val actualDelay = minOf(delay, remainingTime)

                logger.debug(
                    "Attempt $attempt failed with ${e.javaClass.simpleName}, " +
                            "retrying in ${actualDelay}ms (${maxAttempts - attempt} attempts remaining)"
                )

                onRetry?.invoke(context, e)

                if (actualDelay > 0) {
                    Thread.sleep(actualDelay)
                }
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        return RetryResult.Failure(
            lastException = lastException ?: RuntimeException("Max attempts reached"),
            attempts = maxAttempts,
            totalTimeMs = totalTime,
            allExceptions = exceptions.toList()
        )
    }

    /**
     * Execute operation with automatic stale element recovery.
     *
     * @param refind Function to re-find the element if stale
     * @param operation Operation to perform on the element
     * @return RetryResult
     */
    fun <T, E> executeWithStaleRecovery(
        refind: () -> E?,
        operation: (E) -> T,
        maxAttempts: Int = config.staleElementMaxRetries + 1
    ): RetryResult<T> {
        if (!config.staleElementRecoveryEnabled) {
            return execute({ ctx ->
                val element = refind() ?: throw NoSuchElementException("Element not found")
                operation(element)
            }, maxAttempts)
        }

        var currentElement = refind()

        return execute({ ctx ->
            if (currentElement == null || ctx.lastException is StaleElementReferenceException) {
                logger.debug("Re-finding element after stale reference")
                Thread.sleep(config.staleElementRefindDelayMs)
                currentElement = refind() ?: throw NoSuchElementException("Element not found on re-find")
            }
            operation(currentElement!!)
        }, maxAttempts, retryOn = STALE_ELEMENT_EXCEPTIONS)
    }

    /**
     * Execute operation that should succeed within timeout (polling).
     *
     * @param condition Condition that returns true when satisfied
     * @param timeoutMs Maximum time to wait
     * @param pollIntervalMs Interval between condition checks
     * @param description Description for logging
     * @return true if condition was met, false if timeout
     */
    fun waitForCondition(
        condition: () -> Boolean,
        timeoutMs: Long = config.getActionTimeout("DEFAULT"),
        pollIntervalMs: Long = 100,
        description: String = "condition"
    ): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                if (condition()) {
                    val elapsed = System.currentTimeMillis() - startTime
                    logger.trace("$description satisfied after ${elapsed}ms")
                    return true
                }
            } catch (e: Exception) {
                // Condition threw exception, continue polling
                logger.trace("Condition check failed: ${e.message}")
            }
            Thread.sleep(pollIntervalMs)
        }

        logger.debug("$description not satisfied within ${timeoutMs}ms")
        return false
    }

    /**
     * Execute operation with progressive timeout increase.
     *
     * Useful for operations that might need more time on retry.
     */
    fun <T> executeWithProgressiveTimeout(
        operation: (timeoutMs: Long) -> T,
        initialTimeoutMs: Long,
        maxTimeoutMs: Long,
        timeoutMultiplier: Double = 1.5,
        maxAttempts: Int = 3
    ): RetryResult<T> {
        var currentTimeout = initialTimeoutMs
        val exceptions = mutableListOf<Exception>()
        val startTime = System.currentTimeMillis()

        for (attempt in 1..maxAttempts) {
            try {
                val result = operation(currentTimeout)
                return RetryResult.Success(
                    value = result,
                    attempts = attempt,
                    totalTimeMs = System.currentTimeMillis() - startTime,
                    retriedExceptions = exceptions.map { it.javaClass.simpleName }
                )
            } catch (e: Exception) {
                exceptions.add(e)

                if (attempt < maxAttempts && isTimeoutRelated(e)) {
                    currentTimeout = minOf((currentTimeout * timeoutMultiplier).toLong(), maxTimeoutMs)
                    logger.debug("Increasing timeout to ${currentTimeout}ms for attempt ${attempt + 1}")
                } else if (!shouldRetry(e, TRANSIENT_EXCEPTIONS, attempt, maxAttempts, Long.MAX_VALUE)) {
                    break
                }
            }
        }

        return RetryResult.Failure(
            lastException = exceptions.last(),
            attempts = exceptions.size,
            totalTimeMs = System.currentTimeMillis() - startTime,
            allExceptions = exceptions
        )
    }

    /**
     * Determine if exception should trigger a retry.
     */
    private fun shouldRetry(
        exception: Exception,
        retryOn: Set<KClass<out Exception>>,
        currentAttempt: Int,
        maxAttempts: Int,
        remainingTimeMs: Long
    ): Boolean {
        // Check if we have attempts left
        if (currentAttempt >= maxAttempts) {
            return false
        }

        // Check if we have time left
        if (remainingTimeMs <= 0) {
            return false
        }

        // Check if this exception type is retryable
        return retryOn.any { it.java.isInstance(exception) } ||
                isTransientException(exception)
    }

    /**
     * Check if exception is likely transient.
     */
    private fun isTransientException(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return TRANSIENT_KEYWORDS.any { message.contains(it) }
    }

    /**
     * Check if exception is timeout-related.
     */
    private fun isTimeoutRelated(exception: Exception): Boolean {
        return exception is TimeoutException ||
                exception.message?.lowercase()?.contains("timeout") == true ||
                exception.message?.lowercase()?.contains("timed out") == true
    }

    companion object {
        /**
         * Common transient exceptions that should be retried.
         */
        val TRANSIENT_EXCEPTIONS: Set<KClass<out Exception>> = setOf(
            StaleElementReferenceException::class,
            TimeoutException::class,
            WebDriverException::class
        )

        /**
         * Stale element specific exceptions.
         */
        val STALE_ELEMENT_EXCEPTIONS: Set<KClass<out Exception>> = setOf(
            StaleElementReferenceException::class
        )

        /**
         * Keywords in exception messages that indicate transient failures.
         */
        val TRANSIENT_KEYWORDS = listOf(
            "timeout",
            "timed out",
            "connection refused",
            "connection reset",
            "socket",
            "network",
            "temporary",
            "retry",
            "unavailable",
            "busy"
        )

        /**
         * Execute simple operation with default retry settings.
         */
        inline fun <T> withRetry(
            maxAttempts: Int = 3,
            crossinline operation: () -> T
        ): T {
            val retry = RetryWithBackoff()
            val result = retry.execute({ operation() }, maxAttempts)
            return when (result) {
                is RetryResult.Success -> result.value
                is RetryResult.Failure -> throw result.lastException
            }
        }
    }
}

/**
 * Extension function for cleaner retry syntax.
 */
inline fun <T> retryWithBackoff(
    config: StabilityConfig = StabilityConfig.default(),
    maxAttempts: Int = config.maxTransientRetries + 1,
    crossinline operation: () -> T
): T {
    val retry = RetryWithBackoff(config)
    val result = retry.execute({ operation() }, maxAttempts)
    return when (result) {
        is RetryWithBackoff.RetryResult.Success -> result.value
        is RetryWithBackoff.RetryResult.Failure -> throw result.lastException
    }
}
