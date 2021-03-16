package webtrekk.android.sdk.events

import webtrekk.android.sdk.events.eventParams.BaseEvent
import webtrekk.android.sdk.events.eventParams.CampaignParameters
import webtrekk.android.sdk.events.eventParams.ECommerceParameters
import webtrekk.android.sdk.events.eventParams.PageParameters
import webtrekk.android.sdk.events.eventParams.SessionParameters
import webtrekk.android.sdk.events.eventParams.UserCategories

/**
 * Created by Aleksandar Marinkovic on 3/11/21.
 * Copyright (c) 2021 MAPP.
 */
data class PageViewEvent(val name: String) : BaseEvent {
    var pageParameters: PageParameters = PageParameters()
    var sessionParameters: SessionParameters = SessionParameters()
    var userCategories: UserCategories = UserCategories()
    var eCommerceParameters: ECommerceParameters = ECommerceParameters()
    var campaignParameters: CampaignParameters = CampaignParameters()
    val customParameters = mutableMapOf<String, String>()
    override fun toHasMap(): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        map.putAll(customParameters)
        map.putAll(pageParameters.toHasMap())
        map.putAll(sessionParameters.toHasMap())
        map.putAll(userCategories.toHasMap())
        map.putAll(eCommerceParameters.toHasMap())
        map.putAll(campaignParameters.toHasMap())
        return map
    }
}