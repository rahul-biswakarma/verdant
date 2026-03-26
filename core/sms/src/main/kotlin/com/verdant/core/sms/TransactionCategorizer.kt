package com.verdant.core.sms

import com.verdant.core.model.SpendingCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionCategorizer @Inject constructor() {

    fun categorize(merchant: String?): String {
        if (merchant == null) return SpendingCategory.OTHER.name
        val lower = merchant.lowercase()
        return KEYWORD_RULES.entries
            .firstOrNull { (_, keywords) -> keywords.any { lower.contains(it) } }
            ?.key?.name
            ?: SpendingCategory.OTHER.name
    }

    companion object {
        private val KEYWORD_RULES: Map<SpendingCategory, List<String>> = mapOf(
            SpendingCategory.FOOD to listOf(
                "swiggy", "zomato", "dominos", "pizza", "mcdonalds", "kfc",
                "starbucks", "cafe", "restaurant", "food", "biryani", "burger",
                "dunzo", "blinkit", "zepto", "bigbasket", "grofers", "instamart",
            ),
            SpendingCategory.TRANSPORT to listOf(
                "uber", "ola", "rapido", "metro", "irctc", "railway",
                "redbus", "petrol", "diesel", "fuel", "bp", "iocl", "hpcl",
                "parking", "fastag", "toll",
            ),
            SpendingCategory.SHOPPING to listOf(
                "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa",
                "snapdeal", "shoppers", "reliance", "tata", "croma", "dmart",
            ),
            SpendingCategory.BILLS to listOf(
                "electricity", "water", "gas", "broadband", "jio", "airtel",
                "vodafone", "bsnl", "insurance", "lic", "postpaid", "prepaid",
                "recharge", "dth", "tata sky", "rent",
            ),
            SpendingCategory.ENTERTAINMENT to listOf(
                "netflix", "prime", "hotstar", "spotify", "youtube",
                "bookmyshow", "pvr", "inox", "cinema", "game",
            ),
            SpendingCategory.HEALTH to listOf(
                "pharmacy", "apollo", "medplus", "1mg", "netmeds", "pharmeasy",
                "hospital", "clinic", "doctor", "diagnostic", "lab", "gym",
                "cult.fit", "healthify",
            ),
            SpendingCategory.EDUCATION to listOf(
                "school", "college", "university", "udemy", "coursera",
                "unacademy", "byju", "edtech",
            ),
            SpendingCategory.TRANSFERS to listOf(
                "neft", "imps", "rtgs", "transfer",
            ),
            SpendingCategory.INVESTMENTS to listOf(
                "zerodha", "groww", "upstox", "mutual fund", "sip",
                "smallcase", "kuvera", "coin",
            ),
            SpendingCategory.CASH to listOf(
                "atm", "cash withdrawal",
            ),
        )
    }
}
