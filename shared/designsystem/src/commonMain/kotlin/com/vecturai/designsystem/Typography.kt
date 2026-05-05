package com.VecturAI.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.VecturAI.designsystem.generated.resources.Res
import com.VecturAI.designsystem.generated.resources.inter_variable
import org.jetbrains.compose.resources.Font

private fun TextStyle.withFont(fontFamily: FontFamily): TextStyle = copy(fontFamily = fontFamily)

@Composable
private fun interFamily(): FontFamily = FontFamily(Font(Res.font.inter_variable))

object VecturAITypography {
    val NumericDisplay = TextStyle(
        fontSize = 64.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 64.sp,
        fontFeatureSettings = "tnum",
    )

    val NumericLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 32.sp,
        fontFeatureSettings = "tnum",
    )

    val Overline = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
    )

    @Composable
    fun material(): Typography {
        val inter = interFamily()
        return Typography(
            displayLarge = TextStyle(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
            ).withFont(inter),
            displayMedium = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
            ).withFont(inter),
            headlineLarge = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp,
            ).withFont(inter),
            headlineMedium = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 26.sp,
            ).withFont(inter),
            titleLarge = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp,
            ).withFont(inter),
            titleMedium = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
            ).withFont(inter),
            bodyLarge = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp,
            ).withFont(inter),
            bodyMedium = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
            ).withFont(inter),
            bodySmall = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
            ).withFont(inter),
            labelLarge = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
            ).withFont(inter),
            labelMedium = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp,
            ).withFont(inter),
            labelSmall = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp,
            ).withFont(inter),
        )
    }

    @Composable
    fun numericDisplay(): TextStyle = NumericDisplay.withFont(interFamily())

    @Composable
    fun numericLarge(): TextStyle = NumericLarge.withFont(interFamily())

    @Composable
    fun overline(): TextStyle = Overline.withFont(interFamily())
}
