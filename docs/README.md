# Documentação do Projeto

Documentação oficial e versionada do **Gerenciador de Salas de Aula**.

> Status: planejamento. Resultados permanecem pendentes até haver execução identificada por commit, PR, pipeline ou tag.

## Comece aqui

1. [PRD: problema, personas, escopo, RF/RNF e critérios](PRD.md)
2. [Planejamento semanal e capacidade](PLANEJAMENTO.md)
3. [Backlog completo](BACKLOG.md)
4. [Política obrigatória de commits](POLITICA_DE_COMMITS.md)
5. [Guia operacional de cada tarefa](GUIA_DE_EXECUCAO.md)
6. [Cobertura da especificação e das aulas](COBERTURA_ESPECIFICACAO.md)
7. [Flyway e versionamento do banco](FLYWAY.md)

## Arquitetura e decisões

- [ATAM, utility tree, riscos e tradeoffs](arquitetura/ATAM.md)
- [Modelo de domínio](arquitetura/MODELO_DE_DOMINIO.md)
- [Entidades de suporte](arquitetura/ENTIDADES_DE_SUPORTE.md)
- [Estados e transições da reserva](arquitetura/ESTADOS_DA_RESERVA.md)
- [Revisão do modelo contra a especificação](arquitetura/REVISAO_DO_MODELO.md)
- [Diagrama do modelo de domínio](diagramas/MODELO_DE_DOMINIO.md)
- [Índice de ADRs](adr/README.md)
- [Diagramas de contexto, componentes, dados e sequências](diagramas/README.md)
- OpenAPI será gerada pela Issue #10.

## Requisitos, riscos e rastreabilidade

- [RTM](RTM.md)
- [Registro de riscos](RISCOS.md)
- [Registro e fluxo de defeitos](DEFEITOS.md)
- [Diagnóstico inicial](DIAGNOSTICO_INICIAL.md)

## Testes

- [Plano de testes](testes/PLANO_DE_TESTES.md)
- [Catálogo de casos reproduzíveis](testes/CASOS_DE_TESTE.md)
- [Técnicas de caixa-preta, branca e cinza](testes/TECNICAS_DE_PROJETO.md)
- [Relatório de testes](testes/RELATORIO_DE_TESTES.md)

## Segurança

- [Plano de segurança e gates](seguranca/PLANO_DE_SEGURANCA.md)
- [Matriz de permissões por perfil](seguranca/MATRIZ_DE_PERMISSOES.md)
- [Modelo de ameaças](seguranca/MODELO_DE_AMEACAS.md)
- [Registro de vulnerabilidades](seguranca/REGISTRO_DE_VULNERABILIDADES.md)
- [Parecer final de segurança](seguranca/PARECER_DE_SEGURANCA.md)

## Qualidade e SQA

- [Plano independente de SQA e prontidão](qualidade/PLANO_SQA.md)
- [Relatório final de qualidade](qualidade/RELATORIO_FINAL_QUALIDADE.md)

## Desempenho

- [Plano e RNFs de carga](desempenho/PLANO_TESTE_CARGA.md)
- [Resultado da carga](desempenho/RESULTADO_TESTE_CARGA.md)

## Release, autoria e apresentação

- [Autoria e participação](equipe/AUTORIA.md)
- [Pacote de evidências](release/EVIDENCIAS.md)
- [Decisão formal de qualidade](release/DECISAO_DE_QUALIDADE.md)
- [Roteiro, prova oral e contingência](apresentacao/ROTEIRO_DEMONSTRACAO.md)

## Pós-entrega

- [Memória do projeto](pos_entrega/MEMORIA_DO_PROJETO.md)
- [Retrospectiva e plano de evolução](pos_entrega/RETROSPECTIVA.md)

## Estrutura esperada

```text
docs/
├── README.md
├── PRD.md
├── PLANEJAMENTO.md
├── GUIA_DE_EXECUCAO.md
├── BACKLOG.md
├── COBERTURA_ESPECIFICACAO.md
├── POLITICA_DE_COMMITS.md
├── RTM.md
├── RISCOS.md
├── DEFEITOS.md
├── arquitetura/
├── adr/
├── apresentacao/
├── desempenho/
├── diagramas/
├── equipe/
├── pos_entrega/
├── qualidade/
├── release/
├── seguranca/
└── testes/
```

## Regras de manutenção

1. Cada alteração concluída em um arquivo gera commit próprio.
2. Alterações independentes no mesmo arquivo ficam em commits diferentes.
3. Código, teste, configuração, documento e evidência não são agrupados.
4. Resultado não é preenchido sem execução real.
5. Evidência identifica Issue, commit/tag, ambiente, comando, teste e resultado.
6. Outro integrante revisa antes do merge.
7. Pull Request não usa squash.
8. IDs permanecem iguais em PRD, risco, caso e RTM.
9. Mudança de requisito revisa casos e invalida evidências antigas.
10. Dado real, segredo, token, senha, stack trace e corpo sensível não entram nos artifacts.
