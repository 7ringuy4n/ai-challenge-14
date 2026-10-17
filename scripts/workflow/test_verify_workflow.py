import tempfile
import unittest
from pathlib import Path

from verify_workflow import ready_tasks, validate_manifest, validate_patch


def task(task_id, status="planned", dependencies=(), paths=()):
    return {
        "id": task_id,
        "title": task_id,
        "role": "worker",
        "model": "gpt-6-luna",
        "reasoning_effort": "medium",
        "status": status,
        "depends_on": list(dependencies),
        "allowed_paths": list(paths),
        "verification": ["true"],
        "case_ids": [],
        "acceptance": ["criterion"],
        "evidence": [],
        "notes": "",
    }


def manifest(tasks):
    return {"schema_version": 1, "shared_paths": ["pom.xml", "rules/"], "tasks": tasks}


def passing_evidence():
    source_hash = "a" * 64
    return [
        {"command": "python3 -m unittest", "exit_code": 0, "source_hash": source_hash,
         "artifact": "reports/test.log", "result": "PASS"},
        {"reviewer_agent": "/reviewer", "source_hash": source_hash,
         "artifact": "reports/review.md", "result": "PASS"},
    ]


class ValidateManifestTest(unittest.TestCase):
    def test_accepts_valid_dependency_graph(self):
        accepted = task("T00", "accepted")
        accepted["evidence"] = passing_evidence()
        self.assertEqual(validate_manifest(manifest([accepted, task("T01", dependencies=["T00"])])), [])

    def test_rejects_duplicate_task_ids(self):
        errors = validate_manifest(manifest([task("T00"), task("T00")]))
        self.assertTrue(any("duplicate task id" in error for error in errors))

    def test_rejects_missing_dependency(self):
        errors = validate_manifest(manifest([task("T00", dependencies=["T99"])]))
        self.assertTrue(any("unknown task T99" in error for error in errors))

    def test_rejects_dependency_cycle(self):
        errors = validate_manifest(manifest([task("T00", dependencies=["T01"]), task("T01", dependencies=["T00"])]))
        self.assertTrue(any("dependency cycle" in error for error in errors))

    def test_rejects_accepted_task_without_check_and_review_evidence(self):
        errors = validate_manifest(manifest([task("T00", "accepted")]))
        self.assertTrue(any("passing command" in error for error in errors))
        self.assertTrue(any("independent review" in error for error in errors))

    def test_ready_tasks_require_all_dependencies_accepted(self):
        data = manifest([task("T00", "accepted"), task("T01", dependencies=["T00"]), task("T02", dependencies=["T01"])])
        self.assertEqual(ready_tasks(data), ["T01"])

    def test_rejects_unsafe_manifest_paths(self):
        errors = validate_manifest(manifest([task("T00", paths=["../outside"])]))
        self.assertTrue(any("safe repository path" in error for error in errors))


class ValidatePatchTest(unittest.TestCase):
    def test_accepts_patch_inside_task_owned_directory(self):
        data = manifest([task("T01", paths=["src/test/java/example/"])])
        with tempfile.TemporaryDirectory() as directory:
            patch = Path(directory) / "candidate.patch"
            patch.write_text("diff --git a/src/test/java/example/OneTest.java b/src/test/java/example/OneTest.java\n--- /dev/null\n+++ b/src/test/java/example/OneTest.java\n@@ -0,0 +1 @@\n+test\n", encoding="utf-8")
            self.assertEqual(validate_patch("T01", patch, data), [])

    def test_rejects_patch_outside_task_owned_directory(self):
        data = manifest([task("T01", paths=["src/test/java/example/"])])
        with tempfile.TemporaryDirectory() as directory:
            patch = Path(directory) / "candidate.patch"
            patch.write_text("diff --git a/src/main/java/example/App.java b/src/main/java/example/App.java\n--- a/src/main/java/example/App.java\n+++ b/src/main/java/example/App.java\n@@ -1 +1 @@\n-a\n+b\n", encoding="utf-8")
            self.assertTrue(any("outside T01.allowed_paths" in error for error in validate_patch("T01", patch, data)))

    def test_rejects_worker_patch_to_shared_path(self):
        data = manifest([task("T01", paths=["pom.xml"])])
        with tempfile.TemporaryDirectory() as directory:
            patch = Path(directory) / "candidate.patch"
            patch.write_text("diff --git a/pom.xml b/pom.xml\n--- a/pom.xml\n+++ b/pom.xml\n@@ -1 +1 @@\n-a\n+b\n", encoding="utf-8")
            self.assertTrue(any("shared path" in error for error in validate_patch("T01", patch, data)))

    def test_rejects_traversal_in_patch_header(self):
        data = manifest([task("T01", paths=["src/"])])
        with tempfile.TemporaryDirectory() as directory:
            patch = Path(directory) / "candidate.patch"
            patch.write_text("diff --git a/../../etc/passwd b/../../etc/passwd\n", encoding="utf-8")
            self.assertTrue(any("unsafe path" in error for error in validate_patch("T01", patch, data)))

    def test_reviewer_task_cannot_submit_source_patch(self):
        reviewer = task("T07", paths=["src/"])
        reviewer["role"] = "reviewer"
        with tempfile.TemporaryDirectory() as directory:
            patch = Path(directory) / "candidate.patch"
            patch.write_text("diff --git a/src/App.java b/src/App.java\n", encoding="utf-8")
            self.assertTrue(any("cannot provide source patches" in error for error in validate_patch("T07", patch, manifest([reviewer]))))


if __name__ == "__main__":
    unittest.main()
