# Gestão de Defeitos

> Status: registro inicial. Nunca usar dado pessoal, senha, token ou credencial real.

## Vocabulário

- **Erro:** ação/decisão humana equivocada.
- **Defeito:** problema gravado no requisito, código, teste, configuração ou documento.
- **Falha:** comportamento incorreto observado em execução.
- **Incidente:** evento percebido e investigado.

## Ciclo de vida

`Novo → Triado → Em correção → Em verificação → Fechado`

Alternativas: duplicado, não reproduzível, não será corrigido com risco aceito, reaberto. Quem corrige pode mover para verificação; fechamento exige a evidência no ambiente e versão definidos.

## Template de relato

- Título: ação + condição + resultado observado.
- Ambiente/versão/commit e perfil.
- Pré-condições.
- Passos mínimos numerados.
- Resultado obtido.
- Resultado esperado e requisito.
- Evidências sanitizadas.
- Frequência, alcance e impacto.
- Severidade com justificativa.
- Prioridade com justificativa diferente.
- Critério de verificação.

Campo desconhecido diz “não apurado” e gera ação; não recebe conteúdo inventado.

## Severidade e prioridade

Severidade mede dano a confidencialidade, integridade, disponibilidade, fluxo e alcance. Prioridade mede urgência, compromisso, exposição, contorno e custo de atraso. Rótulo sem justificativa não é triagem.

## Correção segura

1. Reproduzir em teste que falha.
2. Localizar e registrar a causa raiz.
3. Corrigir a menor causa controlável.
4. Rodar teste novo e suíte completa.
5. Verificar no ambiente acordado.
6. Atualizar RTM, risco, documento ou processo.

O fechamento exige Issue ligada ao commit/PR, teste de regressão identificável e execução verde reproduzível.

## Causa raiz

Usar 5 Porquês até uma condição controlável, sem obrigação artificial de exatamente cinco respostas. Registrar ações em camadas quando necessário: regra de domínio, constraint, validação, teste e processo.

## Métricas permitidas

- tempo de ciclo por severidade;
- taxa de reabertura;
- defeitos escapados por etapa;
- recorrência por causa;
- idade do backlog crítico.

Quantidade de bugs por pessoa não mede produtividade.

## Registro

| ID | Resumo | Requisito | Severidade | Prioridade | Estado | Causa raiz | Regressão | Evidência |
|---|---|---|---|---|---|---|---|---|
| D01 | Tela de salas recusa capacidade acima de 999999 | RF-02, Issue #15 | Baixa | Média | Em verificação | Regra da tela mais restrita que a da API e a do banco, sem teste de limite | `SalaWebControllerTest` (capacidade até o limite do Integer) | PR #167 |
| D02 | Página inválida na listagem de salas devolve JSON de erro em vez da tela | RF-13 | Baixa | Média | Em verificação | Parâmetro numérico da rota web sem tratamento | `SalaWebControllerTest` (página inválida) | PR #167 |
| D03 | Telas de salas consultáveis por todos os perfis, contra o critério "CRUD somente administrador" | RF-01, Issue #15 | Média | Alta | Em verificação | Critério da Issue lido só para a escrita, sem conferir a consulta | `SalaWebControllerTest` (outros perfis recebem 403) | PR #167 |
| D04 | Histórico de manutenção mostra só o identificador de professor ou material inativado | RF-08, Issue #15 | Média | Média | Em verificação | Nomes obtidos de listagens que só trazem recursos ativos | `MaintenanceBlockWebControllerTest`, `ProfessorNomesPorIdTest`, `MaterialNomesPorIdTest` | PR #168 |
| D05 | Edição recusada usa a mesma mensagem para recurso imutável e bloqueio encerrado | RF-08, Issue #15 | Baixa | Baixa | Em verificação | Duas regras distintas tratadas pela mesma exceção | `MaintenanceBlockWebControllerTest` (recurso diferente) | PR #168 |
| D06 | Página inválida na agenda de manutenção devolve JSON de erro em vez da tela | RF-13 | Baixa | Média | Em verificação | Mesma causa do D02 | `MaintenanceBlockWebControllerTest` (página inválida) | PR #168 |