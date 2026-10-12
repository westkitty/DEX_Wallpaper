"""Source invariants: unlock rotation stays opt-in and videos never become static writes."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2] / "app/src/main/java/com/westkitty/dexwallpaper"

class UnlockVideoGuard(unittest.TestCase):
    def src(self, path):
        return (ROOT/path).read_text(encoding="utf-8")

    def test_locked_wallpaper_not_implicitly_enabled(self):
        pref=self.src("data/Preferences.kt")
        self.assertIn("rotateOnUnlockHome: Boolean = false",pref)
        self.assertIn("rotateOnUnlockLock: Boolean = false",pref)
        self.assertIn('booleanPreferencesKey("rotateOnUnlockLock")',pref)

    def test_deliver_real_user_present_via_running_receiver(self):
        monitor=self.src("worker/UnlockRotationService.kt")
        self.assertIn("registerReceiver(receiver,filter",monitor)
        self.assertIn("Intent.ACTION_USER_PRESENT",monitor)
        self.assertIn("FOREGROUND_SERVICE_TYPE_SPECIAL_USE",monitor)
        self.assertIn("if (target == Destination.HOME)",monitor)
        self.assertIn("WallpaperManager.FLAG_LOCK",monitor)

    def test_video_is_bounded_and_does_not_enter_static_writer(self):
        video=self.src("core/VideoClips.kt")
        repo=self.src("data/WallRepository.kt")
        engine=self.src("wallpaper/DexWallpaperService.kt")
        self.assertIn("MAX_DURATION_MS = 15_000L",video)
        self.assertIn("MAX_BYTES = 30L * 1024L * 1024L",video)
        self.assertIn("it.excluded || VideoClips.isVideo(it)",repo)
        self.assertIn("Animated clips require the live wallpaper picker",repo)
        self.assertIn("player.setVolume(0f,0f)",engine)
        self.assertIn("player.isLooping=true",engine)
        self.assertIn("onVisibilityChanged(isVisible",engine)

if __name__ == "__main__":
    unittest.main()
