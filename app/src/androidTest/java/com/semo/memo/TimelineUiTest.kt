package com.semo.memo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import com.semo.memo.data.BundleEntity
import com.semo.memo.data.BundleWithMemos
import com.semo.memo.data.MemoEntity
import com.semo.memo.ui.SemoTheme
import com.semo.memo.ui.buildTimeline
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimelineUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun oneCharacterMemo_isCompactWithoutMoreButton_andLongPresses() {
        var longPressed = false
        val memo = MemoEntity(1, "1", 1_700_000_000_000, 1_700_000_000_000)
        composeRule.setContent {
            SemoTheme {
                MemoBubble(memo, false, {}, { longPressed = true })
            }
        }

        composeRule.onNodeWithContentDescription("메모 메뉴").assertDoesNotExist()
        composeRule.onNodeWithText("1").assertIsDisplayed().performTouchInput { longClick() }
        assertTrue(longPressed)
    }

    @Test fun memoTap_opensExternalEditDeleteSheet() {
        val memo = MemoEntity(1, "짧은 메모", 1_700_000_000_000, 1_700_000_000_000)
        composeRule.setContent {
            var actionMemo by remember { mutableStateOf<MemoEntity?>(null) }
            SemoTheme {
                MemoBubble(memo, false, { actionMemo = memo }, {})
                actionMemo?.let {
                    MemoActionSheet(it, { actionMemo = null }, {}, {})
                }
            }
        }

        composeRule.onNodeWithText("짧은 메모").performClick()
        composeRule.onNodeWithText("수정").assertIsDisplayed()
        composeRule.onNodeWithText("삭제").assertIsDisplayed()
    }

    @Test fun bundleCard_clicksThroughToDetailAction() {
        var clicked = false
        val memo = MemoEntity(1, "원본 메모", 100, 100)
        val bundle = BundleWithMemos(BundleEntity(2, null, "정리한 내용", 200, 200), listOf(memo))
        composeRule.setContent { SemoTheme { BundleCard(bundle, onClick = { clicked = true }) } }

        composeRule.onNodeWithContentDescription("묶음 메뉴 및 상세").assertDoesNotExist()
        composeRule.onNodeWithText("원본 메모").performTouchInput { click() }
        assertTrue(clicked)
    }

    @Test fun memoComposer_newlineDoesNotSend_andPlaneButtonDoes() {
        var sendCount = 0
        composeRule.setContent {
            var text by remember { mutableStateOf("") }
            SemoTheme {
                MemoComposer(text, { text = it }, { sendCount++ })
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("첫 줄\n둘째 줄")
        composeRule.onNodeWithText("첫 줄\n둘째 줄").assertIsDisplayed()
        assertTrue(sendCount == 0)
        composeRule.onNodeWithContentDescription("메모 보내기").performClick()
        assertTrue(sendCount == 1)
    }

    @Test fun bundleGrid_placesTwoEqualCardsPerRow() {
        var clickedBundleId: Long? = null
        val bundles = (1L..4L).map { id ->
            val memo = MemoEntity(id, "원본 $id", id * 100, id * 100)
            BundleWithMemos(BundleEntity(id, "묶음 $id", "미리보기 내용 $id", id * 200, id * 200), listOf(memo))
        }
        composeRule.setContent {
            SemoTheme {
                BundleGrid(
                    bundles,
                    compact = false,
                    onBundle = { clickedBundleId = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        val first = composeRule.onNodeWithTag("bundle-grid-1").getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithTag("bundle-grid-2").getUnclippedBoundsInRoot()
        val third = composeRule.onNodeWithTag("bundle-grid-3").getUnclippedBoundsInRoot()
        assertEquals(first.top, second.top)
        assertEquals(first.right - first.left, second.right - second.left)
        assertEquals(first.bottom - first.top, second.bottom - second.top)
        assertTrue(third.top > first.top)
        composeRule.onNodeWithTag("bundle-grid-1").performClick()
        assertEquals(1L, clickedBundleId)
    }

    @Test fun sameMinuteGroup_rendersTimeOnlyForLastMemo() {
        val base = (1_700_000_000_000L / 60_000L) * 60_000L
        val timeline = buildTimeline(
            listOf(
                MemoEntity(1, "1", base, base),
                MemoEntity(2, "2", base + 10_000, base + 10_000),
            ),
            emptyList(),
        ).filterIsInstance<com.semo.memo.data.TimelineItem.MemoItem>()

        composeRule.setContent {
            SemoTheme {
                Column {
                    timeline.forEach { item ->
                        MemoBubble(item.memo, false, {}, {}, showTime = item.showTime)
                    }
                }
            }
        }

        composeRule.onNodeWithTag("memo-time-1").assertDoesNotExist()
        composeRule.onNodeWithTag("memo-time-2").assertIsDisplayed()
    }
}
