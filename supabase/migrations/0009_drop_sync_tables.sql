-- Larga as 23 tabelas por utilizador criadas pelas migrações 0001–0008.
--
-- A app deixou de sincronizar. Estas tabelas não recebiam escritas desde 2026-08-04 e o
-- telemóvel nunca mais lê delas — mas quem abrisse o SQL concluía o contrário, e um esquema
-- que descreve uma funcionalidade removida é a mesma armadilha que esvaziou este repositório
-- uma vez. Ver `docs/explicacao/decisoes/0001-a-app-nao-sincroniza.md`.
--
-- **Foram verificadas vazias antes de serem largadas**, a 2026-08-16, com a consulta em
-- `supabase/verificar-tabelas-rls.sql`. Tinham 19 linhas de nove contas anónimas de teste do
-- dono, apagadas por ele com essa confirmação. Largar uma tabela por cima de dados de alguém
-- tiraria ao `delete-account` a capacidade de cumprir o direito ao apagamento, e isso não se
-- desfaz.
--
-- **Ficam de pé** as tabelas que são do serviço e não cópias do telemóvel: `ai_requests`,
-- `ai_admin`, `entitlements`, `purchase_events` e `food_cache`. O `delete-account` continua a
-- apagar dessas, e é por isso que ele continua a existir.

drop table if exists public.workout_set cascade;
drop table if exists public.workout_session cascade;
drop table if exists public.routine_schedule cascade;
drop table if exists public.routine_item cascade;
drop table if exists public.routine cascade;
drop table if exists public.exercise_log cascade;
drop table if exists public.exercise cascade;

drop table if exists public.meal_template_item cascade;
drop table if exists public.meal_template cascade;
drop table if exists public.recipe_ingredient cascade;
drop table if exists public.recipe cascade;
drop table if exists public.food_log cascade;
drop table if exists public.food cascade;
drop table if exists public.water_log cascade;

drop table if exists public.fasting_session cascade;
drop table if exists public.fasting_protocol cascade;
drop table if exists public.run cascade;
drop table if exists public.coach_report cascade;

drop table if exists public.body_measurement_log cascade;
drop table if exists public.goal_history cascade;
drop table if exists public.daily_target_override cascade;
drop table if exists public.weight_log cascade;
drop table if exists public.user_profile cascade;
