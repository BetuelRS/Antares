
export type AdaptiveProposal = {
  newTargetKcal: number;
  previousTargetKcal: number;
  observedTdee: number;
};

export const ALLOWED_AGGREGATE_KEYS = [
  'weekStartEpochDay',
  'weekEndEpochDay',
  'loggedDays',
  'avgKcal',
  'targetKcal',
  'daysOnTarget',
  'avgProteinG',
  'avgCarbsG',
  'avgFatG',
  'weighIns',
  'weightStartKg',
  'weightEndKg',
  'weightTrendDeltaKg',
  'workouts',
  'workoutVolumeKg',
  'exerciseKcal',
  'fastingSessions',
  'fastingAvgHours',
  'runs',
  'runDistanceKm',
  'runMinutes',
] as const;

export type Aggregate = Record<string, any>;

export function sanitizeAggregate(input: Aggregate): Aggregate {
  const out: Aggregate = {};
  for (const key of ALLOWED_AGGREGATE_KEYS) {
    const v = input[key];

    if (typeof v === 'number' && Number.isFinite(v)) out[key] = v;
    else if (typeof v === 'boolean') out[key] = v;
  }
  const gaps = sanitizeMicroGaps(input.microGaps);
  if (Object.keys(gaps).length) out.microGaps = gaps;
  return out;
}

export function sanitizeMicroGaps(input: unknown): Record<string, number> {
  const out: Record<string, number> = {};
  if (!input || typeof input !== 'object' || Array.isArray(input)) return out;
  for (const [key, value] of Object.entries(input as Record<string, unknown>)) {
    if (!CANONICAL_NUTRIENT_KEYS.has(key)) continue;
    if (typeof value !== 'number' || !Number.isFinite(value)) continue;
    const pct = Math.round(value);
    if (pct < 0 || pct > 100) continue;
    out[key] = pct;
    if (Object.keys(out).length >= MAX_MICRO_GAPS) break;
  }
  return out;
}

export const MAX_MICRO_GAPS = 6;

const CANONICAL_NUTRIENT_KEYS = new Set([
  'vitA_ug', 'vitB1_mg', 'vitB2_mg', 'vitB3_mg', 'vitB5_mg', 'vitB6_mg',
  'vitB9_ug', 'vitB12_ug', 'vitC_mg', 'vitD_ug', 'vitE_mg', 'vitK_ug',
  'calcium_mg', 'iron_mg', 'magnesium_mg', 'phosphorus_mg', 'potassium_mg',
  'sodium_mg', 'zinc_mg', 'copper_mg', 'manganese_mg', 'selenium_ug', 'iodine_ug',
  'fiber_g',
]);

export function isSparse(a: Aggregate): boolean {
  return (a.loggedDays ?? 0) < 4;
}

export const MAX_POINTS = 3;

export type CoachReport = {
  wins: string[];
  observations: string[];
  adjustments: string[];
  focus: string;
};

export function clampReport(r: CoachReport): CoachReport {
  return {
    wins: (r.wins ?? []).slice(0, MAX_POINTS),
    observations: (r.observations ?? []).slice(0, MAX_POINTS),
    adjustments: (r.adjustments ?? []).slice(0, MAX_POINTS),
    focus: r.focus ?? '',
  };
}

export function coachUserText(
  aggregate: Aggregate,
  lang: string,
  adaptive?: AdaptiveProposal | null,
): string {
  const clean = sanitizeAggregate(aggregate);

  const parts = [
    `User language: ${lang}`,
    '',
    'Week data (JSON):',
    JSON.stringify(clean),
  ];

  if (isSparse(clean)) {
    parts.push(
      '',
      `This week is SPARSE (${clean.loggedDays ?? 0} logged days out of 7). ` +
        'Say so plainly, do not draw conclusions from it, and suggest exactly one habit: logging.',
    );
  }

  const gaps = clean.microGaps as Record<string, number> | undefined;
  if (gaps && Object.keys(gaps).length) {

    parts.push(
      '',
      'Micronutrients that came in low this week (average % of the daily reference): ' +
        Object.entries(gaps).map(([k, v]) => `${k} ${v}%`).join(', ') + '. ' +
        'Mention at most one of these, the lowest, and name real foods that raise it. ' +
        'Do not give medical advice and do not suggest supplements.',
    );
  }

  if (adaptive) {

    parts.push(
      '',
      `The app's deterministic engine has already computed a new calorie target: ` +
        `${adaptive.previousTargetKcal} -> ${adaptive.newTargetKcal} kcal/day ` +
        `(observed maintenance: ${adaptive.observedTdee} kcal/day). ` +
        'Explain in one of the adjustments WHY this follows from the numbers above. ' +
        'Do not propose a different number and do not question it.',
    );
  }

  return parts.join('\n');
}
