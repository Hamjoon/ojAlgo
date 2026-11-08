# Consistency checks

```json
{
  "nonincreasing": true,
  "index_zero_equals_denominators": true,
  "raw_records": 888,
  "expected_raw_records": 888,
  "raw_count_matches": true,
  "missing_records": [],
  "build_fail_indices": [
    145,
    274,
    275,
    300
  ],
  "measured_raw_records": 888,
  "derived_raw_records": 0,
  "expected_measured_raw_records": 888,
  "expected_derived_raw_records": 0,
  "skipped_indices": [
    11,
    26,
    49,
    51,
    54,
    66,
    68,
    69,
    70,
    74,
    76,
    78,
    80,
    81,
    83,
    84,
    90,
    95,
    102,
    103,
    107,
    119,
    120,
    124,
    131,
    150,
    159,
    162,
    163,
    165,
    171,
    178,
    181,
    188,
    190,
    203,
    205,
    236,
    239,
    248,
    254,
    255,
    262,
    276,
    278,
    280,
    281,
    285,
    287,
    288,
    289,
    290,
    291,
    293,
    296,
    297
  ]
}
```

Coverage: identical executable line sets across all 3 groups at 228 indices. Required indices: 232. Missing measurements: 12 (enumerated in coverage-consistency.json).

## Three-group checks

| Check | Dev (scope) | KTester | Ouédraogo |
|---|---:|---:|---:|
| Fixed denominator | 353 | 197 | 15 |
| Final survivors | 3 | 181 | 0 |
| Survival records | 296 | 296 | 296 |
| Coverage records | 241 | 241 | 241 |
| Matching executable universes | 228 | 228 | 228 |
