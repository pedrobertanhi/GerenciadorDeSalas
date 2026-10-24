package br.com.senac.gerenciadordesalas.salas;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
 
import jakarta.persistence.criteria.Predicate;
 
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
 
public class SalaSpecification {
 
    /** Caractere de escape informado ao LIKE para que % e _ sejam buscados literalmente. */
    private static final char ESCAPE = '\\';
 
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
                        + escaparCuringas(filtro.localizacao().trim().toLowerCase(Locale.ROOT))
                        + "%";
 
                    predicados.add(
                        cb.like(
                            cb.lower(root.get("localizacao")),
                            termoBusca,
                            ESCAPE
                        )
                    );
                }
            }
 
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
 
    /**
     * Neutraliza os curingas do LIKE no texto digitado pelo usuario.
     *
     * A barra invertida e escapada primeiro, de proposito: se os curingas fossem
     * tratados antes, as barras introduzidas por eles seriam escapadas de novo no
     * passo seguinte e o padrao sairia errado.
     */
    private static String escaparCuringas(String termo) {
        return termo
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}