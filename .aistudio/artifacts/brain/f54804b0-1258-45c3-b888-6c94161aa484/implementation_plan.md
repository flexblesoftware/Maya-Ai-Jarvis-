# Maya - Voice AI Assistant & Phone-Control Agent

Maya is a native Android voice AI assistant and phone-control agent built with Kotlin, Jetpack Compose, Material 3, and Room. It connects to the Gemini Live Multimodal WebSocket API (`gemini-3.8-live`) for real-time low-latency bidirectional voice conversations in Hinglish and English, switches seamlessly between sharp action-oriented Jarvis Mode and warm conversational Girlfriend Mode, and operates as an on-device accessibility agent to automate complex tasks across Android apps safely.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences have been confirmed and are integrated into the architecture:

- **Confirmed Decision 1 (Wake-Word Listening)**: Pretrained "Hey Jarvis" wake-word model with acoustic energy tuning and graceful fallback to foreground notification talk button, quick settings tile, and floating mic button.
- **Confirmed Decision 2 (Screen Inspection & Vision)**: Accessibility tree parsing into a structured, compact node hierarchy, complemented by an on-demand 1 fps MediaProjection video stream for dynamic visual tasks (e.g., social feed analysis, gaming assistance).
- **Confirmed Decision 3 (Safety & Action Confirmation)**: Spoken voice confirmation accompanied by an interactive Compose Yes/No card before executing outgoing phone calls, SMS sends, or irreversible actions. Sensitive operations (banking, OTPs, password entry) are strictly blocked in code.

---

## 1. Overview & Core Concept

Maya acts as an intelligent companion and hands-free copilot on the user's smartphone.
- **Bi-directional Low-Latency Audio**: Users converse naturally with Maya without push-to-talk delays. Speech is streamed at 16 kHz PCM to Gemini Live, while 24 kHz response audio streams back in real time with instant barge-in interruption.
- **Dual Personality Personalization**:
  - **Jarvis Mode (Default)**: Cyan/blue neon visual theme, Charon voice, direct, witty, and deeply action-focused.
  - **Girlfriend Mode**: Soft rose/blush theme, Sulafat voice, caring, emotionally attentive, and warm with an "AI Companion" badge.
- **On-Device Phone Automation**: An Accessibility Service reads active screen nodes, executes gestures (tap, scroll, type, press back/home/recents), navigates app deep links (e.g., WhatsApp `wa.me`, Telegram, Instagram), and coordinates actions while adhering to user safety boundaries.
- **Local Private Memory & Logs**: Important personal facts, reminders, alarms, and action histories persist strictly on-device in Room without cloud telemetry.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Onboarding & Setup**:
   - Welcome splash with AMOLED neon aesthetic.
   - Secure Gemini API key entry (validated live via a lightweight ping, saved in `EncryptedSharedPreferences`).
   - One-stop Permissions Checklist: Microphone, Accessibility Service, Notifications, Overlay Bubble, and Battery Optimization.
2. **Main Live Voice Stage**:
   - Central interactive **Dynamic Orb** with organic shader/canvas pulsations representing states: `Idle`, `Listening`, `Thinking`, `Speaking`, and `Interrupted`.
   - Real-time live transcript drawer displaying bidirectional captions.
   - Quick Mode Pill (Jarvis ⇄ Girlfriend) with seamless reconnect and context handover.
   - Action confirmation cards that float above the orb when Maya proposes phone calls, SMS sends, or external automation.
3. **Voice & Settings Customization**:
   - 30 Gemini prebuilt voice cards with category tags and instant preview audio.
   - Wake-word sensitivity slider and background listening service controls.
   - Privacy vault: view and manage memories, inspect action audit logs, and trigger "Delete All Data".

### Visual Identity & Theme Tokens
- **Base Canvas**: Deep AMOLED True Black (`#000000` / `#0A0B0E`) for battery efficiency and high-contrast glow.
- **Jarvis Neon Palette**:
  - Primary Accent: Neon Cyan (`#00E5FF`)
  - Secondary Accent: Electric Cobalt (`#2979FF`)
  - Glow & Ring: Alpha Cyan (`#1A00E5FF`)
- **Girlfriend Rose Palette**:
  - Primary Accent: Soft Coral Pink (`#FF4081` / `#FF80AB`)
  - Secondary Accent: Warm Rose Gold (`#F48FB1`)
  - Glow & Ring: Ambient Rose Mist (`#1AFF4081`)
- **Typography & Motion**:
  - Display Font: High-clarity geometric monospace and sans headers.
  - Motion: Spring-based fluid animations (`spring(dampingRatio = 0.75f)`) with haptic feedback ticks on mode switches and speech events.

---

## 3. Key Product Decisions & Trade-Offs

- **WebSocket Architecture (`wss://generativelanguage.googleapis.com/...`)**:
  - *Chosen Approach*: Direct OkHttp WebSocket handling `BidiGenerateContent` with JSON envelope and Base64 PCM audio chunks.
  - *Rationale*: Eliminates roundtrip HTTP overhead; supports simultaneous input audio streaming, server output audio streaming, and immediate barge-in truncation when user speaks.
- **Audio Pipeline Separation**:
  - *Chosen Approach*: Single high-priority `AudioRecord` thread with AEC (Acoustic Echo Canceler) and NoiseSuppressor. When the session is idle, audio feeds the local wake-word detector; when activated, audio buffers stream directly into the WebSocket.
  - *Playback*: Streaming `AudioTrack` configured for 24 kHz mono PCM16. On receiving `"interrupted"` from the server or user speech detection, the audio buffer is instantly flushed (`audioTrack.flush()`).
- **Screen Reading & Automation Hierarchy**:
  - *Direct Deep Links First*: Prefer `Intent(ACTION_VIEW, "https://wa.me/...")` over navigating multi-step app menus.
  - *Accessibility Tree*: Nodes are parsed into a flat, sanitized list of actionable items (ID, visible text, content description, clickable bounds).
  - *Safety Boundary*: Banking apps, UPI gateways, PIN/password entry fields (`FLAG_SECURE` or `isPassword = true`) are strictly forbidden from interaction, with Maya explicitly explaining the refusal.
- **Local Persistence via Room**:
  - All memories, reminders, timers, and audit action logs are stored in a local SQLite database using Room with Flow observation for reactive UI updates.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                              MAYA APP                                  │
├────────────────────────────────────────────────────────────────────────┤
│  [UI Layer - Jetpack Compose & M3]                                     │
│  ├── MainVoiceScreen (Dynamic Orb, Live Transcript, Mode Toggle)       │
│  ├── VoicePickerScreen (30 Gemini Voices + Preview Engine)             │
│  ├── MemoryScreen & ActionLogScreen (Room Data Viewers)                │
│  └── Settings & PermissionsScreen (API Key, Wake Word, Safety Toggles) │
├────────────────────────────────────────────────────────────────────────┤
│  [Service & Background Layer]                                          │
│  ├── MayaForegroundService (Always-Listening Wake Word & Audio Pipe)   │
│  ├── MayaAccessibilityService (Screen Tree Parser & Gesture Dispatcher)│
│  ├── MayaNotificationListener (Incoming Message Relay & RemoteInput)   │
│  └── OverlayStopBubbleService (Floating Red Panic STOP Button)         │
├────────────────────────────────────────────────────────────────────────┤
│  [AI Core & Networking Engine]                                         │
│  ├── GeminiLiveWebSocketClient (BidiGenerateContent over OkHttp)       │
│  ├── AudioStreamManager (AudioRecord 16kHz + AudioTrack 24kHz)         │
│  └── FunctionCallDispatcher (Executes & Returns Tool Responses)        │
├────────────────────────────────────────────────────────────────────────┤
│  [Local Storage - Room Database]                                       │
│  ├── MemoryDao (User preferences, personal facts, auto-memory)         │
│  ├── ReminderDao (Exact alarm schedules & notifications)               │
│  └── ActionLogDao (Audit log of automated phone actions)               │
└────────────────────────────────────────────────────────────────────────┘
```

### Local Database Entities
1. **MemoryEntity**: `id`, `fact`, `category`, `createdAt`, `updatedAt`
2. **ReminderEntity**: `id`, `title`, `timeMillis`, `isCompleted`, `requestCode`
3. **ActionLogEntity**: `id`, `toolName`, `parametersJson`, `resultSummary`, `timestamp`, `isSuccess`

### Interactive Tools & Function Calling Registry
- `save_memory`, `recall_memory`, `forget_memory`
- `set_reminder`, `list_reminders`, `cancel_reminder`, `set_alarm`, `set_timer`
- `open_app`, `call_contact`, `send_sms`, `search_contacts`, `open_url`
- `media_control`, `set_volume`, `set_brightness`, `flashlight`, `switch_mode`
- `read_screen`, `tap_element`, `type_text`, `scroll_screen`, `press_key`

---

## 5. Verification & Compilation Plan

1. Configure dependencies in `app/build.gradle.kts` and `gradle/libs.versions.toml` (Room, KSP, OkHttp, Compose Material 3, Security Crypto).
2. Set up `AndroidManifest.xml` with all necessary permissions, foreground service declarations, and accessibility/notification service configs.
3. Build the core Room database entities, DAOs, and repository.
4. Implement the Gemini Live WebSocket client and audio streaming engine.
5. Implement the accessibility service and phone control automation agent.
6. Build the Jetpack Compose UI screens, dynamic visual orb, and theme switching.
7. Verify build compilation using `compile_applet`.
