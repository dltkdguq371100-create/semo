package com.semo.memo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.semo.memo.MainActivity
import com.semo.memo.QuickInputActivity
import com.semo.memo.SemoApplication
import com.semo.memo.data.MemoEntity

class SemoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SemoWidget()
}

class SemoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as SemoApplication).repository
        val recent = runCatching { repository.recentMemos(2) }.getOrDefault(emptyList())
        provideContent { SemoWidgetContent(recent) }
    }
}

@Composable
private fun SemoWidgetContent(memos: List<MemoEntity>) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(ColorProvider(Color(0xFFFFFFFF)))
            .cornerRadius(20.dp)
            .padding(14.dp),
    ) {
        Text(
            "세모",
            style = TextStyle(
                color = ColorProvider(Color(0xFF111317)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
        )
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth()
                .background(ColorProvider(Color(0xFFEAECEF)))
                .cornerRadius(22.dp)
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .clickable(actionStartActivity<QuickInputActivity>()),
        ) {
            Text(
                "바로 메모 입력…",
                style = TextStyle(color = ColorProvider(Color(0xFF6F747C)), fontSize = 14.sp),
            )
        }
        memos.forEach { memo ->
            Spacer(GlanceModifier.height(7.dp))
            Text(
                memo.content.lineSequence().firstOrNull()?.trim().orEmpty(),
                maxLines = 1,
                style = TextStyle(color = ColorProvider(Color(0xFF4A4F57)), fontSize = 13.sp),
                modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
            )
        }
    }
}
