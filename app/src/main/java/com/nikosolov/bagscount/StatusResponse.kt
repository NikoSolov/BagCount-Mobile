package com.nikosolov.bagscount

data class StatusResponse(
    val status: String          = "N/A",
    val handbags: String        = "0",
    val suitcases: String       = "0",
    val backpacks: String       = "0"
)