/*
 *  MIT License
 *
 *  Copyright (c) 2019 Webtrekk GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */

package webtrekk.android.sdk.utils

import webtrekk.android.sdk.WebtrekkImpl
import webtrekk.android.sdk.data.util.currentTimeStamp
import kotlin.random.Random

internal val currentEverId: String
    inline get() = WebtrekkImpl.getInstance().sessions.getEverId()

internal val currentSession: String
    @Synchronized inline get() = WebtrekkImpl.getInstance().sessions.getCurrentSession()

internal val appFirstStart: String
    @Synchronized inline get() = WebtrekkImpl.getInstance().sessions.getAppFirstStart()

internal val trackDomain: String
    inline get() = WebtrekkImpl.getInstance().config.trackDomain

internal val trackIds: List<String>
    inline get() = WebtrekkImpl.getInstance().config.trackIds

internal fun generateEverId(): String {
    val date = currentTimeStamp / 1000
    val random = Random

    return "6${String.format("%010d%08d", date, random.nextLong(100000000))}"
}