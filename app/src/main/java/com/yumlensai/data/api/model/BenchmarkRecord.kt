package com.yumlensai.data.api.model

data class BenchmarkRecord(
    val fileName: String,
    val detectedObjects: List<String>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val initialBattery: String,
    val finalBattery: String,
    val elapsedTime: Long
)
