package com.example.parishsync

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParishAppNavigationAndClickabilityTest {

  @get:Rule(order = 0)
  val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
    Manifest.permission.POST_NOTIFICATIONS
  )

  @get:Rule(order = 1)
  val composeRule = createAndroidComposeRule<MainActivity>()

  private fun ensureSignedInAsAdmin() {
    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
        val hasServingTab = composeRule.onAllNodesWithText("Serving Schedule").fetchSemanticsNodes().isNotEmpty()
        val hasVolunteerTab = composeRule.onAllNodesWithText("Volunteer Schedule").fetchSemanticsNodes().isNotEmpty()
        val hasOnboarding = composeRule.onAllNodesWithText("Email Address").fetchSemanticsNodes().isNotEmpty()
        hasServingTab || hasVolunteerTab || hasOnboarding
      }.getOrDefault(false)
    }

    // If on OnboardingScreen, tap quick-fill Admin and Sign In
    if (composeRule.onAllNodesWithText("Serving Schedule").fetchSemanticsNodes().isEmpty()) {
      composeRule.onAllNodes(hasClickAction() and hasText("Admin")).onFirst().performClick()
      composeRule.onAllNodes(hasClickAction() and hasText("Sign In", substring = true, ignoreCase = true)).onFirst().performClick()
      composeRule.waitUntil(timeoutMillis = 20_000) {
        runCatching {
          composeRule.onAllNodesWithText("Serving Schedule").fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
      }
    }
  }

  @Test
  fun appLoadsAndAllTabsAndButtonsAreClickable() {
    ensureSignedInAsAdmin()

    // 1. Tap Serving Schedule bottom navigation tab
    composeRule.onNodeWithText("Serving Schedule").performClick()
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule.onAllNodesWithText("Schedules").fetchSemanticsNodes().isNotEmpty()
    }

    // 2. Verify Schedules tab & Scrape & Replace button clickability
    composeRule.onNodeWithText("Schedules").assertIsDisplayed()
    if (composeRule.onAllNodesWithText("Scrape & Replace Schedules").fetchSemanticsNodes().isNotEmpty()) {
      composeRule.onNodeWithText("Scrape & Replace Schedules").performClick()
      composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodesWithText("Purge & Re-Scrape").fetchSemanticsNodes().isNotEmpty()
      }
      composeRule.onNodeWithText("Cancel").performClick()
    }

    // 2b. Click a District Schedule card and verify unified Details & PDF Preview Modal opens
    composeRule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
    val districtNode = when {
      composeRule.onAllNodes(hasClickAction() and hasText("Kitchener")).fetchSemanticsNodes().isNotEmpty() ->
        composeRule.onAllNodes(hasClickAction() and hasText("Kitchener")).onFirst()
      composeRule.onAllNodes(hasClickAction() and hasText("Alberta")).fetchSemanticsNodes().isNotEmpty() ->
        composeRule.onAllNodes(hasClickAction() and hasText("Alberta")).onFirst()
      else -> null
    }
    if (districtNode != null) {
      districtNode.performClick()
      composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodesWithText("Source PDF URL").fetchSemanticsNodes().isNotEmpty()
      }
      composeRule.onNodeWithText("Copy URL").assertIsDisplayed()
      composeRule.onNodeWithText("Download PDF").assertIsDisplayed()
      composeRule.onNodeWithText("Close").performClick()
    }

    // 3. Switch to DSG tab and click month filter chips
    composeRule.onNodeWithText("DSG").performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText("Current Month (Nov 2026)").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText("Next Month (Dec 2026)").performClick()
    composeRule.onNodeWithText("Current Month (Nov 2026)").performClick()

    if (composeRule.onAllNodesWithText("Scrape & Replace DSG").fetchSemanticsNodes().isNotEmpty()) {
      composeRule.onAllNodesWithText("Scrape & Replace DSG").onFirst().performClick()
      composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodesWithText("Purge & Re-Scrape").fetchSemanticsNodes().isNotEmpty()
      }
      composeRule.onNodeWithText("Cancel").performClick()
    }

    // 3b. Click DSG document & audio chips and verify preview / player modals open
    if (composeRule.onAllNodes(hasClickAction() and hasText("English DSG")).fetchSemanticsNodes().isNotEmpty()) {
      composeRule.onAllNodes(hasClickAction() and hasText("English DSG")).onFirst().performClick()
      composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodesWithText("Source PDF URL").fetchSemanticsNodes().isNotEmpty()
      }
      composeRule.onNodeWithText("Copy URL").assertIsDisplayed()
      composeRule.onNodeWithText("Download PDF").assertIsDisplayed()
      composeRule.onNodeWithText("Close").performClick()
    }
    if (composeRule.onAllNodes(hasClickAction() and hasText("References")).fetchSemanticsNodes().isNotEmpty()) {
      composeRule.onAllNodes(hasClickAction() and hasText("References")).onFirst().performClick()
      composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodesWithText("Source PDF URL").fetchSemanticsNodes().isNotEmpty() ||
          composeRule.onAllNodesWithText("PDF Viewer / Install").fetchSemanticsNodes().isNotEmpty()
      }
      composeRule.onNodeWithText("Close").performClick()
    }
    if (composeRule.onAllNodes(hasClickAction() and hasText("Audio")).fetchSemanticsNodes().isNotEmpty()) {
      composeRule.onAllNodes(hasClickAction() and hasText("Audio")).onFirst().performClick()
      composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodesWithText("Divine Service Audio").fetchSemanticsNodes().isNotEmpty()
      }
      composeRule.onNodeWithText("Source Audio URL").assertIsDisplayed()
      composeRule.onNodeWithText("Copy URL").assertIsDisplayed()
      composeRule.onNodeWithText("Download Audio (.mp3)").assertIsDisplayed()
      composeRule.onNodeWithText("Close").performClick()
    }

    // 4. Switch to Announcements tab
    composeRule.onAllNodes(hasClickAction() and hasText("Announcements", substring = true)).onFirst().performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText("Funerals").fetchSemanticsNodes().isNotEmpty()
    }

    // 5. Switch to Admin tab (using clickable node to avoid top-bar Admin badge) and verify clickability
    val clickableAdminTabs = composeRule.onAllNodes(hasClickAction() and hasText("Admin"))
    if (clickableAdminTabs.fetchSemanticsNodes().isNotEmpty()) {
      clickableAdminTabs.onLast().performClick()
      composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodesWithText("Accounts").fetchSemanticsNodes().isNotEmpty()
      }
      composeRule.onNodeWithText("Accounts").performClick()
      // Verify Rector and Minister are separate independent role pills
      composeRule.onAllNodesWithText("Rector").onFirst().assertIsDisplayed()
      composeRule.onAllNodesWithText("Minister").onFirst().assertIsDisplayed()
      if (composeRule.onAllNodesWithText("Management").fetchSemanticsNodes().isNotEmpty()) {
        composeRule.onNodeWithText("Management").performClick()
      }
    }

    // 6. Switch back to Volunteer Schedule
    composeRule.onAllNodes(hasClickAction() and hasText("Volunteer Schedule")).onLast().performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(hasText("Volunteer Schedule", substring = true)).fetchSemanticsNodes().isNotEmpty()
    }
  }
}
