"""Inventory the pinned official PDF without redistributing its complete text.

Raw paragraph text is written ONLY to the explicitly supplied private cache.
The project ledger contains provenance, IDs, numeric facts and review status.
Requires pdfplumber from the bundled workspace Python runtime.
"""
import argparse
import hashlib
import json
import re
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

import pdfplumber

SOURCE_ID = "FSAE-2027-V1"
EXPECTED_SHA256 = "f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e"
DOWNLOAD = "https://www.fsaeonline.com/cdsweb/gen/DownloadDocument.aspx?DocumentID=da79bcb4-0935-4f7b-83d7-0dbb8ce68d38"
CATALOG = "https://www.fsaeonline.com/cdsweb/gen/DocumentResources.aspx"
RULE = re.compile(r"^(GR|AD|PS|V|F|T|VE|IC|EV|IN|S|D)\.\d+(?:\.\d+)*(?=\s|$)")
REFERENCE = re.compile(r"\b(?:GR|AD|PS|V|F|T|VE|IC|EV|IN|S|D)\.\d+(?:\.\d+)*\b")
CHAPTER = re.compile(r"^(GR|AD|PS|V|F|T|VE|IC|EV|IN|S|D)\s*[-–]\s*(.+)$")
TABLE = re.compile(r"^Table\s+([A-Z]{1,2}[-–]\d+)\b", re.I)
FOOTER = re.compile(r"^(?:Formula SAE.*Rules 2027|Version 1\.0\s+1 Sept 2026)")
BULLET = re.compile(r"^(?:([a-z])\.|(\d+)\.|([•●▪]))\s+")
NUMBER = re.compile(r"(?<![\w.])[-+]?\d+(?:\.\d+)?(?:\s*/\s*\d+)?(?![\w.])")
UNITS = re.compile(r"^\s*(mm(?:[²³23])?|cm|m(?:[²³23])?|inches|inch|in|ft|kg|lbs?|g|MPa|GPa|kPa|Pa|psi|bar|kW|kWh|Wh|W|V\s*DC|V|mA|A|mΩ|kΩ|MΩ|Ω|ohms?|ms|seconds?|minutes?|hours?|s|min|km/hr|km/h|mph|Nm|N-m|N|lbf|rpm|Hz|°\s*C|°\s*F|%)(?=\s|[),.;:/]|$)")


def digest(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def applicability(rule_id, heading, text):
    if rule_id.startswith("IC.") or rule_id.startswith("F.9") or rule_id.startswith("IN.12"):
        return "not_applicable", "Internal-combustion-only section; baseline is human-driven electric."
    if re.search(r"\bIC\s+Only\b", heading, re.I):
        return "not_applicable", "Explicit IC-only heading; baseline is electric."
    if re.search(r"\b(?:driverless|autonomous|DV)\b", heading + " " + text, re.I):
        return "conditional", "Driverless-related text; baseline is human-driven EV and applicability needs clause review."
    if re.match(r"F\.(?:4|6|7)(?:\.|$)", rule_id):
        return "conditional", "Material/chassis construction alternative; chassis choice has not been baselined."
    if re.search(r"\b(?:internal combustion|IC)\b", text, re.I):
        return "unresolved", "Shared paragraph contains IC-specific distinctions; EV applicability requires clause-level review."
    return "applicable", "Shared or EV section included in the human-driven electric baseline; conditions still require review."


def classification(rule_id):
    prefix = rule_id.split(".")[0]
    if prefix == "IN":
        return "inspection"
    if prefix in ("S", "D"):
        return "competition"
    if prefix in ("AD", "PS") or re.match(r"EV\.(?:2|10|11|12|13)(?:\.|$)", rule_id):
        return "process"
    return "general" if prefix == "GR" else "design"


def numeric_facts(text):
    # All numeric tokens survive even when no safe unit/operator can be assigned.
    scrubbed = REFERENCE.sub(lambda match: " " * len(match.group()), text)
    values = []
    for number in NUMBER.finditer(scrubbed):
        unit = UNITS.match(scrubbed[number.end():])
        values.append({"value": number.group().replace(" ", ""),
                       "unit": unit.group(1) if unit else None,
                       "operator": None,
                       "source_character_offset": number.start(),
                       "context_sha256": digest(text),
                       "interpretation_status": "source_token_only_needs_semantic_review"})
    return values


def split_paragraphs(lines, heading):
    paragraphs = []
    previous = None
    for index, line in enumerate(lines):
        text = line["text"]
        if index == 0:
            text = RULE.sub("", text, count=1).strip()
            if heading:
                continue
        if not text:
            continue
        label = BULLET.match(text)
        new_paragraph = not paragraphs or label or (previous and line["top"] - previous["bottom"] > 4)
        if new_paragraph:
            paragraphs.append({"label": label.group(0).strip() if label else None, "lines": []})
        paragraphs[-1]["lines"].append({**line, "text": text})
        previous = line
    return paragraphs


def catalog_manifest(documents):
    sources = []
    for document in documents:
        title = document.get("title", "")
        if title == "2027 Formula SAE Rules":
            status, role = "downloaded_pinned", "governing_main_rulebook"
        elif "2027" in title and "Structural" in title:
            status, role = "published_not_ingested", "conditional_structural_submission_template"
        elif "Driverless" in title or "Driverless" in document.get("folder", ""):
            status, role = "conditional_not_ingested", "driverless_reference_requires_2027_applicability_confirmation"
        elif "2026" in title or "2026" in document.get("folder", ""):
            status, role = "prior_season_not_baselined", "archived_reference"
        elif document.get("publisher") in ("Ruleset and Resources", "Technical Inspection Resources", "Design Event Resources", "Cost Event Resources", "Presentation Event Resources"):
            status, role = "published_applicability_unresolved_not_ingested", "supporting_resource"
        else:
            continue
        sources.append({"catalog_document_id": document.get("id"), "title": title,
                        "catalog_version": document.get("version"), "catalog_status": document.get("status"),
                        "catalog_created_at": document.get("createdAt"), "publisher": document.get("publisher"),
                        "folder": document.get("folder"), "subfolder": document.get("subfolder"),
                        "download_url": "https://www.fsaeonline.com/cdsweb/gen/DownloadDocument.aspx?DocumentID=" + document["id"],
                        "baseline_role": role, "ingestion_status": status,
                        "sha256": EXPECTED_SHA256 if status == "downloaded_pinned" else None})
    return sources


def main():
    args = argparse.ArgumentParser()
    args.add_argument("--pdf", required=True, type=Path)
    args.add_argument("--catalog", required=True, type=Path)
    args.add_argument("--private-cache", required=True, type=Path)
    args.add_argument("--output", type=Path, default=Path(__file__).parent)
    options = args.parse_args()
    sha = hashlib.sha256(options.pdf.read_bytes()).hexdigest()
    if sha != EXPECTED_SHA256:
        raise ValueError("Unexpected PDF checksum: confirm edition before rebuilding the ledger")

    blocks, pages, loose_lines, source_objects = [], [], [], []
    current = None
    current_object = None
    with pdfplumber.open(options.pdf) as pdf:
        if len(pdf.pages) != 147 or "Rules 2027" not in pdf.pages[0].extract_text():
            raise ValueError("Pinned source title/page count did not validate")
        for page_number, page in enumerate(pdf.pages, start=1):
            lines = page.extract_text_lines(return_chars=False)
            content = [line for line in lines if not FOOTER.match(line["text"])] if page_number >= 5 else []
            anchor_words = {(round(word["top"], 1), word["text"]): word for word in page.extract_words(extra_attrs=["fontname"])
                            if word["x0"] <= 75 and RULE.fullmatch(word["text"])}
            page_rule_ids = []
            for line in content:
                line = {**line, "pdf_page": page_number}
                chapter = CHAPTER.match(line["text"])
                if chapter and chapter.group(2).isupper():
                    current = None
                    current_object = {"source_object_id": "SECTION-" + chapter.group(1) + "-INTRO",
                                      "section": chapter.group(1), "title": chapter.group(2),
                                      "kind": "unnumbered_introduction", "lines": []}
                    source_objects.append(current_object)
                    loose_lines.append({**line, "kind": "section_heading", "section": chapter.group(1)})
                    continue
                table = TABLE.match(line["text"])
                if table:
                    current = None
                    table_id = table.group(1).replace("–", "-").upper()
                    current_object = {"source_object_id": "TABLE-" + table_id,
                                      "section": table_id.split("-")[0], "title": line["text"],
                                      "kind": "table", "lines": []}
                    source_objects.append(current_object)
                rule = RULE.match(line["text"]) if line["x0"] <= 75 else None
                if rule:
                    current_object = None
                    rule_id = rule.group()
                    word = anchor_words.get((round(line["top"], 1), rule_id), {})
                    current = {"rule_id": rule_id, "heading": "Bold" in word.get("fontname", ""),
                               "title": line["text"][len(rule_id):].strip() if "Bold" in word.get("fontname", "") else None,
                               "lines": []}
                    blocks.append(current)
                    page_rule_ids.append(rule_id)
                if current_object is not None:
                    current_object["lines"].append(line)
                elif current is None:
                    loose_lines.append({**line, "kind": "unassigned_body_needs_review"})
                else:
                    current["lines"].append(line)
            pages.append({"pdf_page": page_number, "printed_page": page_number,
                          "page_id": f"{SOURCE_ID}:page-{page_number:03}",
                          "role": "front_matter" if page_number < 5 else "rules_body",
                          "body_text_lines": len(content), "rule_ids_starting_here": page_rule_ids,
                          "raster_image_count": len(page.images), "vector_curve_count": len(page.curves),
                          "vector_line_count": len(page.lines), "vector_rectangle_count": len(page.rects),
                          "visual_review_status": "required_for_figures_tables_and_equations"})

    counts = Counter(block["rule_id"] for block in blocks)
    duplicate_ids = sorted(key for key, count in counts.items() if count > 1)
    all_ids = set(counts)
    nodes, clauses, private_text = [], [], []
    for block in blocks:
        rule_id = block["rule_id"]
        parents = [rule_id.rsplit(".", count)[0] for count in range(1, rule_id.count("."))]
        titles = [candidate["title"] for candidate in blocks if candidate["rule_id"] in parents and candidate["title"]]
        heading = " / ".join(titles + ([block["title"]] if block["title"] else []))
        paragraphs = split_paragraphs(block["lines"], block["heading"])
        rule_pages = sorted({line["pdf_page"] for line in block["lines"]})
        node = {"source_rule_id": rule_id, "title": block["title"], "ancestor_titles": titles,
                "source_pages": rule_pages, "is_heading": block["heading"],
                "is_leaf": not any(candidate.startswith(rule_id + ".") for candidate in all_ids),
                "paragraph_count": len(paragraphs), "paragraph_ids": []}
        for paragraph_number, paragraph in enumerate(paragraphs, start=1):
            clause_id = f"{SOURCE_ID}:{rule_id}:p{paragraph_number:03}"
            text = " ".join(line["text"] for line in paragraph["lines"])
            applicable, reason = applicability(rule_id, heading, text)
            record = {"clause_id": clause_id, "source_document_id": SOURCE_ID, "source_sha256": sha,
                      "source_rule_id": rule_id, "source_pages": sorted({line["pdf_page"] for line in paragraph["lines"]}),
                      "source_paragraph_number": paragraph_number, "source_bullet_label": paragraph["label"],
                      "source_text_sha256": digest(text), "source_character_count": len(text),
                      "source_spans": [{key: line[key] for key in ("pdf_page", "x0", "top", "x1", "bottom")} for line in paragraph["lines"]],
                      "title": heading or rule_id, "short_paraphrase": None,
                      "applicability": applicable, "applicability_reason": reason,
                      "applicability_evaluation_status": "scope_filter_only_clause_conditions_need_review",
                      "classification": classification(rule_id),
                      "referenced_rule_ids": sorted(set(REFERENCE.findall(text)) - {rule_id}),
                      "numeric_facts": numeric_facts(text),
                      "normative_language_present": bool(re.search(r"\b(?:must|shall|required|may not|must not)\b", text, re.I)),
                      "interpretation_status": "needs_semantic_review",
                      "model_element_ids": [], "verification_element_ids": [],
                      "coverage_status": "not_modeled", "verification_status": "not_verified"}
            clauses.append(record)
            private_text.append({"clause_id": clause_id, "source_rule_id": rule_id, "text": text})
            node["paragraph_ids"].append(clause_id)
        nodes.append(node)

    object_records = []
    for source_object in source_objects:
        if not source_object["lines"]:
            continue
        paragraphs = split_paragraphs(source_object["lines"], source_object["kind"] == "table")
        for number, paragraph in enumerate(paragraphs, start=1):
            text = " ".join(line["text"] for line in paragraph["lines"])
            clause_id = f"{SOURCE_ID}:{source_object['source_object_id']}:p{number:03}"
            object_records.append({"clause_id": clause_id, "source_document_id": SOURCE_ID,
                                   "source_sha256": sha, "source_rule_id": None,
                                   "source_section_id": source_object["section"],
                                   "source_object_id": source_object["source_object_id"],
                                   "source_object_type": source_object["kind"],
                                   "source_pages": sorted({line["pdf_page"] for line in paragraph["lines"]}),
                                   "source_text_sha256": digest(text), "source_character_count": len(text),
                                   "source_spans": [{key: line[key] for key in ("pdf_page", "x0", "top", "x1", "bottom")} for line in paragraph["lines"]],
                                   "title": source_object["title"], "short_paraphrase": None,
                                   "applicability": "unresolved", "applicability_reason": "Unnumbered text/table requires row and cell-level review for the EV baseline",
                                   "classification": classification(source_object["section"]),
                                   "referenced_rule_ids": sorted(set(REFERENCE.findall(text))),
                                   "numeric_facts": numeric_facts(text), "interpretation_status": "needs_semantic_review",
                                   "model_element_ids": [], "verification_element_ids": [],
                                   "coverage_status": "not_modeled", "verification_status": "not_verified"})
            private_text.append({"clause_id": clause_id, "source_rule_id": None,
                                 "source_object_id": source_object["source_object_id"], "text": text})

    missing_parents = sorted({node["source_rule_id"].rsplit(".", 1)[0] for node in nodes
                              if node["source_rule_id"].count(".") >= 2 and node["source_rule_id"].rsplit(".", 1)[0] not in all_ids})
    unresolved_references = sorted({reference for clause in clauses for reference in clause["referenced_rule_ids"] if reference not in all_ids})
    validation = {"unique_rule_ids": len(all_ids), "rule_anchor_occurrences": len(blocks),
                  "duplicate_rule_ids": duplicate_ids, "missing_parent_rule_ids": missing_parents,
                  "unresolved_reference_ids": unresolved_references,
                  "paragraph_count": len(clauses), "unassigned_body_line_count": sum(line["kind"] == "unassigned_body_needs_review" for line in loose_lines),
                  "unnumbered_source_object_count": len({record["source_object_id"] for record in object_records}),
                  "unnumbered_paragraph_count": len(object_records),
                  "source_numbering_notes": [{"missing_parent_id": "D.8.1", "pdf_page": 136,
                                              "status": "visually_confirmed_source_omission_do_not_invent_rule_id"}],
                  "applicability_counts": dict(Counter(clause["applicability"] for clause in clauses)),
                  "semantic_review_complete": False, "visual_review_complete": False,
                  "compliance_complete": False}
    generated = datetime.now(timezone.utc).isoformat()
    documents = json.loads(options.catalog.read_text(encoding="utf-8"))
    manifest = {"schema_version": 1, "generated_at_utc": generated,
                "event": {"competition": "Formula SAE Electric", "season": 2027, "venue": "Michigan International Speedway",
                          "vehicle_class": "human_driven_electric", "driverless_scope": "conditional_outside_baseline"},
                "main_rulebook": {"source_document_id": SOURCE_ID, "title": "Formula SAE Rules 2027", "version": "1.0",
                                  "document_date": "2026-09-01", "published_date": "2026-09-15", "pdf_pages": 147,
                                  "sha256": sha, "official_url": DOWNLOAD, "catalog_url": CATALOG,
                                  "source_status": "official_final_pinned", "source_text_distribution": "private_cache_only"},
                "supporting_sources": catalog_manifest(documents),
                "pending_sources": [{"title": title, "status": "2027_edition_not_identified_in_catalog_snapshot",
                                     "impact": "No completeness claim until applicability and publication resolved"}
                                    for title in ("2027 Electric Systems Form", "2027 EV technical inspection forms", "2027 Energy Meter User Manual", "2027 applicable event handbooks and supplements")],
                "authoritative_live_resources": ["https://www.fsaeonline.com/Deadlines.aspx?seriesCode=FSAE",
                                                "https://www.fsaeonline.com/cdsweb/app/news.aspx"],
                "validation": validation}
    write_json(options.output / "source-manifest.json", manifest)
    write_json(options.output / "rule-inventory.json", {"schema_version": 1, "source_document_id": SOURCE_ID,
                                                       "pages": pages, "rule_nodes": nodes, "validation": validation})
    write_json(options.output / "normalized-clauses.json", {"schema_version": 1, "source_document_id": SOURCE_ID, "clauses": clauses, "source_objects": object_records})
    write_json(options.private_cache / "private-source-paragraphs.json", private_text)
    write_json(options.private_cache / "private-unassigned-lines.json", loose_lines)
    print(json.dumps(validation, indent=2))


if __name__ == "__main__":
    main()
