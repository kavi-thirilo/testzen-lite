package com.testzen.core.execution

import io.appium.java_client.AppiumDriver
import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Handles touch gestures and swipe actions.
 *
 * Responsible for:
 * - Scroll gestures (up/down/left/right)
 * - Swipe gestures
 * - Long press
 * - Double tap
 *
 * Single Responsibility: Touch gesture execution.
 */
class GestureHandler(
    private val driver: WebDriver
) {
    private val logger = LoggerFactory.getLogger(GestureHandler::class.java)

    /**
     * Perform a scroll gesture in the specified direction.
     *
     * @param direction The scroll direction
     * @param scrollPercent How much of the screen to scroll (0.0 to 1.0)
     */
    fun scroll(direction: ScrollDirection, scrollPercent: Double = 0.6): Boolean {
        val screenSize = getScreenSize()
        val centerX = screenSize.width / 2
        val centerY = screenSize.height / 2

        val scrollDistance = (minOf(screenSize.width, screenSize.height) * scrollPercent * 0.3).toInt()

        val (startX, startY, endX, endY) = when (direction) {
            ScrollDirection.UP -> listOf(centerX, centerY - scrollDistance, centerX, centerY + scrollDistance)
            ScrollDirection.DOWN -> listOf(centerX, centerY + scrollDistance, centerX, centerY - scrollDistance)
            ScrollDirection.LEFT -> listOf(centerX + scrollDistance, centerY, centerX - scrollDistance, centerY)
            ScrollDirection.RIGHT -> listOf(centerX - scrollDistance, centerY, centerX + scrollDistance, centerY)
        }

        return performSwipe(startX, startY, endX, endY)
    }

    /**
     * Perform a swipe gesture from start to end coordinates.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX Ending X coordinate
     * @param endY Ending Y coordinate
     * @param durationMs Duration of swipe in milliseconds
     * @return True if swipe was performed
     */
    fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long = 300): Boolean {
        val appiumDriver = driver as? AppiumDriver
        if (appiumDriver == null) {
            logger.warn("Swipe not supported on this driver type")
            return false
        }

        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val sequence = Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), endX, endY))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

        appiumDriver.perform(listOf(sequence))
        return true
    }

    /**
     * Perform a long press on an element.
     *
     * @param element The element to long press
     * @param durationMs Duration of press in milliseconds
     * @return True if long press was performed
     */
    fun longPress(element: WebElement, durationMs: Long = 1000): Boolean {
        val appiumDriver = driver as? AppiumDriver
        if (appiumDriver == null) {
            logger.warn("Long press not supported on this driver type")
            return false
        }

        val location = element.location
        val size = element.size
        val centerX = location.x + size.width / 2
        val centerY = location.y + size.height / 2

        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val sequence = Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), centerX, centerY))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

        appiumDriver.perform(listOf(sequence))
        return true
    }

    /**
     * Perform a long press at specific coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param durationMs Duration of press in milliseconds
     * @return True if long press was performed
     */
    fun longPressAt(x: Int, y: Int, durationMs: Long = 1000): Boolean {
        val appiumDriver = driver as? AppiumDriver
        if (appiumDriver == null) {
            logger.warn("Long press not supported on this driver type")
            return false
        }

        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val sequence = Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), x, y))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

        appiumDriver.perform(listOf(sequence))
        return true
    }

    /**
     * Perform a double tap on an element.
     *
     * @param element The element to double tap
     * @return True if double tap was performed
     */
    fun doubleTap(element: WebElement): Boolean {
        val appiumDriver = driver as? AppiumDriver
        if (appiumDriver == null) {
            logger.warn("Double tap not supported on this driver type")
            return false
        }

        val location = element.location
        val size = element.size
        val centerX = location.x + size.width / 2
        val centerY = location.y + size.height / 2

        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val sequence = Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

        appiumDriver.perform(listOf(sequence))
        return true
    }

    /**
     * Perform a double tap at specific coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if double tap was performed
     */
    fun doubleTapAt(x: Int, y: Int): Boolean {
        val appiumDriver = driver as? AppiumDriver
        if (appiumDriver == null) {
            logger.warn("Double tap not supported on this driver type")
            return false
        }

        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val sequence = Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

        appiumDriver.perform(listOf(sequence))
        return true
    }

    /**
     * Get screen dimensions.
     */
    fun getScreenSize(): Dimension {
        return driver.manage().window().size
    }

    /**
     * Check if gesture operations are supported.
     */
    fun isSupported(): Boolean {
        return driver is AppiumDriver
    }
}
