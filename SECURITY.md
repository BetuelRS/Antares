# Política de segurança

## Versões suportadas

Só a versão mais recente. Este é um projeto de uma pessoa e não há ramos de manutenção.

| Versão | Suportada |
|---|---|
| 1.0.x | sim |
| < 1.0 | não |

## Comunicar uma vulnerabilidade

**Não abras uma *issue* pública.**

Usa o [comunicado privado de segurança do
GitHub](https://github.com/BetuelRS/Antares/security/advisories/new), ou envia um e-mail para
**betuel801@gmail.com** com `[SEGURANÇA]` no assunto.

Diz o que conseguires:

- o que é possível fazer que não devia ser possível;
- os passos para o reproduzir;
- a versão da app e o Android em que o viste;
- o impacto, se o souberes avaliar.

Respondo em dias, não em horas — é um projeto de tempo livre. Se não tiveres resposta numa semana,
insiste.

## O que conta como vulnerabilidade aqui

O modelo da app é simples: **os dados do utilizador não saem do telemóvel**. Interessa sobretudo
tudo o que quebre isso.

Conta:

- fazer sair dados do utilizador do dispositivo por um caminho não documentado em
  [Privacidade](docs/explicacao/privacidade.md);
- ler ou alterar os dados da app a partir de outra app;
- contornar o controlo de utilizações ou o código de administração das Edge Functions;
- injeção de SQL, travessia de caminhos, ou execução de código a partir de dados importados —
  nomeadamente de um ficheiro de backup preparado à mão;
- uma chave ou segredo exposto no repositório ou no APK.

Não conta:

- os cinco destinos de rede documentados em
  [Privacidade](docs/explicacao/privacidade.md) — são intencionais e estão escritos;
- a chave anónima do Supabase estar dentro do APK: é pública por natureza, e é assim que foi
  desenhada;
- o `release` estar assinado com a chave de depuração. É conhecido, está escrito em
  [Compilar](docs/guias/compilar.md), e é a razão pela qual a app não está numa loja;
- ter acesso físico ao telemóvel desbloqueado.

## O que já se faz

- Nenhum segredo no repositório. O CI procura chaves da Anthropic e JWT em cada *push*, e falha
  se encontrar.
- O código de administração vive só na configuração do servidor. A app envia o que a pessoa
  escreveu e nunca o conhece — desmontar o APK não o revela.
- Os endereços IP das chamadas de análise são guardados em resumo criptográfico, com sal, e nunca
  em claro.
- A app pede ao sistema apenas as permissões que usa, e o `ManifestPermissionsTest` falha se
  alguma sobrar.
