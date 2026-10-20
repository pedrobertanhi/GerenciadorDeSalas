# Diagramas do Sistema

> Status: diagramas de contexto, componentes e dados produzidos em fonte Mermaid editável. Os diagramas de sequência permanecem planejados na #57.

Os diagramas concluídos da GOV-03 estão em [DIAGRAMAS.md](DIAGRAMAS.md). O modelo de domínio complementar está em [MODELO_DE_DOMINIO.md](MODELO_DE_DOMINIO.md).

## Estrutura esperada

```text
docs/diagramas/
├── contexto/
├── componentes/
├── dados/
└── sequencias/
```

Cada diagrama deve possuir:

- fonte editável versionada;
- visualização legível no GitHub; para Mermaid embutido, a renderização do próprio Markdown é a evidência visual, enquanto outros formatos exigem imagem exportada;
- título, versão e data;
- nomes coerentes com código, API e RTM;
- revisão de um integrante que não seja o autor.

## Diagramas obrigatórios

- [x] Contexto do sistema — #3
- [x] Componentes da aplicação — #3
- [ ] Sequência de criação de reserva — #57
- [ ] Sequência de detecção de sobreposição — #57
- [ ] Sequência de solicitação simultânea — #57
- [ ] Sequência de aprovação de recurso restrito — #57
- [ ] Sequência de bloqueio por manutenção — #57
- [ ] Sequência de retirada e devolução — #57

## Diagrama complementar

- [x] Entidade-relacionamento/dados — #3

Cada fonte e cada imagem exportada devem ser adicionadas em commits separados.
