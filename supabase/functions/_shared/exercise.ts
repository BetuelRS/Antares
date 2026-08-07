
export type ModelExercise = {
  activity_en: string;
  activity_original: string;
  duration_min?: number | null;
  distance_km?: number | null;
  met: number;
  confidence: number;
  warnings: string[];
};

export type ResolvedExercise = {
  activity: string;
  activityEn: string;
  durationMin: number | null;
  met: number;
  kcal: number | null;
  confidence: number;
  estimated: boolean;
  warnings: string[];
};

const RUNNING = ['running', 'run', 'jogging', 'jog'];

export const MET_MIN = 1.0;
export const MET_MAX = 23.0;

export function clampMet(met: number): number {
  if (!Number.isFinite(met)) return MET_MIN;
  return Math.min(Math.max(met, MET_MIN), MET_MAX);
}

export function resolveExercise(m: ModelExercise, weightKg: number): ResolvedExercise {
  const met = clampMet(m.met);
  const isRun = RUNNING.some((r) => m.activity_en.toLowerCase().includes(r));

  let kcal: number | null = null;
  if (isRun && m.distance_km != null && m.distance_km > 0) {
    kcal = Math.round(1.0 * weightKg * m.distance_km);
  } else if (m.duration_min != null && m.duration_min > 0) {
    kcal = Math.round(met * weightKg * (m.duration_min / 60));
  }

  const warnings = [...(m.warnings ?? [])];

  if (kcal == null && !warnings.includes('NO_DURATION')) warnings.push('NO_DURATION');

  return {
    activity: m.activity_original,
    activityEn: m.activity_en,
    durationMin: m.duration_min ?? null,
    met,
    kcal,
    confidence: m.confidence,
    estimated: true,
    warnings,
  };
}
