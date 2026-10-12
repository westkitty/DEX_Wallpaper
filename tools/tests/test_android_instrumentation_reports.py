"""Regression checks for the instrumentation harness; no emulator required."""
import tempfile
import unittest
from pathlib import Path
from tools.run_android_instrumentation import collect_results


class InstrumentationReportTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def report(self, name, xml):
        file = self.root / name
        file.write_text(xml)
        return file

    def test_real_test_results_are_counted(self):
        self.report('TEST-ok.xml', '<testsuite tests="3" failures="1" errors="0"/>')
        self.assertEqual((3, 1, []), collect_results(self.root))

    def test_stale_report_cannot_fake_a_pass(self):
        file = self.report('TEST-old.xml', '<testsuite tests="12" failures="0" errors="0"/>')
        before = {file.resolve(): file.stat().st_mtime_ns}
        self.assertEqual((0, 0, []), collect_results(self.root, before))

    def test_malformed_report_is_visible_failure_evidence(self):
        self.report('TEST-broken.xml', '<testsuite tests="2">')
        count, failed, problems = collect_results(self.root)
        self.assertEqual((0, 0), (count, failed))
        self.assertEqual(1, len(problems))

    def test_nested_suite_and_testcases(self):
        self.report('TEST-nested.xml', '<testsuites><testsuite><testcase name="one"/><testcase name="two"><failure/></testcase></testsuite></testsuites>')
        self.assertEqual((2, 1, []), collect_results(self.root))

    def test_missing_directory_cannot_claim_tests(self):
        self.assertEqual((0, 0, []), collect_results(self.root / 'missing'))

    def test_bogus_negative_totals_are_rejected(self):
        self.report('TEST-invalid.xml', '<testsuite tests="-1" failures="0" errors="0"/>')
        _, _, problems = collect_results(self.root)
        self.assertEqual(1, len(problems))


if __name__ == '__main__':
    unittest.main()
