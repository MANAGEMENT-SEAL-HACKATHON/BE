#!/usr/bin/env python3
"""
RBL IRR analysis — RQ1 / RQ2 / RQ3.

Input: ANONYMIZED_RBL long-format CSV (from SEAL Analytics export).
Filters: criterion_type != PENALTY and score_type != PENALTY.

IMPORTANT: Numbers from seed data are pipeline smoke only.
Official thesis numbers = re-run on FINISHED hackathon export.
"""
from __future__ import annotations

import argparse
import math
import sys
from collections import defaultdict
from pathlib import Path

try:
    import pandas as pd
except ImportError:
    print("Install deps: pip install -r requirements-rbl.txt", file=sys.stderr)
    raise


def read_csv_with_comments(path: Path) -> tuple[pd.DataFrame, dict]:
    meta = {}
    with path.open(encoding="utf-8-sig") as f:
        lines = f.readlines()
    data_lines = []
    for line in lines:
        if line.startswith("#"):
            # "# key: value"
            body = line[1:].strip()
            if ":" in body:
                k, v = body.split(":", 1)
                meta[k.strip()] = v.strip()
            continue
        data_lines.append(line)
    from io import StringIO

    df = pd.read_csv(StringIO("".join(data_lines)))
    return df, meta


def filter_irr(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    if "criterion_type" in out.columns:
        out = out[out["criterion_type"].astype(str).str.upper() != "PENALTY"]
    if "score_type" in out.columns:
        out = out[out["score_type"].astype(str).str.upper() != "PENALTY"]
    return out


def wide_matrix(df: pd.DataFrame) -> pd.DataFrame:
    """Rows = (submission_id, criterion_id), cols = anonymized_judge_id."""
    pivot = df.pivot_table(
        index=["submission_id", "criterion_id"],
        columns="anonymized_judge_id",
        values="score_value",
        aggfunc="mean",
    )
    return pivot


def try_icc(matrix: pd.DataFrame) -> str:
    if matrix.shape[0] < 2 or matrix.shape[1] < 2:
        return "n/a (need ≥2 items and ≥2 raters)"
    try:
        import pingouin as pg

        long = matrix.reset_index().melt(
            id_vars=["submission_id", "criterion_id"],
            var_name="rater",
            value_name="score",
        ).dropna()
        long["target"] = (
            long["submission_id"].astype(str) + "_" + long["criterion_id"].astype(str)
        )
        icc = pg.intraclass_corr(
            data=long, targets="target", raters="rater", ratings="score"
        )
        row = icc[icc["Type"].astype(str).str.contains("ICC2", na=False)]
        if row.empty:
            row = icc.iloc[[0]]
        r = row.iloc[0]
        ci = r["CI95%"] if "CI95%" in r.index else "?"
        return f"{r['Type']}={r['ICC']:.4f} (CI {ci})"
    except Exception as e:
        return f"n/a ({e})"


def try_krippendorff(matrix: pd.DataFrame) -> str:
    if matrix.shape[0] < 2 or matrix.shape[1] < 2:
        return "n/a"
    try:
        import krippendorff
        import numpy as np

        # reliability_data: rows=raters, cols=items
        data = matrix.T.to_numpy(dtype=float)
        alpha = krippendorff.alpha(reliability_data=data, level_of_measurement="interval")
        if alpha is None or (isinstance(alpha, float) and math.isnan(alpha)):
            return "n/a"
        return f"α={float(alpha):.4f}"
    except Exception as e:
        return f"n/a ({e})"


def interpret_icc(val: str) -> str:
    return (
        "Interpretation (Koo & Li 2016): <0.50 poor; 0.50–0.75 moderate; "
        "0.75–0.90 good; >0.90 excellent. "
        "Krippendorff: α≥0.667 often cited as acceptable for tentative conclusions."
    )


def analyze(df: pd.DataFrame, meta: dict) -> str:
    lines = []
    lines.append("# RBL IRR Report (RQ1–RQ3)")
    lines.append("")
    lines.append("**WARNING:** If this CSV is from seed/dev data, numbers are pipeline smoke only.")
    lines.append("Official thesis metrics = re-run on FINISHED hackathon ANONYMIZED_RBL export.")
    lines.append("")
    for k in ("excluded_from_rq3", "rq3_faculty_n", "rq3_guest_n", "irr_filter"):
        if k in meta:
            lines.append(f"# {k}: {meta[k]}")
    lines.append("")

    irr = filter_irr(df)
    lines.append(f"Rows (after PENALTY filter): {len(irr)}")
    lines.append("")

    # RQ1
    m = wide_matrix(irr)
    icc1 = try_icc(m)
    a1 = try_krippendorff(m)
    lines.append("## RQ1 — Overall IRR")
    lines.append(f"- {icc1}")
    lines.append(f"- Krippendorff {a1}")
    lines.append(f"- {interpret_icc(icc1)}")
    lines.append("")

    # RQ2 by criterion_type
    lines.append("## RQ2 — By criterion type / criterion")
    if "criterion_type" in irr.columns:
        for ctype, g in irr.groupby("criterion_type"):
            mg = wide_matrix(g)
            lines.append(f"### Type `{ctype}`")
            lines.append(f"- {try_icc(mg)}")
            lines.append(f"- Krippendorff {try_krippendorff(mg)}")
    if "criterion_id" in irr.columns:
        for cid, g in irr.groupby("criterion_id"):
            name = g["criterion_name"].iloc[0] if "criterion_name" in g.columns else cid
            mg = wide_matrix(g)
            lines.append(f"### Criterion {cid} — {name}")
            lines.append(f"- {try_icc(mg)}")
            lines.append(f"- Krippendorff {try_krippendorff(mg)}")
    lines.append("")

    # RQ3 by judge_type
    lines.append("## RQ3 — Faculty vs Guest")
    faculty_n = irr[irr["judge_type"] == "FACULTY"]["anonymized_judge_id"].nunique() if "judge_type" in irr.columns else 0
    guest_n = irr[irr["judge_type"] == "GUEST"]["anonymized_judge_id"].nunique() if "judge_type" in irr.columns else 0
    other_n = irr[irr["judge_type"] == "OTHER"]["anonymized_judge_id"].nunique() if "judge_type" in irr.columns else 0
    lines.append(f"# excluded_from_rq3: {other_n} judges unclassified (OTHER)")
    lines.append(f"# rq3_faculty_n: {faculty_n}")
    lines.append(f"# rq3_guest_n: {guest_n}")
    lines.append("")
    if "judge_type" in irr.columns:
        for jtype in ("FACULTY", "GUEST"):
            g = irr[irr["judge_type"] == jtype]
            if g.empty:
                lines.append(f"### {jtype}: no rows")
                continue
            mg = wide_matrix(g)
            lines.append(f"### {jtype}")
            lines.append(f"- {try_icc(mg)}")
            lines.append(f"- Krippendorff {try_krippendorff(mg)}")
            lines.append(
                "Note: within-type ICC needs multiple raters of that type on the same items; "
                "may be n/a if assignment is mostly mixed pairs."
            )
    lines.append("")
    lines.append("Labels: Faculty/Internal Judge vs Guest Judge — do not equate INTERNAL with 'faculty' in narrative.")
    return "\n".join(lines)


def main():
    p = argparse.ArgumentParser(description="RBL IRR analysis RQ1–RQ3")
    p.add_argument("csv", type=Path, help="ANONYMIZED_RBL CSV path")
    p.add_argument("--out", type=Path, default=None, help="Write markdown report")
    args = p.parse_args()
    df, meta = read_csv_with_comments(args.csv)
    report = analyze(df, meta)
    out = args.out or args.csv.with_suffix(".irr_report.md")
    out.write_text(report, encoding="utf-8")
    try:
        print(report)
    except UnicodeEncodeError:
        sys.stdout.buffer.write(report.encode("utf-8", errors="replace"))
        sys.stdout.buffer.write(b"\n")
    print(f"\nWrote {out}", file=sys.stderr)


if __name__ == "__main__":
    main()
