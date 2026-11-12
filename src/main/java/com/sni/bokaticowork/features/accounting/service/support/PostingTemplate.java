package com.sni.bokaticowork.features.accounting.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.model.AccPostingRule;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Evaluation d'un schema d'ecriture sur un evenement (lot F2).
 *
 * <h2>Variables</h2>
 * <ul>
 *   <li>{@code {chemin}} (commence par une minuscule) : valeur du champ dans l'element courant d'une
 *   ligne repetee, sinon dans l'evenement (chemin pointe : {@code customer.code}) ;</li>
 *   <li>{@code {NOM}} ou {@code {NOM:chemin}} (majuscules) : resolveur ({@link PostingResolver}).</li>
 * </ul>
 *
 * <h2>Montants</h2>
 * Un chemin, un nombre, ou une somme de termes separes par {@code +} et {@code -}
 * ({@code totalTtc - discount}). Une ligne a montant nul est omise. Avec {@code signed}, un montant
 * negatif passe de l'autre cote, en valeur absolue.
 *
 * <h2>Conditions</h2>
 * Objet {@code {"champ": "valeur"}} ou {@code {"champ": ["v1", "v2"]}} ; {@code "!valeur"} exclut ;
 * {@code "*"} exige une valeur non vide ; {@code ""} exige l'absence.
 */
@Component
public class PostingTemplate {

    private static final Pattern VARIABLE = Pattern.compile("\\{(\\?)?([A-Za-z_][A-Za-z0-9_.\\[\\]]*)(?::([A-Za-z0-9_.\\[\\]]+))?}");
    private static final Pattern INDEXED = Pattern.compile("(.*)\\[(\\d+)]");
    private static final TypeReference<List<LineSpec>> LINES = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final List<PostingResolver> resolverBeans;
    private volatile Map<String, PostingResolver> resolvers;

    public PostingTemplate(ObjectMapper objectMapper, @Lazy List<PostingResolver> resolverBeans) {
        this.objectMapper = objectMapper;
        this.resolverBeans = resolverBeans;
    }

    /** Ligne d'un schema. */
    public record LineSpec(
            String side,
            String account,
            String amount,
            String thirdParty,
            String label,
            String reference,
            String foreach,
            Map<String, Object> when,
            Boolean signed,
            String dueDate,
            String taxCode,
            String taxBase,
            String currency,
            String foreignAmount,
            /** Ventilation analytique (lot F9) : axe vers section (variables admises, vide ignore). */
            Map<String, String> analytic
    ) {
    }

    /** Resultat de l'evaluation. */
    public record Plan(String journalCode, String label, List<AccountingRequests.EntryLine> lines, BigDecimal totalDebit,
                       BigDecimal totalCredit, List<String> warnings) {

        public boolean balanced() {
            return totalDebit.compareTo(totalCredit) == 0;
        }
    }

    /** Echec d'evaluation : le motif est lisible par un comptable. */
    public static class PostingException extends RuntimeException {
        public PostingException(String message) {
            super(message);
        }
    }

    public JsonNode read(String json) {
        if (!StringUtils.hasText(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new PostingException("Donnees de l'evenement illisibles : " + exception.getMessage());
        }
    }

    public List<LineSpec> lines(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LINES);
        } catch (Exception exception) {
            throw new PostingException("Lignes du schema illisibles : " + exception.getMessage());
        }
    }

    public Map<String, Object> conditions(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception exception) {
            throw new PostingException("Conditions du schema illisibles : " + exception.getMessage());
        }
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new PostingException("Ecriture JSON impossible : " + exception.getMessage());
        }
    }

    /** Le schema s'applique-t-il a cet evenement ? */
    public boolean matches(AccPostingRule rule, JsonNode event) {
        return matches(conditions(rule.getConditions()), event, null);
    }

    /** Calcule la piece ; {@code lenient} remplace une variable introuvable par {@code ?nom} (apercu). */
    public Plan evaluate(AccPostingRule rule, JsonNode event, boolean lenient) {
        List<String> warnings = new ArrayList<>();
        String journal = substitute(rule.getJournal(), event, null, lenient, warnings);
        if (!StringUtils.hasText(journal)) {
            throw new PostingException("Le schema " + rule.getCode() + " n'indique pas de journal");
        }
        String label = StringUtils.hasText(rule.getEntryLabel())
                ? substitute(rule.getEntryLabel(), event, null, true, new ArrayList<>())
                : rule.getLabel();
        List<AccountingRequests.EntryLine> result = new ArrayList<>();
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        int index = 0;
        for (LineSpec spec : lines(rule.getLines())) {
            index++;
            List<JsonNode> items = new ArrayList<>();
            if (StringUtils.hasText(spec.foreach())) {
                JsonNode array = path(event, null, spec.foreach());
                if (array != null && array.isArray()) {
                    array.forEach(items::add);
                }
            } else {
                items.add(null);
            }
            for (JsonNode item : items) {
                if (spec.when() != null && !matches(spec.when(), event, item)) {
                    continue;
                }
                BigDecimal amount = amount(spec.amount(), event, item, index);
                if (amount.signum() == 0) {
                    continue;
                }
                boolean debitSide = side(spec.side(), index);
                if (amount.signum() < 0) {
                    if (!Boolean.TRUE.equals(spec.signed())) {
                        throw new PostingException("Ligne " + index + " du schema : montant negatif (" + amount.toPlainString()
                                + ") ; cochez « signe » pour le passer de l'autre cote");
                    }
                    amount = amount.negate();
                    debitSide = !debitSide;
                }
                String account = substitute(spec.account(), event, item, lenient, warnings);
                if (!StringUtils.hasText(account)) {
                    throw new PostingException("Ligne " + index + " du schema : compte vide");
                }
                String thirdParty = blankToNull(substitute(spec.thirdParty(), event, item, lenient, warnings));
                String lineLabel = blankToNull(substitute(spec.label(), event, item, true, new ArrayList<>()));
                String reference = blankToNull(substitute(spec.reference(), event, item, true, new ArrayList<>()));
                String dueText = blankToNull(substitute(spec.dueDate(), event, item, true, new ArrayList<>()));
                BigDecimal taxBase = StringUtils.hasText(spec.taxBase()) ? amount(spec.taxBase(), event, item, index) : null;
                BigDecimal foreign = StringUtils.hasText(spec.foreignAmount()) ? amount(spec.foreignAmount(), event, item, index).abs() : null;
                result.add(new AccountingRequests.EntryLine(account.trim(), thirdParty, lineLabel,
                        debitSide ? amount : null, debitSide ? null : amount,
                        blankToNull(substitute(spec.currency(), event, item, true, new ArrayList<>())), foreign, null,
                        blankToNull(substitute(spec.taxCode(), event, item, true, new ArrayList<>())), taxBase,
                        date(dueText), reference, analytic(spec.analytic(), event, item)));
                if (debitSide) {
                    debit = debit.add(amount);
                } else {
                    credit = credit.add(amount);
                }
            }
        }
        if (result.size() < 2 && !lenient) {
            throw new PostingException("Le schema " + rule.getCode() + " ne produit que " + result.size()
                    + " ligne(s) pour cet evenement : une piece a au moins un debit et un credit");
        }
        return new Plan(journal.trim().toUpperCase(Locale.ROOT), label, result, debit, credit, warnings);
    }

    private Map<String, String> analytic(Map<String, String> spec, JsonNode event, JsonNode item) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();
        spec.forEach((axis, template) -> {
            String value = blankToNull(substitute(template, event, item, true, new ArrayList<>()));
            if (value != null && !value.startsWith("?")) {
                result.put(axis.trim().toUpperCase(Locale.ROOT), value);
            }
        });
        return result.isEmpty() ? null : result;
    }

    /** Remplace les variables d'un gabarit. */
    public String substitute(String template, JsonNode event, JsonNode item, boolean lenient, List<String> warnings) {
        if (template == null) {
            return null;
        }
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            boolean optional = matcher.group(1) != null;
            String name = matcher.group(2);
            String argument = matcher.group(3);
            String value = variable(name, argument, event, item);
            if (value == null && optional) {
                // {?chemin} : variable facultative, vide si absente.
                value = "";
            }
            if (value == null) {
                String readable = argument == null ? name : name + ":" + argument;
                if (!lenient) {
                    throw new PostingException("Variable {" + readable + "} introuvable dans l'evenement ou non resolue");
                }
                warnings.add("Variable {" + readable + "} non resolue");
                value = "?" + readable;
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String variable(String name, String argument, JsonNode event, JsonNode item) {
        if (Character.isUpperCase(name.charAt(0))) {
            PostingResolver resolver = resolvers().get(name);
            if (resolver == null) {
                throw new PostingException("Resolveur inconnu : {" + name + "}. Resolveurs disponibles : "
                        + String.join(", ", resolvers().keySet()));
            }
            // L'argument est un chemin de l'evenement ; a defaut, sa valeur litterale ({SETTING:cle}).
            String pathValue = argument == null ? null : text(path(event, item, argument));
            String argumentValue = argument != null && pathValue == null ? argument : pathValue;
            return blankToNull(resolver.resolve(argumentValue, event));
        }
        return blankToNull(text(path(event, item, name)));
    }

    public Map<String, PostingResolver> resolvers() {
        Map<String, PostingResolver> current = resolvers;
        if (current == null) {
            current = resolverBeans.stream().collect(Collectors.toMap(resolver -> resolver.name().toUpperCase(Locale.ROOT),
                    Function.identity(), (first, second) -> first, java.util.TreeMap::new));
            resolvers = current;
        }
        return current;
    }

    private BigDecimal amount(String expression, JsonNode event, JsonNode item, int index) {
        if (!StringUtils.hasText(expression)) {
            throw new PostingException("Ligne " + index + " du schema : montant non indique");
        }
        String normalized = expression.replace("+", " + ").replace(" - ", " - ").trim();
        String[] tokens = normalized.split("\\s+");
        BigDecimal total = BigDecimal.ZERO;
        int sign = 1;
        for (String token : tokens) {
            if (token.equals("+")) {
                sign = 1;
                continue;
            }
            if (token.equals("-")) {
                sign = -1;
                continue;
            }
            BigDecimal value = term(token, event, item);
            total = total.add(sign > 0 ? value : value.negate());
            sign = 1;
        }
        return total.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal term(String token, JsonNode event, JsonNode item) {
        if (token.matches("-?\\d+(\\.\\d+)?")) {
            return new BigDecimal(token);
        }
        JsonNode node = path(event, item, token.replace("{", "").replace("}", ""));
        if (node == null || node.isNull() || node.isMissingNode()) {
            return BigDecimal.ZERO;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText().trim();
        if (text.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            throw new PostingException("Montant illisible pour " + token + " : " + text);
        }
    }

    private boolean matches(Map<String, Object> conditions, JsonNode event, JsonNode item) {
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String actual = text(path(event, item, condition.getKey()));
            Object expected = condition.getValue();
            List<String> values = expected instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : List.of(expected == null ? "" : String.valueOf(expected));
            boolean ok = false;
            boolean onlyExclusions = values.stream().allMatch(value -> value.startsWith("!"));
            for (String value : values) {
                if (value.startsWith("!")) {
                    if (value.substring(1).equalsIgnoreCase(actual == null ? "" : actual)) {
                        return false;
                    }
                } else if (value.equals("*")) {
                    ok |= StringUtils.hasText(actual);
                } else if (value.isEmpty()) {
                    ok |= !StringUtils.hasText(actual);
                } else {
                    ok |= value.equalsIgnoreCase(actual == null ? "" : actual);
                }
            }
            if (!ok && !onlyExclusions) {
                return false;
            }
        }
        return true;
    }

    /** Chemin pointe, cherche dans l'element courant puis dans l'evenement ; {@code event.} force l'evenement. */
    JsonNode path(JsonNode event, JsonNode item, String path) {
        if (!StringUtils.hasText(path)) {
            return NullNode.getInstance();
        }
        if (path.startsWith("event.")) {
            return walk(event, path.substring(6));
        }
        if (item != null) {
            JsonNode found = walk(item, path);
            if (found != null && !found.isMissingNode() && !found.isNull()) {
                return found;
            }
        }
        return walk(event, path);
    }

    private static JsonNode walk(JsonNode root, String path) {
        JsonNode node = root;
        for (String part : path.split("\\.")) {
            if (node == null) {
                return null;
            }
            Matcher index = INDEXED.matcher(part);
            if (index.matches()) {
                node = node.path(index.group(1)).path(Integer.parseInt(index.group(2)));
            } else {
                node = node.path(part);
            }
        }
        return node;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue().stripTrailingZeros().toPlainString();
        }
        if (node.isContainerNode()) {
            return node.toString();
        }
        return node.asText();
    }

    private static boolean side(String side, int index) {
        if (side == null) {
            throw new PostingException("Ligne " + index + " du schema : sens non indique (DEBIT ou CREDIT)");
        }
        return switch (side.trim().toUpperCase(Locale.ROOT)) {
            case "DEBIT", "D" -> true;
            case "CREDIT", "C" -> false;
            default -> throw new PostingException("Ligne " + index + " du schema : sens inconnu " + side);
        };
    }

    private static LocalDate date(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Champs de premier niveau de l'evenement, pour l'aide a la saisie d'un schema. */
    public static List<String> fields(JsonNode event) {
        List<String> names = new ArrayList<>();
        if (event != null && event.isObject()) {
            Iterator<String> iterator = event.fieldNames();
            iterator.forEachRemaining(names::add);
        }
        return names;
    }
}
