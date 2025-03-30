package com.example.app_market_researcher

data class MetadataInfo(
    val page: Int,
    val pages: Int,
    val per_page: Int,
    val total: Int,
    val sourceid: String,
    val lastupdated: String
)

data class Indicator(
    val id: String,
    val value: String
)

data class Country(
    val id: String,
    val value: String
)

data class GDPDataPoint(
    val indicator: Indicator,
    val country: Country,
    val countryiso3code: String,
    val date: String,
    val value: Double,
    val unit: String,
    val obs_status: String,
    val decimal: Int
)