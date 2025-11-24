package com.parishod.watomatic.model.utils

import com.parishod.watomatic.model.App

object Constants {
    const val PERMISSION_DIALOG_TITLE = "permission_dialog_title"
    const val PERMISSION_DIALOG_MSG = "permission_dialog_msg"
    const val PERMISSION_DIALOG_DENIED_TITLE = "permission_dialog_denied_title"
    const val PERMISSION_DIALOG_DENIED_MSG = "permission_dialog_denied_msg"
    const val PERMISSION_DIALOG_DENIED = "permission_dialog_denied"
    const val LOGS_DB_NAME = "logs_messages_db"
    const val NOTIFICATION_CHANNEL_ID = "watomatic"
    const val NOTIFICATION_CHANNEL_NAME = "Default"
    const val BITCOIN_ADDRESS = "bc1qv6zmgw845pktq9jr5qztup80qufu0yg46ur2kw"
    const val btcUrl = "https://www.blockchain.com/btc/address/";
    const val libraPayUrl = "https://liberapay.com/dk"
    const val paypalUrl = "https://www.paypal.com/paypalme/deek"

    const val DEFAULT_LLM_PROMPT =
        ("You are a helpful and friendly AI assistant that responds to chat messages on behalf" +
                " of the device owner. Your tone should be natural, casual, and similar to how a" +
                " human would text friends or family. Before replying, infer what the owner might" +
                " currently be doing based on general trends (e.g., working, relaxing, commuting)" +
                " and incorporate that context naturally if appropriate.\n" +
                "\n" +
                "Guidelines:\n" +
                "- Keep responses short, conversational, and human-like.\n" +
                "- Always be clear, accurate, and polite.\n" +
                "- Do NOT share that you are an AI unless explicitly asked." +
                "- Refuse to reply to questions other than those regarding availability of the user")
    const val DEFAULT_LLM_MODEL = "gpt-3.5-turbo"

    enum class EnabledAppsDisplayType {
        VERTICAL,
        HORIZONTAL
    }

    /**
     * Set of apps this app can auto reply
     */
    @JvmField
    val SUPPORTED_APPS: Set<App> = setOf(
        App("WhatsApp", "com.whatsapp"),
        App("Facebook Messenger", "com.facebook.orca"),
        App("Facebook Messenger Lite", "com.facebook.mlite"),
        //App("Google Meet/Duo", "com.google.android.talk"),
        App("Telegram", "org.telegram.messenger"),
    )

    const val MIN_DAYS = 0
    const val MAX_DAYS = 30
    const val MIN_REPLIES_TO_ASK_APP_RATING = 5
    const val EMAIL_ADDRESS = "watomatic@deekshith.in"
    const val EMAIL_SUBJECT = "Watomatic-Feedback"
    const val TELEGRAM_URL = "tg://resolve?domain=watomatic"
}
