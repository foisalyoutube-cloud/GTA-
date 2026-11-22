package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.SetupToolProfile
import com.example.data.model.TargetOsPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class GuideSectionItem(
    val number: String,
    val title: String,
    val summary: String,
    val details: List<String>
)

data class TroubleshootItem(
    val stage: String,
    val fix: String
)

object SetupGuideRepository {

    const val PDF_ASSET_NAME = "PeakCut-Complete-Setup-Guide_d34c3df8.pdf"
    const val NEON_PROMPT_ASSET_NAME = "NEONCLIPPING-AGENT-SETUP-PROMPT_f670b80e.txt"

    fun loadNeonClippingPrompt(context: Context): String {
        return runCatching {
            context.assets.open(NEON_PROMPT_ASSET_NAME).bufferedReader().use { it.readText() }
        }.getOrElse {
            "NeonClipping prompt asset unavailable."
        }
    }

    suspend fun renderPdfPageBitmap(
        context: Context,
        pageIndex: Int
    ): Result<Pair<Bitmap, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val cachedPdf = File(context.cacheDir, PDF_ASSET_NAME)
            if (!cachedPdf.exists()) {
                context.assets.open(PDF_ASSET_NAME).use { input ->
                    FileOutputStream(cachedPdf).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val fd = ParcelFileDescriptor.open(cachedPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            fd.use { parcelFd ->
                PdfRenderer(parcelFd).use { renderer ->
                    val safeIndex = pageIndex.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
                    renderer.openPage(safeIndex).use { page ->
                        val scale = 2
                        val bmp = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp to renderer.pageCount
                    }
                }
            }
        }
    }

    val peakCutFullAgentPrompt: String = """
Install, configure and test PeakCut on this computer now. Execute the work using your terminal and browser tools. Continue through recoverable errors until a real clip exports.

INPUTS
Source URL or local video path: [paste an allowed creator URL or full file path, or leave blank]
Install folder: C:\peakcut on Windows; ~/peakcut on macOS/Linux.
Use local AI by default. I authorize the normal dependency installs and a suitable local model download. Keep publishing manual.

A. CHECK AND INSTALL
Read https://github.com/haziqahmedqasmi/PeakCut and its current README, AGENTS.md and requirements.txt. Inspect this computer's OS, architecture, Python, Git, FFmpeg/ffprobe, RAM, GPU, free VRAM and disk space. Reuse working installs. Choose a supported Python interpreter compatible with the pinned dependencies, preferably Python 3.11 or newer. Install missing prerequisites from official sources or the system package manager.

I have selected the install folder above. If a compatible PeakCut installation exists there, preserve its settings, database, models and clips. If the folder belongs to another project, use a new short permanent sibling. Clone the official repository into that location. Create or reuse a project virtual environment and install requirements.txt with that environment's interpreter. Fetch PeakCut's speech model using the current documented command. Keep the app bound to localhost.

B. CONFIGURE LOCAL AI
Install or reuse Ollama. Inspect its installed models and available machine resources. Prefer a suitable existing model. If needed and the machine can accommodate it, download llama3.2:3b; on smaller machines choose an appropriate lighter model and explain the trade-off. Configure PeakCut to use the local Ollama service, its actual model ID and its actual address. Use the supported settings UI or installed-version API. Run Save and test, or its documented equivalent, and verify the successful reply.

A no-key loudness test is only an installation check. If local AI cannot run, finish that check, report the specific reason, and identify the next action. If I explicitly choose a hosted provider instead, guide me to enter its key in the local app settings. Keep credentials out of chat and source files.

C. START AND TEST
Start python -m studio.server with the project interpreter. Verify the real dashboard and /healthz. Reuse a healthy existing instance. If another app owns the port, use the documented port setting and report the actual URL. Leave the app running.

Process the supplied creator URL or MP4 through PeakCut's supported UI or the installed version's upload/job API. For a first test, request 3 clips, 15-45 seconds each, 720p, with captions. Watch the actual job through transcription, moment selection and rendering. If no source was supplied, ask for one after completing installation and AI checks. You may create a clearly labelled synthetic test fixture to check rendering; identify it as test-only.

For a blocked or private URL, request the creator's original MP4 and test the upload route. Diagnose download, dependency, model, port and rendering failures from their actual logs. Fix and retry the failed stage. Avoid repeating a failed request indefinitely.

D. VERIFY AND HAND OVER
Check at least one final MP4 with ffprobe and a full decode. Confirm duration, dimensions, an audio track where the source has audio, and playable video. Inspect representative frames and listen to the relevant source/export audio if tools permit. State anything you could not verify. Confirm that an AI selection test actually used the configured model.

Create a one-click restart launcher appropriate to this OS and a short next-video checklist in the install folder. Do not overwrite existing launchers without preserving their useful behavior. Keep source files intact and publishing manual. Report the install path, model, working dashboard URL, output location, test source, verification result and any unresolved blocker in plain English. Stop only for a required system permission, sign-in or missing essential input.
    """.trimIndent()

    fun getTerminalCommands(
        profile: SetupToolProfile,
        os: TargetOsPlatform,
        customRepoUrl: String
    ): String {
        return when (profile) {
            SetupToolProfile.PEAKCUT -> when (os) {
                TargetOsPlatform.WINDOWS -> """
winget install --id Git.Git -e
winget install --id Python.Python.3.11 -e
winget install --id Gyan.FFmpeg -e

git clone https://github.com/haziqahmedqasmi/PeakCut C:\peakcut
cd C:\peakcut
py -3.11 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m studio.fetch_model
.\.venv\Scripts\python.exe -m studio.server

# In another terminal (Local AI Brain):
ollama pull llama3.2:3b
# Dashboard: http://127.0.0.1:8765  |  Ollama: http://127.0.0.1:11434/v1
                """.trimIndent()
                TargetOsPlatform.MAC_LINUX -> """
# macOS: brew install git python ffmpeg
# Debian/Ubuntu: sudo apt install git python3 python3-venv python3-pip ffmpeg

git clone https://github.com/haziqahmedqasmi/PeakCut ~/peakcut
cd ~/peakcut
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m studio.fetch_model
.venv/bin/python -m studio.server

# Local AI Brain: ollama pull llama3.2:3b
# Dashboard: http://127.0.0.1:8765  |  Ollama: http://127.0.0.1:11434/v1
                """.trimIndent()
            }
            SetupToolProfile.NEONCLIPPING -> when (os) {
                TargetOsPlatform.WINDOWS -> """
winget install Python.Python.3.12 Gyan.FFmpeg Git.Git
git clone https://github.com/iamajmeena/neonclipping
cd neonclipping
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
# Optional Local AI: winget install Ollama.Ollama ; ollama pull llama3.2:3b
powershell -ExecutionPolicy Bypass -File run.ps1
# Chrome URL: http://127.0.0.1:8770  |  Health check: /healthz
                """.trimIndent()
                TargetOsPlatform.MAC_LINUX -> """
# macOS: brew install python ffmpeg git
# Linux: sudo apt install python3 python3-venv python3-pip ffmpeg git
git clone https://github.com/iamajmeena/neonclipping
cd neonclipping
python3 -m venv .venv
.venv/bin/pip install --upgrade pip
.venv/bin/pip install -r requirements.txt
# Optional Local AI: ollama pull llama3.2:3b
./run.sh
# Chrome URL: http://127.0.0.1:8770  |  Health check: /healthz
                """.trimIndent()
            }
            SetupToolProfile.CUSTOM -> {
                val cleanUrl = customRepoUrl.trim()
                    .ifBlank { "https://github.com/haziqahmedqasmi/PeakCut.git" }
                val folder = cleanUrl.removeSuffix(".git").substringAfterLast("/").ifBlank { "clip-studio" }
                if (os == TargetOsPlatform.WINDOWS) {
                    """
git clone $cleanUrl C:\$folder
cd C:\$folder
py -3.11 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m studio.server
                    """.trimIndent()
                } else {
                    """
git clone $cleanUrl ~/$folder
cd ~/$folder
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m studio.server
                    """.trimIndent()
                }
            }
        }
    }

    val peakCutFieldGuideSections: List<GuideSectionItem> = listOf(
        GuideSectionItem(
            number = "01 / THE ROUTE",
            title = "Your First Finished Clip (5-Stage Route)",
            summary = "Install once -> Check AI -> Import & Settings -> Review & Download -> Repeat.",
            details = listOf(
                "1. Install once: Run the 3-part agent prompt (installs PeakCut, FFmpeg, speech model, Ollama AI).",
                "2. Check AI: Open Settings > AI, choose Ollama + llama3.2:3b, click Save and test -> confirm green Ready.",
                "3. Import + choose settings: Paste creator link or upload MP4. Start with 3 clips, 15-45s, 720p.",
                "4. Review + download: Watch the clip, inspect captions and framing, then download MP4.",
                "5. Repeat: Reuse restart launcher, check saved files, and submit to eligible campaign briefs."
            )
        ),
        GuideSectionItem(
            number = "02 / WORKSPACE",
            title = "Give the Coding Agent a Permanent Folder",
            summary = "Windows: C:\\peakcut  •  macOS/Linux: ~/peakcut",
            details = listOf(
                "Use a terminal-enabled coding agent (Claude Code, Codex, etc.) on the processing machine.",
                "Avoid temporary agent folders or deeply nested Windows paths.",
                "Phones can receive & publish finished MP4s, while localhost runs on the host desktop."
            )
        ),
        GuideSectionItem(
            number = "04 / LOCAL BRAIN",
            title = "Connect Ollama Local AI (No API Key)",
            summary = "Service: Ollama  •  Address: http://127.0.0.1:11434/v1  •  Model: llama3.2:3b",
            details = listOf(
                "1. Install Ollama from ollama.com and run: ollama pull llama3.2:3b.",
                "2. Address: http://127.0.0.1:11434/v1 (without a key, PeakCut falls back to loudness mode).",
                "3. Model: llama3.2:3b (click Find models if available).",
                "4. Click Save and test — verify the green Ready status badge."
            )
        ),
        GuideSectionItem(
            number = "05-06 / IMPORT & SETTINGS",
            title = "Paste a Link or Upload MP4 + Shape Result",
            summary = "Open http://127.0.0.1:8765 — First test: 3 clips, 15-45s, 720p.",
            details = listOf(
                "Paste YouTube, Twitch, or Kick URL — or click Upload video for private/blocked links.",
                "Captions: Enable Generate captions and pick Bounce, Hype, or Clean.",
                "Layouts: Blurred fill, Zoomed in, Fill screen, or Gameplay + facecam.",
                "Tip: Use Clip Studio in this app to test the exact same 3-clip, 15-45s, 720p & Facecam+Gameplay framing!"
            )
        ),
        GuideSectionItem(
            number = "07-08 / EXPORT & DAILY ROUTINE",
            title = "Watch Before You Post & Next-Video Flow",
            summary = "Always review hook, captions, face/HUD crop, and audio before downloading.",
            details = listOf(
                "1. Confirm processing reached 100% and exported MP4 exists.",
                "2. Watch whole clip: verify hook, readable captions, and uncropped face/HUD.",
                "3. Download MP4 (stored in data/ folder inside install directory).",
                "4. Never delete the data/ directory to fix a failed job — it holds settings, models, and clips."
            )
        ),
        GuideSectionItem(
            number = "12 / CAMPAIGN BRIEF",
            title = "Submit Against a Real Brief (Vyro Campaigns)",
            summary = "Check active rate, budget, eligible creator/source, and posting rules before submitting.",
            details = listOf(
                "1. Choose a current campaign in Vyro campaigns and check eligible creator/source rules.",
                "2. Make the requested clip using approved creator material and required branding/edits.",
                "3. Post manually & submit the exact post link through the campaign workflow.",
                "4. Keep the campaign record and original exported MP4 for approval & payout tracking."
            )
        )
    )

    val neonClippingGuideSections: List<GuideSectionItem> = listOf(
        GuideSectionItem(
            number = "STEP 0-3",
            title = "Machine Check, Clone & Private .venv Setup",
            summary = "Python 3.10+, FFmpeg/ffprobe, Git, 10GB free disk, optional NVIDIA GPU.",
            details = listOf(
                "Clone https://github.com/iamajmeena/neonclipping and create .venv inside the folder.",
                "Install requirements.txt and verify: fastapi, uvicorn, yt_dlp, numpy, cv2, faster_whisper, PIL.",
                "Runs locally on http://127.0.0.1:8770 (or NEONCLIPPING_PORT=8771 if busy)."
            )
        ),
        GuideSectionItem(
            number = "STEP 4-6",
            title = "Ollama (llama3.2:3b) & Whisper Speech Models",
            summary = "Ollama (~2 GB) for AI hooks/titles; Whisper large-v3-turbo (~1.6 GB) or Fast captions.",
            details = listOf(
                "Ask before downloads > 500 MB: Ollama llama3.2:3b (~2 GB) and Whisper large-v3-turbo (~1.6 GB).",
                "Without Ollama, NeonClipping still works using sound & motion loudness detection.",
                "Caption Accuracy: Accurate (default), Fast (skips 1.6 GB Whisper download), Best Hindi (large-v3, ~3 GB)."
            )
        ),
        GuideSectionItem(
            number = "STEP 7-8",
            title = "Start App & 90-Second Quick Test",
            summary = "Windows: run.ps1  •  Mac/Linux: ./run.sh  •  Test video: QdBZY2fkU-0",
            details = listOf(
                "Verify http://127.0.0.1:8770/healthz returns {\"ok\":true,...}, then open in Chrome.",
                "Quick Test: POST /make with https://www.youtube.com/watch?v=QdBZY2fkU-0 (count: 2, 15-30s, blur, fast).",
                "Confirm 2 clips render at 1080x1920 in the data/ folder."
            )
        ),
        GuideSectionItem(
            number = "STEP 9 / HINGLISH",
            title = "4-Step Clip Workflow & Honest Limits",
            summary = "Audio pehle aata hai -> josheele moments -> 9:16 video + captions -> Download MP4.",
            details = listOf(
                "1. Link paste karo (YouTube/Twitch/Kick) ya MP4 upload karo.",
                "2. Facecam position chuno (top-left/right, bottom-left/right/centre) ya Facecam/Gameplay box draw karo.",
                "3. Caption styles: GTA Neon, Hormozi, MrBeast, Impact, Clean (Auto language Hindi -> Hinglish captions).",
                "4. Honest Limits: 10+ min YouTube videos are fastest; Twitch/MP4 take longer for transcription; always preview before posting!"
            )
        )
    )

    val troubleshootStages: List<TroubleshootItem> = listOf(
        TroubleshootItem(
            stage = "Dashboard will not open",
            fix = "Start python -m studio.server from the project .venv, check /healthz, or switch port if 8765 / 8770 is occupied."
        ),
        TroubleshootItem(
            stage = "Speech model download fails",
            fix = "Verify internet & free disk space, keep install path short (C:\\peakcut or ~/peakcut), and rerun studio.fetch_model."
        ),
        TroubleshootItem(
            stage = "Ollama test fails",
            fix = "Start Ollama, run 'ollama list' to confirm llama3.2:3b and http://127.0.0.1:11434/v1, then click Save and test."
        ),
        TroubleshootItem(
            stage = "URL remains importing / blocked",
            fix = "Private or rate-limited links will not resolve by retrying — use the creator's original MP4 via Upload Video."
        ),
        TroubleshootItem(
            stage = "FFmpeg missing or render fails",
            fix = "Run 'ffmpeg -version' in a fresh terminal after installing FFmpeg, check disk space, and test with a short clip."
        ),
        TroubleshootItem(
            stage = "Weak clips or wrong captions",
            fix = "Confirm AI Ready (not loudness-only fallback), use clearer audio or Best Hindi accuracy, and review before posting."
        )
    )
}
