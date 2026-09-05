package com.fitplan.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 把某个 [LocalDate] 转成自 1970-01-01 起的天数 */
fun LocalDate.toEpochDayLong(): Long = toEpochDay()

/** 把天数还原成 LocalDate */
fun Long.epochDayToLocalDate(): LocalDate = LocalDate.ofEpochDay(this)

/** 展示用：yyyy-MM-dd */
fun LocalDate.displayString(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

/** 展示用：yyyy年M月d日 */
fun LocalDate.displayChinese(): String =
    "${year}年${monthValue}月${dayOfMonth}日"

fun Long.timestampToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/** 简洁时间 yyyy-MM-dd */
fun Long.timestampToDateString(): String =
    timestampToLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
