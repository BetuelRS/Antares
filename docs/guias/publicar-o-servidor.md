# Publicar o servidor

O servidor serve **uma** funcionalidade: a análise de refeições por foto e por texto, que precisa
de um modelo de linguagem e por isso não corre no telemóvel. Tudo o resto da app funciona sem ele.

Se só queres compilar e usar a app, salta este guia — [Compilar a app](compilar.md) chega.

## O que precisas

- Um projeto [Supabase](https://supabase.com/).
- A CLI do Supabase.
- Uma chave da API da Anthropic.

## 1. Configurar os segredos

Nenhum destes valores entra no repositório.

```bash
supabase secrets set ANTHROPIC_API_KEY=sk-ant-...
supabase secrets set AI_IP_SALT=$(openssl rand -hex 32)
supabase secrets set ADMIN_CODE=...
```

| Variável | Para quê | Sem ela |
|---|---|---|
| `ANTHROPIC_API_KEY` | falar com o modelo | a análise não funciona |
| `AI_IP_SALT` | resumo criptográfico do IP, para contar utilizações sem guardar o endereço | recorre à chave de serviço |
| `ADMIN_CODE` | levantar o limite de utilizações | **ninguém entra** — que é o comportamento certo |
| `AI_GLOBAL_TRIAL_DAILY` | teto diário global | usa o valor por omissão |
| `USDA_API_KEY` | consultar a tabela americana antes de recorrer ao modelo | recorre-se ao modelo mais vezes |

O `SUPABASE_URL` e o `SUPABASE_SERVICE_ROLE_KEY` são postos pela própria plataforma.

> O `ADMIN_CODE` vive só no servidor. A app envia o que a pessoa escreveu e nunca o conhece —
> desmontar o APK não o revela.

## 2. Aplicar as migrações

```bash
supabase db push
```

Cria as tabelas de controlo de utilizações e a cache de nutrição.

As migrações `0001` a `0008` criavam 23 tabelas por utilizador com RLS, de quando a app
sincronizava. A `0009` larga-as. Num servidor novo elas chegam a ser criadas e são largadas a
seguir — é feio, e é de propósito: reescrever as migrações antigas fazia o servidor de quem já
as aplicou divergir do que o ficheiro diz.

**Numa base que já tenha dados, correr antes `supabase/verificar-tabelas-rls.sql`.** Ele conta
o que lá está sem apagar nada. Largar uma dessas tabelas por cima de linhas de alguém tira ao
`delete-account` a capacidade de cumprir o direito ao apagamento, e isso não se desfaz. Ver
[Dados e licenças](../referencia/dados-e-licencas.md) e
[a decisão de não sincronizar](../explicacao/decisoes/0001-a-app-nao-sincroniza.md).

## 3. Verificar antes de publicar

```bash
cd supabase
deno check functions/analyze-food/index.ts functions/analyze-exercise/index.ts \
            functions/delete-account/index.ts functions/admin-unlock/index.ts
deno test --allow-env --allow-net functions/_shared/
```

## 4. Publicar

```bash
supabase functions deploy analyze-food
supabase functions deploy analyze-exercise
supabase functions deploy delete-account
supabase functions deploy admin-unlock
```

## 5. Ligar a app

No `local.properties` da app:

```properties
SUPABASE_URL=https://o-teu-projeto.supabase.co
SUPABASE_ANON_KEY=a-tua-chave-anonima
```

A chave anónima é pública por natureza — é a que vai dentro do APK. A chave de serviço **nunca**
sai do servidor.

## As quatro funções

| Função | O que faz |
|---|---|
| `supabase/functions/analyze-food/` | descrição ou fotografia de uma refeição → valores nutricionais |
| `supabase/functions/analyze-exercise/` | descrição de um exercício → calorias estimadas |
| `supabase/functions/delete-account/` | apaga do servidor tudo o que esteja associado à conta anónima |
| `supabase/functions/admin-unlock/` | levanta o limite de utilizações |

O que é partilhado está em `supabase/functions/_shared/`: a chamada ao modelo (`claude.ts`), o
controlo de quota e de abuso (`gate.ts`), a nutrição (`nutrition.ts`, `nutrients.ts`) e os textos
de sistema (`prompts.ts`).

O nome do modelo vive só em `supabase/functions/_shared/claude.ts`, na constante `MODEL_ANALYSIS`.
Trocá-lo aí muda-o em todas as funções.
