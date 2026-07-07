package com.bonusflowpj.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ReferenceService {

    private final RestClient restClient = RestClient.create();
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public List<DdiReference> ddis() {
        return cached("ddis", () -> {
            try {
                RestCountry[] countries = restClient.get()
                    .uri("https://restcountries.com/v3.1/all?fields=name,idd,translations")
                    .retrieve()
                    .body(RestCountry[].class);
                if (countries == null) {
                    return fallbackDdis();
                }
                List<DdiReference> references = new ArrayList<>();
                for (RestCountry country : countries) {
                    if (country.idd == null || country.idd.root == null || country.idd.suffixes == null) {
                        continue;
                    }
                    String name = country.translations != null && country.translations.por != null && country.translations.por.common != null
                        ? country.translations.por.common
                        : country.name == null ? "" : country.name.common;
                    for (String suffix : country.idd.suffixes) {
                        references.add(new DdiReference(country.idd.root + suffix, name));
                    }
                }
                references.sort(Comparator.comparing(DdiReference::code).thenComparing(DdiReference::country));
                return references.isEmpty() ? fallbackDdis() : references;
            } catch (RuntimeException exception) {
                return fallbackDdis();
            }
        });
    }

    public List<DddReference> ddds() {
        return cached("ddds", this::fallbackDdds);
    }

    public DddDetail ddd(String ddd) {
        String normalized = digitsOnly(ddd);
        return cached("ddd-" + normalized, () -> {
            try {
                BrasilApiDdd response = restClient.get()
                    .uri("https://brasilapi.com.br/api/ddd/v1/{ddd}", normalized)
                    .retrieve()
                    .body(BrasilApiDdd.class);
                if (response != null && response.state != null) {
                    return new DddDetail(normalized, response.state, response.cities == null ? List.of() : response.cities);
                }
            } catch (RuntimeException ignored) {
                // fallback below
            }
            return fallbackDdds().stream()
                .filter(item -> item.ddd().equals(normalized))
                .findFirst()
                .map(item -> new DddDetail(item.ddd(), item.state(), List.of()))
                .orElse(new DddDetail(normalized, "", List.of()));
        });
    }

    public CepReference cep(String cep) {
        String normalized = digitsOnly(cep);
        return cached("cep-" + normalized, () -> {
            try {
                ViaCep response = restClient.get()
                    .uri("https://viacep.com.br/ws/{cep}/json/", normalized)
                    .retrieve()
                    .body(ViaCep.class);
                if (response == null || Boolean.TRUE.equals(response.erro)) {
                    throw new BusinessRuleException("CEP: Não encontrado.");
                }
                return new CepReference(
                    normalized,
                    response.logradouro == null ? "" : response.logradouro,
                    response.bairro == null ? "" : response.bairro,
                    response.localidade == null ? "" : response.localidade,
                    response.uf == null ? "" : response.uf
                );
            } catch (BusinessRuleException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new BusinessRuleException("CEP: Não foi possível consultar o CEP.");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> supplier) {
        return (T) cache.computeIfAbsent(key, ignored -> supplier.get());
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private List<DdiReference> fallbackDdis() {
        return List.of(
            new DdiReference("+1", "Estados Unidos/Canadá"),
            new DdiReference("+7", "Rússia/Cazaquistão"),
            new DdiReference("+20", "Egito"),
            new DdiReference("+27", "África do Sul"),
            new DdiReference("+30", "Grécia"),
            new DdiReference("+33", "França"),
            new DdiReference("+34", "Espanha"),
            new DdiReference("+39", "Itália"),
            new DdiReference("+44", "Reino Unido"),
            new DdiReference("+49", "Alemanha"),
            new DdiReference("+52", "México"),
            new DdiReference("+54", "Argentina"),
            new DdiReference("+55", "Brasil"),
            new DdiReference("+56", "Chile"),
            new DdiReference("+57", "Colômbia"),
            new DdiReference("+598", "Uruguai"),
            new DdiReference("+351", "Portugal")
        );
    }

    private List<DddReference> fallbackDdds() {
        return List.of(
            new DddReference("11", "SP"), new DddReference("12", "SP"), new DddReference("13", "SP"), new DddReference("14", "SP"),
            new DddReference("15", "SP"), new DddReference("16", "SP"), new DddReference("17", "SP"), new DddReference("18", "SP"),
            new DddReference("19", "SP"), new DddReference("21", "RJ"), new DddReference("22", "RJ"), new DddReference("24", "RJ"),
            new DddReference("27", "ES"), new DddReference("28", "ES"), new DddReference("31", "MG"), new DddReference("32", "MG"),
            new DddReference("33", "MG"), new DddReference("34", "MG"), new DddReference("35", "MG"), new DddReference("37", "MG"),
            new DddReference("38", "MG"), new DddReference("41", "PR"), new DddReference("42", "PR"), new DddReference("43", "PR"),
            new DddReference("44", "PR"), new DddReference("45", "PR"), new DddReference("46", "PR"), new DddReference("47", "SC"),
            new DddReference("48", "SC"), new DddReference("49", "SC"), new DddReference("51", "RS"), new DddReference("53", "RS"),
            new DddReference("54", "RS"), new DddReference("55", "RS"), new DddReference("61", "DF"), new DddReference("62", "GO"),
            new DddReference("63", "TO"), new DddReference("64", "GO"), new DddReference("65", "MT"), new DddReference("66", "MT"),
            new DddReference("67", "MS"), new DddReference("68", "AC"), new DddReference("69", "RO"), new DddReference("71", "BA"),
            new DddReference("73", "BA"), new DddReference("74", "BA"), new DddReference("75", "BA"), new DddReference("77", "BA"),
            new DddReference("79", "SE"), new DddReference("81", "PE"), new DddReference("82", "AL"), new DddReference("83", "PB"),
            new DddReference("84", "RN"), new DddReference("85", "CE"), new DddReference("86", "PI"), new DddReference("87", "PE"),
            new DddReference("88", "CE"), new DddReference("89", "PI"), new DddReference("91", "PA"), new DddReference("92", "AM"),
            new DddReference("93", "PA"), new DddReference("94", "PA"), new DddReference("95", "RR"), new DddReference("96", "AP"),
            new DddReference("97", "AM"), new DddReference("98", "MA"), new DddReference("99", "MA")
        );
    }

    private interface Supplier<T> {
        T get();
    }

    public record DdiReference(String code, String country) {
    }

    public record DddReference(String ddd, String state) {
    }

    public record DddDetail(String ddd, String state, List<String> cities) {
    }

    public record CepReference(String zipCode, String street, String neighborhood, String city, String state) {
    }

    private record RestCountry(RestCountryName name, RestCountryIdd idd, RestCountryTranslations translations) {
    }

    private record RestCountryName(String common) {
    }

    private record RestCountryIdd(String root, List<String> suffixes) {
    }

    private record RestCountryTranslations(RestCountryName por) {
    }

    private record BrasilApiDdd(String state, List<String> cities) {
    }

    private record ViaCep(String logradouro, String bairro, String localidade, String uf, Boolean erro) {
    }
}
