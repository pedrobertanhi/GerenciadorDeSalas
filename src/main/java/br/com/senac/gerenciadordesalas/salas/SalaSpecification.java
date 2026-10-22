package br.com.senac.gerenciadordesalas.salas;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class SalaSpecification {

    public static Specification<Sala> comFiltros(SalaFiltroDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            // Somente salas ativas
            predicados.add(cb.isTrue(root.get("ativa")));

            if (filtro != null) {
                if (filtro.capacidadeMinima() != null) {
                    predicados.add(
                        cb.greaterThanOrEqualTo(
                            root.get("capacidade"),
                            filtro.capacidadeMinima()
                        )
                    );
                }

                if (StringUtils.hasText(filtro.localizacao())) {
                    String termoBusca = "%"
                        + filtro.localizacao().trim().toLowerCase()
                        + "%";

                    predicados.add(
                        cb.like(
                            cb.lower(root.get("localizacao")),
                            termoBusca
                        )
                    );
                }
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}