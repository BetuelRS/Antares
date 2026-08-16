-- Verificação obrigatória antes do E3 — apagar as 23 tabelas por utilizador das migrações
-- 0001–0008.
--
-- É o `delete-account` que cumpre o direito ao apagamento de quem usou versões da app com
-- sincronização, e é destas tabelas que ele apaga. Largá-las por cima de dados de alguém
-- tira à app a capacidade de os apagar a pedido — e isso não se desfaz.
--
-- Correr no SQL Editor do projeto Antares no Supabase. Devolve a contagem **exata** de cada
-- tabela, e não a estimativa do planeador.
--
-- O que se espera: 23 linhas, todas com `linhas = 0`.
--   · Menos de 23 linhas → alguma tabela já não existe. Diz quais faltam.
--   · Alguma com linhas > 0 → **parar**. Há dados de alguém lá dentro, e a decisão do que
--     lhes fazer é do dono, não da migração.

select
    t.table_name as tabela,
    (xpath(
        '/row/c/text()',
        query_to_xml(format('select count(*) as c from public.%I', t.table_name), false, true, '')
    ))[1]::text::bigint as linhas
from information_schema.tables t
where t.table_schema = 'public'
  and t.table_name in (
      'user_profile', 'weight_log', 'daily_target_override',
      'food', 'food_log', 'water_log',
      'recipe', 'recipe_ingredient',
      'exercise_log', 'exercise',
      'routine', 'routine_item', 'routine_schedule',
      'workout_session', 'workout_set',
      'fasting_protocol', 'fasting_session',
      'run',
      'coach_report',
      'meal_template', 'meal_template_item',
      'body_measurement_log', 'goal_history'
  )
order by linhas desc, tabela;

-- As que **ficam de pé** e não entram nesta lista: `ai_requests`, `entitlements`,
-- `ai_admin`, `food_cache` e `purchase_events`. Essas não são cópias do telemóvel — são do
-- serviço, e o E3 não lhes toca.
