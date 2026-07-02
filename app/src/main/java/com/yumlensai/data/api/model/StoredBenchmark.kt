package com.yumlensai.data.api.model

data class StoredBenchmark(
    val id: String,
    val environment: String,
    val records: List<BenchmarkRecord>,
    val synced: Boolean = false
)
