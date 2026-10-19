"""Verify the exact native source inventory and pinned provenance after import/reopen."""
import argparse
import json
from pathlib import Path


def audit(report_root: Path) -> dict:
    source = json.loads((Path(__file__).parent / "normalized-clauses.json").read_text(encoding="utf-8"))
    expected = {row["clause_id"]: row for row in source["clauses"] + source["source_objects"]}
    rows = []
    for path in sorted(report_root.glob("fsae-source-inventory-[0-9]*.json")):
        rows.extend(json.loads(path.read_text(encoding="utf-8-sig")))
    actual = {row["identifier"]: row for row in rows}
    assert len(rows) == len(actual) == len(expected) == 3197, "Duplicate or missing native identifiers"
    assert set(actual) == set(expected), "Native/source identifier bijection failed"
    for identifier, row in actual.items():
        assert row["source_sha256"] == expected[identifier]["source_sha256"], identifier
        assert row["source_text_sha256"] == expected[identifier]["source_text_sha256"], identifier
        assert row["source_rule_id"] == expected[identifier]["source_rule_id"], identifier
        assert row["verification_status"] == "not_verified", "No physical verification evidence has been provided"
    return {"success": True, "native_source_records": len(rows), "exact_identifier_bijection": True,
            "all_document_and_text_hashes_match": True, "engineering_completeness": "in_progress",
            "compliance": "not_claimed"}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("report_root", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    report = audit(args.report_root)
    if args.output:
        args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report))
