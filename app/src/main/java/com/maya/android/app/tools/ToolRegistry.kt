package com.maya.android.app.tools

import android.content.Context
import com.maya.android.app.MayaApplication
import com.maya.android.app.data.model.ActionLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ToolRegistry(
    private val context: Context,
    val phoneExecutor: PhoneToolExecutor,
    val screenExecutor: ScreenAgentExecutor,
    val memoryExecutor: MemoryToolExecutor,
    val reminderExecutor: ReminderToolExecutor
) {

    /**
     * Builds Gemini Live function declarations JSON array for the setup handshake.
     */
    fun buildFunctionDeclarationsJson(): JSONArray {
        val tools = JSONArray()

        fun addTool(name: String, description: String, properties: Map<String, Pair<String, String>>, required: List<String>) {
            val toolObj = JSONObject()
            toolObj.put("name", name)
            toolObj.put("description", description)

            val params = JSONObject()
            params.put("type", "OBJECT")
            val propsObj = JSONObject()
            for ((propName, propDef) in properties) {
                val p = JSONObject()
                p.put("type", propDef.first)
                p.put("description", propDef.second)
                propsObj.put(propName, p)
            }
            params.put("properties", propsObj)
            if (required.isNotEmpty()) {
                val reqArr = JSONArray()
                required.forEach { reqArr.put(it) }
                params.put("required", reqArr)
            }
            toolObj.put("parameters", params)
            tools.put(toolObj)
        }

        // Memory tools
        addTool("save_memory", "Save a personal fact, habit, or preference to local memory",
            mapOf("fact" to Pair("STRING", "The fact or preference to remember"), "category" to Pair("STRING", "Category: personal, work, preference, etc.")), listOf("fact"))

        addTool("recall_memory", "Recall memories matching a query",
            mapOf("query" to Pair("STRING", "Keywords to search for in memory")), listOf("query"))

        addTool("forget_memory", "Forget a memory by ID or search term",
            mapOf("query" to Pair("STRING", "Memory ID or search phrase")), listOf("query"))

        // Reminder / Alarm tools
        addTool("set_reminder", "Schedule an exact reminder with notification",
            mapOf("title" to Pair("STRING", "Reminder content"), "delay_minutes" to Pair("INTEGER", "Minutes from now")), listOf("title"))

        addTool("list_reminders", "List all upcoming scheduled reminders", emptyMap(), emptyList())

        addTool("cancel_reminder", "Cancel a scheduled reminder",
            mapOf("query" to Pair("STRING", "Reminder title or ID")), listOf("query"))

        addTool("set_alarm", "Set a device alarm clock",
            mapOf("hour" to Pair("INTEGER", "Hour in 24hr format 0-23"), "minutes" to Pair("INTEGER", "Minute 0-59"), "message" to Pair("STRING", "Alarm label")), listOf("hour", "minutes"))

        addTool("set_timer", "Start a countdown timer",
            mapOf("seconds" to Pair("INTEGER", "Timer duration in seconds"), "message" to Pair("STRING", "Timer label")), listOf("seconds"))

        // Phone control tools
        addTool("open_app", "Launch an installed Android app",
            mapOf("app_name" to Pair("STRING", "Name of the app")), listOf("app_name"))

        addTool("call_contact", "Initiate a phone call to a contact or phone number",
            mapOf("contact_or_number" to Pair("STRING", "Contact name or phone digits")), listOf("contact_or_number"))

        addTool("send_sms", "Send an SMS text message",
            mapOf("contact_or_number" to Pair("STRING", "Contact name or phone number"), "message" to Pair("STRING", "SMS body text")), listOf("contact_or_number", "message"))

        addTool("open_url", "Open a website or deep link URL",
            mapOf("url" to Pair("STRING", "Web URL")), listOf("url"))

        addTool("search_contacts", "Search address book for phone contacts",
            mapOf("query" to Pair("STRING", "Name to search")), listOf("query"))

        addTool("media_control", "Control music/media playback: play, pause, next, previous, stop",
            mapOf("action" to Pair("STRING", "Action name")), listOf("action"))

        addTool("set_volume", "Set device media volume percentage 0-100",
            mapOf("percent" to Pair("INTEGER", "Volume percentage 0 to 100")), listOf("percent"))

        addTool("flashlight", "Turn device flashlight torch on or off",
            mapOf("turn_on" to Pair("BOOLEAN", "True to turn on, false to turn off")), listOf("turn_on"))

        addTool("open_settings_panel", "Open a system settings page: wifi, bluetooth, display, sound, accessibility",
            mapOf("panel" to Pair("STRING", "Settings panel name")), listOf("panel"))

        addTool("get_clipboard", "Read text currently on the clipboard", emptyMap(), emptyList())
        addTool("set_clipboard", "Copy text to clipboard",
            mapOf("text" to Pair("STRING", "Text to copy")), listOf("text"))

        addTool("switch_mode", "Switch Maya between Jarvis Mode and Girlfriend Mode",
            mapOf("mode" to Pair("STRING", "'jarvis' or 'girlfriend'")), listOf("mode"))

        // Screen & automation tools
        addTool("read_screen", "Inspect the current screen to read visible text and buttons", emptyMap(), emptyList())

        addTool("tap", "Tap on a screen element by coordinates or matching text",
            mapOf("x" to Pair("INTEGER", "X coordinate"), "y" to Pair("INTEGER", "Y coordinate"), "element_text" to Pair("STRING", "Text on the element")), emptyList())

        addTool("type_text", "Type text into the currently focused or specified text field",
            mapOf("text" to Pair("STRING", "Text to type"), "element_id" to Pair("STRING", "Optional element ID")), listOf("text"))

        addTool("scroll", "Scroll the screen: down, up, forward, backward",
            mapOf("direction" to Pair("STRING", "Scroll direction")), listOf("direction"))

        addTool("press_key", "Press device navigation key: back, home, recents",
            mapOf("key" to Pair("STRING", "Key name")), listOf("key"))

        addTool("send_whatsapp", "Send a message using WhatsApp",
            mapOf("contact_or_number" to Pair("STRING", "Recipient name or number"), "message" to Pair("STRING", "Message text")), listOf("contact_or_number", "message"))

        return tools
    }

    suspend fun executeTool(name: String, args: JSONObject): String = withContext(Dispatchers.Default) {
        val result = try {
            when (name) {
                // Memory
                "save_memory" -> memoryExecutor.saveMemory(args.optString("fact"), args.optString("category", "general"))
                "recall_memory" -> memoryExecutor.recallMemory(args.optString("query"))
                "forget_memory" -> memoryExecutor.forgetMemory(args.optString("query"))

                // Reminders
                "set_reminder" -> reminderExecutor.setReminder(args.optString("title"), args.optInt("delay_minutes", 0))
                "list_reminders" -> reminderExecutor.listReminders()
                "cancel_reminder" -> reminderExecutor.cancelReminder(args.optString("query"))
                "set_alarm" -> reminderExecutor.setSystemAlarm(args.optInt("hour", 8), args.optInt("minutes", 0), args.optString("message", "Maya Alarm"))
                "set_timer" -> reminderExecutor.setSystemTimer(args.optInt("seconds", 60), args.optString("message", "Maya Timer"))

                // Phone
                "open_app" -> phoneExecutor.openApp(args.optString("app_name"))
                "call_contact" -> phoneExecutor.callContact(args.optString("contact_or_number"))
                "send_sms" -> phoneExecutor.sendSms(args.optString("contact_or_number"), args.optString("message"))
                "open_url" -> phoneExecutor.openUrl(args.optString("url"))
                "search_contacts" -> {
                    val list = phoneExecutor.searchContacts(args.optString("query"))
                    if (list.isEmpty()) "No contacts found." else list.joinToString("\n") { "${it.first}: ${it.second}" }
                }
                "media_control" -> phoneExecutor.mediaControl(args.optString("action"))
                "set_volume" -> phoneExecutor.setVolume(args.optInt("percent", 50))
                "flashlight" -> phoneExecutor.setFlashlight(args.optBoolean("turn_on", true))
                "open_settings_panel" -> phoneExecutor.openSettingsPanel(args.optString("panel", "settings"))
                "get_clipboard" -> phoneExecutor.getClipboard()
                "set_clipboard" -> phoneExecutor.setClipboard(args.optString("text"))
                "switch_mode" -> {
                    val mode = args.optString("mode").lowercase()
                    if (mode.contains("girl")) {
                        MayaApplication.instance.preferencesManager.activeMode = "girlfriend"
                        "Switched to Girlfriend Mode."
                    } else {
                        MayaApplication.instance.preferencesManager.activeMode = "jarvis"
                        "Switched to Jarvis Mode."
                    }
                }

                // Screen Agent
                "read_screen" -> screenExecutor.readScreen()
                "tap" -> screenExecutor.tapElement(args.optInt("x", 0), args.optInt("y", 0), args.optString("element_text"))
                "type_text" -> screenExecutor.typeText(args.optString("text"), args.optString("element_id"))
                "scroll" -> screenExecutor.scroll(args.optString("direction", "down"))
                "press_key" -> screenExecutor.pressKey(args.optString("key", "back"))
                "send_whatsapp" -> screenExecutor.sendWhatsAppMessage(args.optString("contact_or_number"), args.optString("message"))

                else -> "Unknown tool command: $name"
            }
        } catch (e: Exception) {
            "Tool execution error: ${e.message}"
        }

        // Record in Action Log (Room)
        try {
            val app = context.applicationContext as? MayaApplication
            app?.database?.actionLogDao()?.insertLog(
                ActionLogEntity(
                    toolName = name,
                    parametersJson = args.toString(),
                    resultSummary = result.take(150),
                    isSuccess = !result.contains("error", ignoreCase = true) && !result.contains("failed", ignoreCase = true)
                )
            )
        } catch (ignored: Exception) {}

        result
    }
}
