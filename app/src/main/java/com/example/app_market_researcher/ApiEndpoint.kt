package com.example.app_market_researcher

object ApiEndpoints {
    // World Bank API endpoints
    const val BASE_URL = "https://api.worldbank.org/"

    // GDP growth
    const val GDP_ENDPOINT = "v2/country/WLD/indicator/NY.GDP.MKTP.KD.ZG?format=json"


    // CO2 emissions (metric tons per capita)
    const val CO2_ENDPOINT = "v2/country/WLD/indicator/EN.GHG.CO2.AG.MT.CE.AR5?format=json"



    // Agricultural land (% of land area)
    const val AGRI_LAND_ENDPOINT = "v2/country/WLD/indicator/AG.LND.AGRI.ZS?format=json"


}